import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

class DateTimeReaderTest {

    private static final ZonedDateTime EXAMPLE_TIME = ZonedDateTime.of(1988, 8, 8, 7, 10, 15, 30, ZoneId.of("Europe/Berlin"));

    private static final String[] DATE_SEPARATORS = {
            " ",
            "/",
            "\\",
            ".",
            "-",
            ","
    };

    private static final String[] TIME_SEPARATORS = {
            " ",
            ":",
            ".",
            ",",
    };

    private static final String[] DATE_TIME_SEPARATORS = {
            " ",
            "'T'",
    };

    @Test
    void readAllDateFormats() {
        //TODO: INPUTS
        //  * Locale
        //  * DeepTypeSearch
        //  * Fallback AM/PM
        //  * Fallback TIMEZONE
        //TODO: find separator
        //TODO: DETECT AM/PM (a)
        //TODO: DETECT AD/BC (G)
        //TODO: DETECT Week Days
        //TODO: DETECT Months
        //FIXME:
        // * NO [u] = Day number of week (1 = Monday, ..., 7 = Sunday)
        // * NO [d] = Day in month
        // * NO [w] = Week in year
        // * NO [W] = Week in month
        // * NO [D] = Day in year
        // * NO [F] = Day of week in month
        // * NO [E] = Day name in week

        //CONFIG
        final boolean deepTypeSearch = true;
        final boolean yearIndexLast = true;

        long duration = System.currentTimeMillis();
        final Map<String[], OffsetDateTime> dateFormats = generateDateFormats();
        dateFormats.entrySet().forEach(entry -> {
            //2023-02-21T11:38:09.561+01:00
            //d/MMM/y H.mm.s.SXX 8/Aug/1988 7.10.15.0+0200
            //TODO: Group by separator (start + middle + middle + end)
            //TODO: TIMEZONES [ISO, RFC, GENERAL]
            //TODO: 12/24 (AM/PM? 12 : 24) => (!AM/PM && hour > 12? 24 : fallback_12_24)
            //TODO: parse also human times like "16h", "16:00h" "16:00Uhr"

            //FIXME: Arrays for index access and List for loops

            final SegmentGroup groupedSegments = groupSegments(
                    entry.getKey()[1].toCharArray(),
                    deepTypeSearch,
                    yearIndexLast
            ).resolve();
            final var dateTime = groupedSegments.toOffsetDateTime();
//            assertThat(dateTime, is(equalTo(entry.getValue())));
        });
        duration = System.currentTimeMillis() - duration;
        System.out.printf("duration [%s ms], entries [%s] avg [%s ms]%n", duration, dateFormats.size(), duration / dateFormats.size());
    }

    private static SegmentGroup groupSegments(
            final char[] chars,
            final boolean deepTypeSearch,
            final boolean yearIndexLast
    ) {
//        final List<CharSegment> things = new ArrayList<>();
//        int i = 0;
//        while (i < chars.length) {
//            i = add(chars, i, things, deepTypeSearch, Character::isDigit);
//            i = add(chars, i, things, deepTypeSearch, Character::isLetter);
//            i = add(chars, i, things, deepTypeSearch, c -> !Character.isLetterOrDigit(c));
//        }
        int i = 0;
        final var result = new SegmentGroup(yearIndexLast);
        final var group = new ArrayList<CharSegment>();
        var prevSeparator = new char[0];
        final Function<Character, Boolean> delimiterType = c -> !Character.isLetterOrDigit(c);
        Function<Character, Boolean> currType;
        Function<Character, Boolean> prevType = null;
        CharSegment currSegment;
        while (i < chars.length) {
            currType = Character.isDigit(chars[i]) ? Character::isDigit : (Character.isLetter(chars[i]) ? Character::isLetter : delimiterType);
            currSegment = groupChars(chars, i, currType, deepTypeSearch);
            //RESET GROUP (ON TYPE SWITCH) || (ON SEPARATOR CHANGE)
            if (isDelimiterChangeOrTypeChange(prevSeparator, delimiterType, currType, prevType, currSegment)) {
                result.add(group);
                group.clear();
            }

            //SET
            if (currType.equals(delimiterType)) {
                prevSeparator = currSegment.chars();
            } else {
                //RESET SEPARATOR (ON TYPE SWITCH)
                if (!currType.equals(prevType)) {
                    prevSeparator = new char[0];
                }
                prevType = currType;
            }
            group.add(currSegment);
            //RESET (SPECIAL SEGMENTS NEED A RESET)
            //Special case to produce a group with a single CharSegment
            if (isEndingGroupSegment(currSegment)) {
                result.add(group);
                group.clear();
            }
            i = i + currSegment.length();
        }
        result.add(group);
        return result;
    }

