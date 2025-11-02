package org.siphonlab.ago;

public class GenericArgumentsInfo extends ConcreteTypeInfo{
    private AgoClass templateClass;

    private TypeInfo[] arguments;

    public GenericArgumentsInfo(AgoClass templateClass, TypeInfo[] arguments) {
        super();
        this.templateClass = templateClass;
        this.arguments = arguments;
    }

    public AgoClass getTemplateClass() {
        return templateClass;
    }

    public void setTemplateClass(AgoClass templateClass) {
        this.templateClass = templateClass;
    }

    public TypeInfo[] getArguments() {
        return arguments;
    }

    public void setArguments(TypeInfo[] arguments) {
        this.arguments = arguments;
    }
}
