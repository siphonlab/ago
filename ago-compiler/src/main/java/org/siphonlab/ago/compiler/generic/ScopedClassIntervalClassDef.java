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
package org.siphonlab.ago.compiler.generic;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class ScopedClassIntervalClassDef extends ClassIntervalClassDef {

    public ScopedClassIntervalClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, ClassDef lBound, ClassDef uBound) {
        super(baseClass, parameterizedConstructor, lBound, uBound);
        this.name = composeName(lBound, uBound);
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) {
        ScopedClassIntervalClassDef c = null;
        try {
            c = ((ClassContainer)this.getParent()).getOrCreateScopedClassInterval(baseClass.instantiate(instantiationArguments, null),
                    this.parameterizedConstructor,
                    this.getLBoundClass().instantiate(instantiationArguments, null),
                    this.getUBoundClass().instantiate(instantiationArguments, null),
                    returnExisted);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    public static ClassDef getLBoundClass(ClassIntervalClassDef classIntervalClassDef){
        return classIntervalClassDef.getLBoundClass();
    }

    public static MetaClassDef getMetaOfLBoundClass(ClassIntervalClassDef classIntervalClassDef) {
        var lBound = classIntervalClassDef.getLBoundClass();
        MetaClassDef metaClassDef = lBound.getMetaClassDef();
        if(metaClassDef == null){
            return new PhantomMetaClassDef(lBound);
        }
        return metaClassDef;
    }

    public static String composeName(ClassDef lBound, ClassDef uBound){
        return '[' + composeNameOfClassInClassInterval(lBound) + '~' + composeNameOfClassInClassInterval(uBound) + ']';
    }

}
