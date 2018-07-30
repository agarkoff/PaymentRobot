package ru.misterparser.paymentrobot.siteloader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.jsoup.nodes.Document;
import ru.misterparser.common.AuthUtils;
import ru.misterparser.common.MisterParserFileUtils;
import ru.misterparser.common.ParserUtils;
import ru.misterparser.common.Utils;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.paymentrobot.Configuration;
import ru.misterparser.paymentrobot.MainFrame;
import ru.misterparser.paymentrobot.domain.Debt;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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
public class BratskDebtLoaderNew extends DebtLoader {

    private static final Logger log = LogManager.getLogger(BratskDebtLoaderNew.class);

    private static Set<String> names;
    private static Set<String> patronymics;

    static {
        try {
            names = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(BratskDebtLoaderNew.class.getResourceAsStream("/names.csv"), "UTF-8"), "\n\r\t, ")));
            patronymics = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(BratskDebtLoaderNew.class.getResourceAsStream("/patronymics.csv"), "UTF-8"), "\n\r\t, ")));
        } catch (IOException e) {
            log.debug("IOException", e);
        }
    }

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private DefaultHttpClient httpClient;
    private HtmlCleaner htmlCleaner;
    private EventProcessor eventProcessor;

    private WebClient webClient;

    public BratskDebtLoaderNew(DefaultHttpClient httpClient, HtmlCleaner htmlCleaner, EventProcessor eventProcessor) {
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
        String sborId = ParserUtils.getFirstParameterFromQuery(StringUtils.substringAfter(reportUrl, "?"), "purchase_id", "UTF-8");
        List<Map> json = null;
        {
            log.debug("Обработка отчета с сайта: " + reportUrl);
            reportUrl = StringUtils.trim(reportUrl);
            if (MainFrame.TEST) {
                page = FileUtils.readFileToString(new File("1.html"), "UTF-8");
            } else {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("cmd", "Qgetorders");
                map.put("purchase_id", sborId);
                map.put("pos", "0");
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("X-Requested-With", "XMLHttpRequest");
                String jsonString = Utils.post(httpClient, "http://spbratsk.ru/forum/phpBB3/addsp/purchase_report.php", map, "UTF-8", headers);
                jsonString = StringUtils.replace(jsonString, "\uFeFF", "");
                jsonString = StringUtils.strip(jsonString, ",");
                jsonString = "[" + jsonString + "]";
                JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
                json = gson.fromJson(jsonReader, List.class);
            }
        }
        Map<String, List<Map>> byUserIdMap = ParserUtils.groupBy(json, map -> (String) map.get("user_id"));
        for (String userId : byUserIdMap.keySet()) {
            log.debug("Обработка пользователя: " + userId);
            List<Map> orders = byUserIdMap.get(userId);
            log.debug("Число заказов пользователя: " + orders.size());
            Map<String, Object> order0 = orders.get(0);

            String customerName = null;
            List<String> paymentNames = new ArrayList<>();
            List<String> paymentCards = new ArrayList<>();
            List<Date> dates = new ArrayList<>();
            BigDecimal oplata = null, totalDebt = null;

            customerName = (String) order0.get("username");
            oplata = order0.get("money_value") != null ? new BigDecimal((String) order0.get("money_value")) : new BigDecimal(0);
            {
                BigDecimal pM = order0.get("payment_money") != null ? new BigDecimal((String) order0.get("payment_money")) : new BigDecimal(0);
                BigDecimal dM = order0.get("delivery_money") != null ? new BigDecimal((String) order0.get("delivery_money")) : new BigDecimal(0);
                BigDecimal totalSum = pM.add(dM);
                totalDebt = totalSum.subtract(oplata);
            }
            {
                if (order0.get("payment_text") != null) {
                    String comment = (String) order0.get("payment_text");
                    paymentNames.addAll(getFios(comment));
                }
            }
            {
                {
                    if (order0.get("payment_card") != null) {
                        String s = (String) order0.get("payment_card");
                        paymentCards.add(s);
                    }
                }
                {
                    if (order0.get("delivery_card") != null) {
                        String s = (String) order0.get("delivery_card");
                        paymentCards.add(s);
                    }
                }
            }
            {
                {
                    if (order0.get("payment_date") != null) {
                        String s = (String) order0.get("payment_date");
                        addDate(dates, s);
                    }
                }
                {
                    if (order0.get("delivery_date") != null) {
                        String s = (String) order0.get("delivery_date");
                        addDate(dates, s);
                    }
                }
            }
            Debt debt = new Debt(sborId, null, userId, customerName, oplata, totalDebt, paymentNames, paymentCards, dates, null, "http://spbratsk.ru/");
            log.debug("Сформирована " + debt);
            debts.add(debt);
        }
        return debts;
    }

    private void addDate(List<Date> dates, String s) {
        if (StringUtils.isNotBlank(s)) {
            try {
                Date date = DateUtils.parseDate(s, new String[]{"yyyy-MM-dd"});
                dates.add(date);
            } catch (ParseException e) {
                log.debug("ParseException", e);
            }
        }
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
