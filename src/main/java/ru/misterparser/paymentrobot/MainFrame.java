//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ru.misterparser.paymentrobot;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.java.swing.plaf.windows.WindowsTableHeaderUI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.jdesktop.swingx.prompt.PromptSupport;
import ru.misterparser.common.MavenProperties;
import ru.misterparser.common.YandexUtils;
import ru.misterparser.common.backup.BackupType;
import ru.misterparser.common.backup.BackupUtils;
import ru.misterparser.common.configuration.ApplyConfigurationActionListener;
import ru.misterparser.common.configuration.ConfigurationApplier;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.configuration.StateUpdater;
import ru.misterparser.common.excel.ExcelUtils;
import ru.misterparser.common.flow.LogEventProcessor;
import ru.misterparser.common.flow.ThreadFinishStatus;
import ru.misterparser.common.gui.BrowseItemMouseListener;
import ru.misterparser.common.gui.ContextMenuEventListener;
import ru.misterparser.common.gui.GuiUtils;
import ru.misterparser.common.gui.help.HelpFrame;
import ru.misterparser.common.gui.radiobutton.RadioButtonGroup;
import ru.misterparser.common.gui.tree.TreeUtils;
import ru.misterparser.common.logging.TextAreaAppender;
import ru.misterparser.common.model.Category;
import ru.misterparser.paymentrobot.fit.Fit;
import ru.misterparser.paymentrobot.fit.FitTable;
import ru.misterparser.paymentrobot.fit.FitTableModel;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

public class MainFrame implements ConfigurationApplier {
    private static final Logger log = LogManager.getLogger(MainFrame.class);
    private static final String FRAME_TITLE = "Робот оплат";
    private JFrame frame;
    private JPanel rootPanel;
    private JTabbedPane tabbedPane;
    private JButton actionButton;
    private JLabel itemCountLabel;
    private JTextArea logTextArea;
    private JScrollPane logScrollPane;
    private FitTable itemTable;
    private JScrollPane itemScrollPane;
    private JTextField loginTextField;
    private JPasswordField passwordField;
    private JTextField sourceFilenameTextField;
    private JButton openButton;
    private JButton uploadLogButton;
    private JButton threadControlButton;
    private JButton aboutButton;
    private JButton helpButton;
    private JTextField searchTextField;
    private JButton applyButton;
    private JProgressBar applyProgressBar;
    private JTree categoriesTree;
    private JScrollPane categoriesScrollPane;
    private JButton selectAllButton;
    private JTextArea reportUrlsTextArea;
    private JScrollPane reportUrlsScrollPane;
    private JCheckBox onlySumCheckBox;
    private JCheckBox authByCookiesCheckBox;
    private JPanel formatPanel;
    private JComboBox<String> shiftColumnComboBox;
    private JCheckBox saveInSameFileCheckBox;
    private JCheckBox allReportsCheckBox;
    private JComboBox<String> baseUrlComboBox;
    private JCheckBox isAdminCheckBox;
    private JTextField orgForAdminTextField;
    private JTabbedPane optionTabbedPane;
    private JCheckBox nameAndSumCheckBox;
    private JCheckBox orderAndSumCheckBox;
    private JCheckBox cardAndSumCheckBox;
    private JCheckBox nameAndOrderAndSumCheckBox;
    private JCheckBox nameAndDateAndSumCheckBox;
    private JProgressBar progressBar;
    private JLabel labelCountSum;
    private JLabel labelCountSum2;
    private JCheckBox inaccuracyCheckBox;
    private JPanel sumCountPanel;
    private JTextField inaccuracyValueTextField;
    private JCheckBox dateInaccuracyCheckBox;
    private JPanel bratskReportTypePanel;
    private String currentDirectory;
    private Parser parser;
    private Thread parserThread;
    private boolean isStarted = false;
    private FitTableModel fitTableModel;
    private List<String> filenames;
    public static boolean TEST = false;
    private RadioButtonGroup<Format> formatRadioButtonGroup;
    private ActionListener actionButtonListener;
    private ActionListener threadControlButtonListener;
    private ActionListener openButtonListener;
    private ActionListener aboutButtonListener;
    private ActionListener helpButtonListener;
    private ActionListener uploadLogButtonListener;
    private ErrorLogEventProcessor eventProcessor;
    private TreeUtils.TreeLoader treeLoader;
    private StateUpdater stateUpdater;
    private Properties parserProperties = new Properties();
    public static boolean isBratsk = false;
    private ProgressBarThread progressBarThread;
    private RadioButtonGroup<SumCount> sumCountRadioButtonGroup;
    private RadioButtonGroup<BratskReportType> bratskReportTypeRadioButtonGroup;

    private void uploadLog(String appNamePrefix, boolean verbose) {
        String appName = "Робот оплат".replace("Parser", "");
        appName = appNamePrefix + appName;
        appName = StringUtils.trim(appName);
        YandexUtils.uploadLog(appName, this.logTextArea.getText(), verbose);
    }

    private void resetButtonState() {
        isStarted = false;
        actionButton.setText("Старт");
        threadControlButton.setText("Пауза");
        threadControlButton.setEnabled(false);
        progressBar.setVisible(false);
        if (progressBarThread != null) {
            progressBarThread.interrupt();
        }
    }

    private void updateCounter() {
        String text = "Найдено: " + this.fitTableModel.getFits().size();
        if (StringUtils.isNotBlank(this.searchTextField.getText())) {
            text = text + " (Показано: " + this.itemTable.getRowCount() + ")";
        }

        this.itemCountLabel.setText(text);
    }

