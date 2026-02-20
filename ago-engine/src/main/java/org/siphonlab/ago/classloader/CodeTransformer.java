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
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.opcode.*;
import org.siphonlab.ago.opcode.compare.Equals;
import org.siphonlab.ago.opcode.compare.InstanceOf;

import java.util.Map;
import java.util.Objects;

public class CodeTransformer {

    private final AgoClassLoader classLoader;
    private final ClassHeader header;
    private final Map<String, ClassHeader> headers;
    private final String[] strings;
    private final IoBuffer codeBuffer;

    public CodeTransformer(AgoClassLoader agoClassLoader, ClassHeader header, Map<String, ClassHeader> headers){
        this.classLoader = agoClassLoader;
        this.header = header;
        this.headers = headers;
        this.strings = header.loadStrings();
        this.codeBuffer = header.compiledCode;
    }

    private void updateStringId(int instruction, int offset){
        int size = OpCode.SIZE_MASK & instruction;
        int pos = codeBuffer.position();
        codeBuffer.skip(offset * 4);
        String s = strings[codeBuffer.getInt()];
        int v = classLoader.idOfString(s);
        codeBuffer.skip(-4).putInt(v);
        assert Objects.equals(classLoader.getStrings().get(v), s);
        codeBuffer.position(pos + size * 4);
    }

    private void updateBlobId(int instruction, int offset, int blobOffset){
        assert blobOffset >= 0;
        int size = OpCode.SIZE_MASK & instruction;
        int pos = codeBuffer.position();
        codeBuffer.skip(offset * 4);
        int blobId = codeBuffer.getInt();
        int v = blobId + blobOffset;
        codeBuffer.skip(-4).putInt(v);
        codeBuffer.position(pos + size * 4);
    }

    private void updateClassId(int instruction, int offset){
        int pos = codeBuffer.position();
        int size = OpCode.SIZE_MASK & instruction;

        if(!header.handledInstructions.contains(pos)) {
            codeBuffer.skip(offset * 4);
            int classNameId = codeBuffer.getInt();
            if(classNameId == -1) {
                codeBuffer.position(pos + size * 4);
                return;
            }
            String className = strings[classNameId];
            ClassHeader classHeader = headers.get(className);
//            if(classHeader.isInGenericTemplate(headers) && this.header.genericSource != null) {
//                if (classHeader.type == AgoClass.TYPE_INTERFACE || classHeader.type == AgoClass.TYPE_TRAIT) {
//                    classHeader = classHeader.tryInstantiate()
//                }
//            }
            var classId = classHeader.classId;
            codeBuffer.skip(-4).putInt(classId);
        }
        codeBuffer.position(pos + size * 4);
    }

    private void updateMethodId(int instruction, int offset, boolean liftToMeta){
        int size = OpCode.SIZE_MASK & instruction;
        int pos = codeBuffer.position();
        codeBuffer.skip(offset * 4 - 4);
        // TODO speedup
        int classId = codeBuffer.getInt();
        var methodClass = this.headers.values().stream().filter(h -> h.classId == classId).findFirst().get();
        if(liftToMeta){
            methodClass = this.headers.get(methodClass.getMetaClass());
        }

        String methodName = strings[codeBuffer.getInt()];       // with postfix
        var clazz = classLoader.getClass(methodClass.fullname);
        var methodDesc = methodClass.findMethod(methodName, headers);
        int p = methodDesc.getMethodIndex();
        var method = headers.get(methodDesc.getFullname());
        assert clazz.getMethod(p) == method.agoClass;
        if(method.isInGenericTemplate(headers) && this.header.genericSource != null){
            method = method.resolveTemplateInstantiation(headers, this.header.genericSource.typeArguments());
            p = methodDesc.getMethodIndex();
            assert clazz.getMethod(p).getName().equals(method.agoClass.getName());
        }
        assert p != -1;
        codeBuffer.skip(-4).putInt(p);
        codeBuffer.position(pos + size * 4);
    }

