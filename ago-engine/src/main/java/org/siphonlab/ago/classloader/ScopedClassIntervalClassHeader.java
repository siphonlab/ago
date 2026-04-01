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

public class ScopedClassIntervalClassHeader extends ParameterizedClassHeader{

    private final String lBoundClassName;
    private final String uBoundClassName;

    public ScopedClassIntervalClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.lBoundClassName = ((ClassRefValue) arguments[0]).className();
        this.uBoundClassName = ((ClassRefValue) arguments[1]).className();
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        throw new UnsupportedOperationException("TOOD");
    }

    public ClassHeader getLBound(){
        return classLoader.getClassHeader(lBoundClassName);
    }

    public ClassHeader getUBound(){
        return classLoader.getClassHeader(uBoundClassName);
    }

    public static String composeName(String lBound, String uBound){
        return '[' + composeNameOfClassInClassInterval(lBound) + '~' + composeNameOfClassInClassInterval(uBound) + ']';
    }
}
