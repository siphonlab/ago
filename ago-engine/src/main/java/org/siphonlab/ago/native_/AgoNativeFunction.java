package org.siphonlab.ago.native_;

import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoNativeFunction extends AgoFunction {

    private NativeFunctionCaller nativeFunctionCaller;
    private String nativeEntrance;
    private int resultSlot;

    public AgoNativeFunction(AgoClassLoader agoClassLoader, MetaClass metaClass, String fullname, String name) {
        super(agoClassLoader, metaClass, fullname, name);
    }

    public NativeFunctionCaller getNativeFunctionCaller() {
        return nativeFunctionCaller;
    }

    public void setNativeFunctionCaller(NativeFunctionCaller nativeFunctionCaller) {
        this.nativeFunctionCaller = nativeFunctionCaller;
    }

    public void setResultSlot(int resultSlot) {
        this.resultSlot = resultSlot;
    }

    public int getResultSlot() {
        return resultSlot;
    }

    public String getNativeEntrance() {
        return nativeEntrance;
    }

    public void setNativeEntrance(String nativeEntrance) {
        this.nativeEntrance = nativeEntrance;
    }

    @Override
    protected AgoFunction withScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoNativeFunction(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

    @Override
    protected void copyTo(AgoClass cls) {
        AgoNativeFunction copy = (AgoNativeFunction) cls;
        super.copyTo(cls);
        copy.setNativeEntrance(this.nativeEntrance);
        copy.setResultSlot(this.resultSlot);
        copy.setNativeFunctionCaller(this.nativeFunctionCaller);
    }

}
