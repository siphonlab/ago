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
package org.siphonlab.ago.study;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.siphonlab.ago.opcode.Const;
import org.siphonlab.ago.opcode.*;
import org.siphonlab.ago.opcode.logic.*;
import org.siphonlab.ago.opcode.arithmetic.*;
import org.siphonlab.ago.opcode.compare.*;
import java.util.concurrent.Callable;

public class Performance {

    final static int[] arr = {Const.const_i_vc,
            Const.const_fld_i_ovc,
            Const.const_C_vC,
            Const.const_fld_C_ovC,

            Const.const_B_vc,
            Const.const_fld_B_ovc,

            Const.const_c_vc,
            Const.const_fld_c_ovc,

            Const.const_f_vc,
            Const.const_fld_f_ovc,

            Const.const_d_vc,
            Const.const_fld_d_ovc,

            Const.const_b_vc,
            Const.const_fld_b_ovc,

            Const.const_s_vc,
            Const.const_fld_s_ovc,

            Const.const_l_vc,
            Const.const_fld_l_ovc,

            Const.const_n_vc,
            Const.const_fld_n_ovc,

            Const.const_S_vc,
            Const.const_fld_S_ovc,

            Move.move_i_vv,
            Move.move_fld_i_ovv,
            Move.move_fld_i_vov,

            Move.move_C_vv,
            Move.move_fld_C_ovv,
            Move.move_fld_C_vov,

            Move.move_B_vv,
            Move.move_fld_B_ovv,
            Move.move_fld_B_vov,

            Move.move_c_vv,
            Move.move_fld_c_ovv,
            Move.move_fld_c_vov,

            Move.move_f_vv,
            Move.move_fld_f_ovv,
            Move.move_fld_f_vov,

            Move.move_d_vv,
            Move.move_fld_d_ovv,
            Move.move_fld_d_vov,

            Move.move_b_vv,
            Move.move_fld_b_ovv,
            Move.move_fld_b_vov,

            Move.move_s_vv,
            Move.move_fld_s_ovv,
            Move.move_fld_s_vov,

            Move.move_l_vv,
            Move.move_fld_l_ovv,
            Move.move_fld_l_vov,

            Move.move_o_vv,
            Move.move_fld_o_ovv,
            Move.move_fld_o_vov,

            Move.move_S_vv,
            Move.move_fld_S_ovv,
            Move.move_fld_S_vov,

            Cast.c2f,
            Cast.c2d,
            Cast.c2b,
            Cast.c2s,
            Cast.c2i,
            Cast.c2l,
            Cast.c2S,
            Cast.c2B,

            Cast.f2c,
            Cast.f2d,
            Cast.f2b,
            Cast.f2s,
            Cast.f2i,
            Cast.f2l,
            Cast.f2S,
            Cast.f2B,

            Cast.d2c,
            Cast.d2f,
            Cast.d2b,
            Cast.d2s,
            Cast.d2i,
            Cast.d2l,
            Cast.d2S,
            Cast.d2B,

            Cast.b2c,
            Cast.b2f,
            Cast.b2d,
            Cast.b2s,
            Cast.b2i,
            Cast.b2l,
            Cast.b2S,
            Cast.b2B,

            Cast.s2c,
            Cast.s2f,
            Cast.s2d,
            Cast.s2b,
            Cast.s2i,
            Cast.s2l,
            Cast.s2S,
            Cast.s2B,

            Cast.i2c,
            Cast.i2f,
            Cast.i2d,
            Cast.i2b,
            Cast.i2s,
            Cast.i2l,
            Cast.i2S,
            Cast.i2B,

            Cast.l2c,
            Cast.l2f,
            Cast.l2d,
            Cast.l2b,
            Cast.l2s,
            Cast.l2S,
            Cast.l2B,

            Cast.B2S,

            Cast.S2B,
            Cast.o2B,

            Add.add_i_vc,
            Add.add_i_vvc,
            Add.add_i_vv,
            Add.add_i_vvv,

            Add.add_f_vc,
            Add.add_f_vvc,
            Add.add_f_vv,
            Add.add_f_vvv,

            Add.add_b_vc,
            Add.add_b_vvc,
            Add.add_b_vv,
            Add.add_b_vvv,

            Add.add_s_vc,
            Add.add_s_vvc,
            Add.add_s_vv,
            Add.add_s_vvv,

            Add.add_l_vc,
            Add.add_l_vvc,
            Add.add_l_vv,
            Add.add_l_vvv,

            Add.add_d_vc,
            Add.add_d_vvc,
            Add.add_d_vv,
            Add.add_d_vvv,

            Concat.concat_S_vc,
            Concat.concat_S_vvc,
            Concat.concat_S_vv,
            Concat.concat_S_vvv,
            Concat.concat_S_vcv,

            New.new_vC,
            New.new_child_voC,
            New.new_vo,
            New.new_method_voCm,
            New.new_method_voIm,
            New.new_cls_method_vCm,
            New.new_scope_child_vcC,
            New.new_scope_method_vcCm,
            New.new_scope_method_fix_vcCm,
            Invoke.invoke_v,
            Invoke.invoke_v,
            Return.return_i_c,
            Return.return_i_v,

            Return.return_V,
            Return.return_B_c,
            Return.return_B_v,
            Return.return_c_c,
            Return.return_c_v,

            Return.return_f_c,
            Return.return_f_v,
            Return.return_d_c,
            Return.return_d_v,
            Return.return_b_c,
            Return.return_b_v,
            Return.return_s_c,
            Return.return_s_v,
            Return.return_l_c,
            Return.return_l_v,
            Return.return_o_v,
            Return.return_n,
            Return.return_S_c,
            Return.return_S_v,

            Jump.jump_c,
            Jump.jump_t_B_vc,
            Jump.jump_f_B_vc,
            Jump.switch_dense_cv,
            Jump.switch_sparse_cv,
            Load.loadscope_v,
            Load.loadscope_vc,

            Load.loadcls_scope_vc,
            Load.loadcls_scope_v,
            Load.loadcls_vo,
            Load.loadcls_vC,

            Load.loadcls2_scope_vc,
            Load.loadcls2_scope_v,
            Load.loadcls2_vo,

            Array.array_create_i_vCc,
            Array.array_create_i_vCv,

            Array.array_fill_i_acL,

            Array.array_get_i_vac,
            Array.array_get_i_vav,
            Array.array_put_i_acc,
            Array.array_put_i_acv,

            Box.box_i_vv,
            Box.box_i_vc,
            Box.unbox_i_vo,

            Box.box_C_vv,
            Box.box_C_vC,
            Box.unbox_C_vo,

            Box.box_C_vvC,
            Box.box_C_vCC,

            Box.unbox_o_vo,

            Box.box_S_vv,
            Box.box_S_vc,
            Box.unbox_S_vo,


            Equals.equals_i_vvc,
            Equals.equals_i_vvv,
            Equals.equals_f_vvc,
            Equals.equals_f_vvv,
            Equals.equals_d_vvcc,
            Equals.equals_d_vvv,
            Equals.equals_b_vvc,
            Equals.equals_b_vvv,
            Equals.equals_c_vvc,
            Equals.equals_c_vvv,
            Equals.equals_s_vvc,
            Equals.equals_s_vvv,
            Equals.equals_l_vvcc,
            Equals.equals_l_vvv,
            Equals.equals_S_vvc,
            Equals.equals_S_vvv,
            Equals.equals_B_vvc,
            Equals.equals_B_vvv,
            Equals.equals_o_vvv,
            Equals.equals_C_vvc,
            Equals.equals_C_vvv,

            NotEquals.ne_i_vvc,
            NotEquals.ne_i_vvv,
            NotEquals.ne_f_vvc,
            NotEquals.ne_f_vvv,
            NotEquals.ne_d_vvcc,
            NotEquals.ne_d_vvv,
            NotEquals.ne_b_vvc,
            NotEquals.ne_b_vvv,
            NotEquals.ne_c_vvc,
            NotEquals.ne_c_vvv,
            NotEquals.ne_s_vvc,
            NotEquals.ne_s_vvv,
            NotEquals.ne_l_vvcc,
            NotEquals.ne_l_vvv,
            NotEquals.ne_S_vvc,
            NotEquals.ne_S_vvv,
            NotEquals.ne_B_vvc,
            NotEquals.ne_B_vvv,
            NotEquals.ne_o_vvv,
            NotEquals.ne_C_vvc,
            NotEquals.ne_C_vvv,

            GreaterThan.gt_i_vvc,
            GreaterThan.gt_i_vvv,
            GreaterThan.gt_f_vvc,
            GreaterThan.gt_f_vvv,
            GreaterThan.gt_d_vvcc,
            GreaterThan.gt_d_vvv,
            GreaterThan.gt_b_vvc,
            GreaterThan.gt_b_vvv,
            GreaterThan.gt_c_vvc,
            GreaterThan.gt_c_vvv,
            GreaterThan.gt_s_vvc,
            GreaterThan.gt_s_vvv,
            GreaterThan.gt_l_vvcc,
            GreaterThan.gt_l_vvv,
            GreaterThan.gt_S_vvc,
            GreaterThan.gt_S_vvv,
            GreaterThan.gt_C_vvc,
            GreaterThan.gt_C_vvv,

            LittleThan.lt_i_vvc,
            LittleThan.lt_i_vvv,
            LittleThan.lt_f_vvc,
            LittleThan.lt_f_vvv,
            LittleThan.lt_d_vvcc,
            LittleThan.lt_d_vvv,
            LittleThan.lt_b_vvc,
            LittleThan.lt_b_vvv,
            LittleThan.lt_c_vvc,
            LittleThan.lt_c_vvv,
            LittleThan.lt_s_vvc,
            LittleThan.lt_s_vvv,
            LittleThan.lt_l_vvcc,
            LittleThan.lt_l_vvv,
            LittleThan.lt_S_vvc,
            LittleThan.lt_S_vvv,
            LittleThan.lt_C_vvc,
            LittleThan.lt_C_vvv,

            GreaterEquals.ge_i_vvc,
            GreaterEquals.ge_i_vvv,
            GreaterEquals.ge_f_vvc,
            GreaterEquals.ge_f_vvv,
            GreaterEquals.ge_d_vvcc,
            GreaterEquals.ge_d_vvv,
            GreaterEquals.ge_b_vvc,
            GreaterEquals.ge_b_vvv,
            GreaterEquals.ge_c_vvc,
            GreaterEquals.ge_c_vvv,
            GreaterEquals.ge_s_vvc,
            GreaterEquals.ge_s_vvv,
            GreaterEquals.ge_l_vvcc,
            GreaterEquals.ge_l_vvv,
            GreaterEquals.ge_S_vvc,
            GreaterEquals.ge_S_vvv,
            GreaterEquals.ge_C_vvc,
            GreaterEquals.ge_C_vvv,

            LittleEquals.le_i_vvc,
            LittleEquals.le_i_vvv,
            LittleEquals.le_f_vvc,
            LittleEquals.le_f_vvv,
            LittleEquals.le_d_vvcc,
            LittleEquals.le_d_vvv,
            LittleEquals.le_b_vvc,
            LittleEquals.le_b_vvv,
            LittleEquals.le_c_vvc,
            LittleEquals.le_c_vvv,
            LittleEquals.le_s_vvc,
            LittleEquals.le_s_vvv,
            LittleEquals.le_l_vvcc,
            LittleEquals.le_l_vvv,
            LittleEquals.le_S_vvc,
            LittleEquals.le_S_vvv,
            LittleEquals.le_C_vvc,
            LittleEquals.le_C_vvv,

            Subtract.sub_i_vc,
            Subtract.sub_i_vvc,
            Subtract.sub_i_vv,
            Subtract.sub_i_vvv,
            Subtract.sub_i_vcv,

            Subtract.sub_f_vc,
            Subtract.sub_f_vvc,
            Subtract.sub_f_vv,
            Subtract.sub_f_vvv,
            Subtract.sub_f_vcv,

            Subtract.sub_b_vc,
            Subtract.sub_b_vvc,
            Subtract.sub_b_vv,
            Subtract.sub_b_vvv,
            Subtract.sub_b_vcv,

            Subtract.sub_s_vc,
            Subtract.sub_s_vvc,
            Subtract.sub_s_vv,
            Subtract.sub_s_vvv,
            Subtract.sub_s_vcv,

            Subtract.sub_l_vc,
            Subtract.sub_l_vvc,
            Subtract.sub_l_vv,
            Subtract.sub_l_vvv,
            Subtract.sub_l_vcv,

            Subtract.sub_d_vc,
            Subtract.sub_d_vvc,
            Subtract.sub_d_vv,
            Subtract.sub_d_vvv,
            Subtract.sub_d_vcv,

            Multiply.mul_i_vc,
            Multiply.mul_i_vvc,
            Multiply.mul_i_vv,
            Multiply.mul_i_vvv,

            Multiply.mul_f_vc,
            Multiply.mul_f_vvc,
            Multiply.mul_f_vv,
            Multiply.mul_f_vvv,

            Multiply.mul_b_vc,
            Multiply.mul_b_vvc,
            Multiply.mul_b_vv,
            Multiply.mul_b_vvv,

            Multiply.mul_s_vc,
            Multiply.mul_s_vvc,
            Multiply.mul_s_vv,
            Multiply.mul_s_vvv,

            Multiply.mul_l_vc,
            Multiply.mul_l_vvc,
            Multiply.mul_l_vv,
            Multiply.mul_l_vvv,

            Multiply.mul_d_vc,
            Multiply.mul_d_vvc,
            Multiply.mul_d_vv,
            Multiply.mul_d_vvv,

            Div.div_i_vc,
            Div.div_i_vvc,
            Div.div_i_vv,
            Div.div_i_vvv,
            Div.div_i_vcv,

            Div.div_f_vc,
            Div.div_f_vvc,
            Div.div_f_vv,
            Div.div_f_vvv,
            Div.div_f_vcv,

            Div.div_b_vc,
            Div.div_b_vvc,
            Div.div_b_vv,
            Div.div_b_vvv,
            Div.div_b_vcv,

            Div.div_s_vc,
            Div.div_s_vvc,
            Div.div_s_vv,
            Div.div_s_vvv,
            Div.div_s_vcv,

            Div.div_l_vc,
            Div.div_l_vvc,
            Div.div_l_vv,
            Div.div_l_vvv,
            Div.div_l_vcv,

            Div.div_d_vc,
            Div.div_d_vvc,
            Div.div_d_vv,
            Div.div_d_vvv,
            Div.div_d_vcv,


            Mod.mod_i_vc,
            Mod.mod_i_vvc,
            Mod.mod_i_vv,
            Mod.mod_i_vvv,
            Mod.mod_i_vcv,

            Mod.mod_f_vc,
            Mod.mod_f_vvc,
            Mod.mod_f_vv,
            Mod.mod_f_vvv,
            Mod.mod_f_vcv,

            Mod.mod_b_vc,
            Mod.mod_b_vvc,
            Mod.mod_b_vv,
            Mod.mod_b_vvv,
            Mod.mod_b_vcv,

            Mod.mod_s_vc,
            Mod.mod_s_vvc,
            Mod.mod_s_vv,
            Mod.mod_s_vvv,
            Mod.mod_s_vcv,

            Mod.mod_l_vc,
            Mod.mod_l_vvc,
            Mod.mod_l_vv,
            Mod.mod_l_vvv,
            Mod.mod_l_vcv,

            Mod.mod_d_vc,
            Mod.mod_d_vvc,
            Mod.mod_d_vv,
            Mod.mod_d_vvv,
            Mod.mod_d_vcv,

            Neg.neg_i_vv,
            Neg.neg_f_vv,
            Neg.neg_d_vv,
            Neg.neg_b_vv,
            Neg.neg_s_vv,
            Neg.neg_l_vv,

            IncDec.inc_i_ovc,
            IncDec.inc_i_ovv,
            IncDec.inc_f_ov,
            IncDec.inc_f_ovc,
            IncDec.inc_d_ov,
            IncDec.inc_d_ovcc,
            IncDec.inc_b_ov,
            IncDec.inc_b_ovcc,
            IncDec.inc_s_ov,
            IncDec.inc_s_ovcc,
            IncDec.inc_l_ov,
            IncDec.inc_l_ovcc,

            And.and_vv,
            And.and_vvv,

            Or.or_vv,
            Or.or_vvv,
            Not.not_vv};

/// | 操作           | 时间 (ns)          |
/// | ------------ | ---------------- |
/// | 随机数生成        | 230,614,100 ns   |
/// | tableswitch  | 2,784,915,900 ns |
/// | lookupswitch | 4,934,928,500 ns |
/// | Callable数组   | 4,683,935,800 ns |
/// |.net8 lookupswitch | 6,598,421,600 |
///
    public static void main(String[] args) throws Exception {

//        switch_lookup_458();

//        tableswitch_458_limit_100();

//
        for (int i = 0; i < arr.length; i++) {
            System.out.println("case %d: return \"%s\";".formatted(arr[i], testSparse(arr[i])));
        }

//        callable_array();


    }

