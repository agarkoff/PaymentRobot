//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot;

import org.jetbrains.annotations.NotNull;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;

import java.math.BigDecimal;
import java.util.Date;

public class Payment implements Comparable {
    private Date date;
    private BigDecimal sum;
    private int intSum;
    private String card;
    private String name;
    private String order;
    private AbstractFileRecord abstractFileRecord;

    public Payment() {
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public BigDecimal getSum() {
        return this.sum;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
        this.intSum = (int) (sum.doubleValue() * 100);
    }

    public int getIntSum() {
        return intSum;
    }

    public String getCard() {
        return this.card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrder() {
        return this.order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public AbstractFileRecord getAbstractFileRecord() {
        return abstractFileRecord;
    }

    public void setAbstractFileRecord(AbstractFileRecord abstractFileRecord) {
        this.abstractFileRecord = abstractFileRecord;
    }

    public String toString() {
        return "Payment{date=" + this.date + ", sum=" + this.sum + ", card='" + this.card + '\'' + ", name='" + this.name + '\'' + ", order='" + this.order + '\'' + '}';
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof Payment)) {
            throw new RuntimeException("Нельзя сравнить Payment с " + o.getClass().getCanonicalName());
        }
        Payment p = (Payment) o;
        return getAbstractFileRecord().toString().compareToIgnoreCase(p.getAbstractFileRecord().toString());
    }
}
