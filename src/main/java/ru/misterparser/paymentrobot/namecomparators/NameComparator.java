package ru.misterparser.paymentrobot.namecomparators;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import ru.misterparser.common.TextUtils;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 05.07.16
 * Time: 18:11
 */
public class NameComparator {

    private static final Logger log = LogManager.getLogger(NameComparator.class);

    public NameMatchEnum equals(String name1, String name2) {
        name1 = StringUtils.strip(name1, ".");
        name2 = StringUtils.strip(name2, ".");
        name1 = TextUtils.changeEnglishToRussian(name1);
        name2 = TextUtils.changeEnglishToRussian(name2);
        if (StringUtils.isBlank(name1) || StringUtils.isBlank(name2)) {
            return NameMatchEnum.NOT_MATCH;
        }
        name1 = StringUtils.lowerCase(name1);
        name2 = StringUtils.lowerCase(name2);
        name1 = StringUtils.replace(name1, "ё", "е");
        name2 = StringUtils.replace(name2, "ё", "е");
        if (StringUtils.equalsIgnoreCase(name1, name2)) {
            log.debug("Имена " + name1 + " и " + name2 + " считаем одинаковыми");
            return NameMatchEnum.MATCH;
        }
        Collection<String> strings1 = new ArrayList<>(Arrays.asList(StringUtils.split(name1, ":., ")));
        Collection<String> strings2 = new ArrayList<>(Arrays.asList(StringUtils.split(name2, ":., ")));
        List<String> equals = new ArrayList<>();
        for (Iterator<String> iterator = strings1.iterator(); iterator.hasNext(); ) {
            String s1 = iterator.next();
            if (strings2.contains(s1)) {
                equals.add(s1);
                iterator.remove();
                strings2.remove(s1);
            }
        }
        if (equals.size() == 3) {
            return NameMatchEnum.MATCH; // match
        }
        if (equals.size() == 2) {
            if (strings1.size() == 0 || strings2.size() == 0) {
                log.debug("Совпадение по двум частям имени у " + name1 + " и " + name2);
                return NameMatchEnum.PARTIALLY_MATCH; // partially match
            }
            List<String> shortStrings1 = new ArrayList<>();
            List<String> shortStrings2 = new ArrayList<>();
            for (String s : strings1) {
                shortStrings1.add(s.substring(0, 1));
            }
            for (String s : strings2) {
                shortStrings2.add(s.substring(0, 1));
            }
            List<String> shortEquals = new ArrayList<>();
            for (Iterator<String> iterator = shortStrings1.iterator(); iterator.hasNext(); ) {
                String s1 = iterator.next();
                if (shortStrings2.contains(s1)) {
                    shortEquals.add(s1);
                    iterator.remove();
                    shortStrings2.remove(s1);
                }
            }
            if (shortEquals.size() > 0) {
                return NameMatchEnum.MATCH; // match
            }
        }
        return NameMatchEnum.NOT_MATCH; // not match
    }

    public static void main(String[] args) {
        NameComparator nameComparator = new NameComparator();
        List<Pair<String, String>> pairs = new ArrayList<>(Arrays.asList(
                new Pair<>("МАРИНА ВАЛЕНТИНОВНА Б.", "МАРИНА ВЛАДИМИРОВНА Б.")));
        for (Pair<String, String> pair : pairs) {
            System.out.println(pair + " = " + nameComparator.equals(pair.getValue0(), pair.getValue1()));
        }
    }
}
