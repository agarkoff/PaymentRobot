package ru.misterparser.paymentrobot.namecomparators;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Pair;
import ru.misterparser.common.TextUtils;

import java.io.IOException;
import java.util.*;

@Log4j2
public class NewNameComparator {

    private static Set<String> names;
    private static Set<String> patronymics;

    static {
        try {
            names = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(NewNameComparator.class.getResourceAsStream("/names.csv"), "UTF-8"), "\n\r\t, ")));
            patronymics = new TreeSet<>(Arrays.asList(StringUtils.split(IOUtils.toString(NewNameComparator.class.getResourceAsStream("/patronymics.csv"), "UTF-8"), "\n\r\t, ")));
        } catch (IOException e) {
            log.debug("IOException", e);
        }
    }

    public NameMatchEnum equals(String fio1, String fio2) {
        fio1 = StringUtils.strip(fio1, ".");
        fio2 = StringUtils.strip(fio2, ".");
        fio1 = TextUtils.changeEnglishToRussian(fio1);
        fio2 = TextUtils.changeEnglishToRussian(fio2);
        if (StringUtils.isBlank(fio1) || StringUtils.isBlank(fio2)) {
            return NameMatchEnum.NOT_MATCH;
        }
        fio1 = StringUtils.lowerCase(fio1);
        fio2 = StringUtils.lowerCase(fio2);
        fio1 = StringUtils.replace(fio1, "ё", "е");
        fio2 = StringUtils.replace(fio2, "ё", "е");
        if (StringUtils.equalsIgnoreCase(fio1, fio2)) {
            log.debug("Имена " + fio1 + " и " + fio2 + " считаем одинаковыми");
            return NameMatchEnum.MATCH;
        }
        List<String> strings1 = new ArrayList<>(Arrays.asList(StringUtils.split(fio1, ":., ")));
        List<String> strings2 = new ArrayList<>(Arrays.asList(StringUtils.split(fio2, ":., ")));
        String name1 = getName(strings1);
        String name2 = getName(strings2);
        if (name1 == null || name2 == null) {
            return NameMatchEnum.NOT_MATCH;
        }
        String patronymic1 = getPatronymic(strings1);
        String patronymic2 = getPatronymic(strings2);
        if (patronymic1 == null || patronymic2 == null) {
            return NameMatchEnum.NOT_MATCH;
        }
        String family1 = strings1.size() > 0 ? strings1.get(0) : null;
        String family2 = strings2.size() > 0 ? strings2.get(0) : null;
        if (StringUtils.equalsIgnoreCase(name1, name2) &&
                StringUtils.equalsIgnoreCase(patronymic1, patronymic2) &&
                (family1 == null || family2 == null)) {
            log.debug("Не хватает фамилии у: " + fio1 + " и " + fio2);
            return NameMatchEnum.PARTIALLY_MATCH;
        }
        if (StringUtils.equalsIgnoreCase(name1, name2) &&
                StringUtils.equalsIgnoreCase(patronymic1, patronymic2) &&
                family1 != null && family2 != null &&
                StringUtils.equalsIgnoreCase(family1.substring(0, 1), family2.substring(0, 1))) {
            return NameMatchEnum.MATCH;
        }
        return NameMatchEnum.NOT_MATCH;
    }

    private String getName(Collection<String> strings) {
        for (String s : strings) {
            if (names.contains(s)) {
                strings.remove(s);
                return s;
            }
        }
        return null;
    }

    private String getPatronymic(Collection<String> strings) {
        for (String s : strings) {
            if (patronymics.contains(s)) {
                strings.remove(s);
                return s;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        NewNameComparator nameComparator = new NewNameComparator();
        List<Pair<String, String>> pairs = new ArrayList<>(Arrays.asList(
                new Pair<>("МАРИНА ВАЛЕНТИНОВНА Б.", "МАРИНА ВЛАДИМИРОВНА Б."),
                new Pair<>("МАРИНА ВЛАДИМИРОВНА Б.", "МАРИНА ВЛАДИМИРОВНА Белова"),
                new Pair<>("МАРИНА ВЛАДИМИРОВНА E.", "МАРИНА ВЛАДИМИРОВНА Б.")
                ));
        for (Pair<String, String> pair : pairs) {
            System.out.println(pair + " = " + nameComparator.equals(pair.getValue0(), pair.getValue1()));
        }
    }
}
