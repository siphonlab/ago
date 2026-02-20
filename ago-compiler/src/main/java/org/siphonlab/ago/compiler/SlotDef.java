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

import org.siphonlab.ago.TypeCode;

public class SlotDef {
    private TypeCode typeCode;
    private int index;
    private String name;
    private Variable variable;
    private ClassDef classDef;
    private boolean locked;

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVariable(Variable variable) {
        if(this.variable == null) {
            this.variable = variable;
        } else {
            assert this.variable == variable;
        }
    }

    public Variable getVariable() {
        return variable;
    }

    public boolean isRegister() {
        return variable == null;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public void setClassDef(ClassDef classDef) {
        assert classDef != null;
        assert !(classDef instanceof PhantomMetaClassDef);
        this.classDef = classDef;
        this.typeCode = this.classDef.getTypeCode();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
