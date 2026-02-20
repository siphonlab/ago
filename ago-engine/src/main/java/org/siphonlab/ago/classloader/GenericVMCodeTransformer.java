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
package org.siphonlab.ago.classloader;

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.opcode.*;
import org.siphonlab.ago.opcode.arithmetic.*;
import org.siphonlab.ago.opcode.compare.*;

import java.util.HashMap;
import java.util.Map;

import static org.siphonlab.ago.AgoClass.TYPE_INTERFACE;
import static org.siphonlab.ago.AgoClass.TYPE_TRAIT;
import static org.siphonlab.ago.TypeCode.GENERIC_TYPE_START;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;

public class GenericVMCodeTransformer {

    private final ClassHeader myClassHeader;

    private Map<GenericTypeArguments, IoBuffer> genericCodeCache = new HashMap<>();

    public GenericVMCodeTransformer(ClassHeader classHeader){
        this.myClassHeader = classHeader;
    }

    public IoBuffer transform(IoBuffer bodyCodeBuffer, GenericTypeArguments genericTypeArguments, ClassHeader instantFunction, Map<String, ClassHeader> headers){
        var existed = genericCodeCache.get(genericTypeArguments);
        if (existed != null) {
            return existed;
        }

        var strings = myClassHeader.loadStrings();
        var code = IoBuffer.allocate(bodyCodeBuffer.remaining()).put(bodyCodeBuffer).flip();

        boolean isMethod = false;
        while (code.hasRemaining()) {
            var instruction = code.getInt();
            switch (instruction >> 24) {
//                    case Load.loadcls_vC:
//                    case Load.bindcls_vCo:
//                    case Load.bindcls_scope_vCc:
                case Move.OP:
                case Array.OP:
                case Accept.OP:
                case Return.OP: {
                    var type = (instruction & OpCode.DTYPE_MASK) >> 16;
                    if (type >= GENERIC_TYPE_START) {
                        int instruction2 = instruction & OpCode.DTYPE_MASK_NEG;
                        switch (instruction2) {
                            case Move.move_vv:
                            case Move.move_fld_vov:
                            case Move.move_fld_ovv:

                            case Array.array_put_acc:
                            case Array.array_put_acv:
                            case Array.array_put_avc:
                            case Array.array_put_avv:
                            case Array.array_get_vac:
                            case Array.array_get_vav:

                            case Return.return_v:
                                code.putInt(code.position() - 4, instruction2 | (genericTypeArguments.mapTypeCode(type) << 16));
                                break;
                            case Accept.accept_v:
                                code.putInt(code.position() - 4, instruction2 | (genericTypeArguments.mapTypeCode(type) << 16));
                                break;

                            case Array.array_create_vCc:
                            case Array.array_create_vCv: {
                                if (instruction == Array.array_create_g_vCc || instruction == Array.array_create_g_vCv) {
                                    // replace opcode to array_create_o
                                    code.putInt(code.position() - 4, instruction == Array.array_create_g_vCc ? Array.array_create_o_vCc : Array.array_create_o_vCv);
                                    instantiateClassName(code, 1, strings, genericTypeArguments, instantFunction, headers);
                                } else {
                                    code.putInt(code.position() - 4, instruction2 | (genericTypeArguments.mapTypeCode(type) << 16));
                                }
                                break;
                            }
                            case Move.move_copy_ooC:
                                instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                                break;
                        }
                    }
                }
                break;
                case NewGeneric.OP:
                    switch (instruction) {
                        case NewGeneric.newG_vC: {
                            var classHeader = instantiateClassName(code, 1, strings, genericTypeArguments, instantFunction, headers);
                            replaceWithInstruction(code, classHeader.isNativeClass() ? New.newn_vC : New.new_vC);
                            break;
                        }
                        case NewGeneric.newg_vC: {
                            var instantiation = updateGenericCodeClass(code, 1, strings, genericTypeArguments, instantFunction, headers);
                            replaceWithInstruction(code, instantiation.isNativeClass() ? New.newn_vC : New.new_vC);
                            break;
                        }

                        case NewGeneric.newG_child_voC: {
                            var instantiation = instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            replaceWithInstruction(code, instantiation.isNativeClass() ? New.newn_child_voC : New.new_child_voC);
                            break;
                        }

                        case NewGeneric.newG_method_voCm: {
                            var instantiation = instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            if (instantiation.type == TYPE_INTERFACE || instantiation.type == TYPE_TRAIT) {
                                replaceWithInstruction(code, New.new_method_voIm);
                            } else {
                                replaceWithInstruction(code, New.new_method_voCm);
                            }
                            isMethod = true;
                            break;
                        }
                        case NewGeneric.newG_method_voTm: {
                            replaceWithNew(code, New.new_method_voCm);
                            instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            isMethod = true;
                            break;
                        }
                        case NewGeneric.newg_method_voCm: {
                            replaceWithNew(code, instruction);
                            var instantiation = updateGenericCodeClass(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            if (instantiation.type == TYPE_INTERFACE || instantiation.type == TYPE_TRAIT) {
                                replaceWithInstruction(code, New.new_method_voIm);
                            }
                            isMethod = true;
                            break;
                        }
                        case NewGeneric.newg_method_voTm: {
                            replaceWithNew(code, New.new_method_voCm);
                            updateGenericCodeClass(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            isMethod = true;
                            break;
                        }
//                    case New.new_cls_method_vCm:
                        case NewGeneric.newg_scope_child_vcC: {
                            var cls = updateGenericCodeClass(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            replaceWithInstruction(code, cls.isNativeClass() ? New.newn_scope_child_vcC : New.new_scope_child_vcC);
                            break;
                        }
                        case NewGeneric.newG_scope_child_vcC: {
                            var cls = instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            replaceWithInstruction(code, cls.isNativeClass() ? New.newn_scope_child_vcC : New.new_scope_child_vcC);
                            break;
                        }
                        case NewGeneric.newG_scope_method_vcCm:
                            replaceWithNew(code, instruction);
                            instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            isMethod = true;
                            break;
                        case NewGeneric.newg_scope_method_vcCm:
                            replaceWithNew(code, instruction);
                            updateGenericCodeClass(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            isMethod = true;
                            break;
                        case NewGeneric.newG_scope_method_fix_vcCm:
                            replaceWithNew(code, instruction);
                            instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            isMethod = true;
                            break;
                    }
                    break;

                case InstanceOf.OP: {
                    var type = (instruction & OpCode.DTYPE_MASK) >> 16;
                    if (type >= GENERIC_TYPE_START) {
                        var typeDesc = genericTypeArguments.mapType(type);
                        int instruction2 = (instruction & OpCode.DTYPE_MASK_NEG) | (typeDesc.typeCode.value << 16);
                        replaceWithInstruction(code, instruction2);
                        if(typeDesc.typeCode.isObject()){
                            instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                        }
                    } else if(type == TypeCode.OBJECT_VALUE){
                        instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                    }
                    break;
                }

                case Jump.OP:{
                    int opidx = (instruction & 0x0000ff00) >> 8;
                    if(opidx == 3 || opidx == 4) {
                        var typeCode = (instruction & OpCode.DTYPE_MASK) >> 16;
                        if (typeCode >= GENERIC_TYPE_START) {
                            int instruction2 = instruction & OpCode.DTYPE_MASK_NEG;
                            code.putInt(code.position() - 4, instruction2 | (genericTypeArguments.mapTypeCode(typeCode) << 16));
                        }
                    }
                    break;
                }

                case Add.OP:
                case Subtract.OP:
                case Multiply.OP:
                case Div.OP:
                case Mod.OP:
                case Neg.OP:
                case Equals.OP:
                case NotEquals.OP:
                case LittleThan.OP:
                case GreaterThan.OP:
                case LittleEquals.OP:
                case GreaterEquals.OP:
                case Box.OP: {
                    if(instruction == Box.unbox_force_vot){
                        updateGenericCodeClass(code,2,strings,genericTypeArguments,instantFunction,headers);
                    } else {
                        var type = (instruction & OpCode.DTYPE_MASK) >> 16;
                        if (type >= GENERIC_TYPE_START) {
                            int instruction2 = (instruction & OpCode.DTYPE_MASK_NEG) | (genericTypeArguments.mapTypeCode(type) << 16);
                            replaceWithInstruction(code, instruction2);
                        }
                        switch (instruction) {
                            case Box.box_C_vC:
                                instantiateClassName(code, 1, strings, genericTypeArguments, instantFunction, headers);
                                break;
                            case Box.box_C_vCC:
                                instantiateClassName(code, 1, strings, genericTypeArguments, instantFunction, headers);
                                instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                                break;
                            case Box.box_C_vvC:
                                instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                                break;
                        }
                    }
                    break;
                }

                case Cast.OP: {
                    if(instruction == Cast.cast_object_vvtC){
                        var typeCode = code.getInt(code.position() + 2 * 4);
                        if(typeCode >= GENERIC_TYPE_START) {
                            updateGenericCodeAndClassName(code, 2, typeCode, genericTypeArguments, headers, instantFunction);
                        } else {
                            instantiateClassName(code,3,strings,genericTypeArguments, instantFunction, headers);
                        }
                    } else if(instruction == Cast.cast_to_any_vtCvtC){
                        // vtC
                        var typeCode = code.getInt(code.position() + 1 * 4);
                        if (typeCode >= GENERIC_TYPE_START) {
                            updateGenericCodeAndClassName(code, 1, typeCode, genericTypeArguments, headers, instantFunction);
                        } else {
                            if(typeCode == OBJECT_VALUE) {
                                instantiateClassName(code, 2, strings, genericTypeArguments, instantFunction, headers);
                            }
                        }
                        // ...vtC
                        typeCode = code.getInt(code.position() + 4 * 4);
                        if (typeCode >= GENERIC_TYPE_START) {
                            updateGenericCodeAndClassName(code, 4, typeCode, genericTypeArguments, headers, instantFunction);
                        } else {
                            if(typeCode == OBJECT_VALUE) {
                                instantiateClassName(code, 5, strings, genericTypeArguments, instantFunction, headers);
                            }
                        }
                    } else {
                        var type = (instruction & OpCode.DTYPE_MASK) >> 16;
                        int instruction2 = instruction;
                        if (type >= GENERIC_TYPE_START) {
                            instruction2 = (instruction & OpCode.DTYPE_MASK_NEG) | (genericTypeArguments.mapTypeCode(type) << 16);
                        }
                        var t2 = (instruction & 0x0000ff00) >> 8;
                        if (t2 >= GENERIC_TYPE_START) {
                            instruction2 = (instruction2 & 0xffff00ff) | (genericTypeArguments.mapTypeCode(t2) << 8);
                        }
                        if (instruction2 != instruction)
                            replaceWithInstruction(code, instruction2);
                    }
                    break;
                }

                case New.OP:
                    isMethod = (instruction == New.new_method_voCm || instruction == New.new_cls_method_vCm
                            || instruction == New.new_scope_method_vcCm || instruction == New.new_scope_method_fix_vcCm
                            || instruction == New.new_method_voIm || instruction == New.new_scope_method_fix_voma);
                    break;
            }

            if(isMethod){

            }

            code.skip((OpCode.SIZE_MASK & instruction) * 4);
        }

        genericCodeCache.put(genericTypeArguments, code);
        return code;
    }

    private static void updateGenericCodeAndClassName(IoBuffer code, int offset, int genericTypeCode, GenericTypeArguments genericTypeArguments, Map<String, ClassHeader> headers, ClassHeader instantFunction) {
        if(genericTypeCode < GENERIC_TYPE_START) return;

        var mapped = genericTypeArguments.mapType(genericTypeCode);
        if (mapped != null) {
            code.putInt(code.position() + offset * 4, mapped.getTypeCode().value);
            if(mapped.getTypeCode().value == TypeCode.OBJECT_VALUE) {
                code.putInt(code.position() + (offset + 1) * 4, headers.get(mapped.className).classId);
            } else {
                code.putInt(code.position() + (offset + 1) * 4, -1);
            }
        }
        instantFunction.handledInstructions.add(code.position());
    }

    private void replaceWithNew(IoBuffer codeBuffer, int instruction) {
        var newInstruction = (instruction & 0x0f_ff_ff_ff) | 0x00_ff_00_00;  // from 0x14_xx_xx_xx to 0x04_xx_xx_xx
        replaceWithInstruction(codeBuffer, newInstruction);
    }

    private void replaceWithInstruction(IoBuffer codeBuffer, int newInstruction) {
        codeBuffer.putInt(codeBuffer.position() - 4, newInstruction);
    }

    private ClassHeader instantiateClassName(IoBuffer codeBuffer, int offset, String[] strings, GenericTypeArguments genericTypeArguments, ClassHeader instantFunction, Map<String, ClassHeader> headers) {
        int pos = codeBuffer.position();
        codeBuffer.skip(offset * 4);
        int classNameId = codeBuffer.getInt();
        String className = strings[classNameId];
        ClassHeader cls = headers.get(className);
        ClassHeader instantiation = cls.resolveTemplateInstantiation(headers, genericTypeArguments);
        if (instantiation == null) {
            cls.resolveTemplateInstantiation(headers, genericTypeArguments);
        }
        codeBuffer.putInt(codeBuffer.position() - 4, instantiation.classId);
        instantFunction.handledInstructions.add(pos);
        codeBuffer.position(pos);
        return cls;
    }

    private ClassHeader updateGenericCodeClass(IoBuffer codeBuffer, int offset, String[] strings, GenericTypeArguments genericTypeArguments, ClassHeader instantFunction, Map<String, ClassHeader> headers) {
        int pos = codeBuffer.position();
        codeBuffer.skip(offset * 4);
        int genericCode = codeBuffer.getInt();
        String className = genericTypeArguments.mapTypeCodeToClassName(genericCode);
        ClassHeader header = headers.get(className);
        codeBuffer.putInt(codeBuffer.position() - 4, header.classId);
        instantFunction.handledInstructions.add(pos);
        codeBuffer.position(pos);
        return header;
    }

}
