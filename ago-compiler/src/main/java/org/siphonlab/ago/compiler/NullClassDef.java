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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;

import static org.siphonlab.ago.TypeCode.*;

public class NullClassDef extends ClassDef {

    public NullClassDef(Root root) {
        super(root, "null");
        this.compilingStage = CompilingStage.Compiled;
    }


    public TypeCode getTypeCode() {
        return TypeCode.NULL;
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass) {
        if(this == anotherClass) return this;
        return null;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        return false;
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        return this;
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
        if(returnExisted!=null) returnExisted.setTrue();
        return this;
    }

}