    private static void callable_array() throws Exception {
        // 4,683,935,800 ns
        Callable<String>[] callables = new Callable[arr.length];

        for (int i = 0; i < arr.length; i++) {
            callables[i] = createCallable(i);
        }
        StopWatch stopwatch;
        stopwatch = new StopWatch();
        stopwatch.start();

        long sum = 0;
        for (int i = 0; i < 100000000; i++) {
            var idx = RandomUtils.nextInt(0, arr.length);
            sum += callables[idx].call().length();
        }

        System.out.println(sum);

        stopwatch.stop();
        System.out.println("exhaust " + stopwatch.getNanoTime());
    }

    private static Callable<String> createCallable(int op){
        switch (op){
            case 0: return ()-> "Const.const_i_vc";
            case 1: return ()->"Const.const_fld_i_ovc";
            case 2: return ()->"Const.const_C_vC";
            case 3: return ()->"Const.const_fld_C_ovC";
            case 4: return ()->"Const.const_B_vc";
            case 5: return ()->"Const.const_fld_B_ovc";
            case 6: return ()->"Const.const_c_vc";
            case 7: return ()->"Const.const_fld_c_ovc";
            case 8: return ()->"Const.const_f_vc";
            case 9: return ()->"Const.const_fld_f_ovc";
            case 10: return ()->"Const.const_d_vc";
            case 11: return ()->"Const.const_fld_d_ovc";
            case 12: return ()->"Const.const_b_vc";
            case 13: return ()->"Const.const_fld_b_ovc";
            case 14: return ()->"Const.const_s_vc";
            case 15: return ()->"Const.const_fld_s_ovc";
            case 16: return ()->"Const.const_l_vc";
            case 17: return ()->"Const.const_fld_l_ovc";
            case 18: return ()->"Const.const_n_vc";
            case 19: return ()->"Const.const_fld_n_ovc";
            case 20: return ()->"Const.const_S_vc";
            case 21: return ()->"Const.const_fld_S_ovc";
            case 22: return ()->"Move.move_i_vv";
            case 23: return ()->"Move.move_fld_i_ovv";
            case 24: return ()->"Move.move_fld_i_vov";
            case 25: return ()->"Move.move_C_vv";
            case 26: return ()->"Move.move_fld_C_ovv";
            case 27: return ()->"Move.move_fld_C_vov";
            case 28: return ()->"Move.move_B_vv";
            case 29: return ()->"Move.move_fld_B_ovv";
            case 30: return ()->"Move.move_fld_B_vov";
            case 31: return ()->"Move.move_c_vv";
            case 32: return ()->"Move.move_fld_c_ovv";
            case 33: return ()->"Move.move_fld_c_vov";
            case 34: return ()->"Move.move_f_vv";
            case 35: return ()->"Move.move_fld_f_ovv";
            case 36: return ()->"Move.move_fld_f_vov";
            case 37: return ()->"Move.move_d_vv";
            case 38: return ()->"Move.move_fld_d_ovv";
            case 39: return ()->"Move.move_fld_d_vov";
            case 40: return ()->"Move.move_b_vv";
            case 41: return ()->"Move.move_fld_b_ovv";
            case 42: return ()->"Move.move_fld_b_vov";
            case 43: return ()->"Move.move_s_vv";
            case 44: return ()->"Move.move_fld_s_ovv";
            case 45: return ()->"Move.move_fld_s_vov";
            case 46: return ()->"Move.move_l_vv";
            case 47: return ()->"Move.move_fld_l_ovv";
            case 48: return ()->"Move.move_fld_l_vov";
            case 49: return ()->"Move.move_o_vv";
            case 50: return ()->"Move.move_fld_o_ovv";
            case 51: return ()->"Move.move_fld_o_vov";
            case 52: return ()->"Move.move_S_vv";
            case 53: return ()->"Move.move_fld_S_ovv";
            case 54: return ()->"Move.move_fld_S_vov";
            case 55: return ()->"Cast.c2f";
            case 56: return ()->"Cast.c2d";
            case 57: return ()->"Cast.c2b";
            case 58: return ()->"Cast.c2s";
            case 59: return ()->"Cast.c2i";
            case 60: return ()->"Cast.c2l";
            case 61: return ()->"Cast.c2S";
            case 62: return ()->"Cast.c2B";
            case 63: return ()->"Cast.f2c";
            case 64: return ()->"Cast.f2d";
            case 65: return ()->"Cast.f2b";
            case 66: return ()->"Cast.f2s";
            case 67: return ()->"Cast.f2i";
            case 68: return ()->"Cast.f2l";
            case 69: return ()->"Cast.f2S";
            case 70: return ()->"Cast.f2B";
            case 71: return ()->"Cast.d2c";
            case 72: return ()->"Cast.d2f";
            case 73: return ()->"Cast.d2b";
            case 74: return ()->"Cast.d2s";
            case 75: return ()->"Cast.d2i";
            case 76: return ()->"Cast.d2l";
            case 77: return ()->"Cast.d2S";
            case 78: return ()->"Cast.d2B";
            case 79: return ()->"Cast.b2c";
            case 80: return ()->"Cast.b2f";
            case 81: return ()->"Cast.b2d";
            case 82: return ()->"Cast.b2s";
            case 83: return ()->"Cast.b2i";
            case 84: return ()->"Cast.b2l";
            case 85: return ()->"Cast.b2S";
            case 86: return ()->"Cast.b2B";
            case 87: return ()->"Cast.s2c";
            case 88: return ()->"Cast.s2f";
            case 89: return ()->"Cast.s2d";
            case 90: return ()->"Cast.s2b";
            case 91: return ()->"Cast.s2i";
            case 92: return ()->"Cast.s2l";
            case 93: return ()->"Cast.s2S";
            case 94: return ()->"Cast.s2B";
            case 95: return ()->"Cast.i2c";
            case 96: return ()->"Cast.i2f";
            case 97: return ()->"Cast.i2d";
            case 98: return ()->"Cast.i2b";
            case 99: return ()->"Cast.i2s";
            case 100: return ()->"Cast.i2l";
            case 101: return ()->"Cast.i2S";
            case 102: return ()->"Cast.i2B";
            case 103: return ()->"Cast.l2c";
            case 104: return ()->"Cast.l2f";
            case 105: return ()->"Cast.l2d";
            case 106: return ()->"Cast.l2b";
            case 107: return ()->"Cast.l2s";
            case 108: return ()->"Cast.l2S";
            case 109: return ()->"Cast.l2B";
            case 110: return ()->"Cast.B2S";
            case 111: return ()->"Cast.S2B";
            case 112: return ()->"Cast.o2B";
            case 113: return ()->"Add.add_i_vc";
            case 114: return ()->"Add.add_i_vvc";
            case 115: return ()->"Add.add_i_vv";
            case 116: return ()->"Add.add_i_vvv";
            case 117: return ()->"Add.add_f_vc";
            case 118: return ()->"Add.add_f_vvc";
            case 119: return ()->"Add.add_f_vv";
            case 120: return ()->"Add.add_f_vvv";
            case 121: return ()->"Add.add_b_vc";
            case 122: return ()->"Add.add_b_vvc";
            case 123: return ()->"Add.add_b_vv";
            case 124: return ()->"Add.add_b_vvv";
            case 125: return ()->"Add.add_s_vc";
            case 126: return ()->"Add.add_s_vvc";
            case 127: return ()->"Add.add_s_vv";
            case 128: return ()->"Add.add_s_vvv";
            case 129: return ()->"Add.add_l_vc";
            case 130: return ()->"Add.add_l_vvc";
            case 131: return ()->"Add.add_l_vv";
            case 132: return ()->"Add.add_l_vvv";
            case 133: return ()->"Add.add_d_vc";
            case 134: return ()->"Add.add_d_vvc";
            case 135: return ()->"Add.add_d_vv";
            case 136: return ()->"Add.add_d_vvv";
            case 137: return ()->"Concat.concat_S_vc";
            case 138: return ()->"Concat.concat_S_vvc";
            case 139: return ()->"Concat.concat_S_vv";
            case 140: return ()->"Concat.concat_S_vvv";
            case 141: return ()->"Concat.concat_S_vcv";
            case 142: return ()->"New.new_vC";
            case 143: return ()->"New.new_child_voC";
            case 144: return ()->"New.new_vo";
            case 145: return ()->"New.new_method_voCm";
            case 146: return ()->"New.new_method_voIm";
            case 147: return ()->"New.new_cls_method_vCm";
            case 148: return ()->"New.new_scope_child_vcC";
            case 149: return ()->"New.new_scope_method_vcCm";
            case 150: return ()->"New.new_scope_method_fix_vcCm";
            case 151: return ()->"Invoke.invoke_v";
            case 152: return ()->"Invoke.invoke_vv";
            case 153: return ()->"Return.return_i_c";
            case 154: return ()->"Return.return_i_v";
            case 155: return ()->"Return.return_V";
            case 156: return ()->"Return.return_B_c";
            case 157: return ()->"Return.return_B_v";
            case 158: return ()->"Return.return_c_c";
            case 159: return ()->"Return.return_c_v";
            case 160: return ()->"Return.return_f_c";
            case 161: return ()->"Return.return_f_v";
            case 162: return ()->"Return.return_d_c";
            case 163: return ()->"Return.return_d_v";
            case 164: return ()->"Return.return_b_c";
            case 165: return ()->"Return.return_b_v";
            case 166: return ()->"Return.return_s_c";
            case 167: return ()->"Return.return_s_v";
            case 168: return ()->"Return.return_l_c";
            case 169: return ()->"Return.return_l_v";
            case 170: return ()->"Return.return_o_v";
            case 171: return ()->"Return.return_n";
            case 172: return ()->"Return.return_S_c";
            case 173: return ()->"Return.return_S_v";
            case 174: return ()->"Jump.jump_c";
            case 175: return ()->"Jump.jump_t_vc";
            case 176: return ()->"Jump.jump_f_vc";
            case 177: return ()->"Jump.switch_dense_cv";
            case 178: return ()->"Jump.switch_sparse_cv";
            case 179: return ()->"Load.loadscope_v";
            case 180: return ()->"Load.loadscope_vc";
            case 181: return ()->"Load.loadcls_scope_vc";
            case 182: return ()->"Load.loadcls_scope_v";
            case 183: return ()->"Load.loadcls_vo";
            case 184: return ()->"Load.loadcls_vC";
            case 185: return ()->"Load.loadcls2_scope_vc";
            case 186: return ()->"Load.loadcls2_scope_v";
            case 187: return ()->"Load.loadcls2_vo";
            case 188: return ()->"Array.array_create_i_vCc";
            case 189: return ()->"Array.array_create_i_vCv";
            case 190: return ()->"Array.array_fill_i_acL";
            case 191: return ()->"Array.array_get_i_vac";
            case 192: return ()->"Array.array_get_i_vav";
            case 193: return ()->"Array.array_put_i_acc";
            case 194: return ()->"Array.array_put_i_acv";
            case 195: return ()->"Box.box_i_vv";
            case 196: return ()->"Box.box_i_vc";
            case 197: return ()->"Box.unbox_i_vo";
            case 198: return ()->"Box.box_C_vv";
            case 199: return ()->"Box.box_C_vC";
            case 200: return ()->"Box.unbox_C_vo";
            case 201: return ()->"Box.box_C_vvC";
            case 202: return ()->"Box.box_C_vCC";
            case 203: return ()->"Box.unbox_o_vo";
            case 204: return ()->"Box.box_S_vv";
            case 205: return ()->"Box.box_S_vc";
            case 206: return ()->"Box.unbox_S_vo";
            case 207: return ()->"Equals.equals_i_vvc";
            case 208: return ()->"Equals.equals_i_vvv";
            case 209: return ()->"Equals.equals_f_vvc";
            case 210: return ()->"Equals.equals_f_vvv";
            case 211: return ()->"Equals.equals_d_vvcc";
            case 212: return ()->"Equals.equals_d_vvv";
            case 213: return ()->"Equals.equals_b_vvc";
            case 214: return ()->"Equals.equals_b_vvv";
            case 215: return ()->"Equals.equals_c_vvc";
            case 216: return ()->"Equals.equals_c_vvv";
            case 217: return ()->"Equals.equals_s_vvc";
            case 218: return ()->"Equals.equals_s_vvv";
            case 219: return ()->"Equals.equals_l_vvcc";
            case 220: return ()->"Equals.equals_l_vvv";
            case 221: return ()->"Equals.equals_S_vvc";
            case 222: return ()->"Equals.equals_S_vvv";
            case 223: return ()->"Equals.equals_B_vvc";
            case 224: return ()->"Equals.equals_B_vvv";
            case 225: return ()->"Equals.equals_o_vvv";
            case 226: return ()->"Equals.equals_C_vvc";
            case 227: return ()->"Equals.equals_C_vvv";
            case 228: return ()->"NotEquals.ne_i_vvc";
            case 229: return ()->"NotEquals.ne_i_vvv";
            case 230: return ()->"NotEquals.ne_f_vvc";
            case 231: return ()->"NotEquals.ne_f_vvv";
            case 232: return ()->"NotEquals.ne_d_vvcc";
            case 233: return ()->"NotEquals.ne_d_vvv";
            case 234: return ()->"NotEquals.ne_b_vvc";
            case 235: return ()->"NotEquals.ne_b_vvv";
            case 236: return ()->"NotEquals.ne_c_vvc";
            case 237: return ()->"NotEquals.ne_c_vvv";
            case 238: return ()->"NotEquals.ne_s_vvc";
            case 239: return ()->"NotEquals.ne_s_vvv";
            case 240: return ()->"NotEquals.ne_l_vvcc";
            case 241: return ()->"NotEquals.ne_l_vvv";
            case 242: return ()->"NotEquals.ne_S_vvc";
            case 243: return ()->"NotEquals.ne_S_vvv";
            case 244: return ()->"NotEquals.ne_B_vvc";
            case 245: return ()->"NotEquals.ne_B_vvv";
            case 246: return ()->"NotEquals.ne_o_vvv";
            case 247: return ()->"NotEquals.ne_C_vvc";
            case 248: return ()->"NotEquals.ne_C_vvv";
            case 249: return ()->"GreaterThan.gt_i_vvc";
            case 250: return ()->"GreaterThan.gt_i_vvv";
            case 251: return ()->"GreaterThan.gt_f_vvc";
            case 252: return ()->"GreaterThan.gt_f_vvv";
            case 253: return ()->"GreaterThan.gt_d_vvcc";
            case 254: return ()->"GreaterThan.gt_d_vvv";
            case 255: return ()->"GreaterThan.gt_b_vvc";
            case 256: return ()->"GreaterThan.gt_b_vvv";
            case 257: return ()->"GreaterThan.gt_c_vvc";
            case 258: return ()->"GreaterThan.gt_c_vvv";
            case 259: return ()->"GreaterThan.gt_s_vvc";
            case 260: return ()->"GreaterThan.gt_s_vvv";
            case 261: return ()->"GreaterThan.gt_l_vvcc";
            case 262: return ()->"GreaterThan.gt_l_vvv";
            case 263: return ()->"GreaterThan.gt_S_vvc";
            case 264: return ()->"GreaterThan.gt_S_vvv";
            case 265: return ()->"GreaterThan.gt_C_vvc";
            case 266: return ()->"GreaterThan.gt_C_vvv";
            case 267: return ()->"LittleThan.lt_i_vvc";
            case 268: return ()->"LittleThan.lt_i_vvv";
            case 269: return ()->"LittleThan.lt_f_vvc";
            case 270: return ()->"LittleThan.lt_f_vvv";
            case 271: return ()->"LittleThan.lt_d_vvcc";
            case 272: return ()->"LittleThan.lt_d_vvv";
            case 273: return ()->"LittleThan.lt_b_vvc";
            case 274: return ()->"LittleThan.lt_b_vvv";
            case 275: return ()->"LittleThan.lt_c_vvc";
            case 276: return ()->"LittleThan.lt_c_vvv";
            case 277: return ()->"LittleThan.lt_s_vvc";
            case 278: return ()->"LittleThan.lt_s_vvv";
            case 279: return ()->"LittleThan.lt_l_vvcc";
            case 280: return ()->"LittleThan.lt_l_vvv";
            case 281: return ()->"LittleThan.lt_S_vvc";
            case 282: return ()->"LittleThan.lt_S_vvv";
            case 283: return ()->"LittleThan.lt_C_vvc";
            case 284: return ()->"LittleThan.lt_C_vvv";
            case 285: return ()->"GreaterEquals.ge_i_vvc";
            case 286: return ()->"GreaterEquals.ge_i_vvv";
            case 287: return ()->"GreaterEquals.ge_f_vvc";
            case 288: return ()->"GreaterEquals.ge_f_vvv";
            case 289: return ()->"GreaterEquals.ge_d_vvcc";
            case 290: return ()->"GreaterEquals.ge_d_vvv";
            case 291: return ()->"GreaterEquals.ge_b_vvc";
            case 292: return ()->"GreaterEquals.ge_b_vvv";
            case 293: return ()->"GreaterEquals.ge_c_vvc";
            case 294: return ()->"GreaterEquals.ge_c_vvv";
            case 295: return ()->"GreaterEquals.ge_s_vvc";
            case 296: return ()->"GreaterEquals.ge_s_vvv";
            case 297: return ()->"GreaterEquals.ge_l_vvcc";
            case 298: return ()->"GreaterEquals.ge_l_vvv";
            case 299: return ()->"GreaterEquals.ge_S_vvc";
            case 300: return ()->"GreaterEquals.ge_S_vvv";
            case 301: return ()->"GreaterEquals.ge_C_vvc";
            case 302: return ()->"GreaterEquals.ge_C_vvv";
            case 303: return ()->"LittleEquals.le_i_vvc";
            case 304: return ()->"LittleEquals.le_i_vvv";
            case 305: return ()->"LittleEquals.le_f_vvc";
            case 306: return ()->"LittleEquals.le_f_vvv";
            case 307: return ()->"LittleEquals.le_d_vvcc";
            case 308: return ()->"LittleEquals.le_d_vvv";
            case 309: return ()->"LittleEquals.le_b_vvc";
            case 310: return ()->"LittleEquals.le_b_vvv";
            case 311: return ()->"LittleEquals.le_c_vvc";
            case 312: return ()->"LittleEquals.le_c_vvv";
            case 313: return ()->"LittleEquals.le_s_vvc";
            case 314: return ()->"LittleEquals.le_s_vvv";
            case 315: return ()->"LittleEquals.le_l_vvcc";
            case 316: return ()->"LittleEquals.le_l_vvv";
            case 317: return ()->"LittleEquals.le_S_vvc";
            case 318: return ()->"LittleEquals.le_S_vvv";
            case 319: return ()->"LittleEquals.le_C_vvc";
            case 320: return ()->"LittleEquals.le_C_vvv";
            case 321: return ()->"Subtract.sub_i_vc";
            case 322: return ()->"Subtract.sub_i_vvc";
            case 323: return ()->"Subtract.sub_i_vv";
            case 324: return ()->"Subtract.sub_i_vvv";
            case 325: return ()->"Subtract.sub_i_vcv";
            case 326: return ()->"Subtract.sub_f_vc";
            case 327: return ()->"Subtract.sub_f_vvc";
            case 328: return ()->"Subtract.sub_f_vv";
            case 329: return ()->"Subtract.sub_f_vvv";
            case 330: return ()->"Subtract.sub_f_vcv";
            case 331: return ()->"Subtract.sub_b_vc";
            case 332: return ()->"Subtract.sub_b_vvc";
            case 333: return ()->"Subtract.sub_b_vv";
            case 334: return ()->"Subtract.sub_b_vvv";
            case 335: return ()->"Subtract.sub_b_vcv";
            case 336: return ()->"Subtract.sub_s_vc";
            case 337: return ()->"Subtract.sub_s_vvc";
            case 338: return ()->"Subtract.sub_s_vv";
            case 339: return ()->"Subtract.sub_s_vvv";
            case 340: return ()->"Subtract.sub_s_vcv";
            case 341: return ()->"Subtract.sub_l_vc";
            case 342: return ()->"Subtract.sub_l_vvc";
            case 343: return ()->"Subtract.sub_l_vv";
            case 344: return ()->"Subtract.sub_l_vvv";
            case 345: return ()->"Subtract.sub_l_vcv";
            case 346: return ()->"Subtract.sub_d_vc";
            case 347: return ()->"Subtract.sub_d_vvc";
            case 348: return ()->"Subtract.sub_d_vv";
            case 349: return ()->"Subtract.sub_d_vvv";
            case 350: return ()->"Subtract.sub_d_vcv";
            case 351: return ()->"Multiply.mul_i_vc";
            case 352: return ()->"Multiply.mul_i_vvc";
            case 353: return ()->"Multiply.mul_i_vv";
            case 354: return ()->"Multiply.mul_i_vvv";
            case 355: return ()->"Multiply.mul_f_vc";
            case 356: return ()->"Multiply.mul_f_vvc";
            case 357: return ()->"Multiply.mul_f_vv";
            case 358: return ()->"Multiply.mul_f_vvv";
            case 359: return ()->"Multiply.mul_b_vc";
            case 360: return ()->"Multiply.mul_b_vvc";
            case 361: return ()->"Multiply.mul_b_vv";
            case 362: return ()->"Multiply.mul_b_vvv";
            case 363: return ()->"Multiply.mul_s_vc";
            case 364: return ()->"Multiply.mul_s_vvc";
            case 365: return ()->"Multiply.mul_s_vv";
            case 366: return ()->"Multiply.mul_s_vvv";
            case 367: return ()->"Multiply.mul_l_vc";
            case 368: return ()->"Multiply.mul_l_vvc";
            case 369: return ()->"Multiply.mul_l_vv";
            case 370: return ()->"Multiply.mul_l_vvv";
            case 371: return ()->"Multiply.mul_d_vc";
            case 372: return ()->"Multiply.mul_d_vvc";
            case 373: return ()->"Multiply.mul_d_vv";
            case 374: return ()->"Multiply.mul_d_vvv";
            case 375: return ()->"Div.div_i_vc";
            case 376: return ()->"Div.div_i_vvc";
            case 377: return ()->"Div.div_i_vv";
            case 378: return ()->"Div.div_i_vvv";
            case 379: return ()->"Div.div_i_vcv";
            case 380: return ()->"Div.div_f_vc";
            case 381: return ()->"Div.div_f_vvc";
            case 382: return ()->"Div.div_f_vv";
            case 383: return ()->"Div.div_f_vvv";
            case 384: return ()->"Div.div_f_vcv";
            case 385: return ()->"Div.div_b_vc";
            case 386: return ()->"Div.div_b_vvc";
            case 387: return ()->"Div.div_b_vv";
            case 388: return ()->"Div.div_b_vvv";
            case 389: return ()->"Div.div_b_vcv";
            case 390: return ()->"Div.div_s_vc";
            case 391: return ()->"Div.div_s_vvc";
            case 392: return ()->"Div.div_s_vv";
            case 393: return ()->"Div.div_s_vvv";
            case 394: return ()->"Div.div_s_vcv";
            case 395: return ()->"Div.div_l_vc";
            case 396: return ()->"Div.div_l_vvc";
            case 397: return ()->"Div.div_l_vv";
            case 398: return ()->"Div.div_l_vvv";
            case 399: return ()->"Div.div_l_vcv";
            case 400: return ()->"Div.div_d_vc";
            case 401: return ()->"Div.div_d_vvc";
            case 402: return ()->"Div.div_d_vv";
            case 403: return ()->"Div.div_d_vvv";
            case 404: return ()->"Div.div_d_vcv";
            case 405: return ()->"Mod.mod_i_vc";
            case 406: return ()->"Mod.mod_i_vvc";
            case 407: return ()->"Mod.mod_i_vv";
            case 408: return ()->"Mod.mod_i_vvv";
            case 409: return ()->"Mod.mod_i_vcv";
            case 410: return ()->"Mod.mod_f_vc";
            case 411: return ()->"Mod.mod_f_vvc";
            case 412: return ()->"Mod.mod_f_vv";
            case 413: return ()->"Mod.mod_f_vvv";
            case 414: return ()->"Mod.mod_f_vcv";
            case 415: return ()->"Mod.mod_b_vc";
            case 416: return ()->"Mod.mod_b_vvc";
            case 417: return ()->"Mod.mod_b_vv";
            case 418: return ()->"Mod.mod_b_vvv";
            case 419: return ()->"Mod.mod_b_vcv";
            case 420: return ()->"Mod.mod_s_vc";
            case 421: return ()->"Mod.mod_s_vvc";
            case 422: return ()->"Mod.mod_s_vv";
            case 423: return ()->"Mod.mod_s_vvv";
            case 424: return ()->"Mod.mod_s_vcv";
            case 425: return ()->"Mod.mod_l_vc";
            case 426: return ()->"Mod.mod_l_vvc";
            case 427: return ()->"Mod.mod_l_vv";
            case 428: return ()->"Mod.mod_l_vvv";
            case 429: return ()->"Mod.mod_l_vcv";
            case 430: return ()->"Mod.mod_d_vc";
            case 431: return ()->"Mod.mod_d_vvc";
            case 432: return ()->"Mod.mod_d_vv";
            case 433: return ()->"Mod.mod_d_vvv";
            case 434: return ()->"Mod.mod_d_vcv";
            case 435: return ()->"Neg.neg_i_vv";
            case 436: return ()->"Neg.neg_f_vv";
            case 437: return ()->"Neg.neg_d_vv";
            case 438: return ()->"Neg.neg_b_vv";
            case 439: return ()->"Neg.neg_s_vv";
            case 440: return ()->"Neg.neg_l_vv";
            case 441: return ()->"IncDec.inc_i_ovc";
            case 442: return ()->"IncDec.inc_i_ovv";
            case 443: return ()->"IncDec.inc_f_ov";
            case 444: return ()->"IncDec.inc_f_ovc";
            case 445: return ()->"IncDec.inc_d_ov";
            case 446: return ()->"IncDec.inc_d_ovcc";
            case 447: return ()->"IncDec.inc_b_ov";
            case 448: return ()->"IncDec.inc_b_ovcc";
            case 449: return ()->"IncDec.inc_s_ov";
            case 450: return ()->"IncDec.inc_s_ovcc";
            case 451: return ()->"IncDec.inc_l_ov";
            case 452: return ()->"IncDec.inc_l_ovcc";
            case 453: return ()->"And.and_vv";
            case 454: return ()->"And.and_vvv";
            case 455: return ()->"Or.or_vv";
            case 456: return ()->"Or.or_vvv";
            case 457: return ()->"Not.not_vv";
        }
        throw new RuntimeException();
    }


