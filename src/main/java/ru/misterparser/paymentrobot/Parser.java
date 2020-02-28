//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import ru.misterparser.common.*;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.excel.ExcelUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.common.flow.ThreadFinishStatus;
import ru.misterparser.common.gui.tree.TreeUtils;
import ru.misterparser.common.model.Category;
import ru.misterparser.common.nextpage.NextPageHtmlCleanerUtils;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;
import ru.misterparser.paymentrobot.domain.Debt;
import ru.misterparser.paymentrobot.fileloader.BankTransactionLoader;
import ru.misterparser.paymentrobot.fileloader.SmsLoader;
import ru.misterparser.paymentrobot.fit.Fit;
import ru.misterparser.paymentrobot.siteloader.BratskDebtLoaderNew;
import ru.misterparser.paymentrobot.siteloader.BratskDebtLoaderOld;
import ru.misterparser.paymentrobot.siteloader.DebtLoader;
import ru.misterparser.paymentrobot.siteloader.SpDebtLoader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class Parser extends ControlledRunnable {

    private static final Logger log = LogManager.getLogger(Parser.class);

    private static final String ENCODING = "UTF-8";

    private DefaultHttpClient httpClient;
    private HtmlCleaner htmlCleaner;
    private ScriptEngine engine;
    private DataFormatter dataFormatter;
    private Gson gson;
    private TreeSelectionModel treeSelectionModel;
    private EventProcessor<Fit> eventProcessor;
    private Properties parserProperties;
    private List<String> finishText;
    private List<Debt> debts;
    private SmsLoader smsLoader;
    private BankTransactionLoader bankTransactionLoader;
    private boolean isAdminAllow = false;
    private DebtLoader debtLoader;
    private List<String> currentCategories = new ArrayList<>();

    public Parser(EventProcessor eventProcessor) {
        this.httpClient = PoolingHttpClient.getHttpClient(Configuration.NETWORK_TIMEOUT);
        this.htmlCleaner = new HtmlCleaner();
        this.dataFormatter = new DataFormatter();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.parserProperties = new Properties();
        this.finishText = new ArrayList<>();
        this.debts = new ArrayList<>();

        try {
            parserProperties.load(new FileInputStream(ConfigurationUtils.getCurrentDirectory() + "parser.properties"));
            isAdminAllow = Boolean.parseBoolean(parserProperties.getProperty("isAdminAllow"));
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("Exception", e);
        }

        if (MainFrame.isBratsk) {
            if (Configuration.get().BRATSK_REPORT_TYPE == BratskReportType.OLD) {
                debtLoader = new BratskDebtLoaderOld(httpClient, htmlCleaner, eventProcessor);
            } else {
                debtLoader = new BratskDebtLoaderNew(httpClient, htmlCleaner, eventProcessor);
            }
        } else {
            debtLoader = new SpDebtLoader(httpClient, htmlCleaner, eventProcessor);
        }
    }

    public Parser(TreeSelectionModel treeSelectionModel, EventProcessor<Fit> eventProcessor) {
        this(eventProcessor);
        this.treeSelectionModel = treeSelectionModel;
        this.eventProcessor = eventProcessor;
        this.smsLoader = new SmsLoader(eventProcessor);
        this.bankTransactionLoader = new BankTransactionLoader(eventProcessor);
    }

    public void run() {
        ThreadFinishStatus threadFinishStatus = ThreadFinishStatus.ERROR;
        Throwable throwable = null;

        try {
            log.debug("Конфигурация: " + Configuration.get().toString());
            Utils.setTryCount(1000);
            httpClient.getParams().setParameter("http.protocol.cookie-policy", "compatibility");
            initJavascriptEngine();
            init();
            debts = debtLoader.processSiteReports(getReportUrls());
            log.debug("Найдено ожидающих оплат всего: " + debts.size());
            for (TreePath treePath : treeSelectionModel.getSelectionPaths()) {
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                processTreeNode(lastPathComponent);
            }
            threadFinishStatus = ThreadFinishStatus.COMPLETED;
        } catch (InterruptedException var12) {
            log.debug("Остановка потока");
            threadFinishStatus = ThreadFinishStatus.INTERRUPTED;
        } catch (Throwable e) {
            log.debug("Throwable", e);
            throwable = e;
        } finally {
            log.debug("Обработка завершена");
            this.eventProcessor.finish(threadFinishStatus, throwable, "\n\n" + StringUtils.join(this.finishText, "\n"));
        }
    }

    private Collection<String> getReportUrls() throws InterruptedException {
        Set<String> reportUrls = new LinkedHashSet<>();
        if (MainFrame.isBratsk) {
            if (Configuration.get().ALL_REPORTS) {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("cmd", "Qget_purchase");
                map.put("filter", "5");
                String jsonString = Utils.post(httpClient, "http://spbratsk.ru/forum/phpBB3/addsp/purchase_manager.php", map, ENCODING);
                jsonString = StringUtils.replace(jsonString, "\uFeFF", "");
                jsonString = StringUtils.strip(jsonString, ",");
                jsonString = "[" + jsonString + "]";
                JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
                List<Map> json = gson.fromJson(jsonReader, List.class);
                for (Map m : json) {
                    String purchaseId = (String) m.get("purchase_id");
                    String purchaseUrl;
                    if (Configuration.get().BRATSK_REPORT_TYPE == BratskReportType.OLD) {
                        purchaseUrl = "http://spbratsk.ru/forum/sp/purchase_report.php?&purchase_id=" + purchaseId;
                    } else if (Configuration.get().BRATSK_REPORT_TYPE == BratskReportType.NEW) {
                        purchaseUrl = "http://spbratsk.ru/forum/phpBB3/addsp/purchase_report.php?purchase_id=" + purchaseId;
                    } else {
                        throw new RuntimeException("Ошибка выбора типа отчёта");
                    }
                    reportUrls.add(purchaseUrl);
                }
            } else {
                for (String reportUrl : Configuration.get().REPORT_URLS) {
                    String purchaseUrl;
                    String query = StringUtils.substringAfterLast(reportUrl, "?");
                    String purchaseId = ParserUtils.getFirstParameterFromQuery(query, "purchase_id", "UTF-8");
                    if (Configuration.get().BRATSK_REPORT_TYPE == BratskReportType.OLD) {
                        purchaseUrl = "http://spbratsk.ru/forum/sp/purchase_report.php?&purchase_id=" + purchaseId;
                    } else if (Configuration.get().BRATSK_REPORT_TYPE == BratskReportType.NEW) {
                        purchaseUrl = "http://spbratsk.ru/forum/phpBB3/addsp/purchase_report.php?purchase_id=" + purchaseId;
                    } else {
                        throw new RuntimeException("Ошибка выбора типа отчёта");
                    }
                    reportUrls.add(purchaseUrl);
                }
            }
        } else {
            if (Configuration.get().ALL_REPORTS) {
                if (Configuration.get().IS_ADMIN && isAdminAllow) {
                    if (StringUtils.isNotBlank(Configuration.get().ORG_FOR_ADMIN)) {
                        for (String org : StringUtils.split(Configuration.get().ORG_FOR_ADMIN, ", ")) {
                            List<NameValuePair> parameters = new ArrayList<>();
                            parameters.add(new BasicNameValuePair("Inform[org]", org));
                            String url = Configuration.get().BASE_URL + "/admin/inform/admin?" + URLEncodedUtils.format(parameters, ENCODING);
                            reportUrls.addAll(processAdminAllPageReports(url));
                        }
                    } else {
                        String url = Configuration.get().BASE_URL + "/admin/inform/admin";
                        reportUrls.addAll(processAdminAllPageReports(url));
                    }
                } else {
                    String url = Configuration.get().BASE_URL + "/org/predopl/";
                    log.debug("Получение списка всех отчетов с сайта: " + url);
                    String page = Utils.fetch(this.httpClient, url, "UTF-8");
                    TagNode rootNode = this.htmlCleaner.clean(page);
                    for (TagNode td : HtmlCleanerUtils.evaluateXPath(rootNode, ".//table[@id='tBrend']/tbody/tr/td[6]")) {
                        for (TagNode a : HtmlCleanerUtils.evaluateXPath(td, "./a")) {
                            String t = HtmlCleanerUtils.getText(a);
                            if (StringUtils.equalsIgnoreCase(t, "Перейти в отчет")) {
                                reportUrls.add(Utils.normalizeUrl(a.getAttributeByName("href"), Configuration.get().BASE_URL));
                            }
                        }
                    }
                }
            } else {
                reportUrls = new LinkedHashSet<>(Configuration.get().REPORT_URLS);
            }
        }
        log.debug("Ссылки на отчеты: " + StringUtils.join(reportUrls, "\n"));
        return reportUrls;
    }

    private Set<String> processAdminAllPageReports(String url) throws InterruptedException {
        Set<String> reportUrls = new LinkedHashSet<>();
        log.debug("Получение списка всех отчетов с сайта: " + url);
        String page = Utils.fetch(this.httpClient, url, "UTF-8");
        TagNode rootNode = this.htmlCleaner.clean(page);
        reportUrls.addAll(processAdminPageReports(rootNode));
        String nextPage = getNextPage(rootNode);
        while (nextPage != null) {
            log.debug("Обработка страницы: " + nextPage);
            page = Utils.fetch(httpClient, nextPage, ENCODING);
            rootNode = htmlCleaner.clean(page);
            reportUrls.addAll(processAdminPageReports(rootNode));
            nextPage = getNextPage(rootNode);
        }
        return reportUrls;
    }

    private Set<String> processAdminPageReports(TagNode rootNode) {
        Set<String> reportUrls = new LinkedHashSet<>();
        for (TagNode td : HtmlCleanerUtils.evaluateXPath(rootNode, ".//div[@id='inform-grid']/table/tbody/tr/td[last()]")) {
            for (TagNode a : HtmlCleanerUtils.evaluateXPath(td, "./a")) {
                String t = a.getAttributeByName("title");
                if (StringUtils.equalsIgnoreCase(t, "Перейти в отчет")) {
                    reportUrls.add(Utils.normalizeUrl(a.getAttributeByName("href"), Configuration.get().BASE_URL));
                }
            }
        }
        return reportUrls;
    }

    private String getNextPage(TagNode rootNode) {
        return NextPageHtmlCleanerUtils.getNextPage(rootNode, ".//ul[@id='yw1']/li[@class='next']/a", "Следующая >", Configuration.get().BASE_URL);
    }

    private void processTreeNode(DefaultMutableTreeNode treeNode) throws XPatherException, IOException, InterruptedException, InvalidFormatException {
        if (treeNode.getUserObject() instanceof Category) {
            if (treeNode.getChildCount() > 0) {
                for (int i = 0; i < treeNode.getChildCount(); ++i) {
                    DefaultMutableTreeNode childrenNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
                    processTreeNode(childrenNode);
                }
            } else {
                currentCategories = TreeUtils.getCategories(treeNode, true);
                Category category = (Category) treeNode.getUserObject();
                log.debug("Лист: " + category.getName());
                Object[] userObjectPaths = treeNode.getUserObjectPath();
                String filename = ((Category) userObjectPaths[1]).getName();
                String sheetName = ((Category) userObjectPaths[userObjectPaths.length - 1]).getName();
                processFile(filename, sheetName);
            }
        }
    }

    private void initJavascriptEngine() throws IOException, ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    private void processFile(String filename, String sheetName) throws IOException, InvalidFormatException, InterruptedException {
        log.debug("Обработка файла: " + filename);
        {
            File file = new File(filename);
            boolean fileIsLocked = !file.renameTo(file);
            if (fileIsLocked) {
                throw new RuntimeException("Закройте файл " + filename);
            }
        }
        try (Workbook workbook = WorkbookFactory.create(new File(filename))) {
            log.debug("Число стилей: " + workbook.getNumCellStyles());
            for (Sheet sheet : workbook) {
                if (StringUtils.equalsIgnoreCase(sheet.getSheetName(), sheetName)) {
                    log.debug("Обработка листа: " + sheet.getSheetName());
                    this.processSheet(filename, sheet);
                }
            }
        }
        ExcelUtils.clearColumnNamesCache();
    }

    private void processSheet(String filename, Sheet sheet) throws InterruptedException {
        List<? extends AbstractFileRecord> list;
        if (Configuration.get().FORMAT == Format.SMS) {
            list = smsLoader.getSmsList(filename, sheet);
        } else if (Configuration.get().FORMAT == Format.BANK_ACCOUNT) {
            list = bankTransactionLoader.getBankTransactionList(filename, sheet);
        } else {
            throw new RuntimeException("Ошибка выбора формата файла");
        }

        if (list.size() == 0) {
            log.debug("На листе " + sheet.getSheetName() + " не найдены строки для обработки");
        } else {
            int foundDebtCount;
            if (MainFrame.isBratsk) {
                BratskFinder bratskFinder = new BratskFinder(debts, eventProcessor);
                foundDebtCount = bratskFinder.getFoundDebtCount(filename, sheet, list);
            } else {
                SpFinder spFinder = new SpFinder(debts, eventProcessor);
                foundDebtCount = spFinder.getFoundDebtCount(filename, sheet, list);
            }
            String message = "Книга: " + FilenameUtils.getBaseName(filename) + ", лист " + sheet.getSheetName() + ": найдено соответствий: " + foundDebtCount + " для " + debts.size() + " записей отчёта";
            log.debug(message);
            finishText.add(message);
        }
    }

    public void init() throws InterruptedException, IOException, URISyntaxException {
        httpClient.getCookieStore().clear();
        debtLoader.authorize();
    }

    public boolean updateOpl(Debt debt, String value) throws InterruptedException {
        return debtLoader.updateOpl(debt, value);
    }

    public boolean updateDate(Debt debt, Date date) throws InterruptedException {
        return debtLoader.updateDate(debt, date);
    }

    private boolean isInit(TagNode rootNode) {
        return true;
    }
}
