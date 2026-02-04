package org.siphonlab.ago.opcode;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;

import java.text.MessageFormat;

public class Pause {
    public static final int KIND_PAUSE = 0x32_000000;
    public static final int OP                  = 0x32;

    public static final int pause = 0x32_000000;

    public static String getName(int code) {
        return switch(code){
            case 0x32_000000 -> "pause";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }

}