    public int[] transformCode() {
        while(codeBuffer.hasRemaining()) {
            var instruction = codeBuffer.getInt();
            switch (instruction) {
                case New.new_vC:
                case New.newn_vC:
                    updateClassId(instruction, 1); break;
                case New.new_child_voC:
                case New.newn_child_voC:
                    updateClassId(instruction, 2); break;

                case New.new_scope_child_vcC:
                case New.newn_scope_child_vcC:
                    updateClassId(instruction, 2); break;

                case New.new_method_voCm:
                case New.new_method_voIm:
                {
                    var pos = codeBuffer.position();
                    updateClassId(instruction, 2);
                    codeBuffer.position(pos);
                    updateMethodId(instruction, 3, false);
                    break;
                }
                case New.new_cls_method_vCm: {
                    var pos = codeBuffer.position();
                    updateClassId(instruction, 1);
                    codeBuffer.position(pos);
                    updateMethodId(instruction, 2, true);
                    break;
                }
                case New.new_scope_method_fix_vcCm:
                case New.new_scope_method_vcCm: {
                    var pos = codeBuffer.position();
                    updateClassId(instruction, 2);
                    codeBuffer.position(pos);
                    updateMethodId(instruction, 3, false);
                    break;
                }

                case Concat.concat_S_vc:        updateStringId(instruction, 1); break;
                case Concat.concat_S_vvc:       updateStringId(instruction, 2); break;
                case Concat.concat_S_vcv:       updateStringId(instruction, 1); break;
                case Box.box_S_vc:              updateStringId(instruction, 1); break;

                case Const.const_S_vc:          updateStringId(instruction, 1); break;
                case Const.const_fld_S_ovc:     updateStringId(instruction, 2); break;

                case Return.return_S_c:         updateStringId(instruction, 0); break;

                case Load.loadcls_vC:           updateClassId(instruction, 1); break;
                //TODO not sure it should support Generic
                case Load.bindcls_scope_vCc:    updateClassId(instruction, 1); break;
                case Load.bindcls_vCo:          updateClassId(instruction, 1); break;
                case Cast.C2sbr_oCC: {      // the 2nd is Class Instance
                    updateClassId(instruction, 1);
                    break;
                }
                case Const.const_C_vC:          updateClassId(instruction, 1); break;
                case Const.const_fld_C_ovC:     updateClassId(instruction, 2); break;

                case Array.array_create_i_vCc:
                case Array.array_create_i_vCv:
                case Array.array_create_o_vCc:
                case Array.array_create_o_vCv:
                case Array.array_create_b_vCc:
                case Array.array_create_b_vCv:
                case Array.array_create_B_vCc:
                case Array.array_create_B_vCv:
                case Array.array_create_s_vCc:
                case Array.array_create_s_vCv:
                case Array.array_create_c_vCc:
                case Array.array_create_c_vCv:
                case Array.array_create_S_vCc:
                case Array.array_create_S_vCv:
                case Array.array_create_d_vCc:
                case Array.array_create_d_vCv:
                case Array.array_create_f_vCc:
                case Array.array_create_f_vCv:
                case Array.array_create_l_vCc:
                case Array.array_create_l_vCv:
                    updateClassId(instruction, 1); break;

                case Array.array_fill_i_acL:
                    updateBlobId(instruction, 2, header.getBlobOffset()); break;

                case Box.box_C_vC:              updateClassId(instruction, 1); break;
                case Box.box_C_vvC:             updateClassId(instruction, 2); break;
                case Box.box_C_vCC:{
                    codeBuffer.mark();
                    updateClassId(instruction, 1);
                    codeBuffer.position(codeBuffer.markValue());
                    updateClassId(instruction, 2);
                    break;
                }
                case Equals.equals_C_vvc:       updateClassId(instruction, 2); break;

                case InstanceOf.instanceof_o_vvC:
                case InstanceOf.instanceof_p_vvC:
                    updateClassId(instruction, 2);
                    break;


                case Cast.cast_object_vvtC:
                    updateClassId(instruction,3);
                    break;
                case Cast.cast_to_any_vtCvtC:
                    codeBuffer.mark();
                    updateClassId(instruction,2);
                    codeBuffer.position(codeBuffer.markValue());
                    updateClassId(instruction, 5);
                    break;

                case Debug.print_S_c:           updateStringId(instruction, 0); break;

                case Move.move_copy_ooC:
                    updateClassId(instruction, 2);
                    break;

                default:
                    codeBuffer.skip( (OpCode.SIZE_MASK & instruction) * 4);
            }
        }
        codeBuffer.flip();
        int[] code = new int[codeBuffer.remaining()/4];
        int i = 0;
        while(codeBuffer.hasRemaining()){
            code[i++] = codeBuffer.getInt();
        }
        return code;
    }
}
