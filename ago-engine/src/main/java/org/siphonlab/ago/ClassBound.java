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

import org.siphonlab.ago.classloader.AgoClassLoader;

public interface ClassBound {
    // class bound,
    public static boolean isClassBound(AgoClass classInterval){
        var langClasses = classInterval.getClassLoader().getLangClasses();
        return classInterval.getConcreteTypeInfo() instanceof ParameterizedClassInfo p &&
                (p.getParameterizedBaseClass().equals(langClasses.getClassIntervalClass())
                || p.getParameterizedBaseClass().equals(langClasses.getScopedClassIntervalClass())
                || p.getParameterizedBaseClass().equals(langClasses.getGenericTypeParameterClass()));
    }

    public static AgoClass getLBound(AgoClass classInterval){
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) classInterval.getConcreteTypeInfo();
        return (AgoClass) parameterizedClassInfo.getArguments()[0];
    }
    public static AgoClass getUBound(AgoClass classInterval){
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) classInterval.getConcreteTypeInfo();
        return (AgoClass) parameterizedClassInfo.getArguments()[1];
    }

    // SharedGenericTypeParameterClassDef
    static Variance getVariance(AgoClass sharedGenericTypeParameter) {
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) sharedGenericTypeParameter.getConcreteTypeInfo();
        return Variance.of((Byte)parameterizedClassInfo.getArguments()[2]);
    }
}
