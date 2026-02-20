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

public class Load {
    public static final int KIND_LOAD = 0x08_000000;
    public static final int OP                  = 0x08;

    // load_vc(target_slot, offset),
    // load the scope instance, 0 is the CallFrame, 1 is the scope, 2 is the scope of scope
    public static final int loadscope_vc = 0x08_01_01_02;
    // load(target_slot), offset = 1
    public static final int loadscope_v = 0x08_01_02_01;

    // load class(target_slot, class name), according ConstClass()
    public static final int loadcls_vC = 0x08_10_01_02;
    // load class(target_slot, instance), load class of instance, according ClassOf(Instance)
    public static final int loadcls_vo = 0x08_10_02_02;

    // loadcls_scope(target_slot, offset), load the class of the offseted scope
    // according ClassOf(Scope)
    public static final int loadcls_scope_vc = 0x08_11_01_02;

    // loadcls_scope(target_slot), the class is the class of parent scope, equals loadcls_scope(target_slot, 1)
    // according ClassOf(Scope(1))
    public static final int loadcls_scope_v = 0x08_11_02_01;

    // loadmeta2(target slot, instance)
    public static final int loadcls2_vo = 0x08_20_01_02;
    // loadmeta instruction can be implemented with loadcls_vo, just for shorthands
    // loadmeta2_scope(target_slot, offset), load the meta's meta of the offseted scope
    public static final int loadcls2_scope_vc = 0x08_21_01_02;
    // loadmeta_scope(target_slot), load the meta of the parent scope
    public static final int loadcls2_scope_v = 0x08_21_02_01;

    // bindcls_scope(target_slot, classname, instance), bind the child class of the instance
    public static final int bindcls_vCo = 0x08_30_01_03;
    // bindcls_scope(target_slot, classname, scope_offset), bind the child class of the scope(at offset)
    public static final int bindcls_scope_vCc = 0x08_31_01_03;

    public static String getName(int code) {
        return switch (code) {
            case 0x08_01_01_02 -> "loadscope_vc";
            case 0x08_01_02_01 -> "loadscope_v";

            case 0x08_10_01_02 -> "loadcls_vC";
            case 0x08_10_02_02 -> "loadcls_vo";

            case 0x08_11_01_02 -> "loadcls_scope_vc";
            case 0x08_11_02_01 -> "loadcls_scope_v";

            case 0x08_20_01_02 -> "loadcls2_vo";
            case 0x08_21_01_02 -> "loadcls2_scope_vc";
            case 0x08_21_02_01 -> "loadcls2_scope_v";

            case 0x08_30_01_03 -> "bindcls_vCo";
            case 0x08_31_01_03 -> "bindcls_scope_vCc";

            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }
}
