package ru.misterparser.paymentrobot.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Debt {
    private String sborId;
    private String dbId;
    private String customerId;
    private String customerName;
    private BigDecimal paid;
    private BigDecimal totalDebt;
    private List<String> paymentNames;
    private List<String> paymentCards;
    private List<Date> paymentDates;
    private String orderId;
    private String baseUrl;

    public Debt(String sborId, String dbId, String customerId, String customerName, BigDecimal paid, BigDecimal totalDebt, List<String> paymentNames, List<String> paymentCards, List<Date> paymentDates, String orderId, String baseUrl) {
        this.sborId = sborId;
        this.dbId = dbId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.paid = paid;
        this.totalDebt = totalDebt;
        this.paymentNames = paymentNames;
        this.paymentCards = paymentCards;
        this.paymentDates = paymentDates;
        this.orderId = orderId;
        this.baseUrl = baseUrl;
    }

    public String getSborId() {
        return this.sborId;
    }

    public void setSborId(String sborId) {
        this.sborId = sborId;
    }

    public String getDbId() {
        return this.dbId;
    }

    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getPaid() {
        return this.paid;
    }

    public void setPaid(BigDecimal paid) {
        this.paid = paid;
    }

    public BigDecimal getTotalDebt() {
        return this.totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }

    public List<String> getPaymentNames() {
        return this.paymentNames;
    }

    public void setPaymentNames(List<String> paymentNames) {
        this.paymentNames = paymentNames;
    }

    public String getPaymentName() {
        return paymentNames.stream().collect(Collectors.joining("\n"));
    }

    public List<String> getPaymentCards() {
        return this.paymentCards;
    }

    public String getFirstPaymentCard() {
        return this.paymentCards.get(0);
    }

    public void setPaymentCards(List<String> paymentCards) {
        this.paymentCards = paymentCards;
    }

    public List<Date> getPaymentDates() {
        return this.paymentDates;
    }

    public void setPaymentDates(List<Date> paymentDates) {
        this.paymentDates = paymentDates;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDisplayCustomerName() {
        return getDisplayCustomerName(this.getCustomerName(), this.getCustomerId());
    }

    public static String getDisplayCustomerName(String customerName, String customerId) {
        return customerName + " [" + customerId + "]";
    }

    public String toString() {
        return "Ожидаемая оплата из отчета {sborId='" + this.sborId + '\'' + ", dbId='" + this.dbId + '\'' + ", customerId='" + this.customerId + '\'' + ", customerName='" + this.customerName + '\'' + ", paid=" + this.paid + ", totalDebt=" + this.totalDebt + ", paymentNames='" + this.paymentNames + '\'' + ", paymentCards='" + this.paymentCards + '\'' + ", paymentDates='" + this.paymentDates + '\'' + ", orderId='" + this.orderId + '\'' + '}';
    }
}