    private static void tableswitch_458_limit_100() {
        StopWatch stopwatch;
        stopwatch = new StopWatch();
        stopwatch.start();

        // 仅 Random 生成随机数 230,614,100
        // tableswitch      2,784,915,900
        //        100       1,560,136,800
        //         10       1,672,698,300
        // lookupswitch     4,934,928,500
        long sum = 0;
        for (int i = 0; i < 100000000; i++) {
            var idx = RandomUtils.nextInt(0, 100);
            sum += testDense(idx).length();
        }

        System.out.println(sum);

        stopwatch.stop();
        System.out.println("exhaust " + stopwatch.getNanoTime());
    }


    static String testDense(int op){
        switch (op){
            case 0: return "Const.const_i_vc";
            case 1: return "Const.const_fld_i_ovc";
            case 2: return "Const.const_C_vC";
            case 3: return "Const.const_fld_C_ovC";
            case 4: return "Const.const_B_vc";
            case 5: return "Const.const_fld_B_ovc";
            case 6: return "Const.const_c_vc";
            case 7: return "Const.const_fld_c_ovc";
            case 8: return "Const.const_f_vc";
            case 9: return "Const.const_fld_f_ovc";
            case 10: return "Const.const_d_vc";
            case 11: return "Const.const_fld_d_ovc";
            case 12: return "Const.const_b_vc";
            case 13: return "Const.const_fld_b_ovc";
            case 14: return "Const.const_s_vc";
            case 15: return "Const.const_fld_s_ovc";
            case 16: return "Const.const_l_vc";
            case 17: return "Const.const_fld_l_ovc";
            case 18: return "Const.const_n_vc";
            case 19: return "Const.const_fld_n_ovc";
            case 20: return "Const.const_S_vc";
            case 21: return "Const.const_fld_S_ovc";
            case 22: return "Move.move_i_vv";
            case 23: return "Move.move_fld_i_ovv";
            case 24: return "Move.move_fld_i_vov";
            case 25: return "Move.move_C_vv";
            case 26: return "Move.move_fld_C_ovv";
            case 27: return "Move.move_fld_C_vov";
            case 28: return "Move.move_B_vv";
            case 29: return "Move.move_fld_B_ovv";
            case 30: return "Move.move_fld_B_vov";
            case 31: return "Move.move_c_vv";
            case 32: return "Move.move_fld_c_ovv";
            case 33: return "Move.move_fld_c_vov";
            case 34: return "Move.move_f_vv";
            case 35: return "Move.move_fld_f_ovv";
            case 36: return "Move.move_fld_f_vov";
            case 37: return "Move.move_d_vv";
            case 38: return "Move.move_fld_d_ovv";
            case 39: return "Move.move_fld_d_vov";
            case 40: return "Move.move_b_vv";
            case 41: return "Move.move_fld_b_ovv";
            case 42: return "Move.move_fld_b_vov";
            case 43: return "Move.move_s_vv";
            case 44: return "Move.move_fld_s_ovv";
            case 45: return "Move.move_fld_s_vov";
            case 46: return "Move.move_l_vv";
            case 47: return "Move.move_fld_l_ovv";
            case 48: return "Move.move_fld_l_vov";
            case 49: return "Move.move_o_vv";
            case 50: return "Move.move_fld_o_ovv";
            case 51: return "Move.move_fld_o_vov";
            case 52: return "Move.move_S_vv";
            case 53: return "Move.move_fld_S_ovv";
            case 54: return "Move.move_fld_S_vov";
            case 55: return "Cast.c2f";
            case 56: return "Cast.c2d";
            case 57: return "Cast.c2b";
            case 58: return "Cast.c2s";
            case 59: return "Cast.c2i";
            case 60: return "Cast.c2l";
            case 61: return "Cast.c2S";
            case 62: return "Cast.c2B";
            case 63: return "Cast.f2c";
            case 64: return "Cast.f2d";
            case 65: return "Cast.f2b";
            case 66: return "Cast.f2s";
            case 67: return "Cast.f2i";
            case 68: return "Cast.f2l";
            case 69: return "Cast.f2S";
            case 70: return "Cast.f2B";
            case 71: return "Cast.d2c";
            case 72: return "Cast.d2f";
            case 73: return "Cast.d2b";
            case 74: return "Cast.d2s";
            case 75: return "Cast.d2i";
            case 76: return "Cast.d2l";
            case 77: return "Cast.d2S";
            case 78: return "Cast.d2B";
            case 79: return "Cast.b2c";
            case 80: return "Cast.b2f";
            case 81: return "Cast.b2d";
            case 82: return "Cast.b2s";
            case 83: return "Cast.b2i";
            case 84: return "Cast.b2l";
            case 85: return "Cast.b2S";
            case 86: return "Cast.b2B";
            case 87: return "Cast.s2c";
            case 88: return "Cast.s2f";
            case 89: return "Cast.s2d";
            case 90: return "Cast.s2b";
            case 91: return "Cast.s2i";
            case 92: return "Cast.s2l";
            case 93: return "Cast.s2S";
            case 94: return "Cast.s2B";
            case 95: return "Cast.i2c";
            case 96: return "Cast.i2f";
            case 97: return "Cast.i2d";
            case 98: return "Cast.i2b";
            case 99: return "Cast.i2s";
            case 100: return "Cast.i2l";
            case 101: return "Cast.i2S";
            case 102: return "Cast.i2B";
            case 103: return "Cast.l2c";
            case 104: return "Cast.l2f";
            case 105: return "Cast.l2d";
            case 106: return "Cast.l2b";
            case 107: return "Cast.l2s";
            case 108: return "Cast.l2S";
            case 109: return "Cast.l2B";
            case 110: return "Cast.B2S";
            case 111: return "Cast.S2B";
            case 112: return "Cast.o2B";
            case 113: return "Add.add_i_vc";
            case 114: return "Add.add_i_vvc";
            case 115: return "Add.add_i_vv";
            case 116: return "Add.add_i_vvv";
            case 117: return "Add.add_f_vc";
            case 118: return "Add.add_f_vvc";
            case 119: return "Add.add_f_vv";
            case 120: return "Add.add_f_vvv";
            case 121: return "Add.add_b_vc";
            case 122: return "Add.add_b_vvc";
            case 123: return "Add.add_b_vv";
            case 124: return "Add.add_b_vvv";
            case 125: return "Add.add_s_vc";
            case 126: return "Add.add_s_vvc";
            case 127: return "Add.add_s_vv";
            case 128: return "Add.add_s_vvv";
            case 129: return "Add.add_l_vc";
            case 130: return "Add.add_l_vvc";
            case 131: return "Add.add_l_vv";
            case 132: return "Add.add_l_vvv";
            case 133: return "Add.add_d_vc";
            case 134: return "Add.add_d_vvc";
            case 135: return "Add.add_d_vv";
            case 136: return "Add.add_d_vvv";
            case 137: return "Concat.concat_S_vc";
            case 138: return "Concat.concat_S_vvc";
            case 139: return "Concat.concat_S_vv";
            case 140: return "Concat.concat_S_vvv";
            case 141: return "Concat.concat_S_vcv";
            case 142: return "New.new_vC";
            case 143: return "New.new_child_voC";
            case 144: return "New.new_vo";
            case 145: return "New.new_method_voCm";
            case 146: return "New.new_method_voIm";
            case 147: return "New.new_cls_method_vCm";
            case 148: return "New.new_scope_child_vcC";
            case 149: return "New.new_scope_method_vcCm";
            case 150: return "New.new_scope_method_fix_vcCm";
            case 151: return "Invoke.invoke_v";
            case 152: return "Invoke.invoke_vv";
            case 153: return "Return.return_i_c";
            case 154: return "Return.return_i_v";
            case 155: return "Return.return_V";
            case 156: return "Return.return_B_c";
            case 157: return "Return.return_B_v";
            case 158: return "Return.return_c_c";
            case 159: return "Return.return_c_v";
            case 160: return "Return.return_f_c";
            case 161: return "Return.return_f_v";
            case 162: return "Return.return_d_c";
            case 163: return "Return.return_d_v";
            case 164: return "Return.return_b_c";
            case 165: return "Return.return_b_v";
            case 166: return "Return.return_s_c";
            case 167: return "Return.return_s_v";
            case 168: return "Return.return_l_c";
            case 169: return "Return.return_l_v";
            case 170: return "Return.return_o_v";
            case 171: return "Return.return_n";
            case 172: return "Return.return_S_c";
            case 173: return "Return.return_S_v";
            case 174: return "Jump.jump_c";
            case 175: return "Jump.jump_t_vc";
            case 176: return "Jump.jump_f_vc";
            case 177: return "Jump.switch_dense_cv";
            case 178: return "Jump.switch_sparse_cv";
            case 179: return "Load.loadscope_v";
            case 180: return "Load.loadscope_vc";
            case 181: return "Load.loadcls_scope_vc";
            case 182: return "Load.loadcls_scope_v";
            case 183: return "Load.loadcls_vo";
            case 184: return "Load.loadcls_vC";
            case 185: return "Load.loadcls2_scope_vc";
            case 186: return "Load.loadcls2_scope_v";
            case 187: return "Load.loadcls2_vo";
            case 188: return "Array.array_create_i_vCc";
            case 189: return "Array.array_create_i_vCv";
            case 190: return "Array.array_fill_i_acL";
            case 191: return "Array.array_get_i_vac";
            case 192: return "Array.array_get_i_vav";
            case 193: return "Array.array_put_i_acc";
            case 194: return "Array.array_put_i_acv";
            case 195: return "Box.box_i_vv";
            case 196: return "Box.box_i_vc";
            case 197: return "Box.unbox_i_vo";
            case 198: return "Box.box_C_vv";
            case 199: return "Box.box_C_vC";
            case 200: return "Box.unbox_C_vo";
            case 201: return "Box.box_C_vvC";
            case 202: return "Box.box_C_vCC";
            case 203: return "Box.unbox_o_vo";
            case 204: return "Box.box_S_vv";
            case 205: return "Box.box_S_vc";
            case 206: return "Box.unbox_S_vo";
            case 207: return "Equals.equals_i_vvc";
            case 208: return "Equals.equals_i_vvv";
            case 209: return "Equals.equals_f_vvc";
            case 210: return "Equals.equals_f_vvv";
            case 211: return "Equals.equals_d_vvcc";
            case 212: return "Equals.equals_d_vvv";
            case 213: return "Equals.equals_b_vvc";
            case 214: return "Equals.equals_b_vvv";
            case 215: return "Equals.equals_c_vvc";
            case 216: return "Equals.equals_c_vvv";
            case 217: return "Equals.equals_s_vvc";
            case 218: return "Equals.equals_s_vvv";
            case 219: return "Equals.equals_l_vvcc";
            case 220: return "Equals.equals_l_vvv";
            case 221: return "Equals.equals_S_vvc";
            case 222: return "Equals.equals_S_vvv";
            case 223: return "Equals.equals_B_vvc";
            case 224: return "Equals.equals_B_vvv";
            case 225: return "Equals.equals_o_vvv";
            case 226: return "Equals.equals_C_vvc";
            case 227: return "Equals.equals_C_vvv";
            case 228: return "NotEquals.ne_i_vvc";
            case 229: return "NotEquals.ne_i_vvv";
            case 230: return "NotEquals.ne_f_vvc";
            case 231: return "NotEquals.ne_f_vvv";
            case 232: return "NotEquals.ne_d_vvcc";
            case 233: return "NotEquals.ne_d_vvv";
            case 234: return "NotEquals.ne_b_vvc";
            case 235: return "NotEquals.ne_b_vvv";
            case 236: return "NotEquals.ne_c_vvc";
            case 237: return "NotEquals.ne_c_vvv";
            case 238: return "NotEquals.ne_s_vvc";
            case 239: return "NotEquals.ne_s_vvv";
            case 240: return "NotEquals.ne_l_vvcc";
            case 241: return "NotEquals.ne_l_vvv";
            case 242: return "NotEquals.ne_S_vvc";
            case 243: return "NotEquals.ne_S_vvv";
            case 244: return "NotEquals.ne_B_vvc";
            case 245: return "NotEquals.ne_B_vvv";
            case 246: return "NotEquals.ne_o_vvv";
            case 247: return "NotEquals.ne_C_vvc";
            case 248: return "NotEquals.ne_C_vvv";
            case 249: return "GreaterThan.gt_i_vvc";
            case 250: return "GreaterThan.gt_i_vvv";
            case 251: return "GreaterThan.gt_f_vvc";
            case 252: return "GreaterThan.gt_f_vvv";
            case 253: return "GreaterThan.gt_d_vvcc";
            case 254: return "GreaterThan.gt_d_vvv";
            case 255: return "GreaterThan.gt_b_vvc";
            case 256: return "GreaterThan.gt_b_vvv";
            case 257: return "GreaterThan.gt_c_vvc";
            case 258: return "GreaterThan.gt_c_vvv";
            case 259: return "GreaterThan.gt_s_vvc";
            case 260: return "GreaterThan.gt_s_vvv";
            case 261: return "GreaterThan.gt_l_vvcc";
            case 262: return "GreaterThan.gt_l_vvv";
            case 263: return "GreaterThan.gt_S_vvc";
            case 264: return "GreaterThan.gt_S_vvv";
            case 265: return "GreaterThan.gt_C_vvc";
            case 266: return "GreaterThan.gt_C_vvv";
            case 267: return "LittleThan.lt_i_vvc";
            case 268: return "LittleThan.lt_i_vvv";
            case 269: return "LittleThan.lt_f_vvc";
            case 270: return "LittleThan.lt_f_vvv";
            case 271: return "LittleThan.lt_d_vvcc";
            case 272: return "LittleThan.lt_d_vvv";
            case 273: return "LittleThan.lt_b_vvc";
            case 274: return "LittleThan.lt_b_vvv";
            case 275: return "LittleThan.lt_c_vvc";
            case 276: return "LittleThan.lt_c_vvv";
            case 277: return "LittleThan.lt_s_vvc";
            case 278: return "LittleThan.lt_s_vvv";
            case 279: return "LittleThan.lt_l_vvcc";
            case 280: return "LittleThan.lt_l_vvv";
            case 281: return "LittleThan.lt_S_vvc";
            case 282: return "LittleThan.lt_S_vvv";
            case 283: return "LittleThan.lt_C_vvc";
            case 284: return "LittleThan.lt_C_vvv";
            case 285: return "GreaterEquals.ge_i_vvc";
            case 286: return "GreaterEquals.ge_i_vvv";
            case 287: return "GreaterEquals.ge_f_vvc";
            case 288: return "GreaterEquals.ge_f_vvv";
            case 289: return "GreaterEquals.ge_d_vvcc";
            case 290: return "GreaterEquals.ge_d_vvv";
            case 291: return "GreaterEquals.ge_b_vvc";
            case 292: return "GreaterEquals.ge_b_vvv";
            case 293: return "GreaterEquals.ge_c_vvc";
            case 294: return "GreaterEquals.ge_c_vvv";
            case 295: return "GreaterEquals.ge_s_vvc";
            case 296: return "GreaterEquals.ge_s_vvv";
            case 297: return "GreaterEquals.ge_l_vvcc";
            case 298: return "GreaterEquals.ge_l_vvv";
            case 299: return "GreaterEquals.ge_S_vvc";
            case 300: return "GreaterEquals.ge_S_vvv";
            case 301: return "GreaterEquals.ge_C_vvc";
            case 302: return "GreaterEquals.ge_C_vvv";
            case 303: return "LittleEquals.le_i_vvc";
            case 304: return "LittleEquals.le_i_vvv";
            case 305: return "LittleEquals.le_f_vvc";
            case 306: return "LittleEquals.le_f_vvv";
            case 307: return "LittleEquals.le_d_vvcc";
            case 308: return "LittleEquals.le_d_vvv";
            case 309: return "LittleEquals.le_b_vvc";
            case 310: return "LittleEquals.le_b_vvv";
            case 311: return "LittleEquals.le_c_vvc";
            case 312: return "LittleEquals.le_c_vvv";
            case 313: return "LittleEquals.le_s_vvc";
            case 314: return "LittleEquals.le_s_vvv";
            case 315: return "LittleEquals.le_l_vvcc";
            case 316: return "LittleEquals.le_l_vvv";
            case 317: return "LittleEquals.le_S_vvc";
            case 318: return "LittleEquals.le_S_vvv";
            case 319: return "LittleEquals.le_C_vvc";
            case 320: return "LittleEquals.le_C_vvv";
            case 321: return "Subtract.sub_i_vc";
            case 322: return "Subtract.sub_i_vvc";
            case 323: return "Subtract.sub_i_vv";
            case 324: return "Subtract.sub_i_vvv";
            case 325: return "Subtract.sub_i_vcv";
            case 326: return "Subtract.sub_f_vc";
            case 327: return "Subtract.sub_f_vvc";
            case 328: return "Subtract.sub_f_vv";
            case 329: return "Subtract.sub_f_vvv";
            case 330: return "Subtract.sub_f_vcv";
            case 331: return "Subtract.sub_b_vc";
            case 332: return "Subtract.sub_b_vvc";
            case 333: return "Subtract.sub_b_vv";
            case 334: return "Subtract.sub_b_vvv";
            case 335: return "Subtract.sub_b_vcv";
            case 336: return "Subtract.sub_s_vc";
            case 337: return "Subtract.sub_s_vvc";
            case 338: return "Subtract.sub_s_vv";
            case 339: return "Subtract.sub_s_vvv";
            case 340: return "Subtract.sub_s_vcv";
            case 341: return "Subtract.sub_l_vc";
            case 342: return "Subtract.sub_l_vvc";
            case 343: return "Subtract.sub_l_vv";
            case 344: return "Subtract.sub_l_vvv";
            case 345: return "Subtract.sub_l_vcv";
            case 346: return "Subtract.sub_d_vc";
            case 347: return "Subtract.sub_d_vvc";
            case 348: return "Subtract.sub_d_vv";
            case 349: return "Subtract.sub_d_vvv";
            case 350: return "Subtract.sub_d_vcv";
            case 351: return "Multiply.mul_i_vc";
            case 352: return "Multiply.mul_i_vvc";
            case 353: return "Multiply.mul_i_vv";
            case 354: return "Multiply.mul_i_vvv";
            case 355: return "Multiply.mul_f_vc";
            case 356: return "Multiply.mul_f_vvc";
            case 357: return "Multiply.mul_f_vv";
            case 358: return "Multiply.mul_f_vvv";
            case 359: return "Multiply.mul_b_vc";
            case 360: return "Multiply.mul_b_vvc";
            case 361: return "Multiply.mul_b_vv";
            case 362: return "Multiply.mul_b_vvv";
            case 363: return "Multiply.mul_s_vc";
            case 364: return "Multiply.mul_s_vvc";
            case 365: return "Multiply.mul_s_vv";
            case 366: return "Multiply.mul_s_vvv";
            case 367: return "Multiply.mul_l_vc";
            case 368: return "Multiply.mul_l_vvc";
            case 369: return "Multiply.mul_l_vv";
            case 370: return "Multiply.mul_l_vvv";
            case 371: return "Multiply.mul_d_vc";
            case 372: return "Multiply.mul_d_vvc";
            case 373: return "Multiply.mul_d_vv";
            case 374: return "Multiply.mul_d_vvv";
            case 375: return "Div.div_i_vc";
            case 376: return "Div.div_i_vvc";
            case 377: return "Div.div_i_vv";
            case 378: return "Div.div_i_vvv";
            case 379: return "Div.div_i_vcv";
            case 380: return "Div.div_f_vc";
            case 381: return "Div.div_f_vvc";
            case 382: return "Div.div_f_vv";
            case 383: return "Div.div_f_vvv";
            case 384: return "Div.div_f_vcv";
            case 385: return "Div.div_b_vc";
            case 386: return "Div.div_b_vvc";
            case 387: return "Div.div_b_vv";
            case 388: return "Div.div_b_vvv";
            case 389: return "Div.div_b_vcv";
            case 390: return "Div.div_s_vc";
            case 391: return "Div.div_s_vvc";
            case 392: return "Div.div_s_vv";
            case 393: return "Div.div_s_vvv";
            case 394: return "Div.div_s_vcv";
            case 395: return "Div.div_l_vc";
            case 396: return "Div.div_l_vvc";
            case 397: return "Div.div_l_vv";
            case 398: return "Div.div_l_vvv";
            case 399: return "Div.div_l_vcv";
            case 400: return "Div.div_d_vc";
            case 401: return "Div.div_d_vvc";
            case 402: return "Div.div_d_vv";
            case 403: return "Div.div_d_vvv";
            case 404: return "Div.div_d_vcv";
            case 405: return "Mod.mod_i_vc";
            case 406: return "Mod.mod_i_vvc";
            case 407: return "Mod.mod_i_vv";
            case 408: return "Mod.mod_i_vvv";
            case 409: return "Mod.mod_i_vcv";
            case 410: return "Mod.mod_f_vc";
            case 411: return "Mod.mod_f_vvc";
            case 412: return "Mod.mod_f_vv";
            case 413: return "Mod.mod_f_vvv";
            case 414: return "Mod.mod_f_vcv";
            case 415: return "Mod.mod_b_vc";
            case 416: return "Mod.mod_b_vvc";
            case 417: return "Mod.mod_b_vv";
            case 418: return "Mod.mod_b_vvv";
            case 419: return "Mod.mod_b_vcv";
            case 420: return "Mod.mod_s_vc";
            case 421: return "Mod.mod_s_vvc";
            case 422: return "Mod.mod_s_vv";
            case 423: return "Mod.mod_s_vvv";
            case 424: return "Mod.mod_s_vcv";
            case 425: return "Mod.mod_l_vc";
            case 426: return "Mod.mod_l_vvc";
            case 427: return "Mod.mod_l_vv";
            case 428: return "Mod.mod_l_vvv";
            case 429: return "Mod.mod_l_vcv";
            case 430: return "Mod.mod_d_vc";
            case 431: return "Mod.mod_d_vvc";
            case 432: return "Mod.mod_d_vv";
            case 433: return "Mod.mod_d_vvv";
            case 434: return "Mod.mod_d_vcv";
            case 435: return "Neg.neg_i_vv";
            case 436: return "Neg.neg_f_vv";
            case 437: return "Neg.neg_d_vv";
            case 438: return "Neg.neg_b_vv";
            case 439: return "Neg.neg_s_vv";
            case 440: return "Neg.neg_l_vv";
            case 441: return "IncDec.inc_i_ovc";
            case 442: return "IncDec.inc_i_ovv";
            case 443: return "IncDec.inc_f_ov";
            case 444: return "IncDec.inc_f_ovc";
            case 445: return "IncDec.inc_d_ov";
            case 446: return "IncDec.inc_d_ovcc";
            case 447: return "IncDec.inc_b_ov";
            case 448: return "IncDec.inc_b_ovcc";
            case 449: return "IncDec.inc_s_ov";
            case 450: return "IncDec.inc_s_ovcc";
            case 451: return "IncDec.inc_l_ov";
            case 452: return "IncDec.inc_l_ovcc";
            case 453: return "And.and_vv";
            case 454: return "And.and_vvv";
            case 455: return "Or.or_vv";
            case 456: return "Or.or_vvv";
            case 457: return "Not.not_vv";
        };
        throw new RuntimeException("");
    }


