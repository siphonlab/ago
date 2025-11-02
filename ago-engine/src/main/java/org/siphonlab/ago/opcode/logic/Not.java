package org.siphonlab.ago.opcode.logic;

public class Not {
    public static final int KIND_NOT = 0x22_000000;
    public static final int OP                  = 0x22;

    public static final int not_v  = 0x22_00_01_01;
    public static final int not_vv = 0x22_00_02_02;
//    public static final int not_vov = 0x20_04_02_03;


    public static String getName(int code){
        return switch(code){
            case not_v -> "not_v";
            case not_vv -> "not_vv";
//            case not_vov -> "not_vov";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
