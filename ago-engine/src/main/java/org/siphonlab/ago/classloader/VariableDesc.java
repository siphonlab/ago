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

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;

import java.util.Map;

public class VariableDesc {

    enum VariableKind {Variable, Field, Parameter}

    public VariableDesc(String name, int modifiers, VariableKind variableKind, TypeDesc type,
                        int slotIndex, Object constLiteralValue, SourceLocation sourceLocation) {
        this.name = name;
        this.modifiers = modifiers;
        this.variableKind = variableKind;
        this.type = type;
        this.slotIndex = slotIndex;
        this.constLiteralValue = constLiteralValue;
        this.sourceLocation = sourceLocation;
    }

    final int modifiers;
    VariableKind variableKind;
    String name;
    TypeDesc type;
    int slotIndex;
    final Object constLiteralValue;

    private SourceLocation sourceLocation;

    public int getSlotIndex() {
        return slotIndex;
    }

    VariableDesc applyTemplate(Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        if(!ClassHeader.isGenericType(this.type, headers)) return this;

        if (this.type instanceof GenericTypeDesc genericTypeDesc) {
            var instantiateParamClass = genericTypeDesc.applyTemplate(headers, typeArguments);
            return new VariableDesc(name, modifiers, variableKind, instantiateParamClass, slotIndex, constLiteralValue, sourceLocation);
        } else if(this.type.typeCode == TypeCode.OBJECT){
            var cls = headers.get(this.type.className);
            assert cls != null;
            if (cls instanceof ArrayTypeHeader arrayTypeHeader) {
                var newCls = arrayTypeHeader.tryInstantiate(arrayTypeHeader.parent, headers, typeArguments);
                var newType = new TypeDesc(TypeCode.OBJECT, newCls.fullname);
                return new VariableDesc(name, modifiers, variableKind, newType, slotIndex, constLiteralValue, sourceLocation);
            } else if(cls instanceof GenericInstantiationClassHeader genericInstantiationClassHeader){
                ClassHeader inst = genericInstantiationClassHeader.resolveTemplateInstantiation(headers, typeArguments);
                return new VariableDesc(name, modifiers, variableKind, new TypeDesc(TypeCode.OBJECT, inst.fullname), slotIndex, constLiteralValue, sourceLocation);
            }
        }
        throw new UnsupportedOperationException();
    }

    public boolean resolveGenericTypeDescPlaceHolder(Map<String, ClassHeader> headers) {
        if(this.type instanceof GenericTypeDesc genericTypeDesc && genericTypeDesc.isPlaceHolder){
            GenericTypeDesc resolved = genericTypeDesc.resolveExactType(headers);
            if(resolved == null) return false;
            this.type = resolved;
        }
        if(this.type.typeCode == TypeCode.OBJECT){
            return this.type.resolveGenericTypeDescPlaceHolder(headers);
        }
        return true;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
