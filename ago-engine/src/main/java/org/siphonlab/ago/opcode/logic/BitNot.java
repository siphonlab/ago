package org.siphonlab.ago.opcode.logic;

public class BitNot {
    public static final int KIND_BITNOT = 0x27_000000;
    public static final int OP                  = 0x27;

    public static final int bnot_vv = 0x27_00_01_02;

    public static final int bnot_i_vv= 0x27_0a_01_02;

    public static final int bnot_b_vv = 0x27_08_01_02;

    public static final int bnot_s_vv = 0x27_09_01_02;

    public static final int bnot_l_vv = 0x27_0b_01_02;


    public static String getName(int code){
        return switch(code){
            case bnot_vv -> "bnot_vv";
            case bnot_i_vv -> "bnot_i_vv";
            case bnot_b_vv -> "bnot_b_vv";
            case bnot_s_vv -> "bnot_s_vv";
            case bnot_l_vv -> "bnot_l_vv";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
