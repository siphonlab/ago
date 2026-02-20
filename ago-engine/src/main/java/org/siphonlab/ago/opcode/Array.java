/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.opcode;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;

public class Array implements GenericOpCode{
    public static final int KIND_ARRAY = 0x0c_000000;
    public static final int OP                  = 0x0c;

    // array_create(target, array class name, size)
    // for generic type, use array_create_g(), i.e. T[], G<T>
    // it only works at transform template code to final code, ignore in the final interpreter loop
    public static final int array_create_vCc = 0x0c_00_00_03;       // << 16
    // array_create(target, array class name, size slot, is_generic)
    public static final int array_create_vCv = 0x0c_00_01_03;       // << 16

    public static final int array_put_acc   = 0x0c_00_20_03;        // << 16
    public static final int array_put_acv   = 0x0c_00_21_03;
    public static final int array_put_avc   = 0x0c_00_22_03;
    public static final int array_put_avv   = 0x0c_00_23_03;

    // array_get_i(target, array, index)
    public static final int array_get_vac   = 0x0c_00_10_03;        // << 16
    // array_get_i(target, array, index slot)
    public static final int array_get_vav   = 0x0c_00_11_03;

    public static final int array_create_B_vCv    = 0x0c_04_01_03;
    public static final int array_create_B_vCc    = 0x0c_04_00_03;

    public static final int array_create_c_vCc    = 0x0c_05_00_03;
    public static final int array_create_c_vCv    = 0x0c_05_01_03;

    public static final int array_create_f_vCc    = 0x0c_06_00_03;
    public static final int array_create_f_vCv    = 0x0c_06_01_03;

    public static final int array_create_d_vCc    = 0x0c_07_00_03;
    public static final int array_create_d_vCv    = 0x0c_07_01_03;

    public static final int array_create_b_vCc    = 0x0c_08_00_03;
    public static final int array_create_b_vCv    = 0x0c_08_01_03;

    public static final int array_create_s_vCc    = 0x0c_09_00_03;
    public static final int array_create_s_vCv    = 0x0c_09_01_03;

    public static final int array_create_i_vCc    = 0x0c_0a_00_03;
    public static final int array_create_i_vCv    = 0x0c_0a_01_03;

    public static final int array_create_l_vCc    = 0x0c_0b_00_03;
    public static final int array_create_l_vCv    = 0x0c_0b_01_03;

    // array_create(target, array classname, classname, size), not necessary
    // the element class already stored in array class
//    public static final int array_create_o_vCcc    = 0x0c_01_00_04;
//    public static final int array_create_o_vCv    = 0x0c_01_01_04;

    public static final int array_create_o_vCc    = 0x0c_01_00_03;
    public static final int array_create_o_vCv    = 0x0c_01_01_03;

    public static final int array_create_g_vCc    = 0x0c_10_00_03;
    public static final int array_create_g_vCv    = 0x0c_10_01_03;

    public static final int array_create_S_vCc    = 0x0c_03_00_03;
    public static final int array_create_S_vCv    = 0x0c_03_01_03;

    // array_get_i(target, array, index)
    public static final int array_get_i_vac   = 0x0c_0a_10_03;

    // array_get_i(target, array, index slot)
    public static final int array_get_i_vav   = 0x0c_0a_11_03;

    public static final int array_put_i_acc   = 0x0c_0a_20_03;
    public static final int array_put_i_acv   = 0x0c_0a_21_03;
    public static final int array_put_i_avc   = 0x0c_0a_22_03;
    public static final int array_put_i_avv   = 0x0c_0a_23_03;

    public static final int array_get_B_vac   = 0x0c_04_10_03;
    public static final int array_get_B_vav   = 0x0c_04_11_03;
    public static final int array_put_B_acc   = 0x0c_04_20_03;
    public static final int array_put_B_acv   = 0x0c_04_21_03;
    public static final int array_put_B_avc   = 0x0c_04_22_03;
    public static final int array_put_B_avv   = 0x0c_04_23_03;

    public static final int array_get_c_vac   = 0x0c_05_10_03;
    public static final int array_get_c_vav   = 0x0c_05_11_03;
    public static final int array_put_c_acc   = 0x0c_05_20_03;
    public static final int array_put_c_acv   = 0x0c_05_21_03;
    public static final int array_put_c_avc   = 0x0c_05_22_03;
    public static final int array_put_c_avv   = 0x0c_05_23_03;

