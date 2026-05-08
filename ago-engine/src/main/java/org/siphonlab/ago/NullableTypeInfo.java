package org.siphonlab.ago;

// this can derive from parameterized info, but, it's only a flag, just leave it
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
