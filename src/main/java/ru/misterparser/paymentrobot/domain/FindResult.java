package ru.misterparser.paymentrobot.domain;

import ru.misterparser.paymentrobot.Payment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MisterParser on 05.10.2017.
 */
public class FindResult {

    private Debt debt;
    private boolean danger;
    private String reason;
    private List<Payment> payments = new ArrayList<>();

    public FindResult(Debt debt, boolean danger, String reason) {
        this.debt = debt;
        this.danger = danger;
        this.reason = reason;
    }

    public FindResult(Debt debt, boolean danger, String reason, Payment payment) {
        this(debt, danger, reason);
        this.payments.add(payment);
    }

    public FindResult(Debt debt, boolean danger, String reason, List<Payment> payments) {
        this(debt, danger, reason);
        this.payments = payments;
    }

    public Debt getDebt() {
        return debt;
    }

    public void setDebt(Debt debt) {
        this.debt = debt;
    }

    public boolean isDanger() {
        return danger;
    }

    public void setDanger(boolean danger) {
        this.danger = danger;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    @Override
    public String toString() {
        return "FindResult{" +
                "debt=" + debt +
                ", danger=" + danger +
                ", reason='" + reason + '\'' +
                '}';
    }
}
