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
package org.siphonlab.ago;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.FieldMaker;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;

import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;

public class DefaultSlotsCreatorFactory implements SlotsCreatorFactory {

    private static String fieldName(AgoSlotDef agoSlotDef) {
        return agoSlotDef.getName() + "_" + agoSlotDef.getIndex();
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();
        if(slotDefs == null || slotDefs.length == 0) return null;

        ListValuedMap<TypeCode, AgoSlotDef> groupBy = new ArrayListValuedHashMap<>();
        var clsCM = ClassMaker.begin(composeNativeClassName(agoClass) + "_slots").public_().implement(Slots.class);
        clsCM.addConstructor().public_();

        FieldMaker[] fileMakers = new FieldMaker[slotDefs.length];
        for (int i = 0; i < slotDefs.length; i++) {
            var slotDesc = slotDefs[i];
            groupBy.put(slotDesc.getTypeCode(), slotDesc);
            fileMakers[i] = clsCM.addField(typeOf(slotDesc.getTypeCode()), fieldName(slotDesc)).private_();
        }

        for (var typeCode : groupBy.keySet()) {
            List<AgoSlotDef> cases = groupBy.get(typeCode);
            int[] casesInts = new int[cases.size()];
            for (int i = 0; i < cases.size(); i++) {
                var caseSlot = cases.get(i);
                casesInts[i] = caseSlot.getIndex();
            }
            if(typeCode != VOID) {
                MethodMaker getter = clsCM.addMethod(typeOf(typeCode), "get" + slotFunctionName(typeCode), int.class).public_().override();
                Label defaultBranch = getter.label();
                Label[] entrances = new Label[cases.size()];
                for (int i = 0; i < cases.size(); i++) {
                    entrances[i] = getter.label();
                }
                getter.param(0).switch_(defaultBranch, casesInts, entrances);
                defaultBranch.here();
                getter.new_(UnsupportedOperationException.class, getter.concat(getter.param(0), " not found")).throw_();
                for (int i = 0; i < cases.size(); i++) {
                    var caseSlot = cases.get(i);
                    entrances[i].here();
                    getter.return_(getter.field(fieldName(caseSlot)));
                }
            }

            if (typeCode != VOID) {
                MethodMaker setter = clsCM.addMethod(void.class, "set" + slotFunctionName(typeCode), int.class, typeOf(typeCode)).public_().override();
                Label defaultBranch = setter.label();
                Label[] entrances = new Label[cases.size()];
                for (int i = 0; i < cases.size(); i++) {
                    entrances[i] = setter.label();
                }
                setter.param(0).switch_(defaultBranch, casesInts, entrances);
                defaultBranch.here();
                setter.new_(UnsupportedOperationException.class, setter.concat(setter.param(0), " not found")).throw_();
                for (int i = 0; i < cases.size(); i++) {
                    var caseSlot = cases.get(i);
                    entrances[i].here();
                    setter.field(fieldName(caseSlot)).set(setter.param(1));
                    setter.return_();
                }
            }

            if (typeCode.isNumber()) {
                MethodMaker inc = clsCM.addMethod(void.class, "inc" + slotFunctionName(typeCode), int.class, typeOf(typeCode)).public_().override();
                Label defaultBranch = inc.label();
                Label[] entrances = new Label[cases.size()];
                for (int i = 0; i < cases.size(); i++) {
                    entrances[i] = inc.label();
                }
                inc.param(0).switch_(defaultBranch, casesInts, entrances);
                defaultBranch.here();
                inc.new_(UnsupportedOperationException.class, inc.concat(inc.param(0), " not found")).throw_();
                for (int i = 0; i < cases.size(); i++) {
                    var caseSlot = cases.get(i);
                    entrances[i].here();
                    var fld = inc.field(fieldName(caseSlot));
                    fld.inc(inc.param(1));
//                        var exitLabel = inc.label();
//                        fld.ifGe(zero(typeCode), exitLabel);
//                        inc.new_(IllegalArgumentException.class, "ooooops").throw_();
//                        exitLabel.here();
                    inc.return_();
                }
            }
        }

        var slotsClass = clsCM.finish();

        var providerCM = ClassMaker.begin(composeNativeClassName(agoClass) + "_slots_provider").public_().implement(SlotsCreator.class);
        providerCM.addConstructor().public_();
        Class<?>[] classes = new Class[slotDefs.length];
        for (int i = 0; i < slotDefs.length; i++) {
            var slotDesc = slotDefs[i];
            TypeCode typeCode = slotDesc.getTypeCode();
            if (typeCode == CLASS_REF) {
                classes[i] = AgoClass.class;
            } else {
                classes[i] = typeOf(typeCode);
            }
        }


        var slotTypesField = providerCM.addField(Class[].class, "slotTypes").private_().static_().final_();
        MethodMaker clinit = providerCM.addClinit();
        var arr = clinit.new_(Class[].class, classes.length);
        for (int i = 0; i < classes.length; i++) {
            Class<?> aClass = classes[i];
            arr.aset(i, aClass);
        }
        clinit.field("slotTypes").set(arr);

        MethodMaker provide = providerCM.addMethod(Slots.class, "create").public_().override();
        provide.return_(provide.new_(slotsClass));

        var getSlotsType = providerCM.addMethod(Class.class, "getSlotType", int.class).public_().override();
        getSlotsType.return_(getSlotsType.field("slotTypes").aget(getSlotsType.param(0)));
        try {
            Class<?> providerCls = providerCM.finish();
            return (SlotsCreator) ConstructorUtils.invokeConstructor(providerCls);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String composeNativeClassName(AgoClass agoClass) {
        return URLEncoder.encode(agoClass.fullname, StandardCharsets.UTF_8);
        // return agoClass.fullname.replace('.', '$');
    }

    private static Object zero(TypeCode typeCode) {
        return switch (typeCode.value) {
            case CHAR_VALUE -> (char) 0;
            case FLOAT_VALUE -> (float) 0;
            case DOUBLE_VALUE -> (double) 0;
            case BYTE_VALUE -> (byte) 0;
            case SHORT_VALUE -> (short) 0;
            case INT_VALUE -> (int) 0;
            case LONG_VALUE -> (long) 0;
            default -> throw new IllegalArgumentException("'%s' is not a number".formatted(typeCode));
        };
    }

    private static Object one(TypeCode typeCode) {
        return switch (typeCode.value) {
            case CHAR_VALUE -> (char) 1;
            case FLOAT_VALUE -> (float) 1;
            case DOUBLE_VALUE -> (double) 1;
            case BYTE_VALUE -> (byte) 1;
            case SHORT_VALUE -> (short) 1;
            case INT_VALUE -> (int) 1;
            case LONG_VALUE -> (long) 1;
            default -> throw new IllegalArgumentException("'%s' is not a number".formatted(typeCode));
        };
    }

    public static Class<?> typeOf(TypeCode typeCode) {
        return switch (typeCode.value) {
            case VOID_VALUE -> Object.class;
            case BOOLEAN_VALUE -> boolean.class;
            case CHAR_VALUE -> char.class;
            case FLOAT_VALUE -> float.class;
            case DOUBLE_VALUE -> double.class;
            case BYTE_VALUE -> byte.class;
            case SHORT_VALUE -> short.class;
            case INT_VALUE -> int.class;
            case LONG_VALUE -> long.class;
            case OBJECT_VALUE -> Instance.class;
            case NULL_VALUE -> null;
            case STRING_VALUE -> String.class;
            case CLASS_REF_VALUE -> int.class;
            default -> throw new IllegalStateException("Unexpected value: " + typeCode);
        };
    }

    public static String slotFunctionName(TypeCode typeCode) {
        return switch (typeCode.value) {
            case VOID_VALUE -> "Void";
            case BOOLEAN_VALUE -> "Boolean";
            case CHAR_VALUE -> "Char";
            case FLOAT_VALUE -> "Float";
            case DOUBLE_VALUE -> "Double";
            case BYTE_VALUE -> "Byte";
            case SHORT_VALUE -> "Short";
            case INT_VALUE -> "Int";
            case LONG_VALUE -> "Long";
            case OBJECT_VALUE -> "Object";
            case NULL_VALUE -> "Null";
            case STRING_VALUE -> "String";
            case CLASS_REF_VALUE -> "ClassRef";
            default -> {
                if (typeCode.isGeneric())
                    yield "Object";
                throw new IllegalStateException("Unexpected value: " + typeCode);
            }
        };
    }

}
