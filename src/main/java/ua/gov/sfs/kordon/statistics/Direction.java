package ua.gov.sfs.kordon.statistics;

public enum Direction {

    I("в'їзд"),
    O("виїзд");

    private final String readableName;

    Direction(final String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }

}
