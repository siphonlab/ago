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
 *
 */
public class New {
    public static final int KIND_NEW = 0x04_000000;
    public static final int OP                  = 0x04;

    // new_vc(target, class name)
    public static final int new_vC = 0x04_ff_01_02;

    // new_vco(target, parent scope instance, child_class_name or method_fixed_name), create an inner class of `class of the scope instance`
    public static final int new_child_voC = 0x04_ff_03_03;

    /**
     * new_method(target, parent scope instance, scope class, method_simple_name), create instance of method
     * invoke method by name, under class of scope instance, the scope class is indicate
     * THESE _method_ OPCODES SUPPORT OVERRIDING, how it works?
     * from the `C.methods + method_simple_name`, we got a method index, and for its class hierarchy, same signature method has same method index
     * therefore the C class is not exactly `o.class`, but a container of method_simple_name to locate method index, it can be o.class.super or o.class.extends,
     * while invoke, `o.class.methods[method_index]` will be used
     */
    public static final int new_method_voCm = 0x04_ff_04_04;

    // new_cls_method(target, class name, method_simple_name), create a method of a top level class instance, static method
    public static final int new_cls_method_vCm = 0x04_ff_05_03;

    // new_scope_child(target, scope offset, classname), create an inner class in scope, if offset = 0 it's the method itself
    public static final int new_scope_child_vcC = 0x04_ff_06_03;

    // new_scope_method(target, scope offset, scope class, method_simple_name), create instance of method
    // the scope class is indicate, for transformCode need parent class of method
    public static final int new_scope_method_vcCm = 0x04_ff_07_04;

    // new_scope_method_fix_vcCm(target, scope offset, scope class, method_simple_name), create instance of method
    // scope class is parent of method, it's not the class of scope instance,
    // it was designed for `super.xxx`, include GenericIntermediaClass, the intermediate class will apply template and transform to terminal instantiation class
    // maybe can work for (class.this as SomeClass).method(), but not sure that can be optimized
    public static final int new_scope_method_fix_vcCm = 0x04_ff_0b_04;

    // new_vv(target, scoped class instance)
    public static final int new_vo = 0x04_ff_08_02;

    // new_dynamic(target, scoped class instance, args_array)
    // auto find constructor to match the args to create instance
    // if the class instance is an inner class (need scope) and no scope given, throw error
    public static final int new_dynamic_voa = 0x04_ff_09_03;

    // new_dynamic(target, parent scope instance, method_simple_name, args_array)
    // auto find method for the simple name that matches args
    public static final int new_scope_method_fix_voma = 0x04_ff_0a_04;

    // invoke method of interface. from I.method -> Instance.C.implemented[interface id][method id in interface-> method id in class]
    public static final int new_method_voIm = 0x04_ff_0c_04;


    // native version of above op
    public static final int newn_vC = 0x04_ff_11_02;
    public static final int newn_child_voC = 0x04_ff_13_03;
    public static final int newn_scope_child_vcC = 0x04_ff_16_03;


    public static String getName(int code) {
        return switch (code) {
            case 0x04_ff_01_02 -> "new_vC";
            case 0x04_ff_03_03 -> "new_child_voC";
            case 0x04_ff_04_04 -> "new_method_voCm";
            case 0x04_ff_05_03 -> "new_cls_method_vCm";
            case 0x04_ff_06_03 -> "new_scope_child_vcC";
            case 0x04_ff_07_04 -> "new_scope_method_vcCm";
            case 0x04_ff_08_02 -> "new_vo";
            case 0x04_ff_09_03 -> "new_dynamic_voa";
            case 0x04_ff_0a_04 -> "new_scope_method_fix_voma";
            case 0x04_ff_0b_04 -> "new_scope_method_fix_vcCm";
            case 0x04_ff_0c_04 -> "new_method_voIm";
            case newn_vC        -> "newn_vC";
            case newn_child_voC     -> "newn_child_voC";
            case newn_scope_child_vcC       -> "newn_scope_child_vcC";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }
}
