package org.siphonlab.ago.opcode;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;

public class Concat {
    public static final int KIND_CONCAT = 0x0b_000000;
    public static final int OP                  = 0x0b;

    public static final int concat_S_vc     = 0x0b_03_01_02;
    public static final int concat_S_vvc    = 0x0b_03_02_03;
    public static final int concat_S_vv     = 0x0b_03_03_02;
    public static final int concat_S_vvv    = 0x0b_03_04_03;
    public static final int concat_S_vcv    = 0x0b_03_05_03;


    public static String getName(int code){
        return switch(code){
            case 0x0b_03_01_02 -> "concat_S_vc";
            case 0x0b_03_02_03 -> "concat_S_vvc";
            case 0x0b_03_03_02 -> "concat_S_vv";
            case 0x0b_03_04_03 -> "concat_S_vvv";
            case 0x0b_03_05_03 -> "concat_S_vcv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