    private static boolean isEndingGroupSegment(final CharSegment currSegment) {
        return currSegment.type() == DateFormatType.AM_PM || currSegment.type() == DateFormatType.ERA;
    }

    private static boolean isDelimiterChangeOrTypeChange(final char[] prevSeparator, final Function<Character, Boolean> delimiterType, final Function<Character, Boolean> currType, final Function<Character, Boolean> prevType, final CharSegment currSegment) {
        return (prevType != null && !currType.equals(delimiterType) && !currType.equals(prevType))
                || (prevSeparator.length != 0 && currType.equals(delimiterType) && !Arrays.equals(currSegment.chars(), prevSeparator));
    }

    private static CharSegment groupChars(final char[] chars, int i, final Function<Character, Boolean> charFilter, final boolean deepTypeSearch) {
        final var start = i;
        while (i < chars.length && charFilter.apply(chars[i])) {
            i++;
        }
        return new CharSegment(Arrays.copyOfRange(chars, start, i), deepTypeSearch);
    }

//    private static int add(final char[] chars, int i, final List<CharSegment> things, final boolean deepTypeSearch, final Function<Character, Boolean> filter) {
//        if (i < chars.length && filter.apply(chars[i])) {
//            final var start = i;
//            while (i < chars.length && filter.apply(chars[i])) {
//                i++;
//            }
//            things.add(new CharSegment(Arrays.copyOfRange(chars, start, i), deepTypeSearch));
//        }
//        return i;
//    }
//
//    private static boolean isTimeSeparator(final char[] chars, final int start, final int end, final char[] toBeAdded) {
//        return (start != 0 && end != chars.length && toBeAdded.length == 1 && toBeAdded[0] == 'T' && !Character.isLetter(chars[start - 1]) && !Character.isLetter(chars[end + 1]));
//    }


