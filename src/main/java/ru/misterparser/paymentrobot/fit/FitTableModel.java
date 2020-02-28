//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot.fit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.collection.ArrayListValuedLinkedHashMap;
import ru.misterparser.paymentrobot.MainFrame;
import ru.misterparser.paymentrobot.Payment;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FitTableModel extends DefaultTableModel {
    private static final Logger log = LogManager.getLogger(FitTableModel.class);
    private static final String[] COLUMN_NAMES_SP = new String[]{"Дата", "Строка в файле", "Сумма из файла", "ФИО/карта из файла", "Пользователь СП", "Долг из СП", "ФИО из отчета", "ID заказа из отчета", "ID заказа из назначения платежа", "Карта из отчета", "Сбор", "Причина", "Отметить оплату"};
    private static final String[] COLUMN_NAMES_BRATSK = new String[]{"Дата", "Строка в файле", "Сумма из файла", "ФИО/карта из файла", "Пользователь СП", "Долг из СП", "ФИО из отчета", "Карта из отчета", "Сбор", "Причина", "Отметить оплату"};
    private List<Fit> fits = Collections.synchronizedList(new ArrayList<Fit>());
    public static final int CHECKBOX_COLUMN_SP = 12;
    public static final int CHECKBOX_COLUMN_BRATSK = 10;
    private FitTable table;

    public FitTableModel() {
        super(getColumnNames(), 0);
        addTableModelListener(e -> updateRowHeights());
    }

    private static String[] getColumnNames() {
        return MainFrame.isBratsk ? COLUMN_NAMES_BRATSK : COLUMN_NAMES_SP;
    }

    public Class<?> getColumnClass(int columnIndex) {
        Class clazz;
        if (MainFrame.isBratsk) {
            switch (columnIndex) {
                case 1:
                case 3:
                case 7:
                    clazz = List.class;
                    break;
                case CHECKBOX_COLUMN_BRATSK:
                    clazz = Boolean.class;
                    break;
                default:
                    clazz = String.class;
            }
        } else {
            switch (columnIndex) {
                case 1:
                case 3:
                case 9:
                    clazz = List.class;
                    break;
                case CHECKBOX_COLUMN_SP:
                    clazz = Boolean.class;
                    break;
                default:
                    clazz = String.class;
            }
        }
        return clazz;
    }

    public boolean isCellEditable(int row, int column) {
        Fit fit = this.fits.get(row);
        int checkBoxIndex = MainFrame.isBratsk ? CHECKBOX_COLUMN_BRATSK : CHECKBOX_COLUMN_SP;
        return column == checkBoxIndex && fit.isEnabled();
    }

    public void setValueAt(Object aValue, int row, int column) {
        int checkBoxIndex = MainFrame.isBratsk ? CHECKBOX_COLUMN_BRATSK : CHECKBOX_COLUMN_SP;
        if (aValue instanceof Boolean && column == checkBoxIndex) {
            Fit fit = fits.get(row);
            this.setAllow(fit, ((Boolean) aValue));
            this.fireTableCellUpdated(row, column);
        }
    }

    public int getRowCount() {
        return this.fits != null ? this.fits.size() : 0;
    }

    public int getColumnCount() {
        return MainFrame.isBratsk ? COLUMN_NAMES_BRATSK.length : COLUMN_NAMES_SP.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = "??";
        Fit fit = fits.get(rowIndex);
        if (MainFrame.isBratsk) {
            switch (columnIndex) {
                case 0:
                    value = fit.getDebt().getPaymentDates().stream().map(d -> DateFormatUtils.format(d, "yyyy-MM-dd")).collect(Collectors.joining(";"));
                    if (fit.getFirstPayment().getDate() != null) {
                        value += "/" + DateFormatUtils.format(fit.getFirstPayment().getDate(), "yyyy-MM-dd");
                    }
                    break;
                case 1:
                    value = fit.getPayments().stream().
                            map(Payment::getAbstractFileRecord).
                            map(AbstractFileRecord::getText).collect(Collectors.toList());
                    break;
                case 2:
                    value = fit.getPayments().stream().map(Payment::getSum).reduce(BigDecimal.ZERO, BigDecimal::add);
                    break;
                case 3:
                    value = fit.getPayments().stream().map(payment -> {
                        if (StringUtils.isNotBlank(payment.getName())) {
                            return payment.getName();
                        } else if (StringUtils.isNotBlank(payment.getCard())) {
                            return payment.getCard();
                        } else {
                            return "";
                        }
                    }).collect(Collectors.toList());
                    break;
                case 4:
                    value = fit.getDebt().getDisplayCustomerName();
                    break;
                case 5:
                    value = fit.getDebt().getTotalDebt();
                    break;
                case 6:
                    value = fit.getDebt().getPaymentName();
                    break;
                case 7:
                    value = fit.getDebt().getPaymentCards();
                    break;
                case 8:
                    value = fit.getDebt().getSborId();
                    break;
                case 9:
                    value = fit.getReason();
                    break;
                case 10:
                    value = fit.isAllow();
                    break;
            }
        } else {
            switch (columnIndex) {
                case 0:
                    value = fit.getDebt().getPaymentDates().stream().map(d -> DateFormatUtils.format(d, "yyyy-MM-dd")).collect(Collectors.joining(";"));
                    if (fit.getFirstPayment().getDate() != null) {
                        value += "/" + DateFormatUtils.format(fit.getFirstPayment().getDate(), "yyyy-MM-dd");
                    }
                    break;
                case 1:
                    value = fit.getPayments().stream().
                            map(Payment::getAbstractFileRecord).
                            map(AbstractFileRecord::getText).collect(Collectors.toList());
                    break;
                case 2:
                    value = fit.getPayments().stream().map(Payment::getSum).reduce(BigDecimal.ZERO, BigDecimal::add);
                    break;
                case 3:
                    value = fit.getPayments().stream().map(payment -> {
                        if (StringUtils.isNotBlank(payment.getName())) {
                            return payment.getName();
                        } else if (StringUtils.isNotBlank(payment.getCard())) {
                            return payment.getCard();
                        } else {
                            return "";
                        }
                    }).collect(Collectors.toList());
                    break;
                case 4:
                    value = fit.getDebt().getDisplayCustomerName();
                    break;
                case 5:
                    value = fit.getDebt().getTotalDebt();
                    break;
                case 6:
                    value = fit.getDebt().getPaymentName();
                    break;
                case 7:
                    value = fit.getDebt().getOrderId();
                    break;
                case 8:
                    value = fit.getFirstPayment().getOrder();
                    break;
                case 9:
                    value = fit.getDebt().getPaymentCards();
                    break;
                case 10:
                    value = fit.getDebt().getSborId();
                    break;
                case 11:
                    value = fit.getReason();
                    break;
                case 12:
                    value = fit.isAllow();
                    break;
            }
        }
        return value;
    }

    public Fit getFit(int row) {
        return (Fit) this.fits.get(row);
    }

    public void clear() {
        this.fits.clear();
        this.fireTableDataChanged();
    }

    public void addFit(Fit fit) {
        this.fits.add(fit);
        this.fireTableDataChanged();
    }

    public List<Fit> getFits() {
        return this.fits;
    }

    public boolean isApplyEnabled() {
        for (Fit fit : fits) {
            if (fit.isEnabled() && fit.isAllow()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        Iterator i$ = this.fits.iterator();

        Fit fit;
        do {
            if (!i$.hasNext()) {
                return false;
            }

            fit = (Fit) i$.next();
        } while (!fit.isEnabled());

        return true;
    }

    public void setAllow(Fit fit, boolean f) {
        for (Fit currentFit : fits) {
            if (currentFit == fit) {
                currentFit.setAllow(f);
            } else if (currentFit.getAbstractFileRecords().equals(fit.getAbstractFileRecords()) && f) {
                currentFit.setAllow(false);
            }
        }
    }

    public boolean isSelectAll() {
        boolean selectAll = true;
        ArrayListValuedLinkedHashMap<List<AbstractFileRecord>, Fit> map = new ArrayListValuedLinkedHashMap();
        {
            Iterator i$ = this.fits.iterator();

            while (i$.hasNext()) {
                Fit fit = (Fit) i$.next();
                if (fit.isEnabled()) {
                    map.put(fit.getAbstractFileRecords(), fit);
                }
            }
        }
        {
            Iterator i1$ = map.keySet().iterator();

            while (i1$.hasNext()) {
                List<AbstractFileRecord> abstractFileRecords = (List<AbstractFileRecord>) i1$.next();
                boolean f = false;
                Iterator i2$ = map.get(abstractFileRecords).iterator();

                while (i2$.hasNext()) {
                    Fit fit = (Fit) i2$.next();
                    if (fit.isEnabled() && fit.isAllow()) {
                        f = true;
                    }
                }

                if (!f) {
                    selectAll = false;
                }
            }
        }

        if (selectAll) {
            log.debug("Все необработанные строки выделены");
        } else {
            log.debug("Не все необработанные строки выделены");
        }

        return selectAll;
    }

    public void disableFits(Fit fit) {

        Iterator i$ = this.fits.iterator();

        while (true) {
            Fit f;
            do {
                if (!i$.hasNext()) {
                    return;
                }

                f = (Fit) i$.next();
            } while (f != fit && (!f.getAbstractFileRecords().equals(fit.getAbstractFileRecords())));

            f.setEnabled(false);
        }
    }

    public void setTable(FitTable table) {
        this.table = table;
    }

    private synchronized void updateRowHeights() {
        if (table == null) {
            return;
        }
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();
            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(cellRenderer, row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
            table.setRowHeight(row, rowHeight);
        }
    }
}
