package de.lheinrich.lhdef;

public enum ECKeySize {

    LOWEST(160), LOW(224), MEDIUM(256), HIGH(384), HIGHEST(521);

    private final int size;

    ECKeySize(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }
}
