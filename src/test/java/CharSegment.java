import java.text.DateFormatSymbols;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class CharSegment {
    public static final char[] AM = {'A', 'M'};
    public static final char[] PM = {'P', 'M'};
    private DateFormatType type;
    private final char[] chars;
    private final int length;
    private int value;

    private static final Map<Integer, Collection<CharSegment>> CHAR_SEGMENTS = initCharTypes();

    /**
     * @param chars          charArray
     * @param deepTypeSearch TypeSearch dos function only on letters: `true`= search through approx 40k strings to get the right type.<br/> `false`= search through approx 10 strings to get the right type.
     */
    public CharSegment(final char[] chars, final boolean deepTypeSearch) {
        this.type = parseType(chars, deepTypeSearch);
        this.chars = chars;
        this.length = chars.length;
    }

    public CharSegment(final DateFormatType type, final char[] array) {
        this.type = type;
        this.chars = array;
        this.length = chars.length;
    }

    public DateFormatType type() {
        return type;
    }

    public void type(final DateFormatType type) {
        this.type = type;
    }

    public char[] chars() {
        return chars;
    }

    public int length() {
        return length;
    }

    public int value() {
        return value;
    }

    public CharSegment value(final int value) {
        this.value = value;
        return this;
    }

    public boolean isTimeSeparator() {
        if (type != DateFormatType.TIME_SEPARATOR && (chars.length == 1 && (chars[0] == 'T' || chars[0] == 't'))) {
            type = DateFormatType.TIME_SEPARATOR;
        }
        return type == DateFormatType.TIME_SEPARATOR;
    }

    public boolean isAmPM() {
        if (type != DateFormatType.AM_PM && (chars.length == 2 && (Arrays.equals(chars, AM) || Arrays.equals(chars, PM)))) {
            type = DateFormatType.AM_PM;
        }
        return type == DateFormatType.AM_PM;
    }

    public boolean isDigit() {
        return type == DateFormatType.DIGIT
                || type == DateFormatType.HOUR_12
                || type == DateFormatType.HOUR_24
                || type == DateFormatType.MINUTES
                || type == DateFormatType.SECONDS
                || type == DateFormatType.NANOSECONDS;
    }

    private static Map<Integer, Collection<CharSegment>> initCharTypes() {
        final Map<Integer, Collection<CharSegment>> result = new HashMap<>();
        final DateFormatSymbols dfs = new DateFormatSymbols(Locale.ENGLISH);
        final Predicate<char[]> nonEmptyArray = a -> a.length > 0;

        addToMap(result, dfs.getWeekdays(), DateFormatType.DAY_OF_WEEK);
        addToMap(result, dfs.getShortWeekdays(), DateFormatType.DAY_OF_WEEK);
        addToMap(result, dfs.getMonths(), DateFormatType.MONTH_OF_YEAR);
        addToMap(result, dfs.getShortMonths(), DateFormatType.MONTH_OF_YEAR);

        result.put(1, List.of(new CharSegment(DateFormatType.TIME_SEPARATOR, new char[]{'T'})));
        stream(dfs.getZoneStrings())
                .flatMap(Arrays::stream).map(String::toCharArray).filter(nonEmptyArray)
                .map(chars -> new CharSegment(DateFormatType.TIMEZONE_UNKNOWN, chars))
                .forEach(charSegment -> addToMap(result, charSegment));
        stream(dfs.getAmPmStrings())
                .map(String::toCharArray)
                .filter(nonEmptyArray)
                .map(CharSegment::toUpperChars)
                .map(chars -> new CharSegment(DateFormatType.AM_PM, chars))
                .forEach(charSegment -> addToMap(result, charSegment));
        stream(dfs.getEras())
                .map(String::toCharArray).
                filter(nonEmptyArray)
                .map(CharSegment::toUpperChars)
                .map(chars -> new CharSegment(DateFormatType.ERA, chars))
                .forEach(charSegment -> addToMap(result, charSegment));
        return result;
    }

    private static void addToMap(final Map<Integer, Collection<CharSegment>> result, final String[] array, final DateFormatType type) {
        for (int i = 0; i < array.length; i++) {
            final int index = i;
            Optional.of(array[i])
                    .map(String::toCharArray)
                    .filter(a -> a.length > 0)
                    .map(chars -> new CharSegment(type, chars))
                    .ifPresent(segment -> {
                        segment.value(index);
                        addToMap(result, segment);
                    });
        }
    }

    private static void addZonesToMap(final Map<Integer, Collection<CharSegment>> result, final String[] array, final DateFormatType type) {
        for (int i = 0; i < array.length; i++) {
            Optional.of(array[i])
                    .map(String::toCharArray)
                    .filter(a -> a.length > 0)
                    .map(chars -> new CharSegment(type, chars))
                    .ifPresent(segment -> {
                        segment.value();
                        addToMap(result, segment);
                    });
        }
    }

    private static void addToMap(final Map<Integer, Collection<CharSegment>> stringTypeMap, final CharSegment charSegment) {
        final Collection<CharSegment> item = stringTypeMap.getOrDefault(charSegment.length(), new ArrayList<>());
        item.add(charSegment);
        stringTypeMap.put(charSegment.length(), item);
    }

    public static char[] toUpperChars(final char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = Character.toUpperCase(chars[i]);
        }
        return chars;
    }


    private static DateFormatType parseType(final char[] chars, final boolean deepTypeSearch) {
        var type = DateFormatType.UNKNOWN;
        if (chars.length > 0) {
            if (Character.isDigit(chars[0])) {
                type = DateFormatType.DIGIT;
            } else if (Character.isLetter(chars[0])) {
                type = findType(CharSegment.toUpperChars(chars), deepTypeSearch);
            }
        }
        return type;
    }

    private static DateFormatType findType(final char[] array, final boolean deepTypeSearch) {
        return (deepTypeSearch || array.length < 3)
                ? CHAR_SEGMENTS.get(array.length).stream()
                .filter(charSegment -> Arrays.equals(charSegment.chars(), array))
                .map(charSegment -> charSegment.type)
                .findFirst().orElse(DateFormatType.UNKNOWN)
                : DateFormatType.UNKNOWN;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CharSegment that = (CharSegment) o;
        return Arrays.equals(chars, that.chars);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(chars);
    }

    @Override
    public String toString() {
        return "CharType{" +
                "type=" + type +
                ", string=" + Arrays.toString(chars) +
                '}';
    }
}
