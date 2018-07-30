package ru.misterparser.paymentrobot.siteloader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.misterparser.common.*;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.paymentrobot.Configuration;
import ru.misterparser.paymentrobot.MainFrame;
import ru.misterparser.paymentrobot.domain.Debt;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by MisterParser on 10.10.2017.
 */
public class BratskDebtLoaderOld extends DebtLoader {

    private static final Logger log = LogManager.getLogger(BratskDebtLoaderOld.class);

    private static Set<String> names;
    private static Set<String> patronymics;

    static {
        try {
            names = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(BratskDebtLoaderOld.class.getResourceAsStream("/names.csv"), "UTF-8"), "\n\r\t, ")));
            patronymics = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(BratskDebtLoaderOld.class.getResourceAsStream("/patronymics.csv"), "UTF-8"), "\n\r\t, ")));
        } catch (IOException e) {
            log.debug("IOException", e);
        }
    }

    private DefaultHttpClient httpClient;
    private HtmlCleaner htmlCleaner;
    private EventProcessor eventProcessor;

    private WebClient webClient;

    public BratskDebtLoaderOld(DefaultHttpClient httpClient, HtmlCleaner htmlCleaner, EventProcessor eventProcessor) {
        this.httpClient = httpClient;
        this.htmlCleaner = htmlCleaner;
        this.eventProcessor = eventProcessor;
    }

    private void init() {
        if (webClient == null) {
            webClient = new WebClient(BrowserVersion.FIREFOX_52);
            try {
                webClient.getPage("http://spbratsk.ru/");
            } catch (IOException e) {
                log.debug("IOException", e);
            }
        }
    }

    public void authorize() throws InterruptedException, IOException, URISyntaxException {
        init();
        if (Configuration.get().AUTH_BY_COOKIES) {
            String cookies = MisterParserFileUtils.readFileToStringQuietly(new File(ConfigurationUtils.getCurrentDirectory() + "cookies.txt"), "UTF-8");
            AuthUtils.setCookies(httpClient, cookies);
        } else {
            Map<String, String> credentials = new LinkedHashMap<>();
            credentials.put("username", Configuration.get().LOGIN);
            credentials.put("password", Configuration.get().PASSWORD);
            AuthUtils.authorize(httpClient, "http://spbratsk.ru/forum/phpBB3/ucp.php?mode=login", ".//form[@id='login']", credentials, "UTF-8");
        }
        for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
            if (StringUtils.startsWithIgnoreCase(cookie.getName(), "phpbb3")) {
                webClient.addCookie(Utils.cookieToString(cookie), new URL("http://spbratsk.ru/"), null);
            }
        }
    }

    public List<Debt> processSiteReport(String reportUrl) throws InterruptedException, IOException, URISyntaxException {
        init();
        List<Debt> debts = new ArrayList<>();
        Document document;
        HtmlPage htmlPage;
        String page;
        {
            log.debug("Обработка отчета с сайта: " + reportUrl);
            reportUrl = StringUtils.trim(reportUrl);
            if (MainFrame.TEST) {
                page = FileUtils.readFileToString(new File("1.html"), "UTF-8");
            } else {
                htmlPage = webClient.getPage(reportUrl);
                page = htmlPage.asXml();
            }
            document = Jsoup.parse(page);
        }
        List<Element> trs = document.select("div#report > table > tbody > tr");
        {
            String sborId = ParserUtils.getFirstParameterFromQuery(StringUtils.substringAfter(reportUrl, "?"), "purchase_id", "UTF-8");
            String customerId = null, customerName = null;
            List<String> paymentNames = new ArrayList<>();
            List<String> paymentCards = new ArrayList<>();
            List<Date> dates = new ArrayList<>();
            Debt debt = null;
            for (Element tr : trs) {
                if (StringUtils.startsWithIgnoreCase(tr.attr("id"), "order")) {
                    if (sborId != null && debt != null && debt.getCustomerId() != null && debt.getCustomerName() != null && debt.getPaid() != null && debt.getTotalDebt() != null) {
                        log.debug("Сформирована " + debt);
                        debts.add(debt);
                        paymentNames = new ArrayList<>();
                        paymentCards = new ArrayList<>();
                        dates = new ArrayList<>();
                        debt = null;
                    }
                    {
                        Element a = tr.select("> td > a").first();
                        String url = a.attr("href");
                        customerId = ParserUtils.getFirstParameterFromQuery(StringUtils.substringAfter(url, "?"), "user_id", "UTF-8");
                        customerName = a.text();
                    }
                    log.debug("Обработка заказа пользователя: " + customerId + ":" + customerName);
                } else if (StringUtils.endsWithIgnoreCase(tr.attr("id"), "_total")) {
                    Element oplataElement = tr.select("input[id$=_money]").first();
                    BigDecimal oplata = new BigDecimal(oplataElement.val());
                    Element totalSumElement = tr.select("div[id$=_total_sum]").first();
                    BigDecimal totalSum = new BigDecimal(totalSumElement.text());
                    BigDecimal totalDebt = totalSum.subtract(oplata);
                    debt = new Debt(sborId, null, customerId, customerName, oplata, totalDebt, paymentNames, paymentCards, dates, null, "http://spbratsk.ru/");
                } else {
                    {
                        Element commentElement = tr.select("td[id=payment" + customerId + "]").first();
                        if (commentElement != null) {
                            String comment = commentElement.text();
                            paymentNames.addAll(getFios(comment));
                        }
                    }
                    {
                        Element paymentDateElement = tr.select("td[id^=payment_date]").first();
                        if (paymentDateElement != null) {
                            String t = JSoupUtils.getText(paymentDateElement);
                            t = StringUtils.substringAfter(t, "Дата:");
                            t = Utils.squeezeText(t);
                            if (StringUtils.isNotBlank(t)) {
                                try {
                                    Date date = DateUtils.parseDate(t, new String[]{"yyyy-MM-dd"});
                                    dates.add(date);
                                } catch (ParseException e) {
                                    log.debug("ParseException", e);
                                }
                            }
                        }
                    }
                    {
                        Element deliveryDateElement = tr.select("td[id^=delivery_date]").first();
                        if (deliveryDateElement != null) {
                            String t = JSoupUtils.getText(deliveryDateElement);
                            t = StringUtils.substringAfter(t, "Дата:");
                            t = Utils.squeezeText(t);
                            if (StringUtils.isNotBlank(t)) {
                                try {
                                    Date date = DateUtils.parseDate(t, new String[]{"yyyy-MM-dd"});
                                    dates.add(date);
                                } catch (ParseException e) {
                                    log.debug("ParseException", e);
                                }
                            }
                        }
                    }
                    {
                        Element paymentCardElement = tr.select("td[id^=payment_card]").first();
                        if (paymentCardElement != null) {
                            String t = JSoupUtils.getText(paymentCardElement);
                            t = StringUtils.substringAfter(t, "Карта:");
                            t = Utils.squeezeText(t);
                            if (StringUtils.isNotBlank(t)) {
                                paymentCards.add(t);
                            }
                        }
                    }
                    {
                        Element deliveryCardElement = tr.select("td[id^=delivery_card]").first();
                        if (deliveryCardElement != null) {
                            String t = JSoupUtils.getText(deliveryCardElement);
                            t = StringUtils.substringAfter(t, "Карта:");
                            t = Utils.squeezeText(t);
                            if (StringUtils.isNotBlank(t)) {
                                paymentCards.add(t);
                            }
                        }
                    }
                }
            }
            if (sborId != null && debt != null && debt.getCustomerId() != null && debt.getCustomerName() != null && debt.getPaid() != null && debt.getTotalDebt() != null) {
                log.debug("Сформирована " + debt);
                debts.add(debt);
            }
        }
        return debts;
    }

    private static List<String> getFios(String string) throws IOException {
        List<String> paymentNames = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String s : StringUtils.split(string, "\n\r\t ")) {
            switch (current.size()) {
                case 0:
                    if (names.contains(StringUtils.lowerCase(s))) {
                        current.add(s);
                    }
                    break;
                case 1:
                    if (patronymics.contains(StringUtils.lowerCase(s))) {
                        current.add(s);
                    } else {
                        current = new ArrayList<>();
                    }
                    break;
                case 2:
                    Pattern pattern = Pattern.compile("[А-Яа-я](\\.)?");
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.matches()) {
                        current.add(s);
                    }
                    paymentNames.add(StringUtils.join(current, " "));
                    current = new ArrayList<>();
                    break;
                default:
                    throw new RuntimeException("Ошибка потока управления, размер текущего списка: " + current.size());
            }
        }
        return paymentNames;
    }

    public boolean updateOpl(Debt debt, String value) throws InterruptedException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("cmd", "Qsavemoney");
        map.put("money", value);
        map.put("purchase_id", debt.getSborId());
        map.put("user_id", debt.getCustomerId());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Requested-With", "XMLHttpRequest");
        String page = Utils.post(this.httpClient, Utils.normalizeUrl("/forum/phpBB3/addsp/purchase_report.php", debt.getBaseUrl()), map, "UTF-8", headers);
        page = StringUtils.replace(page, "\uFeFF", "");
        return StringUtils.equalsIgnoreCase(page, "ok");
    }

    public boolean updateDate(Debt debt, Date date) throws InterruptedException {
        log.debug("Установка даты оплаты нереализована");
        return true;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getFios("АНЖЕЛИКА МИХАЙЛОВНА Т. предоплата 245,00 АЛЕКСАНДР АЛЕКСАНДРОВИЧ Л. доплата 333,5 ИРИНА ВИКТОРОВНА А. Доставка 33,00"));
    }
}