    public static final int array_get_f_vac   = 0x0c_06_10_03;
    public static final int array_get_f_vav   = 0x0c_06_11_03;
    public static final int array_put_f_acc   = 0x0c_06_20_03;
    public static final int array_put_f_acv   = 0x0c_06_21_03;
    public static final int array_put_f_avc   = 0x0c_06_22_03;
    public static final int array_put_f_avv   = 0x0c_06_23_03;

    public static final int array_get_d_vac   = 0x0c_07_10_04;
    public static final int array_get_d_vav   = 0x0c_07_11_03;
    public static final int array_put_d_acc   = 0x0c_07_20_04;
    public static final int array_put_d_acv   = 0x0c_07_21_03;
    public static final int array_put_d_avc   = 0x0c_07_22_04;
    public static final int array_put_d_avv   = 0x0c_07_23_03;

    public static final int array_get_b_vac   = 0x0c_08_10_03;
    public static final int array_get_b_vav   = 0x0c_08_11_03;
    public static final int array_put_b_acc   = 0x0c_08_20_03;
    public static final int array_put_b_acv   = 0x0c_08_21_03;
    public static final int array_put_b_avc   = 0x0c_08_22_03;
    public static final int array_put_b_avv   = 0x0c_08_23_03;

    public static final int array_get_s_vac   = 0x0c_09_10_03;
    public static final int array_get_s_vav   = 0x0c_09_11_03;
    public static final int array_put_s_acc   = 0x0c_09_20_03;
    public static final int array_put_s_acv   = 0x0c_09_21_03;
    public static final int array_put_s_avc   = 0x0c_09_22_03;
    public static final int array_put_s_avv   = 0x0c_09_23_03;

    public static final int array_get_l_vac   = 0x0c_0b_10_04;
    public static final int array_get_l_vav   = 0x0c_0b_11_03;
    public static final int array_put_l_acc   = 0x0c_0b_20_04;
    public static final int array_put_l_acv   = 0x0c_0b_21_03;
    public static final int array_put_l_avc   = 0x0c_0b_22_04;
    public static final int array_put_l_avv   = 0x0c_0b_23_03;

    public static final int array_get_o_vac   = 0x0c_01_10_03;
    public static final int array_get_o_vav   = 0x0c_01_11_03;
    // put null
    public static final int array_put_o_acn   = 0x0c_01_20_02;
    public static final int array_put_o_aco   = 0x0c_01_21_03;
    // put null
    public static final int array_put_o_avn   = 0x0c_01_22_02;
    public static final int array_put_o_avo   = 0x0c_01_23_03;

    public static final int array_get_S_vac   = 0x0c_03_10_03;
    public static final int array_get_S_vav   = 0x0c_03_11_03;
    public static final int array_put_S_acc   = 0x0c_03_20_03;
    public static final int array_put_S_acv   = 0x0c_03_21_03;
    public static final int array_put_S_avc   = 0x0c_03_22_03;
    public static final int array_put_S_avv   = 0x0c_03_23_03;

    // array_size(target, array), it should be stored in Array field, but some interpreter may need invoke function
    public static final int array_size_va       = 0x0c_f1_00_01;

    // array_fill(array, size -- that is element count, BLOB index), for char and string, it's UTF-8 encoded
    public static final int array_fill_acL      = 0x0c_f2_00_03;

    public static final int array_fill_i_acL    = 0x0c_f2_0a_03;
    public static final int array_fill_B_acL    = 0x0c_f2_04_03;
    public static final int array_fill_c_acL    = 0x0c_f2_05_03;
    public static final int array_fill_f_acL    = 0x0c_f2_06_03;
    public static final int array_fill_d_acL    = 0x0c_f2_07_03;
    public static final int array_fill_b_acL    = 0x0c_f2_08_03;
    public static final int array_fill_s_acL    = 0x0c_f2_09_03;
    public static final int array_fill_l_acL    = 0x0c_f2_0b_03;
//    // array_fill_null(array, size)
//    public static final int array_fill_n_ac     = 0x0c_f2_02_02;
    public static final int array_fill_S_acL    = 0x0c_f2_03_03;

