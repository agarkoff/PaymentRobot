package ru.misterparser.paymentrobot.siteloader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.misterparser.common.HtmlCleanerUtils;
import ru.misterparser.common.MisterParserFileUtils;
import ru.misterparser.common.Utils;
import ru.misterparser.common.collection.ArrayListValuedLinkedHashMap;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.common.sp.auth.Credentials;
import ru.misterparser.common.sp.auth.Sp5Authenticator;
import ru.misterparser.paymentrobot.Configuration;
import ru.misterparser.paymentrobot.domain.Debt;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by MisterParser on 10.10.2017.
 */
public class SpDebtLoader extends DebtLoader {

    private static final Logger log = LogManager.getLogger(SpDebtLoader.class);

    private Pattern PAYMENT_CARD_PATTERN = Pattern.compile("[. *,]([0-9]{4})[. *,]");

    private DefaultHttpClient httpClient;
    private HtmlCleaner htmlCleaner;
    private EventProcessor eventProcessor;

    public SpDebtLoader(DefaultHttpClient httpClient, HtmlCleaner htmlCleaner, EventProcessor eventProcessor) {
        this.httpClient = httpClient;
        this.htmlCleaner = htmlCleaner;
        this.eventProcessor = eventProcessor;
    }

    public void authorize() throws InterruptedException, IOException, URISyntaxException {
        String cookies = MisterParserFileUtils.readFileToStringQuietly(new File(ConfigurationUtils.getCurrentDirectory() + "cookies.txt"), "UTF-8");
        URI uri;
        if (Configuration.get().ALL_REPORTS) {
            uri = new URI(Configuration.get().BASE_URL);
        } else {
            uri = new URI(Configuration.get().REPORT_URLS.get(0));
        }
        new Sp5Authenticator().authorize(new Credentials(Configuration.get().LOGIN, Configuration.get().PASSWORD, Configuration.get().AUTH_BY_COOKIES, cookies), this.httpClient, uri.getScheme() + "://" + uri.getHost(), "UTF-8");
    }

