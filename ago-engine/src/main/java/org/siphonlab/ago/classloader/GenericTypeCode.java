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

public class GenericTypeCode extends TypeCode implements Comparable<GenericTypeCode> {
    int paramIndex;
    String name;
    GenericTypeCodeAvatarClassHeader genericTypeCodeAvatarClassHeader;

    public GenericTypeCode(int genericTypeCode, int paramIndex, String name, String description){
        super(genericTypeCode, description);
        this.name = name;
        this.paramIndex = paramIndex;
    }

    @Override
    public int compareTo(GenericTypeCode o) {
        var r = this.genericTypeCodeAvatarClassHeader.templateClassName.compareTo(o.genericTypeCodeAvatarClassHeader.templateClassName);
        if(r != 0) return r;
        return this.value - o.value;
    }
}
