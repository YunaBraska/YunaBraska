import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public class SegmentGroup {

    public static final DateFormatType[] TIME_FORMATS = {
            DateFormatType.HOUR_24,
            DateFormatType.MINUTES,
            DateFormatType.SECONDS,
            DateFormatType.NANOSECONDS,
    };

    private int year = 1970;
    private int month = 1;
    private int day = 1;
    private int minute = 0;
    private int hour = 0;
    private int seconds = 0;
    private int nanoSeconds = 0;
    private int offset = 0;

    private String timeZonePattern = null;
    private int indexAmPm = -1;
    private int indexDate = -1;
    private int indexTime = -1;
    private int indexTimeZone = -1;
    private int indexTimeZonePrefix = -1;
    private final boolean yearIndexLast;
    private final List<CharSegment[]> groups = new ArrayList<>();
    public static List<String> TIME_INDICATORS = initTimeIndicators();

    public SegmentGroup(final boolean yearIndexLast) {
        this.yearIndexLast = yearIndexLast;
    }

    public void add(final List<CharSegment> group) {
        add(group.toArray(CharSegment[]::new));
    }

    public void add(final CharSegment[] group) {
        //TODO: RESOLVE method to do calculations as AM/PM information can come afterwards
        this.groups.add(group);
    }

    public SegmentGroup resolve() {
        final CharSegment[] empty = new CharSegment[0];
        for (int i = 0; i < groups.size(); i++) {
            final var prevGroup = i - 1 > -1 ? groups.get(i - 1) : empty;
            final var nextGroup = i + 1 < groups.size() ? groups.get(i + 1) : empty;
            final var group = groups.get(i);
            if (!hasDate() && isTimeIndicator(nextGroup)) {
                indexDate = i;
                convertGroupToDate(group);
            } else if (!hasTime() && isTimeIndicator(prevGroup) || isAmPm(nextGroup)) {
                indexTime = i;
                indexAmPm = isAmPm(nextGroup) ? i + 1 : indexAmPm;
                convertGroupToTime(group);
            } else if (i + 1 >= groups.size() && isTimeZoneIndicator(prevGroup)) {
                indexTimeZone = i;
                if (isTimeZoneIndicator(prevGroup)) {
                    indexTimeZonePrefix = i - 1;
                    prevGroup[0].type(DateFormatType.TIMEZONE_AGGREGATOR);
                }
                convertGroupToTimeZone(group);
            }

        }
        return this;
    }

    private static boolean isTimeIndicator(final CharSegment[] group) {
        return group.length == 1 && group[0].isTimeSeparator();
    }

    private static boolean isAmPm(final CharSegment[] group) {
        return group.length == 1 && group[0].isAmPM();
    }

    private static boolean isTimeZoneIndicator(final CharSegment[] group) {
        if (group.length == 1 && group[0].length() == 1) {
            final var c = group[0].chars()[0];
            return c == '+' || c == '-';
        }
        return false;
    }

    public boolean hasDate() {
        return indexDate != -1;
    }

    public boolean hasTime() {
        return indexTime != -1;
    }

    public boolean hasTimeZone() {
        return indexTimeZone != -1;
    }

    public boolean hasTimeZonePrefix() {
        return indexTimeZonePrefix != -1;
    }

    public boolean hasAmPm() {
        return indexAmPm != -1;
    }

    public boolean yearIndexLast() {
        return yearIndexLast;
    }

    public boolean yearTimeZonePattern() {
        return timeZonePattern != null;
    }

    private void convertGroupToDate(final CharSegment[] group) {
        final CharSegment[] digits = stream(group).filter(CharSegment::isDigit).toArray(CharSegment[]::new);
        //SET YEAR
        final int indexYear;
        final int indexLast = digits.length;
        if (indexLast > 1 && digits[0].length() != digits[indexLast - 1].length()) {
            indexYear = digits[0].length() > digits[indexLast - 1].length() ? 0 : indexLast - 1;
        } else {
            indexYear = IntStream.range(0, indexLast)
                    .reduce((a, b) -> digits[a].length() < digits[b].length() ? b : a)
                    .orElse(yearIndexLast ? indexLast - 1 : 0);
        }

        //SET YEAR
        digits[indexYear].type(DateFormatType.YEAR);
        year = Integer.parseInt(new String(digits[indexYear].chars()));

        //FIXME: do more dynamic?
        //SET MONTH
        if (indexLast > 1) {
            digits[1].type(DateFormatType.MONTH_OF_YEAR);
            month = Integer.parseInt(new String(digits[1].chars()));
        }
        //SET DAY
        if (indexLast > 2) {
            digits[indexYear == 0 ? indexLast - 1 : 0].type(DateFormatType.DAY_OF_WEEK);
            day = Integer.parseInt(new String(digits[indexYear == 0 ? indexLast - 1 : 0].chars()));
        }
    }

    private void convertGroupToTime(final CharSegment[] group) {
        final CharSegment[] digits = stream(group).filter(CharSegment::isDigit).toArray(CharSegment[]::new);
        //FIXME: more flexible && nuber checks
        for (int i = 0; i < TIME_FORMATS.length && i < digits.length; i++) {
            //TODO fallback am_pm
            digits[i].type(i == 0 && hasAmPm() ? DateFormatType.HOUR_12 : TIME_FORMATS[i]);
        }
    }

    private void convertGroupToTimeZone(final CharSegment[] group) {
        //EST     (z)
        //Eastern Standard Time (zzzz)
        timeZonePattern = "X";
        final CharSegment[] digits = stream(group).filter(CharSegment::isDigit).map(cs -> {
            cs.type(DateFormatType.TIMEZONE_UNKNOWN);
            return cs;
        }).toArray(CharSegment[]::new);
        if (digits.length < group.length) {
            timeZonePattern = "XXX"; //05:00
        } else if (digits.length == 4 && digits.length == group.length) {
            timeZonePattern = "XX"; //0500 (Z)
        } else if (digits.length == 2 && digits.length == group.length) {
            timeZonePattern = "X"; //05
        }
    }

    public OffsetDateTime toOffsetDateTime() {
        final Map<DateFormatType, List<CharSegment>> typeMap = groups.stream().flatMap(Arrays::stream).filter(charSegment -> charSegment.type() != DateFormatType.UNKNOWN).filter(charSegment -> charSegment.type() != DateFormatType.IGNORED).collect(Collectors.groupingBy(CharSegment::type));
        return OffsetDateTime.of(
                //TODO: default time now or 1970?
                toInt(typeMap.get(DateFormatType.YEAR), 1970),
                toInt(typeMap.get(DateFormatType.MONTH_OF_YEAR), 1),
                toInt(typeMap.get(DateFormatType.DAY_OF_WEEK), 1),
                //TODO: what to do on 12H format? 
                toInt(typeMap.getOrDefault(DateFormatType.HOUR_12, typeMap.get(DateFormatType.HOUR_24)), 0),
                toInt(typeMap.get(DateFormatType.MINUTES), 0),
                toInt(typeMap.get(DateFormatType.SECONDS), 0),
                toInt(typeMap.get(DateFormatType.NANOSECONDS), 0),
                //ZonedDateTime.now(ZoneId.of(new String(segment.chars()))).getOffset()
                ZoneOffset.ofHours(charListToString(typeMap.get(DateFormatType.TIMEZONE_UNKNOWN)).map(ts -> Integer.parseInt(charListToString(typeMap.get(DateFormatType.TIMEZONE_AGGREGATOR)).orElse("") + ts)).orElse(0))
        );
    }

    private int toInt(final List<CharSegment> cs, final int fallback) {
        return ofNullable(cs).flatMap(this::charListToString).map(Integer::parseInt).orElse(fallback);
    }

    private Optional<String> charListToString(final List<CharSegment> cs) {
        return ofNullable(cs).map(c -> new String(c.iterator().next().chars()));
    }

    //Date:
    // * 100% if before 'T'
    // * 3 digits || 5 Segments
    // * Separator in ['/','-']
    // * digit length of first || last == 4
    // * digit length of all digits == 2
    // * is first digit group
    //Time:
    // * 100% if after 'T' (single group)
    // * 100% if next is one of [timeIndicators] !!! (segment starts with) !!! can be seperated by a space
    // * 100% 2 digits && separated by 'h'
    // * 100% 2 digits && separated by 'H'
    // * > 3 digits || > 5 Segments
    // * Separator in [':']
    // * digit length of all digits == 2
    // Timezone
    // * 100% if after '+' (single group)
    // * 100% if after '-' (single group)

    private static List<String> initTimeIndicators() {
        return stream(new String[]{
                "h",
                "uhr",
                "uur",
                "hour",
                "horas",
                "heures",
                "p.m.",
                "a.m.",
                "p.m",
                "a.m",
                "pm",
                "am",
                "τμ",
                "μμ",
                "下午4点",
                "時",
                "点",
                "时00分",
                "الساعة",
                "مساءً",
                "الرابعة عصرا",
                "시",
                "час",
                "下午",
                "午後",
                "낮",
                "낮",
                "ทุ่ม",
                "น.",
                "오후",
                "후",
                "t",
                "Ч"
        }).map(String::toUpperCase).collect(Collectors.toList());
    }
}