    public List<Debt> processSiteReport(String reportUrl) throws InterruptedException, IOException, URISyntaxException {
        List<Debt> debts = new ArrayList<>();
        TagNode rootNode, printRootNode;
        Document document;
        Map<String, String> customerNameMap = new LinkedHashMap<>();
        {
            log.debug("Обработка отчета с сайта: " + reportUrl);
            reportUrl = StringUtils.trim(reportUrl);
            String page = Utils.fetch(httpClient, reportUrl, "UTF-8");
            rootNode = htmlCleaner.clean(page);
            document = Jsoup.parse(page);
        }
        {
            log.debug("Пауза 5 секунд перед обработкой отчета для печати");
            Thread.sleep(5000L);
            String printReportUrl = StringUtils.replace(reportUrl, "/report/", "/print/");
            log.debug("Загрузка версии отчета для печати: " + printReportUrl);
            String printPage = Utils.fetch(httpClient, printReportUrl, "UTF-8");
            printRootNode = htmlCleaner.clean(printPage);
        }
        ArrayListValuedLinkedHashMap<String, Element> customerIdTrMap = new ArrayListValuedLinkedHashMap<>();
        {
            Elements trs = document.select("table#t > tbody > tr");
            log.debug("В отчете сайта найдено товаров: " + trs.size());
            for (Element tr : trs) {
                Element element = tr.select("div.user").first();
                if (element == null) {
                    this.eventProcessor.log("В блоке заказа не найден идентификатор покупателя");
                } else {
                    Element customerNameElement = element.parent().select("div").first();
                    if (customerNameElement == null) {
                        this.eventProcessor.log("В блоке заказа не найдено имя покупателя");
                    } else {
                        String customerId = element.text();
                        customerIdTrMap.put(customerId, tr);
                        customerNameMap.put(customerId, customerNameElement.text());
                    }
                }
            }
            log.debug("Товары сгруппированы по покупателям. Уникальных участников в отчете: " + customerIdTrMap.keySet().size());
        }
        for (String customerId : customerIdTrMap.keySet()) {
            String customerName = customerNameMap.get(customerId);
            log.debug("Обработка заказов пользователя: " + customerName);
            BigDecimal total = new BigDecimal(0.00);
            List<Element> trs = customerIdTrMap.get(customerId);
            {
                log.debug("У пользователя " + customerName + " количество позиций заказа: " + trs.size());
                for (Element element : trs) {
                    Element sumElement = element.select("td.sum").first();
                    Element notFoundElement = element.select("td[status=3]").first();
                    Element replaceElement = element.select("td[status=8]").first();
                    if (notFoundElement == null && replaceElement == null) {
                        if (sumElement == null) {
                            eventProcessor.log("Не найдена сумма в строке покупки пользователя " + customerName);
                            continue;
                        }
                        BigDecimal c = new BigDecimal(sumElement.text());
                        total = total.add(c);
                    } else {
                        log.debug("Пропускаем строку: " + Utils.squeezeText(element.text()));
                    }
                }
            }
            Element tr0 = trs.get(0);
            BigDecimal oplata = getBigDecimalField(tr0, "div[id^=opl_]", rootNode, customerName, "Сумма оплаты");
            BigDecimal dost = getBigDecimalField(tr0, "div[id^=dost_]", rootNode, customerName, "Сумма доставки");
            if (oplata == null || dost == null) {
                continue;
            }
            total = total.subtract(oplata);
            total = total.add(dost);
            BigDecimal priceMarkup = getPriceMarkup(printRootNode, Debt.getDisplayCustomerName(customerName, customerId));
            log.debug("Наценка/скидка: " + priceMarkup);
            total = total.add(priceMarkup);
            if (total.compareTo(new BigDecimal(0)) == 0) {
                log.debug("Пропускаем заказы пользователя " + customerName + ", потому что его долг равен 0");
                continue;
            }
            Element paymentTd = tr0.select("> td > table > tbody > tr:nth-child(2) > td:nth-child(2)").first();
            if (paymentTd == null) {
                eventProcessor.log("Не найден блок информации об оплате в строке покупки пользователя " + customerName);
                continue;
            }

            String paymentText = paymentTd.html();
            TagNode paymentRootNode = htmlCleaner.clean(paymentText);
            String t = HtmlCleanerUtils.getText(paymentRootNode);
            String paymentName = null;
            String paymentCard = null;
            Date paymentDate = null;
            String orderId = null;
            String[] split = StringUtils.split(t, "\n");
            log.debug("Информация об оплате: " + Arrays.toString(split));
            for (String s : split) {
                s = StringUtils.trim(s);
                String v = StringUtils.substringAfter(s, ":");
                v = StringUtils.trim(v);
                if (StringUtils.startsWithIgnoreCase(s, "ФИО") && StringUtils.isBlank(paymentName)) {
                    paymentName = v;
                } else if (StringUtils.startsWithIgnoreCase(s, "Карта") && StringUtils.isBlank(paymentCard)) {
                    Matcher matcher = PAYMENT_CARD_PATTERN.matcher(" " + v + " ");
                    if (matcher.find()) {
                        paymentCard = matcher.group(1);
                    }
                } else if (StringUtils.startsWithIgnoreCase(s, "ID заказа") && StringUtils.isBlank(orderId)) {
                    orderId = v;
                } else if (StringUtils.startsWithIgnoreCase(s, "Дата оплаты") && StringUtils.isBlank(paymentName)) {
                    v = StringUtils.substringBefore(v, " ");
                    v = StringUtils.trim(v);
                    try {
                        Pattern pattern = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}");
                        Matcher matcher = pattern.matcher(v);
                        if (matcher.find()) {
                            String dt = matcher.group();
                            paymentDate = DateUtils.parseDate(dt, new String[]{"yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm"});
                        }
                    } catch (ParseException e) {
                        log.debug("ParseException", e);
                    }
                }
            }
            if (paymentDate == null) {
                log.debug("Пропускаем заказ без оплаты");
                continue;
            }
            Element idElement = tr0.select("div[id^=opl_]").first();
            String dbId = idElement.id();
            dbId = StringUtils.substringAfter(dbId, "opl_");
            String sborId = StringUtils.strip(reportUrl, "/");
            sborId = StringUtils.substringAfterLast(sborId, "/");
            URI uri = new URI(reportUrl);
            Debt debt = new Debt(sborId, dbId, customerId, customerName, oplata, total, Arrays.asList(paymentName), Arrays.asList(paymentCard), Arrays.asList(paymentDate), orderId, uri.getScheme() + "://" + uri.getHost());
            log.debug("Сформирована " + debt);
            debts.add(debt);
        }
        return debts;
    }

    private BigDecimal getBigDecimalField(Element tr0, String fieldCssQuery, TagNode rootNode, String customerName, String fieldName) {
        Element fieldElement = tr0.select(fieldCssQuery).first();
        if (fieldElement == null) {
            eventProcessor.log("Не найдено поле '" + fieldName + "' в строке покупки пользователя " + customerName);
            return null;
        } else {
            TagNode tagNode = Utils.getFirstTagNode(rootNode, ".//div[@id='" + fieldElement.id() + "']");
            if (tagNode == null) {
                eventProcessor.log("Не найдено поле '" + fieldName + "' в строке покупки пользователя " + customerName);
                return null;
            } else {
                ContentNode doubleNode = HtmlCleanerUtils.getNearestSiblingContentNode(tagNode, 1);
                if (doubleNode == null) {
                    eventProcessor.log("Не найдено значение поля '" + fieldName + "' в строке покупки пользователя " + customerName);
                    return null;
                } else {
                    String s = doubleNode.getContent();

                    BigDecimal value;
                    try {
                        value = new BigDecimal(s);
                        log.debug("Значение поля " + fieldName + ": " + value);
                    } catch (Exception var13) {
                        eventProcessor.log("Ошибка распознавания поля '" + fieldName + "' в строке покупки пользователя " + customerName);
                        return null;
                    }

                    return value;
                }
            }
        }
    }

    private BigDecimal getPriceMarkup(TagNode printRootNode, String displayCustomerName) throws InterruptedException {
        Iterator i$ = HtmlCleanerUtils.evaluateXPath(printRootNode, ".//table[@id='sbor_user']").iterator();

        TagNode table;
        String currentDisplayCustomerName;
        do {
            if (!i$.hasNext()) {
                this.eventProcessor.log("Не найден блок информации для пользователя " + displayCustomerName + " в версии отчета для печати");
                return new BigDecimal(0);
            }

            table = (TagNode) i$.next();
            TagNode td0 = Utils.getFirstTagNode(table, "./tbody/tr/td", false);
            currentDisplayCustomerName = "";
            String t = HtmlCleanerUtils.getText(td0);
            String[] arr$ = StringUtils.split(t, "\n");
            int len = arr$.length;

            for (int i = 0; i < len; ++i) {
                String s = arr$[i];
                s = StringUtils.trim(s);
                String v;
                if (StringUtils.startsWithIgnoreCase(s, "Логин:")) {
                    v = StringUtils.substringAfter(s, ":");
                    v = Utils.squeezeText(v);
                    currentDisplayCustomerName = currentDisplayCustomerName + v;
                } else if (StringUtils.startsWithIgnoreCase(s, "ID:")) {
                    v = StringUtils.substringAfter(s, ":");
                    v = StringUtils.trim(v);
                    currentDisplayCustomerName = currentDisplayCustomerName + " [" + v + "]";
                }
            }
        } while (!StringUtils.equalsIgnoreCase(displayCustomerName, currentDisplayCustomerName));

        log.debug("Найден блок информации для пользователя " + displayCustomerName + " в версии отчета для печати");
        TagNode td3 = Utils.getFirstTagNode(table, "./tbody/tr/td[3]", false);
        String tt = HtmlCleanerUtils.getText(td3);
        if (StringUtils.containsIgnoreCase(tt, "Наценка/скидка:")) {
            String v = StringUtils.substringAfter(tt, ":");
            v = StringUtils.trim(v);

            try {
                return new BigDecimal(v);
            } catch (NumberFormatException var13) {
                this.eventProcessor.log("Ошибка получения числа из строки " + v);
                return new BigDecimal(0);
            }
        } else {
            this.eventProcessor.log("Не найдена информация о наценке/скидке в блоке информации пользователя " + displayCustomerName);
            return new BigDecimal(0);
        }
    }

    public boolean updateOpl(Debt debt, String value) throws InterruptedException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "opl_" + debt.getDbId());
        map.put("value", value);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Requested-With", "XMLHttpRequest");
        String page = Utils.post(this.httpClient, debt.getBaseUrl() + "/org/predopl/updateOpl", map, "UTF-8", headers);
        return StringUtils.equalsIgnoreCase(page, value);
    }

    public boolean updateDate(Debt debt, Date date) throws InterruptedException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "date_" + debt.getDbId());
        String value = DateFormatUtils.format(date, "MM/dd/yyyy");
        map.put("value", value);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Requested-With", "XMLHttpRequest");
        String page = Utils.post(this.httpClient, debt.getBaseUrl() + "/org/predopl/updateDate", map, "UTF-8", headers);
        return StringUtils.equalsIgnoreCase(page, value);
    }

    public static void main(String[] args) throws ParseException {
        Date date = DateUtils.parseDate("2018-07-04T18:37", new String[]{"yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd"});
        System.out.println(date);
    }
}
