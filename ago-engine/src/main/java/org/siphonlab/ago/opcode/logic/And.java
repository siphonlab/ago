package org.siphonlab.ago.opcode.logic;

public class And {
    public static final int KIND_AND = 0x20_000000;
    public static final int OP                  = 0x20;

    public static final int and_vv = 0x20_04_03_02;
    public static final int and_vvv = 0x20_04_04_03;

    public static String getName(int code){
        return switch(code){
            case and_vv -> "and_vv";
            case and_vvv -> "and_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
