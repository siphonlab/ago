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
