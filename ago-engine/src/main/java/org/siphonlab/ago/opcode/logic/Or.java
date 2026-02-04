package org.siphonlab.ago.opcode.logic;

public class Or {
    public static final int KIND_OR = 0x21_000000;
    public static final int OP                  = 0x21;

    public static final int or_vv = 0x21_04_03_02;
    public static final int or_vvv = 0x21_04_04_03;


    public static String getName(int code){
        return switch(code){
            case or_vv -> "or_vv";
            case or_vvv -> "or_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
