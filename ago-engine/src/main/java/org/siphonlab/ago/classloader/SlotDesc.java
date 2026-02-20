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

import org.siphonlab.ago.TypeCode;

import java.util.Map;
import java.util.Objects;

public final class SlotDesc {
    private final int index;
    private final String name;
    private TypeDesc type;

    public SlotDesc(int index, String name, TypeDesc type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    SlotDesc applyTemplate(Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        var newType = this.type.applyTemplate(headers, typeArguments);
        if (newType != this.type) {
            return new SlotDesc(this.index(), this.name, newType);
        }
        return this;
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

    public int index() {
        return index;
    }

    public String name() {
        return name;
    }

    public TypeDesc type() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SlotDesc) obj;
        return this.index == that.index &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, name, type);
    }

    public void setType(TypeDesc type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SlotDesc[" +
                "index=" + index + ", " +
                "name=" + name + ", " +
                "type=" + type + ']';
    }

}
