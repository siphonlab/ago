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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnionTypeHeader extends ParameterizedClassHeader{

    protected final List<String> classNames;

    public UnionTypeHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.classNames = Arrays.stream(arguments).map( a -> ((ClassRefValue)a).className()).toList();
    }

    @Override
    public boolean resolveHierarchicalClasses() {
        return super.resolveHierarchicalClasses();
    }

    public static String composeName(Object[] arguments){
        return "@|" + Arrays.stream(arguments).map(a -> ((ClassRefValue)a).className()).collect(Collectors.joining("|")) + ';';
    }

    @Override
    public TypeCode getTypeCode() {
        return TypeCode.UNION;
    }
}
