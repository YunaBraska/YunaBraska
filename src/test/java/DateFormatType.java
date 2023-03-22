public enum DateFormatType {
    //FULL LIST: https://stackoverflow.com/a/12781297
    DIGIT('u'),
    YEAR('y'),
    HOUR_24('H'),
    HOUR_12('K'),
    MINUTES('m'),
    SECONDS('s'),
    NANOSECONDS('S'),
    TIME_SEPARATOR('T'),
    AM_PM('a'),
    TIMEZONE_GTZ('z'),
    TIMEZONE_822('Z'),
    TIMEZONE_8601('X'),
    DAY_OF_WEEK('E'),
    MONTH_OF_YEAR('M'),
    ERA('G'),
    UNKNOWN('\0'),
    IGNORED('\0'),
    TIMEZONE_UNKNOWN('\0'),
    TIMEZONE_AGGREGATOR('\0'),
    ;

    private final char dateFormat;

    public static final char NULL = '\0';

    DateFormatType(final char dateFormat) {
        this.dateFormat = dateFormat;
    }

    public char dateFormat() {
        return dateFormat;
    }
}