    public MainFrame(String currentDirectory, String[] args) {
        this.filenames = new ArrayList();
        //this.BANK_ACCOUNT = false;
        $$$setupUI$$$();
        this.actionButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                MainFrame.this.applyConfiguration(false);
                if (MainFrame.this.isStarted) {
                    MainFrame.this.parserThread.interrupt();
                } else {
                    try {
                        eventProcessor.reset();
                        fitTableModel.clear();
                        updateCounter();
                        parser = new Parser(MainFrame.this.categoriesTree.getSelectionModel(), MainFrame.this.eventProcessor);
                        parserThread = new Thread(MainFrame.this.parser, "Parser");
                        parserThread.start();
                        progressBarThread = new ProgressBarThread();
                        progressBarThread.start();
                        actionButton.setText("Стоп");
                        threadControlButton.setEnabled(true);
                        isStarted = true;
                        applyButton.setEnabled(false);
                        selectAllButton.setEnabled(false);
                        progressBar.setVisible(true);
                    } catch (Exception var3) {
                        MainFrame.log.debug("Exception", var3);
                        JOptionPane.showMessageDialog(MainFrame.this.frame, "Ошибка запуска потока\n" + ExceptionUtils.getFullStackTrace(var3), "Робот оплат", 0);
                        resetButtonState();
                    }
                }

            }
        };
        this.threadControlButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainFrame.this.parser.isSuspended()) {
                    MainFrame.this.parser.resume();
                    MainFrame.this.threadControlButton.setText("Пауза");
                } else {
                    MainFrame.this.parser.suspend();
                    MainFrame.this.threadControlButton.setText("Продолжить");
                }

            }
        };
        this.openButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String filename = GuiUtils.getFilename(MainFrame.this.frame, "Открыть файл...", 0, "xlsx", null);
                if (StringUtils.isNotBlank(filename)) {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(filename);
                        Workbook workbook = WorkbookFactory.create(fileInputStream);
                        if (workbook instanceof HSSFWorkbook) {
                            JOptionPane.showMessageDialog(MainFrame.this.frame, "Робот корректно работает только с файлами в формате Excel 2007 (xlsx)\nПересохраните, пожалуйста, отчёт в формат Книга Excel в программе MS Excel версии 2007 или новее", "Ошибка формата файла", JOptionPane.WARNING_MESSAGE);
                        } else {
                            sourceFilenameTextField.setText(filename);
                            applyConfiguration(false);
                            TreeUtils.initTree(categoriesTree, null, treeLoader);
                        }
                    } catch (Exception e) {
                        log.debug("Exception", e);
                    }
                }
            }
        };
        this.aboutButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                MavenProperties mavenProperties = MavenProperties.load();
                JOptionPane.showMessageDialog(MainFrame.this.frame, "Робот оплат\nВерсия: " + mavenProperties.getProjectVersion() + "\nДата: " + mavenProperties.getFormatBuildDate() + "\nРазработчик: Станислав Агарков\nSkype: stagarkov", "О программе", 1);
            }
        };
        this.helpButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                HelpFrame helpFrame = new HelpFrame();

                try {
                    helpFrame.show(IOUtils.toString(this.getClass().getResourceAsStream("/help.html"), "UTF-8"), new Dimension(600, 400));
                } catch (Exception var4) {
                    MainFrame.log.debug("Exception", var4);
                }

            }
        };
        this.uploadLogButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                (new Thread() {
                    public void run() {
                        MainFrame.this.uploadLog("", true);
                    }
                }).start();
            }
        };
        this.treeLoader = new TreeUtils.TreeLoader() {
            public TreeModel load() throws InterruptedException {
                DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
                DefaultTreeModel defaultTreeModel = new DefaultTreeModel(rootTreeNode);
                String[] arr$ = StringUtils.split(Configuration.get().SOURCE_FILENAME, ";");
                int len$ = arr$.length;
                int i$ = 0;
                if (i$ < len$) {
                    String filename = arr$[i$];
                    Category category = new Category(filename, (String) null);
                    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(category);
                    rootTreeNode.add(newChild);

                    try {
                        MainFrame.this.addChild(newChild, filename);
                    } catch (Exception var10) {
                        MainFrame.log.debug("Exception", var10);
                    }
                }

                return defaultTreeModel;
            }
        };
        this.stateUpdater = new StateUpdater() {
            public void update() {
                MainFrame.this.loginTextField.setEnabled(!Configuration.get().AUTH_BY_COOKIES);
                MainFrame.this.passwordField.setEnabled(!Configuration.get().AUTH_BY_COOKIES);
            }
        };
        this.currentDirectory = currentDirectory;
        ConfigurationUtils.setCurrentDirectory(currentDirectory);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel.setLayout(new GridLayoutManager(8, 3, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane = new JTabbedPane();
        rootPanel.add(tabbedPane, new GridConstraints(0, 0, 7, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 3, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane.addTab("Главная", panel1);
        itemScrollPane = new JScrollPane();
        panel1.add(itemScrollPane, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Авторизация"));
        final JLabel label1 = new JLabel();
        label1.setText("Логин");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginTextField = new JTextField();
        panel2.add(loginTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        authByCookiesCheckBox = new JCheckBox();
        authByCookiesCheckBox.setText("по файлу cookies.txt");
        panel2.add(authByCookiesCheckBox, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Пароль");
        panel2.add(label2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordField = new JPasswordField();
        panel2.add(passwordField, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        searchTextField = new JTextField();
        panel1.add(searchTextField, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        applyProgressBar = new JProgressBar();
        applyProgressBar.setVisible(false);
        panel3.add(applyProgressBar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        applyButton = new JButton();
        applyButton.setEnabled(false);
        applyButton.setText("Применить");
        panel1.add(applyButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectAllButton = new JButton();
        selectAllButton.setEnabled(false);
        selectAllButton.setText("Выделить всё");
        panel1.add(selectAllButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        optionTabbedPane = new JTabbedPane();
        panel1.add(optionTabbedPane, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(5, 3, new Insets(5, 5, 5, 5), -1, -1));
        optionTabbedPane.addTab("Отчёты", panel4);
        final JLabel label3 = new JLabel();
        label3.setText("Ссылка на отчет");
        panel4.add(label3, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reportUrlsScrollPane = new JScrollPane();
        panel4.add(reportUrlsScrollPane, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        reportUrlsTextArea = new JTextArea();
        reportUrlsScrollPane.setViewportView(reportUrlsTextArea);
        allReportsCheckBox = new JCheckBox();
        allReportsCheckBox.setText("Все отчеты");
        panel4.add(allReportsCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        baseUrlComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        baseUrlComboBox.setModel(defaultComboBoxModel1);
        panel4.add(baseUrlComboBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(spacer1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        isAdminCheckBox = new JCheckBox();
        isAdminCheckBox.setText("Я — админ");
        panel4.add(isAdminCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        orgForAdminTextField = new JTextField();
        panel4.add(orgForAdminTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        bratskReportTypePanel = new JPanel();
        bratskReportTypePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(bratskReportTypePanel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 3, new Insets(5, 5, 5, 5), -1, -1));
        optionTabbedPane.addTab("Файл оплат", panel5);
        final JLabel label4 = new JLabel();
        label4.setText("Имя файла");
        panel5.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sourceFilenameTextField = new JTextField();
        sourceFilenameTextField.setEditable(false);
        panel5.add(sourceFilenameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        openButton = new JButton();
        openButton.setText("Обзор...");
        panel5.add(openButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Формат");
        panel5.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formatPanel = new JPanel();
        formatPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(formatPanel, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        categoriesScrollPane = new JScrollPane();
        panel5.add(categoriesScrollPane, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(-1, 100), new Dimension(-1, 100), new Dimension(-1, 100), 0, false));
        categoriesTree = new JTree();
        categoriesScrollPane.setViewportView(categoriesTree);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Смещение колонок с доп. информацией (применимо для банковской выписки)");
        panel6.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shiftColumnComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("6");
        defaultComboBoxModel2.addElement("15");
        defaultComboBoxModel2.addElement("26");
        defaultComboBoxModel2.addElement("40");
        shiftColumnComboBox.setModel(defaultComboBoxModel2);
        panel6.add(shiftColumnComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(10, 2, new Insets(5, 5, 5, 5), -1, -1));
        optionTabbedPane.addTab("Совпадения", panel7);
        onlySumCheckBox = new JCheckBox();
        onlySumCheckBox.setText("только по сумме");
        panel7.add(onlySumCheckBox, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameAndSumCheckBox = new JCheckBox();
        nameAndSumCheckBox.setText("по ФИО и сумме");
        panel7.add(nameAndSumCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        orderAndSumCheckBox = new JCheckBox();
        orderAndSumCheckBox.setText("по номеру заказа и сумме");
        panel7.add(orderAndSumCheckBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cardAndSumCheckBox = new JCheckBox();
        cardAndSumCheckBox.setText("по номеру карты и сумме");
        panel7.add(cardAndSumCheckBox, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameAndOrderAndSumCheckBox = new JCheckBox();
        nameAndOrderAndSumCheckBox.setText("по ФИО, номеру заказа и сумме");
        panel7.add(nameAndOrderAndSumCheckBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameAndDateAndSumCheckBox = new JCheckBox();
        nameAndDateAndSumCheckBox.setText("по ФИО, дате и сумме");
        panel7.add(nameAndDateAndSumCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelCountSum = new JLabel();
        labelCountSum.setText("поиск по");
        panel8.add(labelCountSum, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel8.add(spacer4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        sumCountPanel = new JPanel();
        sumCountPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(sumCountPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelCountSum2 = new JLabel();
        labelCountSum2.setText("совпадениям");
        panel8.add(labelCountSum2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel9, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        inaccuracyCheckBox = new JCheckBox();
        inaccuracyCheckBox.setText("поиск по сумме с учётом погрешности");
        panel9.add(inaccuracyCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel9.add(spacer5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        inaccuracyValueTextField = new JTextField();
        panel9.add(inaccuracyValueTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        dateInaccuracyCheckBox = new JCheckBox();
        dateInaccuracyCheckBox.setText("+/− один день");
        panel7.add(dateInaccuracyCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane.addTab("Лог", panel10);
        logScrollPane = new JScrollPane();
        panel10.add(logScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logScrollPane.setViewportView(logTextArea);
        actionButton = new JButton();
        actionButton.setText("Старт");
        rootPanel.add(actionButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        itemCountLabel = new JLabel();
        itemCountLabel.setText("Найдено: 0");
        rootPanel.add(itemCountLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        rootPanel.add(spacer6, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        uploadLogButton = new JButton();
        uploadLogButton.setText("Отправить лог");
        rootPanel.add(uploadLogButton, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadControlButton = new JButton();
        threadControlButton.setEnabled(false);
        threadControlButton.setText("Пауза");
        rootPanel.add(threadControlButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aboutButton = new JButton();
        aboutButton.setText("О программе");
        rootPanel.add(aboutButton, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        helpButton = new JButton();
        helpButton.setText("Справка");
        rootPanel.add(helpButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveInSameFileCheckBox = new JCheckBox();
        saveInSameFileCheckBox.setText("Сохранять в тот же файл");
        rootPanel.add(saveInSameFileCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(100);
        progressBar.setVisible(false);
        rootPanel.add(progressBar, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private class ErrorLogEventProcessor extends LogEventProcessor<Fit> {

        public ErrorLogEventProcessor(String tabTitle, String messageTitle, JTabbedPane tabbedPane) {
            super(tabTitle, messageTitle, tabbedPane);
        }

        public void find(Fit fit) {
            log.debug("Найдено соответствие: " + fit);
            fitTableModel.addFit(fit);
            updateCounter();
        }

        public void finish(ThreadFinishStatus threadFinishStatus, Throwable throwable, String dopText) {
            selectAllButton.setEnabled(MainFrame.this.fitTableModel.getFits().size() > 0);
            applyButton.setEnabled(MainFrame.this.fitTableModel.isApplyEnabled());
            log.debug("Найдено соответствий: " + MainFrame.this.fitTableModel.getFits().size());
            resetButtonState();
            super.finish(threadFinishStatus, throwable, dopText);
        }
    }

    ;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        String currentDirectoryTemp = "";
        if (args.length > 0) {
            currentDirectoryTemp = args[0] + System.getProperty("file.separator");
        }

        MainFrame mainFrame = new MainFrame(currentDirectoryTemp, args);
        mainFrame.start();
    }

    private void start() {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainFrame.this.createAndShowGUI();
                }
            });
        } catch (Throwable var2) {
            JOptionPane.showMessageDialog(this.frame, "Ошибка создания окна\n" + ExceptionUtils.getFullStackTrace(var2), "Робот оплат", 0);
        }
    }

    private class ProgressBarThread extends Thread {
        @Override
        public void run() {
            progressBar.setValue(0);
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            while (true) {
                try {
                    if (progressBar.getValue() >= 100) {
                        progressBar.setValue(0);
                    }
                    progressBar.setValue(progressBar.getValue() + 4);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.debug("Остановка потока прогрессбара...");
                    return;
                }
            }
        }
    }

    private void createAndShowGUI() {

        eventProcessor = new ErrorLogEventProcessor("Ошибки", "Робот оплат", this.tabbedPane);

        ZipSecureFile.setMinInflateRatio(0.001);

        Toolkit.getDefaultToolkit().addAWTEventListener(new ContextMenuEventListener(), AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        try {
            parserProperties.load(new FileInputStream(ConfigurationUtils.getCurrentDirectory() + "parser.properties"));
            isBratsk = StringUtils.equalsIgnoreCase(parserProperties.getProperty("siteLoader"), "bratsk");
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("Exception", e);
        }

        this.fitTableModel = new FitTableModel();

        MavenProperties mavenProperties = MavenProperties.load();
        frame = new JFrame(FRAME_TITLE + " " + mavenProperties.getProjectVersion());
        this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.frame.setVisible(true);
        this.frame.setContentPane(this.rootPanel);
        Dimension minimumSize = new Dimension(850, 700);
        this.frame.setMinimumSize(minimumSize);
        this.frame.setSize(minimumSize);
        GuiUtils.updateUIOnPanel(this.rootPanel);
        GuiUtils.adjustmentScrollPane(this.reportUrlsScrollPane);
        GuiUtils.adjustmentScrollPane(this.categoriesScrollPane);
        GuiUtils.adjustmentScrollPane(this.itemScrollPane);
        GuiUtils.adjustmentScrollPaneWithTextArea(this.reportUrlsScrollPane, this.reportUrlsTextArea);
        GuiUtils.adjustmentScrollPaneWithTextArea(this.logScrollPane, this.logTextArea);
        GuiUtils.frameDisplayCenter(this.frame);
        this.itemTable = new FitTable();
        this.itemTable.setFillsViewportHeight(false);
        this.itemScrollPane.getViewport().add(this.itemTable);
        this.actionButton.addActionListener(this.actionButtonListener);
        this.threadControlButton.addActionListener(this.threadControlButtonListener);
        this.openButton.addActionListener(this.openButtonListener);
        this.helpButton.addActionListener(this.helpButtonListener);
        this.aboutButton.addActionListener(this.aboutButtonListener);
        uploadLogButton.addActionListener(this.uploadLogButtonListener);
        itemTable.setModel(fitTableModel);
        itemTable.getColumnModel().getColumn(1).setCellRenderer(new MultiLineTableCellRenderer());
        itemTable.getColumnModel().getColumn(3).setCellRenderer(new MultiLineTableCellRenderer());
        itemTable.getColumnModel().getColumn(9).setCellRenderer(new MultiLineTableCellRenderer());
        itemTable.getTableHeader().setUI(new WindowsTableHeaderUI());
        itemTable.setUI(new BasicTableUI());
        itemTable.addMouseListener(new BrowseItemMouseListener(this.itemTable));
        categoriesTree.setUI(new BasicTreeUI());
        itemTable.setRowHeight(16);
        fitTableModel.setTable(itemTable);
        TextAreaAppender.setTextArea(this.logTextArea);
        GuiUtils.setupSearchByKeyboard(this.logTextArea);
        GuiUtils.setupFrameIconImage(this.frame);
        applyButton.setIcon(new ImageIcon(this.frame.getClass().getResource("/apply.png")));
        searchTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                MainFrame.this.itemTable.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        DefaultTableModel tableModel = entry.getModel();
                        int ei = entry.getIdentifier();

                        for (int i = 0; i < tableModel.getColumnCount(); ++i) {
                            String s = String.valueOf(tableModel.getValueAt(ei, i));
                            if (StringUtils.containsIgnoreCase(s, MainFrame.this.searchTextField.getText())) {
                                return true;
                            }
                        }

                        return false;
                    }
                });
                MainFrame.this.updateCounter();
            }
        });
        PromptSupport.setPrompt("Поиск по таблице...", searchTextField);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                MainFrame.this.applyConfiguration(false);
            }
        });
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        processTable();
                        try {
                            FileUtils.write(new File(ConfigurationUtils.getCurrentDirectory() + "логи/log_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HHmmss") + ".txt"), logTextArea.getText(), "windows-1251");
                        } catch (Throwable e) {
                            log.debug("Throwable", e);
                        }
                    }
                });
            }
        });
        itemTable.getDefaultEditor(Boolean.class).addCellEditorListener(new CellEditorListener() {
            public void editingCanceled(ChangeEvent e) {
            }

            public void editingStopped(ChangeEvent e) {
                MainFrame.this.applyButton.setEnabled(MainFrame.this.fitTableModel.isApplyEnabled());
                MainFrame.this.updateSelectAllText();
            }
        });
        selectAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean selectAll = MainFrame.this.fitTableModel.isSelectAll();
                for (Fit fit : fitTableModel.getFits()) {
                    if (fit.isEnabled()) {
                        if (selectAll) {
                            fitTableModel.setAllow(fit, false);
                        } else {
                            fitTableModel.setAllow(fit, true);
                        }
                    }
                }
                fitTableModel.fireTableDataChanged();
                updateSelectAllText();
                applyButton.setEnabled(fitTableModel.isApplyEnabled());
            }
        });

        authByCookiesCheckBox.addActionListener(new ApplyConfigurationActionListener(this, false, stateUpdater));
        saveInSameFileCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        allReportsCheckBox.addActionListener(new ApplyConfigurationActionListener(this, false, allReportsStateUpdater));
        isAdminCheckBox.addActionListener(new ApplyConfigurationActionListener(this, false, allReportsStateUpdater));
        orderAndSumCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        nameAndSumCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        nameAndOrderAndSumCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        nameAndDateAndSumCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        cardAndSumCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        onlySumCheckBox.addActionListener(new ApplyConfigurationActionListener(this, false, onlySumStateUpdater));
        inaccuracyCheckBox.addActionListener(new ApplyConfigurationActionListener(this));
        dateInaccuracyCheckBox.addActionListener(new ApplyConfigurationActionListener(this));

        formatRadioButtonGroup = new RadioButtonGroup<>(this.formatPanel, Format.class, new ApplyConfigurationActionListener(this));
        formatRadioButtonGroup.recreateRadiobuttons(RadioButtonGroup.Direction.HORIZONTAL);

        sumCountRadioButtonGroup = new RadioButtonGroup<>(sumCountPanel, SumCount.class, new ApplyConfigurationActionListener(this));
        sumCountRadioButtonGroup.recreateRadiobuttons(RadioButtonGroup.Direction.HORIZONTAL);

        bratskReportTypeRadioButtonGroup = new RadioButtonGroup<>(bratskReportTypePanel, BratskReportType.class, new ApplyConfigurationActionListener(this));
        bratskReportTypeRadioButtonGroup.recreateRadiobuttons(RadioButtonGroup.Direction.HORIZONTAL);

        ConfigurationUtils.restoreConfiguration(Configuration.get());
        populateMainFrame();
        TreeUtils.initTree(this.categoriesTree, null, treeLoader);

        // эта строка всегда должна быть в конце, чтобы не было вызова applyConfiguration из populateMainFrame
        shiftColumnComboBox.addActionListener(new ApplyConfigurationActionListener(this));
        baseUrlComboBox.addActionListener(new ApplyConfigurationActionListener(this));
    }

    private StateUpdater onlySumStateUpdater = new StateUpdater() {
        @Override
        public void update() {
            if (isBratsk) {
                orderAndSumCheckBox.setVisible(false);
                nameAndOrderAndSumCheckBox.setVisible(false);
                nameAndDateAndSumCheckBox.setVisible(false);
                onlySumCheckBox.setText("по сумме");
                isAdminCheckBox.setVisible(false);
                orgForAdminTextField.setVisible(false);
                DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
                defaultComboBoxModel.addElement("http://spbratsk.ru");
                baseUrlComboBox.setModel(defaultComboBoxModel);
            } else {
                orderAndSumCheckBox.setEnabled(!Configuration.get().ONLY_SUM);
                nameAndSumCheckBox.setEnabled(!Configuration.get().ONLY_SUM);
                nameAndOrderAndSumCheckBox.setEnabled(!Configuration.get().ONLY_SUM);
                nameAndDateAndSumCheckBox.setEnabled(!Configuration.get().ONLY_SUM);
                cardAndSumCheckBox.setEnabled(!Configuration.get().ONLY_SUM);
                labelCountSum.setVisible(false);
                labelCountSum2.setVisible(false);
                sumCountPanel.setVisible(false);
                inaccuracyCheckBox.setVisible(false);
                inaccuracyValueTextField.setVisible(false);
                dateInaccuracyCheckBox.setVisible(false);
                DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
                defaultComboBoxModel.addElement("https://30sp.ru");
                defaultComboBoxModel.addElement("https://62sp.ru");
                defaultComboBoxModel.addElement("https://64pokupki.ru");
                defaultComboBoxModel.addElement("https://kras-sp.ru");
                defaultComboBoxModel.addElement("https://krai-sp.ru");
                defaultComboBoxModel.addElement("https://kuz-sp.ru");
                defaultComboBoxModel.addElement("https://msk-sp.ru");
                defaultComboBoxModel.addElement("https://nn-sp.ru");
                defaultComboBoxModel.addElement("https://piter-sp.ru");
                defaultComboBoxModel.addElement("https://sp.38mama.ru");
                defaultComboBoxModel.addElement("https://sp-barnaul.ru");
                defaultComboBoxModel.addElement("https://spomsk.ru");
                defaultComboBoxModel.addElement("https://sppenza.ru");
                defaultComboBoxModel.addElement("https://spvhmao.ru");
                defaultComboBoxModel.addElement("https://spvkazani.ru");
                defaultComboBoxModel.addElement("https://spvrostove.ru");
                defaultComboBoxModel.addElement("https://spvsamare.ru");
                defaultComboBoxModel.addElement("https://spvtomske.ru");
                defaultComboBoxModel.addElement("https://tula-sp.ru");
                defaultComboBoxModel.addElement("https://vart-sp.ru");
                defaultComboBoxModel.addElement("https://vladimir-sp.ru");
                defaultComboBoxModel.addElement("https://volgograd-sp.ru");
                baseUrlComboBox.setModel(defaultComboBoxModel);
                bratskReportTypePanel.setVisible(false);
            }
        }
    };

    private StateUpdater allReportsStateUpdater = new StateUpdater() {
        @Override
        public void update() {
            GuiUtils.setEnabledRecursively(reportUrlsScrollPane, !Configuration.get().ALL_REPORTS);
            baseUrlComboBox.setEnabled(Configuration.get().ALL_REPORTS);
            isAdminCheckBox.setEnabled(Configuration.get().ALL_REPORTS);
            orgForAdminTextField.setEnabled(Configuration.get().ALL_REPORTS && Configuration.get().IS_ADMIN);
        }
    };

    private void processTable() {
        long resultFileLength = 0L;

        try {
            applyButton.setEnabled(false);
            applyConfiguration(false);
            Parser p = new Parser(eventProcessor);
            if (!TEST) {
                p.init();
            }

            List<Fit> processFits = new ArrayList<>();
            log.debug("В таблице строк всего: " + fitTableModel.getFits().size());

            for (Fit fit : fitTableModel.getFits())
                if (fit.isAllow() && fit.isEnabled()) {
                    processFits.add(fit);
                } else {
                    log.debug("Пропускаем совпадение: " + fit);
                }

            log.debug("Строк для обработки: " + processFits.size());
            this.applyProgressBar.setVisible(true);
            this.applyProgressBar.setValue(0);
            this.applyProgressBar.setMaximum(processFits.size());
            Workbook workbook = null;
            String filename = null;
            FileInputStream fileInputStream = null;
            if (processFits.size() > 0) {
                String dateSuffix = DateFormatUtils.format(new Date(), "yyyyMMdd_HHmmss");

                for (Fit fit : processFits) {
                    if (workbook == null) {
                        filename = fit.getFirstPayment().getAbstractFileRecord().getFilename();
                        BackupUtils.backupFile(filename, BackupType.ONE, true);
                        log.debug("Загрузка файла: " + filename);
                        fileInputStream = new FileInputStream(filename);
                        workbook = WorkbookFactory.create(fileInputStream);
                    }

                    try {
                        log.debug("Обработка совпадения: " + fit);
                        BigDecimal value = fit.getDebt().getPaid();
                        log.debug("Старое значение оплаты: " + value);
                        value = value.add(fit.getPayments().stream().map(Payment::getSum).reduce(BigDecimal.ZERO, BigDecimal::add));
                        log.debug("Новое значение оплаты: " + value);
                        if (!TEST) {
                            boolean b1 = p.updateOpl(fit.getDebt(), String.valueOf(value));
                            boolean b2 = p.updateDate(fit.getDebt(), fit.getFirstPayment().getDate());
                            if (!b1 || !b2) {
                                this.eventProcessor.log("Ошибка при проставлении оплаты " + fit);
                                continue;
                            }
                        }

                        Sheet sheet = workbook.getSheet(fit.getFirstPayment().getAbstractFileRecord().getSheetName());
                        for (Payment payment : fit.getPayments()) {
                            log.debug("Окрашивание строки " + (payment.getAbstractFileRecord().getRow() + 1) + " листа " + sheet.getSheetName() + " файла " + fit.getFirstPayment().getAbstractFileRecord().getFilename());
                            Row row = sheet.getRow(payment.getAbstractFileRecord().getRow());
                            int columnCount = Integer.parseInt(Configuration.get().SHIFT_COLUMN);
                            Cell customerNameCell = row.createCell(columnCount);
                            customerNameCell.setCellValue(fit.getDebt().getDisplayCustomerName());
                            Cell sborIdCell = row.createCell(columnCount + 1);
                            sborIdCell.setCellValue(fit.getDebt().getSborId());
                            Cell paymentSumCell = row.createCell(columnCount + 2);
                            paymentSumCell.setCellValue(payment.getSum().setScale(2, BigDecimal.ROUND_HALF_DOWN).toString());
                            Cell orderIdCell = row.createCell(columnCount + 3);
                            orderIdCell.setCellValue(fit.getDebt().getOrderId());
                            if (Configuration.get().FORMAT == Format.SMS) {
                                int messageColumn = ExcelUtils.getHeaderColumnNumber(sheet, true, "Message");
                                Cell cell = row.getCell(messageColumn);
                                ExcelUtils.setSolidForegroundColor(cell, IndexedColors.YELLOW);
                            } else if (Configuration.get().FORMAT == Format.BANK_ACCOUNT) {
                                for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); ++i) {
                                    Cell cell = row.getCell(i);
                                    if (cell != null) {
                                        ExcelUtils.setSolidForegroundColor(cell, IndexedColors.YELLOW);
                                    }
                                }
                            } else {
                                throw new RuntimeException("Ошибка выбора формата файла!");
                            }
                        }

                        fitTableModel.disableFits(fit);
                        fitTableModel.fireTableDataChanged();
                        applyProgressBar.setValue(this.applyProgressBar.getValue() + 1);
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        log.debug("Exception", e);
                        this.eventProcessor.log("Критическая ошибка при окраске строки " + (fit.getFirstPayment().getAbstractFileRecord().getRow() + 1) + " листа " + fit.getFirstPayment().getAbstractFileRecord().getSheetName() + " файла " + fit.getFirstPayment().getAbstractFileRecord().getFilename());
                    }
                }
                String newName;
                if (Configuration.get().SAVE_IN_SAME_FILE) {
                    fileInputStream.close();
                    File oldFile = new File(filename);
                    while (oldFile.exists()) {
                        log.debug("Удаляем исходный файл: " + filename);
                        FileUtils.deleteQuietly(oldFile);
                        Thread.sleep(1000);
                    }
                    newName = filename;
                } else {
                    newName = FilenameUtils.getFullPath(filename) + FilenameUtils.getBaseName(filename) + " new " + dateSuffix + "." + FilenameUtils.getExtension(filename);
                }
                log.debug("Сохранение файла: " + newName);
                FileOutputStream fileOutputStream = new FileOutputStream(newName);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                resultFileLength = (new File(newName)).length();
                log.debug("Размер файла на диске: " + resultFileLength);
            } else {
                JOptionPane.showMessageDialog(this.frame, "Не выбраны совпадения", "Робот оплат", 2);
            }
        } catch (InterruptedException var36) {
            log.debug("Остановка обработки", var36);
        } catch (Throwable var37) {
            log.debug("Throwable", var37);
            JOptionPane.showMessageDialog(this.frame, "Ошибка при обработке\n" + var37.getMessage(), "Робот оплат", 0);
        } finally {
            this.applyButton.setEnabled(this.fitTableModel.isApplyEnabled());
            this.applyProgressBar.setVisible(false);
            this.selectAllButton.setEnabled(this.fitTableModel.isEnabled());
            ExcelUtils.clearColumnNamesCache();
            JOptionPane.showMessageDialog(this.frame, "Обработка завершена\nРазмер файла отчета на диске: " + resultFileLength, "Робот оплат", 1);
        }
    }

    private void updateSelectAllText() {
        if (this.fitTableModel.isSelectAll()) {
            this.selectAllButton.setText("Снять всё выделение");
        } else {
            this.selectAllButton.setText("Выделить всё");
        }
    }

    private void addChild(DefaultMutableTreeNode treeNode, String filename) throws IOException, InvalidFormatException {
        log.debug("Обработка файла: " + filename);

        try {
            Workbook workbook = WorkbookFactory.create(new File(filename), (String) null, true);
            Throwable var4 = null;

            try {
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); ++sheetIndex) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    Category category = new Category(sheet.getSheetName(), (String) null);
                    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(category);
                    treeNode.add(newChild);
                }
            } catch (Throwable var17) {
                var4 = var17;
                throw var17;
            } finally {
                if (workbook != null) {
                    if (var4 != null) {
                        try {
                            workbook.close();
                        } catch (Throwable var16) {
                            var4.addSuppressed(var16);
                        }
                    } else {
                        workbook.close();
                    }
                }

            }
        } catch (Exception var19) {
            log.debug("Exception", var19);
            JOptionPane.showMessageDialog(this.frame, var19.getClass().getSimpleName() + " " + var19.getMessage(), "Робот оплат", 0);
        }

    }

    public void applyConfiguration(boolean verbose) {
        try {
            Configuration.get().LOGIN = this.loginTextField.getText();
            Configuration.get().PASSWORD = new String(this.passwordField.getPassword());
            Configuration.get().AUTH_BY_COOKIES = this.authByCookiesCheckBox.isSelected();
            Configuration.get().SOURCE_FILENAME = this.sourceFilenameTextField.getText();
            Configuration.get().REPORT_URLS = new ArrayList(Arrays.asList(StringUtils.split(this.reportUrlsTextArea.getText(), "\n")));
            //Configuration.get().TIME_ZONE = ((Integer)this.timeZoneSpinner.getValue()).intValue();
            Configuration.get().FORMAT = (Format) this.formatRadioButtonGroup.getSelected();
            Configuration.get().SHIFT_COLUMN = (String) shiftColumnComboBox.getSelectedItem();
            Configuration.get().SAVE_IN_SAME_FILE = saveInSameFileCheckBox.isSelected();
            Configuration.get().ALL_REPORTS = allReportsCheckBox.isSelected();
            Configuration.get().BASE_URL = (String) baseUrlComboBox.getSelectedItem();
            Configuration.get().IS_ADMIN = isAdminCheckBox.isSelected();
            Configuration.get().ORG_FOR_ADMIN = orgForAdminTextField.getText();
            Configuration.get().ORDER_AND_SUM = orderAndSumCheckBox.isSelected();
            Configuration.get().NAME_AND_SUM = nameAndSumCheckBox.isSelected();
            Configuration.get().NAME_AND_ORDER_AND_SUM = nameAndOrderAndSumCheckBox.isSelected();
            Configuration.get().NAME_AND_DATE_AND_SUM = nameAndDateAndSumCheckBox.isSelected();
            Configuration.get().CARD_AND_SUM = cardAndSumCheckBox.isSelected();
            Configuration.get().ONLY_SUM = onlySumCheckBox.isSelected();
            Configuration.get().SUM_COUNT = sumCountRadioButtonGroup.getSelected();
            Configuration.get().INACCURACY = inaccuracyCheckBox.isSelected();
            Configuration.get().INACCURACY_VALUE = new BigDecimal(inaccuracyValueTextField.getText());
            Configuration.get().DATE_INACCURACY = dateInaccuracyCheckBox.isSelected();
            Configuration.get().BRATSK_REPORT_TYPE = bratskReportTypeRadioButtonGroup.getSelected();
            log.debug("Настройки установлены");
            ConfigurationUtils.saveConfiguration(Configuration.get());
            if (verbose) {
                JOptionPane.showMessageDialog(this.frame, "Настройки установлены", "Робот оплат", 1);
            }
        } catch (Exception var3) {
            log.debug("Exception", var3);
            if (verbose) {
                JOptionPane.showMessageDialog(this.frame, "Ошибка установки параметров\n" + ExceptionUtils.getFullStackTrace(var3), "Робот оплат", 0);
            }
        }

    }

    public void populateMainFrame() {
        this.loginTextField.setText(Configuration.get().LOGIN);
        this.passwordField.setText(Configuration.get().PASSWORD);
        this.authByCookiesCheckBox.setSelected(Configuration.get().AUTH_BY_COOKIES);
        this.sourceFilenameTextField.setText(Configuration.get().SOURCE_FILENAME);
        if (Configuration.get().CURRENT_DIRECTORY == null) {
            Configuration.get().CURRENT_DIRECTORY = new GuiUtils.DirectoryHolder();
        }

        this.reportUrlsTextArea.setText(StringUtils.join(Configuration.get().REPORT_URLS, "\n"));
        //this.timeZoneSpinner.setValue(Integer.valueOf(Configuration.get().TIME_ZONE));
        this.stateUpdater.update();
        this.formatRadioButtonGroup.setSelected(Configuration.get().FORMAT);
        if (StringUtils.isBlank(Configuration.get().SHIFT_COLUMN)) {
            Configuration.get().SHIFT_COLUMN = shiftColumnComboBox.getModel().getElementAt(0);
        }
        this.shiftColumnComboBox.setSelectedItem(Configuration.get().SHIFT_COLUMN);
        this.saveInSameFileCheckBox.setSelected(Configuration.get().SAVE_IN_SAME_FILE);
        this.allReportsCheckBox.setSelected(Configuration.get().ALL_REPORTS);
        allReportsStateUpdater.update();
        isAdminCheckBox.setSelected(Configuration.get().IS_ADMIN);
        orgForAdminTextField.setText(Configuration.get().ORG_FOR_ADMIN);
        orderAndSumCheckBox.setSelected(Configuration.get().ORDER_AND_SUM);
        nameAndSumCheckBox.setSelected(Configuration.get().NAME_AND_SUM);
        nameAndOrderAndSumCheckBox.setSelected(Configuration.get().NAME_AND_ORDER_AND_SUM);
        nameAndDateAndSumCheckBox.setSelected(Configuration.get().NAME_AND_DATE_AND_SUM);
        cardAndSumCheckBox.setSelected(Configuration.get().CARD_AND_SUM);
        onlySumCheckBox.setSelected(Configuration.get().ONLY_SUM);
        onlySumStateUpdater.update();
        sumCountRadioButtonGroup.setSelected(Configuration.get().SUM_COUNT);
        shiftColumnComboBox.setSelectedItem(String.valueOf(Configuration.get().SHIFT_COLUMN));
        inaccuracyCheckBox.setSelected(Configuration.get().INACCURACY);
        if (Configuration.get().BASE_URL == null) {
            Configuration.get().BASE_URL = baseUrlComboBox.getModel().getElementAt(0);
        }
        baseUrlComboBox.setSelectedItem(Configuration.get().BASE_URL);
        if (Configuration.get().INACCURACY_VALUE == null) {
            Configuration.get().INACCURACY_VALUE = new BigDecimal("0.01");
        }
        inaccuracyValueTextField.setText(String.valueOf(Configuration.get().INACCURACY_VALUE));
        dateInaccuracyCheckBox.setSelected(Configuration.get().DATE_INACCURACY);
        bratskReportTypeRadioButtonGroup.setSelected(Configuration.get().BRATSK_REPORT_TYPE);
    }

    private void createUIComponents() {
        GuiUtils.initLAF();
        this.rootPanel = new JPanel();
    }
}