    private Map<String[], OffsetDateTime> generateDateFormats() {
        final Map<String[], OffsetDateTime> dateFormats = new HashMap<>();
        for (String dateSep : DATE_SEPARATORS) {
            for (String timeSep : TIME_SEPARATORS) {
                for (String dataTimeSep : DATE_TIME_SEPARATORS) {
                    for (String amPm : new String[]{"", "a"}) {
                        for (int d = 1; d != 3; d++) {
                            final var day = IntStream.range(0, d).mapToObj(x -> "d").collect(Collectors.joining());
                            for (int m = 1; m != 4; m++) {
                                final var month = IntStream.range(0, m).mapToObj(x -> "M").collect(Collectors.joining());
                                for (int y = 1; y != 4; y++) {
                                    final var year = IntStream.range(0, y).mapToObj(x -> "y").collect(Collectors.joining());
                                    for (int h = 1; h != 3; h++) {
                                        final var hour = IntStream.range(0, h).mapToObj(x -> "H").collect(Collectors.joining());
                                        for (int mi = 1; mi != 3; mi++) {
                                            final var minutes = IntStream.range(0, mi).mapToObj(x -> "m").collect(Collectors.joining());
                                            for (int s = 0; s != 2; s++) {
                                                final var seconds = s == 0 ? "" : IntStream.range(0, s).mapToObj(x -> "s").collect(Collectors.joining());
                                                for (int ms = 0; ms != 4; ms++) {
                                                    final var milliseconds = s == 0 || ms == 0 ? "" : IntStream.range(0, ms).mapToObj(x -> "S").collect(Collectors.joining());
                                                    final var yyyy_MM_dd_HH_mm_ss = new StringBuilder();
                                                    yyyy_MM_dd_HH_mm_ss.append(year).append(dateSep).append(month).append(dateSep).append(day);
                                                    addToMap(dateFormats, yyyy_MM_dd_HH_mm_ss.toString());
                                                    yyyy_MM_dd_HH_mm_ss.append(dataTimeSep);
                                                    yyyy_MM_dd_HH_mm_ss.append(hour).append(timeSep).append(minutes).append(timeSep).append(seconds);
                                                    optString(milliseconds).ifPresent(milli -> yyyy_MM_dd_HH_mm_ss.append(timeSep).append(milli));
                                                    addToMap(dateFormats, yyyy_MM_dd_HH_mm_ss.toString());

                                                    final var dd_MM_yyyy_HH_mm_ss = new StringBuilder();
                                                    dd_MM_yyyy_HH_mm_ss.append(day).append(dateSep).append(month).append(dateSep).append(year);
                                                    addToMap(dateFormats, dd_MM_yyyy_HH_mm_ss.toString());
                                                    dd_MM_yyyy_HH_mm_ss.append(dataTimeSep);
                                                    dd_MM_yyyy_HH_mm_ss.append(hour).append(timeSep).append(minutes).append(timeSep).append(seconds);
                                                    optString(milliseconds).ifPresent(milli -> dd_MM_yyyy_HH_mm_ss.append(timeSep).append(milli));
                                                    optString(amPm).ifPresent(dd_MM_yyyy_HH_mm_ss::append);
                                                    addToMap(dateFormats, dd_MM_yyyy_HH_mm_ss.toString());

                                                    //TIMEZONE GENERAL
                                                    for (int z = 0; z != 4; z++) {
                                                        final var timezone_GTZ = s == 0 ? "" : IntStream.range(0, z).mapToObj(x -> "z").collect(Collectors.joining());
                                                        addToMap(dateFormats, yyyy_MM_dd_HH_mm_ss + timezone_GTZ);
                                                        addToMap(dateFormats, dd_MM_yyyy_HH_mm_ss + timezone_GTZ);
                                                    }
                                                    //TIMEZONE RFC 822
                                                    for (int z1 = 0; z1 != 4; z1++) {
                                                        final var timezone_822 = s == 0 ? "" : IntStream.range(0, z1).mapToObj(x -> "Z").collect(Collectors.joining());
                                                        addToMap(dateFormats, yyyy_MM_dd_HH_mm_ss + timezone_822);
                                                        addToMap(dateFormats, dd_MM_yyyy_HH_mm_ss + timezone_822);
                                                    }

                                                    //TIMEZONE ISO 8601
                                                    for (int z2 = 0; z2 != 4; z2++) {
                                                        final var timezone_8601 = s == 0 ? "" : IntStream.range(0, z2).mapToObj(x -> "X").collect(Collectors.joining());
                                                        addToMap(dateFormats, yyyy_MM_dd_HH_mm_ss + timezone_8601);
                                                        addToMap(dateFormats, dd_MM_yyyy_HH_mm_ss + timezone_8601);
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return dateFormats;
    }

    private void addToMap(final Map<String[], OffsetDateTime> map, final String pattern) {
        map.put(new String[]{pattern, EXAMPLE_TIME.format(DateTimeFormatter.ofPattern(pattern))}, EXAMPLE_TIME.toOffsetDateTime());
    }

    private static Optional<String> optString(final String string) {
        return string != null && !string.isBlank() && !string.isBlank() ? Optional.of(string) : Optional.empty();
    }
}
