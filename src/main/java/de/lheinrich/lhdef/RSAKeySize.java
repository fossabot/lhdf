package de.lheinrich.lhdef;

public enum RSAKeySize {

    LOWEST(1024), LOW(2048), MEDIUM(3072), HIGH(4096), HIGHEST(8192);

    private final int size;

    private RSAKeySize(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }
}
