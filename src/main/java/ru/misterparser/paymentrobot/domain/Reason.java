package ru.misterparser.paymentrobot.domain;

/**
 * Created by MisterParser on 05.10.2017.
 */
public enum Reason {

    ORDER_AND_SUM("номер заказа и сумма"),
    NAME_AND_SUM("ФИО и сумма"),
    NAME_AND_ORDER_AND_SUM("ФИО, номер заказа и сумма"),
    NAME_AND_DATE_AND_SUM("ФИО, дата и сумма"),
    CARD_AND_SUM("номер карты и сумма"),
    ONLY_SUM("только сумма"),
    TEST("ТЕСТОВАЯ ЗАПИСЬ");

    private String description;

    Reason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Reason{" +
                "description='" + description + '\'' +
                '}';
    }
}
