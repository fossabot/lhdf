package de.lheinrich.lhdef;

public enum AESKeySize {

    LOW(128, 16), MEDIUM(192, 24), HIGH(256, 32);

    private final int size;
    private final int sub;

    AESKeySize(int size, int sub) {
        this.size = size;
        this.sub = sub;
    }

    public int getSize() {
        return this.size;
    }

    public int getSub() {
        return this.sub;
    }
}
