package org.siphonlab.ago;

public class NullableTypeInfo extends UnionConcreteTypeInfo{

    private final AgoClass baseClass;

    public NullableTypeInfo(AgoClass baseClass, AgoNullClass agoNullClass) {
        super(new AgoClass[]{baseClass, agoNullClass});
        this.baseClass = baseClass;
    }

    public AgoClass getBaseClass() {
        return baseClass;
    }
}
