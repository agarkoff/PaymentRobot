package ru.misterparser.paymentrobot;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import ru.misterparser.common.configuration.ConfigurationSetter;
import ru.misterparser.common.gui.GuiUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Стас
 * Date: 02.09.12
 * Time: 12:34
 */
public class Configuration implements Serializable, ConfigurationSetter<Configuration> {

	private static final long serialVersionUID = 1;

	private static Configuration configuration = new Configuration();

    public Configuration() {
    }

    public static Configuration get() {
        return configuration;
    }

    public void set(Configuration configuration) {
        Configuration.configuration = configuration;
    }

    public String LOGIN;
    public String PASSWORD;
	public String SOURCE_FILENAME;
    public GuiUtils.DirectoryHolder CURRENT_DIRECTORY;
    public List<String> REPORT_URLS;
    //public int TIME_ZONE;
    public boolean AUTH_BY_COOKIES;
    public Format FORMAT;
    public String SHIFT_COLUMN;
    public boolean SAVE_IN_SAME_FILE;
    public boolean ALL_REPORTS;
    public String BASE_URL;
    public boolean IS_ADMIN;
    public String ORG_FOR_ADMIN;
    public boolean ORDER_AND_SUM = true;
    public boolean NAME_AND_SUM = true;
    public boolean NAME_AND_ORDER_AND_SUM = true;
    public boolean NAME_AND_DATE_AND_SUM = true;
    public boolean CARD_AND_SUM = true;
    public boolean ONLY_SUM = false;
    public SumCount SUM_COUNT;
    public boolean INACCURACY = true;
    public BigDecimal INACCURACY_VALUE;
    public boolean DATE_INACCURACY;
    public BratskReportType BRATSK_REPORT_TYPE;

    public static int NETWORK_TIMEOUT = 60000;

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).setExcludeFieldNames(new String[] {"PASSWORD"}).toString();
    }
}
