package ru.misterparser.paymentrobot;

import ru.misterparser.common.model.NamedElement;

public enum SumCount implements NamedElement {

    BY_1("1"),
    BY_2("2"),
    BY_3("3");

    private String name;

    private SumCount(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SumCount{" +
                "name='" + name + '\'' +
                '}';
    }
}