    private static void switch_lookup_458() {
        StopWatch stopwatch;
        stopwatch = new StopWatch();
        stopwatch.start();

        long sum = 0;
        for (int i = 0; i < 100000000; i++) {
            var idx = RandomUtils.nextInt(0, arr.length);
            sum += testSparse(arr[idx]).length();
        }

        System.out.println(sum);

        stopwatch.stop();
        System.out.println("exhaust " + stopwatch.getNanoTime());

        // 4934928500, 4.9s  依然无法解释 1min 耗时
    }

    static String testSparse(int op) {
        switch (op) {
            case Const.const_i_vc:
                return "Const.const_i_vc";
            case Const.const_fld_i_ovc:
                return "Const.const_fld_i_ovc";
            case Const.const_C_vC:
                return "Const.const_C_vC";
            case Const.const_fld_C_ovC:
                return "Const.const_fld_C_ovC";
            case Const.const_B_vc:
                return "Const.const_B_vc";
            case Const.const_fld_B_ovc:
                return "Const.const_fld_B_ovc";
            case Const.const_c_vc:
                return "Const.const_c_vc";
            case Const.const_fld_c_ovc:
                return "Const.const_fld_c_ovc";
            case Const.const_f_vc:
                return "Const.const_f_vc";
            case Const.const_fld_f_ovc:
                return "Const.const_fld_f_ovc";
            case Const.const_d_vc:
                return "Const.const_d_vc";
            case Const.const_fld_d_ovc:
                return "Const.const_fld_d_ovc";
            case Const.const_b_vc:
                return "Const.const_b_vc";
            case Const.const_fld_b_ovc:
                return "Const.const_fld_b_ovc";
            case Const.const_s_vc:
                return "Const.const_s_vc";
            case Const.const_fld_s_ovc:
                return "Const.const_fld_s_ovc";
            case Const.const_l_vc:
                return "Const.const_l_vc";
            case Const.const_fld_l_ovc:
                return "Const.const_fld_l_ovc";
            case Const.const_n_vc:
                return "Const.const_n_vc";
            case Const.const_fld_n_ovc:
                return "Const.const_fld_n_ovc";
            case Const.const_S_vc:
                return "Const.const_S_vc";
            case Const.const_fld_S_ovc:
                return "Const.const_fld_S_ovc";
            case Move.move_i_vv:
                return "Move.move_i_vv";
            case Move.move_fld_i_ovv:
                return "Move.move_fld_i_ovv";
            case Move.move_fld_i_vov:
                return "Move.move_fld_i_vov";
            case Move.move_C_vv:
                return "Move.move_C_vv";
            case Move.move_fld_C_ovv:
                return "Move.move_fld_C_ovv";
            case Move.move_fld_C_vov:
                return "Move.move_fld_C_vov";
            case Move.move_B_vv:
                return "Move.move_B_vv";
            case Move.move_fld_B_ovv:
                return "Move.move_fld_B_ovv";
            case Move.move_fld_B_vov:
                return "Move.move_fld_B_vov";
            case Move.move_c_vv:
                return "Move.move_c_vv";
            case Move.move_fld_c_ovv:
                return "Move.move_fld_c_ovv";
            case Move.move_fld_c_vov:
                return "Move.move_fld_c_vov";
            case Move.move_f_vv:
                return "Move.move_f_vv";
            case Move.move_fld_f_ovv:
                return "Move.move_fld_f_ovv";
            case Move.move_fld_f_vov:
                return "Move.move_fld_f_vov";
            case Move.move_d_vv:
                return "Move.move_d_vv";
            case Move.move_fld_d_ovv:
                return "Move.move_fld_d_ovv";
            case Move.move_fld_d_vov:
                return "Move.move_fld_d_vov";
            case Move.move_b_vv:
                return "Move.move_b_vv";
            case Move.move_fld_b_ovv:
                return "Move.move_fld_b_ovv";
            case Move.move_fld_b_vov:
                return "Move.move_fld_b_vov";
            case Move.move_s_vv:
                return "Move.move_s_vv";
            case Move.move_fld_s_ovv:
                return "Move.move_fld_s_ovv";
            case Move.move_fld_s_vov:
                return "Move.move_fld_s_vov";
            case Move.move_l_vv:
                return "Move.move_l_vv";
            case Move.move_fld_l_ovv:
                return "Move.move_fld_l_ovv";
            case Move.move_fld_l_vov:
                return "Move.move_fld_l_vov";
            case Move.move_o_vv:
                return "Move.move_o_vv";
            case Move.move_fld_o_ovv:
                return "Move.move_fld_o_ovv";
            case Move.move_fld_o_vov:
                return "Move.move_fld_o_vov";
            case Move.move_S_vv:
                return "Move.move_S_vv";
            case Move.move_fld_S_ovv:
                return "Move.move_fld_S_ovv";
            case Move.move_fld_S_vov:
                return "Move.move_fld_S_vov";
            case Cast.c2f:
                return "Cast.c2f";
            case Cast.c2d:
                return "Cast.c2d";
            case Cast.c2b:
                return "Cast.c2b";
            case Cast.c2s:
                return "Cast.c2s";
            case Cast.c2i:
                return "Cast.c2i";
            case Cast.c2l:
                return "Cast.c2l";
            case Cast.c2S:
                return "Cast.c2S";
            case Cast.c2B:
                return "Cast.c2B";
            case Cast.f2c:
                return "Cast.f2c";
            case Cast.f2d:
                return "Cast.f2d";
            case Cast.f2b:
                return "Cast.f2b";
            case Cast.f2s:
                return "Cast.f2s";
            case Cast.f2i:
                return "Cast.f2i";
            case Cast.f2l:
                return "Cast.f2l";
            case Cast.f2S:
                return "Cast.f2S";
            case Cast.f2B:
                return "Cast.f2B";
            case Cast.d2c:
                return "Cast.d2c";
            case Cast.d2f:
                return "Cast.d2f";
            case Cast.d2b:
                return "Cast.d2b";
            case Cast.d2s:
                return "Cast.d2s";
            case Cast.d2i:
                return "Cast.d2i";
            case Cast.d2l:
                return "Cast.d2l";
            case Cast.d2S:
                return "Cast.d2S";
            case Cast.d2B:
                return "Cast.d2B";
            case Cast.b2c:
                return "Cast.b2c";
            case Cast.b2f:
                return "Cast.b2f";
            case Cast.b2d:
                return "Cast.b2d";
            case Cast.b2s:
                return "Cast.b2s";
            case Cast.b2i:
                return "Cast.b2i";
            case Cast.b2l:
                return "Cast.b2l";
            case Cast.b2S:
                return "Cast.b2S";
            case Cast.b2B:
                return "Cast.b2B";
            case Cast.s2c:
                return "Cast.s2c";
            case Cast.s2f:
                return "Cast.s2f";
            case Cast.s2d:
                return "Cast.s2d";
            case Cast.s2b:
                return "Cast.s2b";
            case Cast.s2i:
                return "Cast.s2i";
            case Cast.s2l:
                return "Cast.s2l";
            case Cast.s2S:
                return "Cast.s2S";
            case Cast.s2B:
                return "Cast.s2B";
            case Cast.i2c:
                return "Cast.i2c";
            case Cast.i2f:
                return "Cast.i2f";
            case Cast.i2d:
                return "Cast.i2d";
            case Cast.i2b:
                return "Cast.i2b";
            case Cast.i2s:
                return "Cast.i2s";
            case Cast.i2l:
                return "Cast.i2l";
            case Cast.i2S:
                return "Cast.i2S";
            case Cast.i2B:
                return "Cast.i2B";
            case Cast.l2c:
                return "Cast.l2c";
            case Cast.l2f:
                return "Cast.l2f";
            case Cast.l2d:
                return "Cast.l2d";
            case Cast.l2b:
                return "Cast.l2b";
            case Cast.l2s:
                return "Cast.l2s";
            case Cast.l2S:
                return "Cast.l2S";
            case Cast.l2B:
                return "Cast.l2B";
            case Cast.B2S:
                return "Cast.B2S";
            case Cast.S2B:
                return "Cast.S2B";
            case Cast.o2B:
                return "Cast.o2B";
            case Add.add_i_vc:
                return "Add.add_i_vc";
            case Add.add_i_vvc:
                return "Add.add_i_vvc";
            case Add.add_i_vv:
                return "Add.add_i_vv";
            case Add.add_i_vvv:
                return "Add.add_i_vvv";
            case Add.add_f_vc:
                return "Add.add_f_vc";
            case Add.add_f_vvc:
                return "Add.add_f_vvc";
            case Add.add_f_vv:
                return "Add.add_f_vv";
            case Add.add_f_vvv:
                return "Add.add_f_vvv";
            case Add.add_b_vc:
                return "Add.add_b_vc";
            case Add.add_b_vvc:
                return "Add.add_b_vvc";
            case Add.add_b_vv:
                return "Add.add_b_vv";
            case Add.add_b_vvv:
                return "Add.add_b_vvv";
            case Add.add_s_vc:
                return "Add.add_s_vc";
            case Add.add_s_vvc:
                return "Add.add_s_vvc";
            case Add.add_s_vv:
                return "Add.add_s_vv";
            case Add.add_s_vvv:
                return "Add.add_s_vvv";
            case Add.add_l_vc:
                return "Add.add_l_vc";
            case Add.add_l_vvc:
                return "Add.add_l_vvc";
            case Add.add_l_vv:
                return "Add.add_l_vv";
            case Add.add_l_vvv:
                return "Add.add_l_vvv";
            case Add.add_d_vc:
                return "Add.add_d_vc";
            case Add.add_d_vvc:
                return "Add.add_d_vvc";
            case Add.add_d_vv:
                return "Add.add_d_vv";
            case Add.add_d_vvv:
                return "Add.add_d_vvv";
            case Concat.concat_S_vc:
                return "Concat.concat_S_vc";
            case Concat.concat_S_vvc:
                return "Concat.concat_S_vvc";
            case Concat.concat_S_vv:
                return "Concat.concat_S_vv";
            case Concat.concat_S_vvv:
                return "Concat.concat_S_vvv";
            case Concat.concat_S_vcv:
                return "Concat.concat_S_vcv";
            case New.new_vC:
                return "New.new_vC";
            case New.new_child_voC:
                return "New.new_child_voC";
            case New.new_vo:
                return "New.new_vo";
            case New.new_method_voCm:
                return "New.new_method_voCm";
            case New.new_method_voIm:
                return "New.new_method_voIm";
            case New.new_cls_method_vCm:
                return "New.new_cls_method_vCm";
            case New.new_scope_child_vcC:
                return "New.new_scope_child_vcC";
            case New.new_scope_method_vcCm:
                return "New.new_scope_method_vcCm";
            case New.new_scope_method_fix_vcCm:
                return "New.new_scope_method_fix_vcCm";
            case Invoke.invoke_v:
                return "Invoke.invoke_v";
            case Return.return_i_c:
                return "Return.return_i_c";
            case Return.return_i_v:
                return "Return.return_i_v";
            case Return.return_V:
                return "Return.return_V";
            case Return.return_B_c:
                return "Return.return_B_c";
            case Return.return_B_v:
                return "Return.return_B_v";
            case Return.return_c_c:
                return "Return.return_c_c";
            case Return.return_c_v:
                return "Return.return_c_v";
            case Return.return_f_c:
                return "Return.return_f_c";
            case Return.return_f_v:
                return "Return.return_f_v";
            case Return.return_d_c:
                return "Return.return_d_c";
            case Return.return_d_v:
                return "Return.return_d_v";
            case Return.return_b_c:
                return "Return.return_b_c";
            case Return.return_b_v:
                return "Return.return_b_v";
            case Return.return_s_c:
                return "Return.return_s_c";
            case Return.return_s_v:
                return "Return.return_s_v";
            case Return.return_l_c:
                return "Return.return_l_c";
            case Return.return_l_v:
                return "Return.return_l_v";
            case Return.return_o_v:
                return "Return.return_o_v";
            case Return.return_n:
                return "Return.return_n";
            case Return.return_S_c:
                return "Return.return_S_c";
            case Return.return_S_v:
                return "Return.return_S_v";
            case Jump.jump_c:
                return "Jump.jump_c";
            case Jump.jump_t_B_vc:
                return "Jump.jump_t_vc";
            case Jump.jump_f_B_vc:
                return "Jump.jump_f_vc";
            case Jump.switch_dense_cv:
                return "Jump.switch_dense_cv";
            case Jump.switch_sparse_cv:
                return "Jump.switch_sparse_cv";
            case Load.loadscope_v:
                return "Load.loadscope_v";
            case Load.loadscope_vc:
                return "Load.loadscope_vc";
            case Load.loadcls_scope_vc:
                return "Load.loadcls_scope_vc";
            case Load.loadcls_scope_v:
                return "Load.loadcls_scope_v";
            case Load.loadcls_vo:
                return "Load.loadcls_vo";
            case Load.loadcls_vC:
                return "Load.loadcls_vC";
            case Load.loadcls2_scope_vc:
                return "Load.loadcls2_scope_vc";
            case Load.loadcls2_scope_v:
                return "Load.loadcls2_scope_v";
            case Load.loadcls2_vo:
                return "Load.loadcls2_vo";
            case Array.array_create_i_vCc:
                return "Array.array_create_i_vCc";
            case Array.array_create_i_vCv:
                return "Array.array_create_i_vCv";
            case Array.array_fill_i_acL:
                return "Array.array_fill_i_acL";
            case Array.array_get_i_vac:
                return "Array.array_get_i_vac";
            case Array.array_get_i_vav:
                return "Array.array_get_i_vav";
            case Array.array_put_i_acc:
                return "Array.array_put_i_acc";
            case Array.array_put_i_acv:
                return "Array.array_put_i_acv";
            case Box.box_i_vv:
                return "Box.box_i_vv";
            case Box.box_i_vc:
                return "Box.box_i_vc";
            case Box.unbox_i_vo:
                return "Box.unbox_i_vo";
            case Box.box_C_vv:
                return "Box.box_C_vv";
            case Box.box_C_vC:
                return "Box.box_C_vC";
            case Box.unbox_C_vo:
                return "Box.unbox_C_vo";
            case Box.box_C_vvC:
                return "Box.box_C_vvC";
            case Box.box_C_vCC:
                return "Box.box_C_vCC";
            case Box.unbox_o_vo:
                return "Box.unbox_o_vo";
            case Box.box_S_vv:
                return "Box.box_S_vv";
            case Box.box_S_vc:
                return "Box.box_S_vc";
            case Box.unbox_S_vo:
                return "Box.unbox_S_vo";
            case Equals.equals_i_vvc:
                return "Equals.equals_i_vvc";
            case Equals.equals_i_vvv:
                return "Equals.equals_i_vvv";
            case Equals.equals_f_vvc:
                return "Equals.equals_f_vvc";
            case Equals.equals_f_vvv:
                return "Equals.equals_f_vvv";
            case Equals.equals_d_vvcc:
                return "Equals.equals_d_vvcc";
            case Equals.equals_d_vvv:
                return "Equals.equals_d_vvv";
            case Equals.equals_b_vvc:
                return "Equals.equals_b_vvc";
            case Equals.equals_b_vvv:
                return "Equals.equals_b_vvv";
            case Equals.equals_c_vvc:
                return "Equals.equals_c_vvc";
            case Equals.equals_c_vvv:
                return "Equals.equals_c_vvv";
            case Equals.equals_s_vvc:
                return "Equals.equals_s_vvc";
            case Equals.equals_s_vvv:
                return "Equals.equals_s_vvv";
            case Equals.equals_l_vvcc:
                return "Equals.equals_l_vvcc";
            case Equals.equals_l_vvv:
                return "Equals.equals_l_vvv";
            case Equals.equals_S_vvc:
                return "Equals.equals_S_vvc";
            case Equals.equals_S_vvv:
                return "Equals.equals_S_vvv";
            case Equals.equals_B_vvc:
                return "Equals.equals_B_vvc";
            case Equals.equals_B_vvv:
                return "Equals.equals_B_vvv";
            case Equals.equals_o_vvv:
                return "Equals.equals_o_vvv";
            case Equals.equals_C_vvc:
                return "Equals.equals_C_vvc";
            case Equals.equals_C_vvv:
                return "Equals.equals_C_vvv";
            case NotEquals.ne_i_vvc:
                return "NotEquals.ne_i_vvc";
            case NotEquals.ne_i_vvv:
                return "NotEquals.ne_i_vvv";
            case NotEquals.ne_f_vvc:
                return "NotEquals.ne_f_vvc";
            case NotEquals.ne_f_vvv:
                return "NotEquals.ne_f_vvv";
            case NotEquals.ne_d_vvcc:
                return "NotEquals.ne_d_vvcc";
            case NotEquals.ne_d_vvv:
                return "NotEquals.ne_d_vvv";
            case NotEquals.ne_b_vvc:
                return "NotEquals.ne_b_vvc";
            case NotEquals.ne_b_vvv:
                return "NotEquals.ne_b_vvv";
            case NotEquals.ne_c_vvc:
                return "NotEquals.ne_c_vvc";
            case NotEquals.ne_c_vvv:
                return "NotEquals.ne_c_vvv";
            case NotEquals.ne_s_vvc:
                return "NotEquals.ne_s_vvc";
            case NotEquals.ne_s_vvv:
                return "NotEquals.ne_s_vvv";
            case NotEquals.ne_l_vvcc:
                return "NotEquals.ne_l_vvcc";
            case NotEquals.ne_l_vvv:
                return "NotEquals.ne_l_vvv";
            case NotEquals.ne_S_vvc:
                return "NotEquals.ne_S_vvc";
            case NotEquals.ne_S_vvv:
                return "NotEquals.ne_S_vvv";
            case NotEquals.ne_B_vvc:
                return "NotEquals.ne_B_vvc";
            case NotEquals.ne_B_vvv:
                return "NotEquals.ne_B_vvv";
            case NotEquals.ne_o_vvv:
                return "NotEquals.ne_o_vvv";
            case NotEquals.ne_C_vvc:
                return "NotEquals.ne_C_vvc";
            case NotEquals.ne_C_vvv:
                return "NotEquals.ne_C_vvv";
            case GreaterThan.gt_i_vvc:
                return "GreaterThan.gt_i_vvc";
            case GreaterThan.gt_i_vvv:
                return "GreaterThan.gt_i_vvv";
            case GreaterThan.gt_f_vvc:
                return "GreaterThan.gt_f_vvc";
            case GreaterThan.gt_f_vvv:
                return "GreaterThan.gt_f_vvv";
            case GreaterThan.gt_d_vvcc:
                return "GreaterThan.gt_d_vvcc";
            case GreaterThan.gt_d_vvv:
                return "GreaterThan.gt_d_vvv";
            case GreaterThan.gt_b_vvc:
                return "GreaterThan.gt_b_vvc";
            case GreaterThan.gt_b_vvv:
                return "GreaterThan.gt_b_vvv";
            case GreaterThan.gt_c_vvc:
                return "GreaterThan.gt_c_vvc";
            case GreaterThan.gt_c_vvv:
                return "GreaterThan.gt_c_vvv";
            case GreaterThan.gt_s_vvc:
                return "GreaterThan.gt_s_vvc";
            case GreaterThan.gt_s_vvv:
                return "GreaterThan.gt_s_vvv";
            case GreaterThan.gt_l_vvcc:
                return "GreaterThan.gt_l_vvcc";
            case GreaterThan.gt_l_vvv:
                return "GreaterThan.gt_l_vvv";
            case GreaterThan.gt_S_vvc:
                return "GreaterThan.gt_S_vvc";
            case GreaterThan.gt_S_vvv:
                return "GreaterThan.gt_S_vvv";
            case GreaterThan.gt_C_vvc:
                return "GreaterThan.gt_C_vvc";
            case GreaterThan.gt_C_vvv:
                return "GreaterThan.gt_C_vvv";
            case LittleThan.lt_i_vvc:
                return "LittleThan.lt_i_vvc";
            case LittleThan.lt_i_vvv:
                return "LittleThan.lt_i_vvv";
            case LittleThan.lt_f_vvc:
                return "LittleThan.lt_f_vvc";
            case LittleThan.lt_f_vvv:
                return "LittleThan.lt_f_vvv";
            case LittleThan.lt_d_vvcc:
                return "LittleThan.lt_d_vvcc";
            case LittleThan.lt_d_vvv:
                return "LittleThan.lt_d_vvv";
            case LittleThan.lt_b_vvc:
                return "LittleThan.lt_b_vvc";
            case LittleThan.lt_b_vvv:
                return "LittleThan.lt_b_vvv";
            case LittleThan.lt_c_vvc:
                return "LittleThan.lt_c_vvc";
            case LittleThan.lt_c_vvv:
                return "LittleThan.lt_c_vvv";
            case LittleThan.lt_s_vvc:
                return "LittleThan.lt_s_vvc";
            case LittleThan.lt_s_vvv:
                return "LittleThan.lt_s_vvv";
            case LittleThan.lt_l_vvcc:
                return "LittleThan.lt_l_vvcc";
            case LittleThan.lt_l_vvv:
                return "LittleThan.lt_l_vvv";
            case LittleThan.lt_S_vvc:
                return "LittleThan.lt_S_vvc";
            case LittleThan.lt_S_vvv:
                return "LittleThan.lt_S_vvv";
            case LittleThan.lt_C_vvc:
                return "LittleThan.lt_C_vvc";
            case LittleThan.lt_C_vvv:
                return "LittleThan.lt_C_vvv";
            case GreaterEquals.ge_i_vvc:
                return "GreaterEquals.ge_i_vvc";
            case GreaterEquals.ge_i_vvv:
                return "GreaterEquals.ge_i_vvv";
            case GreaterEquals.ge_f_vvc:
                return "GreaterEquals.ge_f_vvc";
            case GreaterEquals.ge_f_vvv:
                return "GreaterEquals.ge_f_vvv";
            case GreaterEquals.ge_d_vvcc:
                return "GreaterEquals.ge_d_vvcc";
            case GreaterEquals.ge_d_vvv:
                return "GreaterEquals.ge_d_vvv";
            case GreaterEquals.ge_b_vvc:
                return "GreaterEquals.ge_b_vvc";
            case GreaterEquals.ge_b_vvv:
                return "GreaterEquals.ge_b_vvv";
            case GreaterEquals.ge_c_vvc:
                return "GreaterEquals.ge_c_vvc";
            case GreaterEquals.ge_c_vvv:
                return "GreaterEquals.ge_c_vvv";
            case GreaterEquals.ge_s_vvc:
                return "GreaterEquals.ge_s_vvc";
            case GreaterEquals.ge_s_vvv:
                return "GreaterEquals.ge_s_vvv";
            case GreaterEquals.ge_l_vvcc:
                return "GreaterEquals.ge_l_vvcc";
            case GreaterEquals.ge_l_vvv:
                return "GreaterEquals.ge_l_vvv";
            case GreaterEquals.ge_S_vvc:
                return "GreaterEquals.ge_S_vvc";
            case GreaterEquals.ge_S_vvv:
                return "GreaterEquals.ge_S_vvv";
            case GreaterEquals.ge_C_vvc:
                return "GreaterEquals.ge_C_vvc";
            case GreaterEquals.ge_C_vvv:
                return "GreaterEquals.ge_C_vvv";
            case LittleEquals.le_i_vvc:
                return "LittleEquals.le_i_vvc";
            case LittleEquals.le_i_vvv:
                return "LittleEquals.le_i_vvv";
            case LittleEquals.le_f_vvc:
                return "LittleEquals.le_f_vvc";
            case LittleEquals.le_f_vvv:
                return "LittleEquals.le_f_vvv";
            case LittleEquals.le_d_vvcc:
                return "LittleEquals.le_d_vvcc";
            case LittleEquals.le_d_vvv:
                return "LittleEquals.le_d_vvv";
            case LittleEquals.le_b_vvc:
                return "LittleEquals.le_b_vvc";
            case LittleEquals.le_b_vvv:
                return "LittleEquals.le_b_vvv";
            case LittleEquals.le_c_vvc:
                return "LittleEquals.le_c_vvc";
            case LittleEquals.le_c_vvv:
                return "LittleEquals.le_c_vvv";
            case LittleEquals.le_s_vvc:
                return "LittleEquals.le_s_vvc";
            case LittleEquals.le_s_vvv:
                return "LittleEquals.le_s_vvv";
            case LittleEquals.le_l_vvcc:
                return "LittleEquals.le_l_vvcc";
            case LittleEquals.le_l_vvv:
                return "LittleEquals.le_l_vvv";
            case LittleEquals.le_S_vvc:
                return "LittleEquals.le_S_vvc";
            case LittleEquals.le_S_vvv:
                return "LittleEquals.le_S_vvv";
            case LittleEquals.le_C_vvc:
                return "LittleEquals.le_C_vvc";
            case LittleEquals.le_C_vvv:
                return "LittleEquals.le_C_vvv";
            case Subtract.sub_i_vc:
                return "Subtract.sub_i_vc";
            case Subtract.sub_i_vvc:
                return "Subtract.sub_i_vvc";
            case Subtract.sub_i_vv:
                return "Subtract.sub_i_vv";
            case Subtract.sub_i_vvv:
                return "Subtract.sub_i_vvv";
            case Subtract.sub_i_vcv:
                return "Subtract.sub_i_vcv";
            case Subtract.sub_f_vc:
                return "Subtract.sub_f_vc";
            case Subtract.sub_f_vvc:
                return "Subtract.sub_f_vvc";
            case Subtract.sub_f_vv:
                return "Subtract.sub_f_vv";
            case Subtract.sub_f_vvv:
                return "Subtract.sub_f_vvv";
            case Subtract.sub_f_vcv:
                return "Subtract.sub_f_vcv";
            case Subtract.sub_b_vc:
                return "Subtract.sub_b_vc";
            case Subtract.sub_b_vvc:
                return "Subtract.sub_b_vvc";
            case Subtract.sub_b_vv:
                return "Subtract.sub_b_vv";
            case Subtract.sub_b_vvv:
                return "Subtract.sub_b_vvv";
            case Subtract.sub_b_vcv:
                return "Subtract.sub_b_vcv";
            case Subtract.sub_s_vc:
                return "Subtract.sub_s_vc";
            case Subtract.sub_s_vvc:
                return "Subtract.sub_s_vvc";
            case Subtract.sub_s_vv:
                return "Subtract.sub_s_vv";
            case Subtract.sub_s_vvv:
                return "Subtract.sub_s_vvv";
            case Subtract.sub_s_vcv:
                return "Subtract.sub_s_vcv";
            case Subtract.sub_l_vc:
                return "Subtract.sub_l_vc";
            case Subtract.sub_l_vvc:
                return "Subtract.sub_l_vvc";
            case Subtract.sub_l_vv:
                return "Subtract.sub_l_vv";
            case Subtract.sub_l_vvv:
                return "Subtract.sub_l_vvv";
            case Subtract.sub_l_vcv:
                return "Subtract.sub_l_vcv";
            case Subtract.sub_d_vc:
                return "Subtract.sub_d_vc";
            case Subtract.sub_d_vvc:
                return "Subtract.sub_d_vvc";
            case Subtract.sub_d_vv:
                return "Subtract.sub_d_vv";
            case Subtract.sub_d_vvv:
                return "Subtract.sub_d_vvv";
            case Subtract.sub_d_vcv:
                return "Subtract.sub_d_vcv";
            case Multiply.mul_i_vc:
                return "Multiply.mul_i_vc";
            case Multiply.mul_i_vvc:
                return "Multiply.mul_i_vvc";
            case Multiply.mul_i_vv:
                return "Multiply.mul_i_vv";
            case Multiply.mul_i_vvv:
                return "Multiply.mul_i_vvv";
            case Multiply.mul_f_vc:
                return "Multiply.mul_f_vc";
            case Multiply.mul_f_vvc:
                return "Multiply.mul_f_vvc";
            case Multiply.mul_f_vv:
                return "Multiply.mul_f_vv";
            case Multiply.mul_f_vvv:
                return "Multiply.mul_f_vvv";
            case Multiply.mul_b_vc:
                return "Multiply.mul_b_vc";
            case Multiply.mul_b_vvc:
                return "Multiply.mul_b_vvc";
            case Multiply.mul_b_vv:
                return "Multiply.mul_b_vv";
            case Multiply.mul_b_vvv:
                return "Multiply.mul_b_vvv";
            case Multiply.mul_s_vc:
                return "Multiply.mul_s_vc";
            case Multiply.mul_s_vvc:
                return "Multiply.mul_s_vvc";
            case Multiply.mul_s_vv:
                return "Multiply.mul_s_vv";
            case Multiply.mul_s_vvv:
                return "Multiply.mul_s_vvv";
            case Multiply.mul_l_vc:
                return "Multiply.mul_l_vc";
            case Multiply.mul_l_vvc:
                return "Multiply.mul_l_vvc";
            case Multiply.mul_l_vv:
                return "Multiply.mul_l_vv";
            case Multiply.mul_l_vvv:
                return "Multiply.mul_l_vvv";
            case Multiply.mul_d_vc:
                return "Multiply.mul_d_vc";
            case Multiply.mul_d_vvc:
                return "Multiply.mul_d_vvc";
            case Multiply.mul_d_vv:
                return "Multiply.mul_d_vv";
            case Multiply.mul_d_vvv:
                return "Multiply.mul_d_vvv";
            case Div.div_i_vc:
                return "Div.div_i_vc";
            case Div.div_i_vvc:
                return "Div.div_i_vvc";
            case Div.div_i_vv:
                return "Div.div_i_vv";
            case Div.div_i_vvv:
                return "Div.div_i_vvv";
            case Div.div_i_vcv:
                return "Div.div_i_vcv";
            case Div.div_f_vc:
                return "Div.div_f_vc";
            case Div.div_f_vvc:
                return "Div.div_f_vvc";
            case Div.div_f_vv:
                return "Div.div_f_vv";
            case Div.div_f_vvv:
                return "Div.div_f_vvv";
            case Div.div_f_vcv:
                return "Div.div_f_vcv";
            case Div.div_b_vc:
                return "Div.div_b_vc";
            case Div.div_b_vvc:
                return "Div.div_b_vvc";
            case Div.div_b_vv:
                return "Div.div_b_vv";
            case Div.div_b_vvv:
                return "Div.div_b_vvv";
            case Div.div_b_vcv:
                return "Div.div_b_vcv";
            case Div.div_s_vc:
                return "Div.div_s_vc";
            case Div.div_s_vvc:
                return "Div.div_s_vvc";
            case Div.div_s_vv:
                return "Div.div_s_vv";
            case Div.div_s_vvv:
                return "Div.div_s_vvv";
            case Div.div_s_vcv:
                return "Div.div_s_vcv";
            case Div.div_l_vc:
                return "Div.div_l_vc";
            case Div.div_l_vvc:
                return "Div.div_l_vvc";
            case Div.div_l_vv:
                return "Div.div_l_vv";
            case Div.div_l_vvv:
                return "Div.div_l_vvv";
            case Div.div_l_vcv:
                return "Div.div_l_vcv";
            case Div.div_d_vc:
                return "Div.div_d_vc";
            case Div.div_d_vvc:
                return "Div.div_d_vvc";
            case Div.div_d_vv:
                return "Div.div_d_vv";
            case Div.div_d_vvv:
                return "Div.div_d_vvv";
            case Div.div_d_vcv:
                return "Div.div_d_vcv";
            case Mod.mod_i_vc:
                return "Mod.mod_i_vc";
            case Mod.mod_i_vvc:
                return "Mod.mod_i_vvc";
            case Mod.mod_i_vv:
                return "Mod.mod_i_vv";
            case Mod.mod_i_vvv:
                return "Mod.mod_i_vvv";
            case Mod.mod_i_vcv:
                return "Mod.mod_i_vcv";
            case Mod.mod_f_vc:
                return "Mod.mod_f_vc";
            case Mod.mod_f_vvc:
                return "Mod.mod_f_vvc";
            case Mod.mod_f_vv:
                return "Mod.mod_f_vv";
            case Mod.mod_f_vvv:
                return "Mod.mod_f_vvv";
            case Mod.mod_f_vcv:
                return "Mod.mod_f_vcv";
            case Mod.mod_b_vc:
                return "Mod.mod_b_vc";
            case Mod.mod_b_vvc:
                return "Mod.mod_b_vvc";
            case Mod.mod_b_vv:
                return "Mod.mod_b_vv";
            case Mod.mod_b_vvv:
                return "Mod.mod_b_vvv";
            case Mod.mod_b_vcv:
                return "Mod.mod_b_vcv";
            case Mod.mod_s_vc:
                return "Mod.mod_s_vc";
            case Mod.mod_s_vvc:
                return "Mod.mod_s_vvc";
            case Mod.mod_s_vv:
                return "Mod.mod_s_vv";
            case Mod.mod_s_vvv:
                return "Mod.mod_s_vvv";
            case Mod.mod_s_vcv:
                return "Mod.mod_s_vcv";
            case Mod.mod_l_vc:
                return "Mod.mod_l_vc";
            case Mod.mod_l_vvc:
                return "Mod.mod_l_vvc";
            case Mod.mod_l_vv:
                return "Mod.mod_l_vv";
            case Mod.mod_l_vvv:
                return "Mod.mod_l_vvv";
            case Mod.mod_l_vcv:
                return "Mod.mod_l_vcv";
            case Mod.mod_d_vc:
                return "Mod.mod_d_vc";
            case Mod.mod_d_vvc:
                return "Mod.mod_d_vvc";
            case Mod.mod_d_vv:
                return "Mod.mod_d_vv";
            case Mod.mod_d_vvv:
                return "Mod.mod_d_vvv";
            case Mod.mod_d_vcv:
                return "Mod.mod_d_vcv";
            case Neg.neg_i_vv:
                return "Neg.neg_i_vv";
            case Neg.neg_f_vv:
                return "Neg.neg_f_vv";
            case Neg.neg_d_vv:
                return "Neg.neg_d_vv";
            case Neg.neg_b_vv:
                return "Neg.neg_b_vv";
            case Neg.neg_s_vv:
                return "Neg.neg_s_vv";
            case Neg.neg_l_vv:
                return "Neg.neg_l_vv";
            case IncDec.inc_i_ovc:
                return "IncDec.inc_i_ovc";
            case IncDec.inc_i_ovv:
                return "IncDec.inc_i_ovv";
            case IncDec.inc_f_ov:
                return "IncDec.inc_f_ov";
            case IncDec.inc_f_ovc:
                return "IncDec.inc_f_ovc";
            case IncDec.inc_d_ov:
                return "IncDec.inc_d_ov";
            case IncDec.inc_d_ovcc:
                return "IncDec.inc_d_ovcc";
            case IncDec.inc_b_ov:
                return "IncDec.inc_b_ov";
            case IncDec.inc_b_ovcc:
                return "IncDec.inc_b_ovcc";
            case IncDec.inc_s_ov:
                return "IncDec.inc_s_ov";
            case IncDec.inc_s_ovcc:
                return "IncDec.inc_s_ovcc";
            case IncDec.inc_l_ov:
                return "IncDec.inc_l_ov";
            case IncDec.inc_l_ovcc:
                return "IncDec.inc_l_ovcc";
            case And.and_vv:
                return "And.and_vv";
            case And.and_vvv:
                return "And.and_vvv";
            case Or.or_vv:
                return "Or.or_vv";
            case Or.or_vvv:
                return "Or.or_vvv";
            case Not.not_vv:
                return "Not.not_vv";
        }
        return null;
    }
}
