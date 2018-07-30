//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.fit;

import ru.misterparser.paymentrobot.Payment;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;
import ru.misterparser.paymentrobot.domain.Debt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Fit {
    private List<Payment> payments = new ArrayList<>();
    private Debt debt;
    private boolean danger;
    private boolean allow;
    private boolean enabled;
    private String reason;

    public Fit(Payment payment, Debt debt, boolean danger, String reason) {
        this.payments.add(payment);
        this.debt = debt;
        this.danger = danger;
        this.allow = false;
        this.enabled = true;
        this.reason = reason;
    }

    public Fit(List<Payment> payments, Debt debt, boolean danger, String reason) {
        this.payments = payments;
        this.debt = debt;
        this.danger = danger;
        this.allow = false;
        this.enabled = true;
        this.reason = reason;
    }

    public Debt getDebt() {
        return this.debt;
    }

    public boolean isDanger() {
        return this.danger;
    }

    public boolean isAllow() {
        return this.allow;
    }

    void setAllow(boolean allow) {
        this.allow = allow;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReason() {
        return reason;
    }

    public List<AbstractFileRecord> getAbstractFileRecords() {
        return payments.stream().map(Payment::getAbstractFileRecord).collect(Collectors.toList());
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public Payment getFirstPayment() {
        return payments.get(0);
    }

    public String toString() {
        return "Fit{payments=" + this.payments + ", debt=" + this.debt + ", allow=" + this.allow + ", enabled=" + this.enabled + '}';
    }
}
