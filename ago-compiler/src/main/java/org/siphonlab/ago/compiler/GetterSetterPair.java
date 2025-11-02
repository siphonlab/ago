package org.siphonlab.ago.compiler;

public final class GetterSetterPair {
    private FunctionDef getter;
    private FunctionDef setter;


    public FunctionDef getGetter() {
        return getter;
    }

    public FunctionDef getSetter() {
        return setter;
    }

    public void setGetter(FunctionDef getter) {
        this.getter = getter;
    }

    public void setSetter(FunctionDef setter) {
        this.setter = setter;
    }
}
