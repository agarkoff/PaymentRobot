package ru.misterparser.paymentrobot.domain;

import ru.misterparser.paymentrobot.Payment;

public abstract class AbstractFileRecord {
    private String filename;
    private String sheetName;
    private int row;

    public AbstractFileRecord(String filename, String sheetName, int row) {
        this.filename = filename;
        this.sheetName = sheetName;
        this.row = row;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSheetName() {
        return this.sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getRow() {
        return this.row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public abstract String getText();

    public abstract Payment toPayment();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractFileRecord that = (AbstractFileRecord) o;

        if (getRow() != that.getRow()) return false;
        if (!getFilename().equals(that.getFilename())) return false;
        return getSheetName().equals(that.getSheetName());
    }

    @Override
    public int hashCode() {
        int result = getFilename().hashCode();
        result = 31 * result + getSheetName().hashCode();
        result = 31 * result + getRow();
        return result;
    }

    public String toString() {
        return "AbstractFileRecord{filename='" + this.filename + '\'' + ", sheetName='" + this.sheetName + '\'' + ", row=" + this.row + '}';
    }
}
