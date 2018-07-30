//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot;

import ru.misterparser.common.model.NamedElement;

public enum Format implements NamedElement {
    SMS("СМС"),
    BANK_ACCOUNT("банковская выписка");

    private String name;

    private Format(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return "Format{name='" + this.name + '\'' + '}';
    }
}
