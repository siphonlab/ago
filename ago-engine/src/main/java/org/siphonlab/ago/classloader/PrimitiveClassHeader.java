package org.siphonlab.ago.classloader;

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.TypeCode;

public class PrimitiveClassHeader extends ClassHeader{

    private TypeCode typeCode;

    public PrimitiveClassHeader(String fullname, byte type, int modifiers, IoBuffer slice, AgoClassLoader classLoader) {
        super(fullname, type, modifiers, slice, classLoader);
    }

    @Override
    public boolean resolveHierarchicalClasses() {
        var b = super.resolveHierarchicalClasses();
        if(b){
            ParameterizedClassHeader primitiveSuperClass = (ParameterizedClassHeader) this.getSuperClassHeader();
            this.typeCode = TypeCode.of((Integer) primitiveSuperClass.arguments[0]);
        }
        return b;
    }

    @Override
    public TypeCode getTypeCode() {
        if(this.loadingStage == LoadingStage.ResolveHierarchicalClasses) this.resolveHierarchicalClasses();
        return typeCode;
    }

    @Override
    public boolean isGenericTerminated() {
        return true;
    }
}
