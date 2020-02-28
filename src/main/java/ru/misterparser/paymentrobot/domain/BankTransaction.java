//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.domain;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.Utils;
import ru.misterparser.paymentrobot.Payment;
import ru.misterparser.paymentrobot.siteloader.BratskDebtLoaderOld;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankTransaction extends AbstractFileRecord {

    private static final Logger log = LogManager.getLogger(BankTransaction.class);

    private static final Pattern ORDER_PATTERN = Pattern.compile("[0-9]{6,7}");
    private static final Pattern ORDER_PATTERN_OTHER = Pattern.compile("[0-9]+");

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

    private Date date;
    private String credit;
    private String name;
    private String purpose;

    public BankTransaction(String filename, String sheetName, int row, Date date, String credit, String name, String purpose) {
        super(filename, sheetName, row);
        this.date = date;
        this.credit = credit;
        this.name = parseName(name);
        this.purpose = purpose != null ? purpose : "";
    }

    private String parseName(String name) {
        for (String s : StringUtils.split(name, "//")) {
            s = StringUtils.replace(s, "ИП", "");
            s = StringUtils.replaceChars(s, "()", "");
            //System.out.println(s);
            String[] ss = StringUtils.split(s, " ");
            if (ss.length == 3) {
                int c = 0;
                for (String sss : ss) {
                    if (names.contains(StringUtils.lowerCase(sss))) {
                        c++;
                    }
                    if (patronymics.contains(StringUtils.lowerCase(sss))) {
                        c++;
                    }
                }
                if (c > 1) {
                    //System.out.println(s);
                    s = Utils.squeezeText(s);
                    return s;
                }
            }
        }
        log.debug("Не удалось прочитать ФИО из строки " + name);
        return "";
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCredit() {
        return this.credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return this.purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getText() {
        return StringUtils.trim(getPurpose() + " " + getName());
    }

    public String toString() {
        return "BankTransaction{filename='" + this.getFilename() + '\'' + ", sheetName='" + this.getSheetName() + '\'' + ", row=" + this.getRow() + ", date=" + this.date + ", credit='" + this.credit + '\'' + ", name='" + this.name + '\'' + ", purpose='" + this.purpose + '\'' + '}';
    }

    public Payment toPayment() {
        Payment payment = new Payment();
        payment.setDate(getDate());
        payment.setSum(new BigDecimal(getCredit()));
        payment.setName(getName());
        Matcher matcher = ORDER_PATTERN.matcher(getPurpose());
        if(matcher.find()) {
            payment.setOrder(matcher.group(0));
        } else {
            String[] ts = StringUtils.split(getPurpose(), ";");
            if (ts.length > 0) {
                String last = ts[ts.length - 1];
                Matcher matcherOther = ORDER_PATTERN_OTHER.matcher(last);
                if(matcherOther.find()) {
                    payment.setOrder(matcherOther.group(0));
                }
            }
        }
        payment.setAbstractFileRecord(this);

        return payment;
    }

    public static void main(String[] args) {
        BankTransaction bankTransaction = new BankTransaction("1", "1", 1, new Date(), "100", "", "ЗА 08/02/2018;Малинина Лариса Евгеньевна;Москва;Оплата за заказ 717,НДС не облагается");
        Payment payment = bankTransaction.toPayment();
        System.out.println(payment.getOrder());
    }
}

