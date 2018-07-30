package ru.misterparser.paymentrobot;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 29.06.16
 * Time: 13:13
 */
public class Sms {

    private String filename;
    private String sheetName;
    private int row;
    private Date date;
    private String message;

    public Sms(String filename, String sheetName, int row, Date date, String message) {
        this.filename = filename;
        this.sheetName = sheetName;
        this.row = row;
        this.date = date;
        this.message = message;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "filename=" + filename +
                ", sheetName=" + sheetName +
                ", row=" + row +
                ", date=" + date +
                ", message='" + message + '\'' +
                '}';
    }
}
