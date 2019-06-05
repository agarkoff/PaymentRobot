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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BratskFinder {

    private static final Logger log = LogManager.getLogger(BratskFinder.class);

    private NewNameComparator nameComparator = new NewNameComparator();
    private CardComparator cardComparator = new CardComparator();

    private List<Debt> debts;
    private EventProcessor<Fit> eventProcessor;

    public BratskFinder(List<Debt> debts, EventProcessor<Fit> eventProcessor) {
        this.debts = debts;
        this.eventProcessor = eventProcessor;
    }

    @SuppressWarnings("Duplicates")
    public int getFoundDebtCount(String filename, Sheet sheet, List<? extends AbstractFileRecord> list) {
        log.debug("Обработка в режиме города Братска");
        List<Payment> payments = list.stream().map(AbstractFileRecord::toPayment).collect(Collectors.toList());

        if (MainFrame.TEST) {
            debts = new ArrayList<>();
            debts.add(new Debt("sbor", "dbid", "customerid", "Ваня", new BigDecimal(0), new BigDecimal(1000), Arrays.asList("РАИЛЯ РАИЛЬЕВНА С."), Arrays.asList("1234", "1060"), Arrays.asList(parseDate("2017-11-15")), "orderid", "baseurl"));
            debts.add(new Debt("sbor", "dbid", "customerid", "Ваня", new BigDecimal(0), new BigDecimal(2000), Arrays.asList("РАИЛЯ РАИЛЬЕВНА С.", "НАТАЛЬЯ ВИКТОРОВНА Д."), Arrays.asList("8000", "9000"), Arrays.asList(parseDate("2017-11-15")), "orderid", "baseurl"));
            debts.add(new Debt("sbor", "dbid", "customerid", "Ваня", new BigDecimal(0), new BigDecimal(750), Arrays.asList("НАТАЛЬЯ ВИКТОРОВНА Д."), Arrays.asList("1234"), Arrays.asList(parseDate("2017-11-14")), "orderid", "baseurl"));

            payments = new ArrayList<>();
            payments.add(new ru.misterparser.paymentrobot.domain.Sms("filename", "sheetname", 1, parseDate("2017-11-15"), "ECMC5478 19:23 перевод 2390.00р от РАИЛЯ РАИЛЬЕВНА С.").toPayment());
            payments.add(new ru.misterparser.paymentrobot.domain.Sms("filename", "sheetname", 2, parseDate("2017-11-15"), "ECMC5478 07:24 перевод 232.00р от НАТАЛЬЯ ВИКТОРОВНА Д.").toPayment());
            payments.add(new ru.misterparser.paymentrobot.domain.Sms("filename", "sheetname", 3, parseDate("2017-11-14"), "ECMC5478 07:24 перевод 232.00р от НАТАЛЬЯ ВИКТОРОВНА Д.").toPayment());
        }

        int foundDebtCount = 0;
        List<FindResult> findResults = new ArrayList<>();

        for (Debt debt : debts) {
            log.debug("Обработка: " + debt);
//            if (!debt.getCustomerId().equalsIgnoreCase("38629")) {
//                continue;
//            }
            Set<Payment> ps = new LinkedHashSet<>();
            Map<Payment, Reason> reasonMap = new LinkedHashMap<>();
            if (Configuration.get().NAME_AND_SUM) {
                for (String paymentName : debt.getPaymentNames()) {
                    for (Payment payment : payments) {
                        NameMatchEnum nameMatchEnum = nameComparator.equals(paymentName, payment.getName());
                        if (nameMatchEnum == NameMatchEnum.MATCH) {
                            ps.add(payment);
                            reasonMap.putIfAbsent(payment, Reason.NAME_AND_SUM);
                        }
                    }
                }
            }
            if (Configuration.get().CARD_AND_SUM) {
                for (String paymentCard : debt.getPaymentCards()) {
                    for (Payment payment : payments) {
                        if (cardComparator.equals(paymentCard, payment.getCard())) {
                            ps.add(payment);
                            reasonMap.putIfAbsent(payment, Reason.CARD_AND_SUM);
                        }
                    }
                }
            }
            if (Configuration.get().ONLY_SUM) {
                for (Payment payment : payments) {
                    ps.add(payment);
                    reasonMap.putIfAbsent(payment, Reason.ONLY_SUM);
                }
            }
            Set<TreeSet<Payment>> paymentLists = new LinkedHashSet<>();
            {
                if (Configuration.get().SUM_COUNT == SumCount.BY_1) {
                    log.debug("Создание " + ps.size() + " комбинаций...");
                    for (Payment p : ps) {
                        paymentLists.add(new TreeSet<>(Arrays.asList(p)));
                    }
                } else if (Configuration.get().SUM_COUNT == SumCount.BY_2) {
                    log.debug("Создание " + ps.size() * ps.size() + " комбинаций...");
                    for (Payment p1 : ps) {
                        for (Payment p2 : ps) {
                            paymentLists.add(new TreeSet<>(Arrays.asList(p1, p2)));
                        }
                    }
                } else if (Configuration.get().SUM_COUNT == SumCount.BY_3) {
                    log.debug("Создание " + ps.size() * ps.size() * ps.size() + " комбинаций...");
                    for (Payment p1 : ps) {
                        for (Payment p2 : ps) {
                            for (Payment p3 : ps) {
                                paymentLists.add(new TreeSet<>(Arrays.asList(p1, p2, p3)));
                            }
                        }
                    }
                }
            }
            log.debug("Будет обработано " + paymentLists.size() + " комбинаций");
            AtomicLong count = new AtomicLong();
            findResults.addAll(paymentLists.parallelStream().map(paymentList -> {
                        if (count.incrementAndGet() % 10000 == 0) {
                            log.debug("Обработаны очередные 10000 комбинаций...");
                        }
                        BigDecimal s = new BigDecimal(0);
                        for (Payment p : paymentList) {
                            s = s.add(p.getSum());
                        }
                        //System.out.println(paymentList.iterator().next() + " " + s + " " + debt.getTotalDebt() + " " + s.subtract(debt.getTotalDebt()).abs());
                        BigDecimal inaccuracy = Configuration.get().INACCURACY ? Configuration.get().INACCURACY_VALUE : new BigDecimal(0);
                        if (s.subtract(debt.getTotalDebt()).abs().compareTo(inaccuracy) <= 0 && dateEquals(paymentList, debt)) {
                            String reasons = reasonMap.entrySet().stream().filter(e -> paymentList.contains(e.getKey())).map(Map.Entry::getValue).map(Reason::getDescription).distinct().collect(Collectors.joining(", "));
                            return new FindResult(debt, false, reasons, new ArrayList<>(paymentList));
                        } else {
                            return null;
                        }
                    }
            ).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        for (FindResult findResult : findResults) {
            log.debug("Найдено совпадение: " + findResult);
            eventProcessor.find(new Fit(findResult.getPayments(), findResult.getDebt(), findResult.isDanger(), findResult.getReason()));
            foundDebtCount++;
        }
        return foundDebtCount;
    }

    private Date parseDate(String s) {
        try {
            return DateUtils.parseDate(s, new String[]{"yyyy-MM-dd"});
        } catch (ParseException e) {
            log.debug("ParseException", e);
        }
        return null;
    }

    private boolean dateEquals(TreeSet<Payment> paymentList, Debt debt) {
        if (debt.getPaymentDates() == null || debt.getPaymentDates().size() == 0) {
            return false;
        }
        for (Payment payment : paymentList) {
            for (Date d : debt.getPaymentDates()) {
                d = DateUtils.truncate(d, Calendar.DAY_OF_MONTH);
                Date p = payment.getDate();
                p = DateUtils.truncate(p, Calendar.DAY_OF_MONTH);
                long delta = Configuration.get().DATE_INACCURACY ? DateUtils.MILLIS_PER_DAY : 0;
                if (Math.abs(p.getTime() - d.getTime()) <= delta) {
                    return true;
                }
            }
        }
        return false;
    }

    private class CardComparator {
        public boolean equals(String o1, String o2) {
            if (o1 == null || o2 == null) {
                return false;
            }
            o1 = o1.replaceAll("[^0-9]", "");
            o2 = o2.replaceAll("[^0-9]", "");
            o1 = StringUtils.substring(o1, -4);
            o2 = StringUtils.substring(o2, -4);
            return o1.equals(o2);
        }
    }
}
