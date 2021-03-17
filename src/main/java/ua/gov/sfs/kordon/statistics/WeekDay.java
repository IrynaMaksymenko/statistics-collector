package ua.gov.sfs.kordon.statistics;

public enum WeekDay {

    MONDAY(1, "Понеділок"),
    TUESDAY(2, "Вівторок"),
    WEDNESDAY(3, "Середа"),
    THURSDAY(4, "Четвер"),
    FRIDAY(5, "П'ятниця"),
    SATURDAY(6, "Субота"),
    SUNDAY(7, "Неділя");

    private final int ordinal;
    private final String readableName;

    WeekDay(final int ordinal, final String readableName) {
        this.ordinal = ordinal;
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }

    public static WeekDay lookup(final int ordinal) {
        for (final WeekDay weekDay : WeekDay.values()) {
            if (weekDay.ordinal == ordinal) {
                return weekDay;
            }
        }
        throw new IllegalArgumentException();
    }

}
