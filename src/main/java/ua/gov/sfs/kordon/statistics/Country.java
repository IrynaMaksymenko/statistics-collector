package ua.gov.sfs.kordon.statistics;

public enum Country {

    BY("Білорусь"),
    HU("Угорщина"),
    MD("Молдова"),
    PL("Польща"),
    RO("Румунія"),
    RU("Росія"),
    SK("Словаччина");

    private final String readableName;

    Country(final String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }
}
