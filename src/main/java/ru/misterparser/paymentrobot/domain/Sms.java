//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.domain;

import ru.misterparser.paymentrobot.Payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sms extends AbstractFileRecord {
    private Date date;
    private String message;
    public static final Pattern PATTERN_1 = Pattern.compile("Сбербанк Онлайн. (.*) перевел\\(а\\) Вам (\\d+.\\d{2}) RUB(\\.\\s*Сообщение: \"(.*)\")?");
    public static final Pattern PATTERN_2 = Pattern.compile("зачисление ([0-9.]+)р.*s karty \\d{4}\\*{4}(\\d{4})");
    public static final Pattern PATTERN_3 = Pattern.compile("зачисление ([0-9.]+)р.*Баланс");
    public static final Pattern PATTERN_4 = Pattern.compile("зачисление ([0-9.]+)р.*от отправителя (.*)");

    public Sms(String filename, String sheetName, int row, Date date, String message) {
        super(filename, sheetName, row);
        this.date = date;
        this.message = message;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getText() {
        return this.getMessage();
    }

    public String toString() {
        return "Sms{filename=" + this.getFilename() + ", sheetName=" + this.getSheetName() + ", row=" + this.getRow() + ", date=" + this.date + ", message='" + this.message + '\'' + '}';
    }

    public Payment toPayment() {
        Payment payment = new Payment();
        payment.setDate(getDate());
        Matcher matcher1 = PATTERN_1.matcher(this.getMessage());
        Matcher matcher2 = PATTERN_2.matcher(this.getMessage());
        Matcher matcher3 = PATTERN_3.matcher(this.getMessage());
        Matcher matcher4 = PATTERN_4.matcher(this.getMessage());
        if(matcher1.find()) {
            payment.setSum(new BigDecimal(matcher1.group(2)));
            payment.setName(matcher1.group(1));
            payment.setCard(matcher1.group(4));
        } else if(matcher2.find()) {
            payment.setSum(new BigDecimal(matcher2.group(1)));
            payment.setCard(matcher2.group(2));
        } else if(matcher3.find()) {
            payment.setSum(new BigDecimal(matcher3.group(1)));
        } else {
            if(!matcher4.find()) {
                throw new RuntimeException("Строка " + this.getMessage() + " не распознана как платеж");
            }

            payment.setSum(new BigDecimal(matcher4.group(1)));
            payment.setName(matcher4.group(2));
        }
        payment.setAbstractFileRecord(this);
        return payment;
    }
}
