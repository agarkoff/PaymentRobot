package ru.misterparser.paymentrobot;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import ru.misterparser.common.flow.EventProcessor;
import ru.misterparser.paymentrobot.domain.AbstractFileRecord;
import ru.misterparser.paymentrobot.domain.Debt;
import ru.misterparser.paymentrobot.domain.FindResult;
import ru.misterparser.paymentrobot.domain.Reason;
import ru.misterparser.paymentrobot.fit.Fit;
import ru.misterparser.paymentrobot.namecomparators.NameMatchEnum;
import ru.misterparser.paymentrobot.namecomparators.NewNameComparator;

import java.util.*;

public class SpFinder {

    private static final Logger log = LogManager.getLogger(SpFinder.class);

    private NewNameComparator nameComparator = new NewNameComparator();

    private List<Debt> debts;
    private EventProcessor<Fit> eventProcessor;

    public SpFinder(List<Debt> debts, EventProcessor<Fit> eventProcessor) {
        this.debts = new ArrayList<>(debts);
        this.eventProcessor = eventProcessor;
    }

    @SuppressWarnings("Duplicates")
    public int getFoundDebtCount(String filename, Sheet sheet, List<? extends AbstractFileRecord> list) {
        log.debug("Обработка в режиме Союза покупателей");
        int foundDebtCount = 0;
        for (AbstractFileRecord abstractFileRecord : list) {
            log.debug("Обработка " + abstractFileRecord);
            try {
                Payment payment = abstractFileRecord.toPayment();
                log.debug("Обработка платежа: " + payment);
                List<FindResult> findResults = findDebts(payment);
                for (FindResult findResult : findResults) {
                    log.debug("Найдено совпадение: " + findResult);
                    eventProcessor.find(new Fit(payment, findResult.getDebt(), findResult.isDanger(), findResult.getReason()));
                    foundDebtCount++;
                }
            } catch (RuntimeException e) {
                log.debug("RuntimeException", e);
                eventProcessor.log("Строка " + abstractFileRecord + " не распознана как платеж в строке " + (abstractFileRecord.getRow() + 1) + " листа " + sheet.getSheetName() + " файла " + filename);
            }
        }
        return foundDebtCount;
    }

    private List<FindResult> findDebts(Payment payment) {
        List<FindResult> findResults = new ArrayList<>();

        //findResults.add(new FindResult(debts.iterator().next(), false, Reason.ONLY_SUM.getDescription(), payment));

        for (Iterator<Debt> iterator = debts.iterator(); iterator.hasNext(); ) {
            Debt debt = iterator.next();

            boolean sumEquals = payment.getSum().compareTo(debt.getTotalDebt()) == 0;
            boolean orderEquals = StringUtils.isNotBlank(debt.getOrderId()) && StringUtils.isNotBlank(payment.getOrder()) &&
                    StringUtils.equalsIgnoreCase(debt.getOrderId(), payment.getOrder());
            NameMatchEnum nameMatchEnum = nameComparator.equals(payment.getName(), debt.getPaymentName());

            if (MainFrame.TEST) {
                findResults.add(new FindResult(debt, false, Reason.TEST.getDescription(), payment));
                iterator.remove();
                break;
            } else if (Configuration.get().ONLY_SUM && sumEquals) {
                findResults.add(new FindResult(debt, false, Reason.ONLY_SUM.getDescription(), payment));
                iterator.remove();

            } else if (Configuration.get().NAME_AND_ORDER_AND_SUM &&
                    StringUtils.isNotBlank(debt.getPaymentName()) && StringUtils.isNotBlank(payment.getName()) &&
                    nameMatchEnum != NameMatchEnum.NOT_MATCH &&
                    orderEquals &&
                    sumEquals) {
                String prefix = "";
                if (nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH) {
                    prefix = "неполное ";
                }
                findResults.add(new FindResult(debt, nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH, prefix + Reason.NAME_AND_ORDER_AND_SUM.getDescription(), payment));
                iterator.remove();

            } else if (Configuration.get().NAME_AND_DATE_AND_SUM &&
                    StringUtils.isNotBlank(debt.getPaymentName()) && StringUtils.isNotBlank(payment.getName()) &&
                    nameMatchEnum != NameMatchEnum.NOT_MATCH &&
                    dateEquals(debt, payment) &&
                    sumEquals) {
                String prefix = "";
                if (nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH) {
                    prefix = "неполное ";
                }
                findResults.add(new FindResult(debt, nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH, prefix + Reason.NAME_AND_DATE_AND_SUM.getDescription(), payment));
                iterator.remove();

            } else if (Configuration.get().ORDER_AND_SUM &&
                    orderEquals &&
                    sumEquals) {
                findResults.add(new FindResult(debt, false, Reason.ORDER_AND_SUM.getDescription(), payment));
                iterator.remove();

            } else if ((Configuration.get().NAME_AND_SUM) &&
                    StringUtils.isNotBlank(debt.getPaymentName()) && StringUtils.isNotBlank(payment.getName()) &&
                    nameMatchEnum != NameMatchEnum.NOT_MATCH &&
                    sumEquals) {
                String prefix = "";
                if (nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH) {
                    prefix = "неполное ";
                }
                findResults.add(new FindResult(debt, nameMatchEnum == NameMatchEnum.PARTIALLY_MATCH, prefix + Reason.NAME_AND_SUM.getDescription(), payment));
                iterator.remove();

            } else if (Configuration.get().CARD_AND_SUM &&
                    StringUtils.isNotBlank(debt.getFirstPaymentCard()) && StringUtils.isNotBlank(payment.getCard()) &&
                    StringUtils.equalsIgnoreCase(debt.getFirstPaymentCard(), payment.getCard()) &&
                    sumEquals) {
                findResults.add(new FindResult(debt, false, Reason.CARD_AND_SUM.getDescription(), payment));
                iterator.remove();
            }
        }
        return findResults;
    }

    private boolean dateEquals(Debt debt, Payment payment) {
        Date d = debt.getPaymentDates().get(0);
        d = DateUtils.truncate(d, Calendar.DAY_OF_MONTH);
        Date p = payment.getDate();
        p = DateUtils.truncate(p, Calendar.DAY_OF_MONTH);
        return p.equals(d);
    }
}
