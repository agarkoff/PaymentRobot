package ru.misterparser.paymentrobot;

import ru.misterparser.common.model.NamedElement;

public enum BratskReportType implements NamedElement {

    OLD("Отчет №1 (старый)"),
    NEW("Отчет");

    private String name;

    BratskReportType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BratskReportType{" +
                "name='" + name + '\'' +
                '}';
    }
}
