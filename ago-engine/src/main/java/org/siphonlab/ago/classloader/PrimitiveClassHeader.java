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

import java.util.Set;

public class PrimitiveClassHeader extends ClassHeader{

    private TypeCode typeCode;

    public PrimitiveClassHeader(String fullname, byte type, int modifiers, IoBuffer slice, AgoClassLoader classLoader) {
        super(fullname, type, modifiers, slice, classLoader);
    }

    @Override
    public boolean resolveHierarchicalClasses() {
        var b = super.resolveHierarchicalClasses();
        if(b){
            ParameterizedClassHeader primitiveSuperClass = (ParameterizedClassHeader) this.getSuperClassHeader();
            this.typeCode = TypeCode.of((Integer) primitiveSuperClass.arguments[0]);
        }
        return b;
    }

    @Override
    public TypeCode getTypeCode() {
        if(this.loadingStage == LoadingStage.ResolveHierarchicalClasses) this.resolveHierarchicalClasses();
        return typeCode;
    }

    @Override
    public boolean isGenericTerminated(Set<String> visited) {
        return true;
    }

}
