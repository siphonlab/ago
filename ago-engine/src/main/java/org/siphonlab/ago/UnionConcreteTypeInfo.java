package org.siphonlab.ago;

public class UnionConcreteTypeInfo extends ConcreteTypeInfo {
    private final AgoClass[] classes;

    public UnionConcreteTypeInfo(AgoClass[] classes) {
        this.classes = classes;
    }

    public AgoClass[] getClasses() {
        return classes;
    }
}
