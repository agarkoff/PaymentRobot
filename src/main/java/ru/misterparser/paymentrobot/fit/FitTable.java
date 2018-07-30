package ru.misterparser.paymentrobot.fit;

import org.jdesktop.swingx.JXTable;

import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 04.07.16
 * Time: 9:19
 */
public class FitTable extends JXTable {

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        int modelRow = convertRowIndexToModel(row);
        FitTableModel fitTableModel = (FitTableModel) getModel();
        Fit currentFit = fitTableModel.getFit(modelRow);
        {
            if (!currentFit.isEnabled()) {
                c.setBackground(Color.GREEN);
            } else {
                boolean danger = currentFit.isDanger();
                if (!danger) {
                    List<Fit> fits = new ArrayList<>(fitTableModel.getFits());
                    for (Fit fit : fits) {
                        if (fit != currentFit && (fit.getAbstractFileRecords().equals(currentFit.getAbstractFileRecords()))) {
                            danger = true;
                            break;
                        }
                    }
                }
                if (danger) {
                    c.setBackground(Color.ORANGE);
                }
            }
        }
        return c;
    }
}
