package ru.misterparser.paymentrobot.siteloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.paymentrobot.domain.Debt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by MisterParser on 10.10.2017.
 */
public abstract class DebtLoader {

    private static final Logger log = LogManager.getLogger(DebtLoader.class);

    public abstract void authorize() throws InterruptedException, IOException, URISyntaxException;

    public List<Debt> processSiteReports(Collection<String> reportUrls) throws InterruptedException, IOException, URISyntaxException {
        if (reportUrls.size() == 0) {
            throw new RuntimeException("Ссылки на отчеты не найдены!");
        } else {
            List<Debt> debts = new ArrayList<>();
            for (String reportUrl : reportUrls) {
                List<Debt> debtList = processSiteReport(reportUrl);
                log.debug("Найдено ожидающих оплат: " + debtList.size());
                debts.addAll(debtList);
            }
            return debts;
        }
    }

    public abstract List<Debt> processSiteReport(String reportUrl) throws InterruptedException, IOException, URISyntaxException;

    public abstract boolean updateOpl(Debt debt, String value) throws InterruptedException;

    public abstract boolean updateDate(Debt debt, Date date) throws InterruptedException;
}
