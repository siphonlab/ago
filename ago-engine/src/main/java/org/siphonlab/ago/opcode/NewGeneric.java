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

/**
 * new generic, each opcode is according {{@link New}}
 */
public class NewGeneric {
    public static final int KIND_NEW_GENERIC = 0x14_000000;
    public static final int OP                  = 0x14;

    // G -> these Class need instantiate with type arguments within the context,
    // i.e. SClass<T> {g as GClass<T>}
    public static final int newG_vC = 0x14_00_01_02;
    public static final int newG_child_voC = 0x14_00_03_03;
    public static final int newG_method_voCm = 0x14_00_04_04;
    public static final int newG_cls_method_vCm = 0x14_00_05_03;
    public static final int newG_scope_child_vcC = 0x14_00_06_03;
    public static final int newG_scope_method_vcCm = 0x14_00_07_04;
    public static final int newG_scope_method_fix_vcCm = 0x14_00_0b_04;
    public static final int newG_dynamic_voa = 0x14_00_09_03;
    public static final int newG_scope_method_fix_voma = 0x14_00_0a_04;
    // method in trait-within-scope
    public static final int newG_method_voTm = 0x14_00_0c_04;

    // g -> the Class is GenericTypeCode, i.e. class C<T as [Animal]>{ t as T; .. t.bark()}
    // here type of t is GenericTypeCode, but we know it can accept Animal
    public static final int newg_vC = 0x14_01_01_02;
    public static final int newg_child_voC = 0x14_01_03_03;
    public static final int newg_method_voCm = 0x14_01_04_04;
    public static final int newg_cls_method_vCm = 0x14_01_05_03;
    public static final int newg_scope_child_vcC = 0x14_01_06_03;
    public static final int newg_scope_method_vcCm = 0x14_01_07_04;
    public static final int newg_scope_method_fix_vcCm = 0x14_01_0b_04;
    public static final int newg_dynamic_voa = 0x14_01_09_03;
    public static final int newg_scope_method_fix_voma = 0x14_01_0a_04;
    // method in trait-within-scope
    public static final int newg_method_voTm = 0x14_01_0c_04;

    public static String getName(int code) {
        return switch (code) {
            case 0x14_00_01_02 -> "newG_vC";
            case 0x14_00_03_03 -> "newG_child_voC";
            case 0x14_00_04_04 -> "newG_method_voCm";
            case 0x14_00_05_03 -> "newG_cls_method_vCm";
            case 0x14_00_06_03 -> "newG_scope_child_vcC";
            case 0x14_00_07_04 -> "newG_scope_method_vcCm";
            case 0x14_00_0b_04 -> "newG_scope_method_fix_vcCm";
            case 0x14_00_09_03 -> "newG_dynamic_voa";
            case 0x14_00_0a_04 -> "newG_scope_method_fix_voma";
            case 0x14_00_0c_04 -> "newG_method_voTm";
            case 0x14_01_01_02 -> "newg_vC";
            case 0x14_01_03_03 -> "newg_child_voC";
            case 0x14_01_04_04 -> "newg_method_voCm";
            case 0x14_01_05_03 -> "newg_cls_method_vCm";
            case 0x14_01_06_03 -> "newg_scope_child_vcC";
            case 0x14_01_07_04 -> "newg_scope_method_vcCm";
            case 0x14_01_0b_04 -> "newg_scope_method_fix_vcCm";
            case 0x14_01_09_03 -> "newg_dynamic_voa";
            case 0x14_01_0a_04 -> "newg_scope_method_fix_voma";
            case 0x14_01_0c_04 -> "newg_method_voTm";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }
}
