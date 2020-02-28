//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.fileloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import ru.misterparser.common.Utils;
import ru.misterparser.common.excel.ExcelUtils;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.common.flow.ThreadFinishStatus;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;
import ru.misterparser.paymentrobot.domain.BankTransaction;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BankTransactionLoader {

    private static final Logger log = LogManager.getLogger(BankTransactionLoader.class);

    private EventProcessor eventProcessor;

    public BankTransactionLoader(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    public List<? extends AbstractFileRecord> getBankTransactionList(String filename, Sheet sheet) throws InterruptedException {
        List<BankTransaction> bankTransactionList = new ArrayList<>();
        int dateColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Дата операции", "Дата транзакции", "Дата док-та", "Дата опер.", "Дата проводки", "Дата");
        int creditColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Сумма по кредиту", "Кредит", "Сумма в валюте счета", "по кредиту", "Оборот Кт", "Зачисление");
        int nameColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Наименование", "Дебет", "Название корр.", "Контрагент");
        int purposeColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Назначение платежа", "Содержание", "Назначение");
        if (dateColumn != -1 && creditColumn != -1 && purposeColumn != -1) {
            int currentRow = 0;

            while (currentRow <= sheet.getLastRowNum()) {
                Row row = sheet.getRow(currentRow++);
                if (row != null) {
                    try {
                        Cell dateCell = row.getCell(dateColumn);
                        Cell creditCell = row.getCell(creditColumn);
                        Cell nameCell = nameColumn > 0 ? row.getCell(nameColumn) : null;
                        Cell purposeCell = row.getCell(purposeColumn);
                        String dateString = ExcelUtils.getTextFromCell(dateCell);
                        String credit = null;
                        if (creditCell != null) {
                            if (creditCell.getCellTypeEnum() == CellType.NUMERIC) {
                                credit = String.valueOf(creditCell.getNumericCellValue());
                            } else if (creditCell.getCellTypeEnum() == CellType.STRING) {
                                credit = String.valueOf(creditCell.getStringCellValue());
                                credit = Utils.squeezeText(credit);
                                credit = StringUtils.replace(credit, "RUR", "");
                                credit = StringUtils.replace(credit, " ", "");
                                credit = StringUtils.replace(credit, ",", ".");
                                //credit = credit.replace((char) 0xA0C2, ' ');c
                                credit = String.valueOf(((Double) Double.parseDouble(credit)).intValue());
                            }
                        }

                        if (credit != null) {
                            String name = ExcelUtils.getTextFromCell(nameCell);
                            String purpose = ExcelUtils.getTextFromCell(purposeCell);
                            if (StringUtils.isNotBlank(dateString) && StringUtils.isNotBlank(credit)) {
                                boolean colored = ExcelUtils.isColored(purposeCell);
                                if (colored) {
                                    this.eventProcessor.log("Пропускаем окрашенную строку #" + (row.getRowNum() + 1) + " '" + purpose + "' листа " + sheet.getSheetName() + " файла " + filename);
                                } else {//if (StringUtils.isNotBlank(purpose)) {
                                    Date date = null;
                                    try {
                                        date = dateCell.getDateCellValue();
                                    } catch (IllegalStateException | NumberFormatException e) {
                                        try {
                                            date = DateUtils.parseDate(dateCell.getStringCellValue(), new String[]{"dd.MM.yyyy"});
                                        } catch (IllegalArgumentException | ParseException e2) {
                                            log.debug("Не удалось прочитать дату из ячейки: " + dateCell);
                                        }
                                    }
                                    BankTransaction bankTransaction = new BankTransaction(filename, sheet.getSheetName(), row.getRowNum(), date, credit, name, purpose);
                                    bankTransactionList.add(bankTransaction);
                                    log.debug(bankTransaction);
                                    //} else {
                                    //    this.eventProcessor.log("Ошибка 'Не заполнены колонки' в строке " + (row.getRowNum() + 1) + " листа " + sheet.getSheetName() + " файла " + filename);
                                }
                            } else {
                                log.debug("Дата, сумма или назначение платежа пустые в строке: " + (row.getRowNum() + 1));
                            }
                        } else {
                            log.debug("Не найдена сумма оплаты в строке " + (row.getRowNum() + 1));
                        }
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        log.debug("Exception", e);
                    }
                }
            }

            log.debug("С листа " + sheet.getSheetName() + " файла " + filename + " загружено оплат: " + bankTransactionList.size());
            return bankTransactionList;
        } else {
            log.debug("Не найдены все необходимые колонки на листе " + sheet.getSheetName());
            return bankTransactionList;
        }
    }

    public static void main(String[] args) throws IOException, InvalidFormatException, InterruptedException {
        BankTransactionLoader bankTransactionLoader = new BankTransactionLoader(new ConsoleEventProcessor());
        String filename = "c:\\Users\\stasa\\Downloads\\15.07.2018-21.07.2018.xlsx";
        try (Workbook workbook = WorkbookFactory.create(new File(filename), null, true)) {
            for (Sheet sheet : workbook) {
                List<? extends AbstractFileRecord> list = bankTransactionLoader.getBankTransactionList(filename, sheet);
                list.forEach(System.out::println);
            }
        }
    }

    private static class ConsoleEventProcessor implements EventProcessor {
        @Override
        public void find(Object o) {
            System.out.println(o);
        }

        @Override
        public void log(Object object) {
            System.out.println(object);
        }

        @Override
        public void finish(ThreadFinishStatus threadFinishStatus, Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void finish(ThreadFinishStatus threadFinishStatus, Throwable throwable, String dopText) {
            throwable.printStackTrace();
            System.out.println(dopText);
        }
    }
}
