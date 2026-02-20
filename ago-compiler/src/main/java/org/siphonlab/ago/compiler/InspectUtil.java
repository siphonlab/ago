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
package org.siphonlab.ago.compiler;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Modifier;
import org.siphonlab.ago.opcode.OpCode;

import java.util.stream.Collectors;

public class InspectUtil {

    public static String inspect(Root root){
        StringBuilder sb = new StringBuilder();
        for (var classDef : root.getUniqueChildren()) {
            sb.append(inspect(classDef)).append('\n');
        }
        return sb.toString();
    }

    public static String inspect(Package pkg){
        StringBuilder sb = new StringBuilder();
        sb.append(pkg.getFullname()).append('\n');
        for (ClassDef classDef : pkg.getUniqueChildren()) {
            sb.append(inspect(classDef)).append('\n');
        }
        return sb.toString();
    }

    public static String inspect(ClassDef classDef){
        return inspect(classDef, 0);
    }

    public static String inspect(ClassDef classDef, int tabDepth){
        StringBuilder sb = new StringBuilder();
        sb.append("\t".repeat(tabDepth)).append(Modifier.toString(classDef.modifiers)).append(' ');
        sb.append(switch (classDef.getClassType()){
            case AgoClass.TYPE_CLASS -> "class";
            case AgoClass.TYPE_FUNCTION -> "function";
            case AgoClass.TYPE_INTERFACE -> "interface";
            case AgoClass.TYPE_METACLASS -> "metaclass";
            case AgoClass.TYPE_ENUM -> "enum";
            case AgoClass.TYPE_TRAIT -> "trait";
            default -> throw new RuntimeException("illegal class type %s".formatted(classDef.getClassType()));
        }).append(' ');
        sb.append(classDef.getFullname());
        if(classDef.getTypeParamsContext() != null && classDef.getTypeParamsContext().size() > 0){
            sb.append('<').append(classDef.getTypeParamsContext()).append('>');
        }
        if(classDef.superClass != null && classDef.superClass != classDef.getRoot().getObjectClass()) {
            sb.append(" from ").append(classDef.superClass.getFullname());
        }
        if(classDef.getInterfaces() != null && !classDef.getInterfaces().isEmpty()){
            sb.append(" with ").append(classDef.getInterfaces().stream().map(Namespace::getFullname).collect(Collectors.joining(",")));
        }
        if(classDef instanceof FunctionDef functionDef){
            sb.append("()").append(" as ").append(functionDef.getResultType().getFullname()).append(':').append(functionDef.getNativeResultSlot());
        }
        sb.append("{\n");
        if(classDef.getGenericSource() != null){
            sb.append("\t".repeat(tabDepth + 1)).append("generic source:\n");
            sb.append("\t".repeat(tabDepth + 2));
            sb.append(" template: ").append(classDef.getGenericSource().originalTemplate().getFullname()).append('\n');
            sb.append("\t".repeat(tabDepth + 2));
            sb.append("arguments: ").append(classDef.getGenericSource().instantiationArguments()).append('\n');
        }
        if(!classDef.getConcreteTypes().isEmpty() && classDef.getParentClass() == null){
            sb.append("\t".repeat(tabDepth + 1)).append("concrete types:\n");
            for (var entry : classDef.getConcreteTypes().entrySet()) {
                sb.append("\t".repeat(tabDepth + 2)).append(entry.getValue()).append('\n');
            }
        }
        if(!classDef.fields.isEmpty()) {
            sb.append("\t".repeat(tabDepth + 1)).append("fields:\n");
            for (Field field : classDef.fields.values()) {
                sb.append("\t".repeat(tabDepth + 2)).append(Modifier.toString(field.getModifiers())).append(' ').append(field.getName());
                if (field.getOwnerClass() != classDef) {
                    sb.append(" (").append(field.ownerClass.getFullname()).append(')');
                }
                sb.append(" as ").append(field.getType().getFullname());
                if (field.getSlot() != null) {
                    sb.append(" slot:").append(field.getSlot().getIndex());
                }
                sb.append('\n');
            }
        }
        if(classDef instanceof FunctionDef functionDef && !functionDef.getLocalVariables().isEmpty()) {
            sb.append("\t".repeat(tabDepth + 1)).append("variables:\n");
            for (Variable variable : functionDef.getLocalVariables().values()) {
                sb.append("\t".repeat(tabDepth + 2)).append(Modifier.toString(variable.getModifiers())).append(' ')
                        .append(variable.name).append(" (").append(variable.ownerClass.getFullname()).append(')')
                        .append(" as ").append(variable.getType().getFullname());
                if (variable.getSlot() != null) {
                    sb.append(" slot:").append(variable.getSlot().getIndex());
                }
                sb.append('\n');
            }
        }
        if(!classDef.slotsAllocator.slots.isEmpty()) {
            sb.append("\t".repeat(tabDepth + 1)).append("slots:\n");
            for (SlotDef slot : classDef.slotsAllocator.getSlots()) {
                sb.append("\t".repeat(tabDepth + 2)).append(slot.getIndex()).append(": ").append(slot.getName()).append(' ')
                        .append(slot.getTypeCode()).append(' ').append(slot.getClassDef().getFullname())
                        .append(slot.isRegister() ? " R" : "")
                        .append(slot.isLocked() ? " L" : "")
                        .append('\n')
                ;
            }
        }

        for (ClassDef child : classDef.getUniqueChildren()) {
            if(classDef.gotFromInherited(child)){
                continue;
            }
            sb.append("\n");
            sb.append(inspect(child,tabDepth + 1)).append('\n');
            sb.append("\n");
        }
        sb.append("\t".repeat(tabDepth + 1)).append("code:\n");
        if(classDef.isFunction() && classDef.getGenericSource() == null) {
            FunctionDef functionDef = (FunctionDef) classDef;
            if(functionDef.getBody() != null){
                sb.append(inspectCode(functionDef.getBody(), tabDepth + 2));
            } else {
                sb.append("\t".repeat(tabDepth + 2)).append("<NA>\n");
            }
        }
        sb.append("\t".repeat(tabDepth)).append('}');
        return sb.toString();
    }

    public static String inspectCode(int[] code){
        return inspectCode(code, 0);
    }
    public static String inspectCode(int[] code, int tabDepth){
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < code.length; ) {
            int instruction = code[j++];
            sb.append("\t".repeat(tabDepth))
                    .append(j-1).append('\t')
                    .append(OpCode.getName(instruction)).append('\t');
            for (int i = 0; i < (instruction & OpCode.SIZE_MASK); i++) {
                sb.append(code[j++]).append(',');
            }
            sb.deleteCharAt(sb.length() - 1).append('\n');
        }
        return sb.toString();
    }
}
