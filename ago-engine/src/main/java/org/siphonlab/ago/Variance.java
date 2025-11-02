package org.siphonlab.ago;

public enum Variance {
    Invariance(0),
    Covariance(1),
    Contravariance(2);

    final int value;
    Variance(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public byte byteValue() {
        return (byte)value;
    }

    public static Variance of(int value){
        return switch (value){
            case 0 -> Invariance;
            case 1 -> Covariance;
            case 2 -> Contravariance;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
