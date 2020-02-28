//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.fileloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import ru.misterparser.common.Utils;
import ru.misterparser.common.excel.ExcelUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.paymentrobot.Configuration;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;
import ru.misterparser.paymentrobot.domain.Sms;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsLoader {
    private static final Logger log = LogManager.getLogger(SmsLoader.class);
    private EventProcessor eventProcessor;

    public SmsLoader(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    public List<? extends AbstractFileRecord> getSmsList(String filename, Sheet sheet) {
        List<Sms> smsList = new ArrayList<>();
        int dateColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Дата", "Date", "Time");
        int messageColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Message");
        if (messageColumn == -1) {
            this.eventProcessor.log("Не найдена колонка 'Message' на листе " + sheet.getSheetName());
            return smsList;
        } else {
            int currentRow = 0;

            while (currentRow <= sheet.getLastRowNum()) {
                Row row = sheet.getRow(currentRow++);
                if (row != null) {
                    Cell dateCell = row.getCell(dateColumn);
                    Cell messageCell = row.getCell(messageColumn);
                    String dateString = ExcelUtils.getTextFromCell(dateCell);
                    if (messageCell != null) {
                        boolean colored = ExcelUtils.isColored(messageCell);
                        String message = ExcelUtils.getTextFromCell(messageCell);
                        message = Utils.squeezeText(message);
                        if (StringUtils.equalsIgnoreCase(message, "Message")) {
                            continue;
                        }
                        if (colored) {
                            this.eventProcessor.log("Пропускаем окрашенную строку #" + (row.getRowNum() + 1) + " '" + message + "' листа " + sheet.getSheetName() + " файла " + filename);
                        } else if (StringUtils.isNotBlank(message)) {
                            Date date = null;
                            try {
                                date = dateCell.getDateCellValue();
                            } catch (IllegalStateException | NumberFormatException e) {
                                String t = dateCell.getStringCellValue();
                                try {
                                    date = DateUtils.parseDate(t, new String[] {
                                            "EE dd/MM/yyyy 'at' HH:mm:ss a",
                                            "dd.MM.yyyy HH:mm:ss",
                                            "yyyy-MM-dd EE HH:mm:ss"
                                    });
                                } catch (IllegalArgumentException | ParseException e1) {
                                    log.debug("Не удалось прочитать дату из строки: " + dateString);
                                }
                            }
                            Sms sms = new Sms(filename, sheet.getSheetName(), row.getRowNum(), date, message);
                            smsList.add(sms);
                            log.debug(sms);
                        } else {
                            this.eventProcessor.log("Ошибка 'Не заполнены колонки' в строке " + (row.getRowNum() + 1) + " листа " + sheet.getSheetName() + " файла " + filename);
                        }
                    }
                }
            }

            log.debug("С листа " + sheet.getSheetName() + " файла " + filename + " загружено SMS: " + smsList.size());
            return filterSms(smsList);
        }
    }

    private List<Sms> filterSms(List<Sms> smsList) {
        List<Sms> processSms = new ArrayList<>();
        for (Sms sms : smsList) {
            Pattern pattern1 = Pattern.compile("зачисление (\\d+)р Баланс:");
            Pattern pattern2 = Pattern.compile("списание (\\d+)р.*Баланс:");
            Pattern pattern3 = Pattern.compile("Вход в Сбербанк Онлайн");
            Pattern pattern4 = Pattern.compile("выдача наличных");
            Pattern pattern5 = Pattern.compile("оплата услуг (\\d+)р");
            Pattern pattern6 = Pattern.compile("оплата Мобильного банка");
            Pattern pattern7 = Pattern.compile("отправьте код");
            Pattern pattern8 = Pattern.compile("пароль: \\d+");
            Pattern pattern9 = Pattern.compile("покупка \\d+р");
            Matcher paymentMatcher0 = Sms.PATTERN_0.matcher(sms.getMessage());
            Matcher paymentMatcher1 = Sms.PATTERN_1.matcher(sms.getMessage());
            Matcher paymentMatcher2 = Sms.PATTERN_2.matcher(sms.getMessage());
            Matcher paymentMatcher3 = Sms.PATTERN_3.matcher(sms.getMessage());
            Matcher paymentMatcher4 = Sms.PATTERN_4.matcher(sms.getMessage());
            Matcher matcher1 = pattern1.matcher(sms.getMessage());
            Matcher matcher2 = pattern2.matcher(sms.getMessage());
            Matcher matcher3 = pattern3.matcher(sms.getMessage());
            Matcher matcher4 = pattern4.matcher(sms.getMessage());
            Matcher matcher5 = pattern5.matcher(sms.getMessage());
            Matcher matcher6 = pattern6.matcher(sms.getMessage());
            Matcher matcher7 = pattern7.matcher(sms.getMessage());
            Matcher matcher8 = pattern8.matcher(sms.getMessage());
            Matcher matcher9 = pattern9.matcher(sms.getMessage());
            if (paymentMatcher0.find()) {
                Double.parseDouble(paymentMatcher0.group(1));
            } else if (paymentMatcher1.find()) {
                Double.parseDouble(paymentMatcher1.group(2));
            } else if (paymentMatcher2.find()) {
                Double.parseDouble(paymentMatcher2.group(1));
            } else if (paymentMatcher3.find() && Configuration.get().ONLY_SUM) {
                Double.parseDouble(paymentMatcher3.group(1));
            } else if (paymentMatcher4.find()) {
                Double.parseDouble(paymentMatcher4.group(1));
            } else {
               continue;
            }
            boolean m1 = matcher1.find();
            boolean m2 = matcher2.find();
            boolean m3 = matcher3.find();
            boolean m4 = matcher4.find();
            boolean m5 = matcher5.find();
            boolean m6 = matcher6.find();
            boolean m7 = matcher7.find();
            boolean m8 = matcher8.find();
            boolean m9 = matcher9.find();
            if (m1 || m2 || m3 || m4 || m5 || m6 || m7 || m8 || m9) {
                eventProcessor.log("СМС " + sms.getMessage() + " не распознано как содержащее информацию об оплате");
            } else {
                log.debug("Платёжное СМС: " + sms);
                processSms.add(sms);
            }
        }
        return processSms;
    }

    public static void main(String[] args) throws ParseException {
        String t = "вс 19/11/2017 at 10:22:00 AM";
        Date date = DateUtils.parseDate(t, new String[]{"EE dd/MM/yyyy 'at' HH:mm:ss a"});
        System.out.println(date);
    }
}