    public static String getName(int code) {
        return switch (code) {
            case array_create_vCc   ->  "array_create_vCc";
            case array_create_vCv   ->  "array_create_vCv";
            case array_put_acc   ->     "array_put_acc";
            case array_put_acv   ->     "array_put_acv";
            case array_put_avc   ->     "array_put_avc";
            case array_put_avv   ->     "array_put_avv";
            case array_get_vac   ->     "array_get_vac";
            case array_get_vav   ->     "array_get_vav";
            case array_create_B_vCv   ->    "array_create_B_vCv";
            case array_create_B_vCc   ->    "array_create_B_vCc";
            case array_create_c_vCc   ->    "array_create_c_vCc";
            case array_create_c_vCv   ->    "array_create_c_vCv";
            case array_create_f_vCc   ->    "array_create_f_vCc";
            case array_create_f_vCv   ->    "array_create_f_vCv";
            case array_create_d_vCc   ->    "array_create_d_vCc";
            case array_create_d_vCv   ->    "array_create_d_vCv";
            case array_create_b_vCc   ->    "array_create_b_vCc";
            case array_create_b_vCv   ->    "array_create_b_vCv";
            case array_create_s_vCc   ->    "array_create_s_vCc";
            case array_create_s_vCv   ->    "array_create_s_vCv";
            case array_create_i_vCc   ->    "array_create_i_vCc";
            case array_create_i_vCv   ->    "array_create_i_vCv";
            case array_create_l_vCc   ->    "array_create_l_vCc";
            case array_create_l_vCv   ->    "array_create_l_vCv";
            case array_create_o_vCc   ->    "array_create_o_vCc";
            case array_create_o_vCv   ->    "array_create_o_vCv";
            case array_create_g_vCc   ->    "array_create_g_vCc";
            case array_create_g_vCv   ->    "array_create_g_vCv";
            case array_create_S_vCc   ->    "array_create_S_vCc";
            case array_create_S_vCv   ->    "array_create_S_vCv";
            case array_get_i_vac   ->   "array_get_i_vac";
            case array_get_i_vav   ->   "array_get_i_vav";
            case array_put_i_acc   ->   "array_put_i_acc";
            case array_put_i_acv   ->   "array_put_i_acv";
            case array_put_i_avc   ->   "array_put_i_avc";
            case array_put_i_avv   ->   "array_put_i_avv";
            case array_get_B_vac   ->   "array_get_B_vac";
            case array_get_B_vav   ->   "array_get_B_vav";
            case array_put_B_acc   ->   "array_put_B_acc";
            case array_put_B_acv   ->   "array_put_B_acv";
            case array_put_B_avc   ->   "array_put_B_avc";
            case array_put_B_avv   ->   "array_put_B_avv";
            case array_get_c_vac   ->   "array_get_c_vac";
            case array_get_c_vav   ->   "array_get_c_vav";
            case array_put_c_acc   ->   "array_put_c_acc";
            case array_put_c_acv   ->   "array_put_c_acv";
            case array_put_c_avc   ->   "array_put_c_avc";
            case array_put_c_avv   ->   "array_put_c_avv";
            case array_get_f_vac   ->   "array_get_f_vac";
            case array_get_f_vav   ->   "array_get_f_vav";
            case array_put_f_acc   ->   "array_put_f_acc";
            case array_put_f_acv   ->   "array_put_f_acv";
            case array_put_f_avc   ->   "array_put_f_avc";
            case array_put_f_avv   ->   "array_put_f_avv";
            case array_get_d_vac   ->   "array_get_d_vac";
            case array_get_d_vav   ->   "array_get_d_vav";
            case array_put_d_acc   ->   "array_put_d_acc";
            case array_put_d_acv   ->   "array_put_d_acv";
            case array_put_d_avc   ->   "array_put_d_avc";
            case array_put_d_avv   ->   "array_put_d_avv";
            case array_get_b_vac   ->   "array_get_b_vac";
            case array_get_b_vav   ->   "array_get_b_vav";
            case array_put_b_acc   ->   "array_put_b_acc";
            case array_put_b_acv   ->   "array_put_b_acv";
            case array_put_b_avc   ->   "array_put_b_avc";
            case array_put_b_avv   ->   "array_put_b_avv";
            case array_get_s_vac   ->   "array_get_s_vac";
            case array_get_s_vav   ->   "array_get_s_vav";
            case array_put_s_acc   ->   "array_put_s_acc";
            case array_put_s_acv   ->   "array_put_s_acv";
            case array_put_s_avc   ->   "array_put_s_avc";
            case array_put_s_avv   ->   "array_put_s_avv";
            case array_get_l_vac   ->   "array_get_l_vac";
            case array_get_l_vav   ->   "array_get_l_vav";
            case array_put_l_acc   ->   "array_put_l_acc";
            case array_put_l_acv   ->   "array_put_l_acv";
            case array_put_l_avc   ->   "array_put_l_avc";
            case array_put_l_avv   ->   "array_put_l_avv";
            case array_get_o_vac   ->   "array_get_o_vac";
            case array_get_o_vav   ->   "array_get_o_vav";
            case array_put_o_acn   ->   "array_put_o_acn";
            case array_put_o_aco   ->   "array_put_o_aco";
            case array_put_o_avn   ->   "array_put_o_avn";
            case array_put_o_avo   ->   "array_put_o_avo";
            case array_get_S_vac   ->   "array_get_S_vac";
            case array_get_S_vav   ->   "array_get_S_vav";
            case array_put_S_acc   ->   "array_put_S_acc";
            case array_put_S_acv   ->   "array_put_S_acv";
            case array_put_S_avc   ->   "array_put_S_avc";
            case array_put_S_avv   ->   "array_put_S_avv";
            case array_size_va   ->     "array_size_va";
            case array_fill_acL   ->    "array_fill_acL";
            case array_fill_i_acL   ->  "array_fill_i_acL";
            case array_fill_B_acL   ->  "array_fill_B_acL";
            case array_fill_c_acL   ->  "array_fill_c_acL";
            case array_fill_f_acL   ->  "array_fill_f_acL";
            case array_fill_d_acL   ->  "array_fill_d_acL";
            case array_fill_b_acL   ->  "array_fill_b_acL";
            case array_fill_s_acL   ->  "array_fill_s_acL";
            case array_fill_l_acL   ->  "array_fill_l_acL";
            case array_fill_S_acL   ->  "array_fill_S_acL";

            default -> {
                var t = OpCode.extractType(code);
                if(t >= TypeCode.GENERIC_TYPE_START){
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)){
                        case array_create_vCc: yield "array_create_G[%s]_vCc:".formatted(t);
                        case array_create_vCv: yield "array_create_G[%s]_vCv:".formatted(t);
                        case array_put_acc: yield "array_put_G[%s]_acc:".formatted(t);
                        case array_put_acv: yield "array_put_G[%s]_acv:".formatted(t);
                        case array_put_avc: yield "array_put_G[%s]_avc:".formatted(t);
                        case array_put_avv: yield "array_put_G[%s]_avv:".formatted(t);
                        case array_get_vac: yield "array_get_G[%s]_vac:".formatted(t);
                        case array_get_vav: yield "array_get_G[%s]_vav:".formatted(t);
                    };
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

    public static void main(String[] args) {
        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            int addition = 0;
            if(typeCode == TypeCode.LONG || typeCode == TypeCode.DOUBLE){
                addition = 1;
            }
            String s = MessageFormat.format("""
            	    public static final int array_create_{0}_vc    = 0x0c_{1}_00_02;
            	    public static final int array_create_{0}_vv    = 0x0c_{1}_01_02;
            	""",
            	    typeCode.toShortString(), type
            	);
            System.out.println(s);
            /*
              public static final int array_get_\{typeCode.toShortString()}_vac   = 0x0c_\{type}_10_0\{3 + addition};
              public static final int array_get_\{typeCode.toShortString()}_vav   = 0x0c_\{type}_11_03;
              public static final int array_put_\{typeCode.toShortString()}_acc   = 0x0c_\{type}_20_0\{3 + addition};
              public static final int array_put_\{typeCode.toShortString()}_acv   = 0x0c_\{type}_21_0\{3 + addition};
              public static final int array_put_\{typeCode.toShortString()}_avc   = 0x0c_\{type}_22_0\{3 + addition};
              public static final int array_put_\{typeCode.toShortString()}_avv   = 0x0c_\{type}_23_03;

             */
        }
    }

}
