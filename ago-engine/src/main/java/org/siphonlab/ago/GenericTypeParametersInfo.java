package org.siphonlab.ago;

public class GenericTypeParametersInfo extends ConcreteTypeInfo{
    private TypeInfo[] genericParameters;

    public GenericTypeParametersInfo(TypeInfo[] genericParameters) {
        this.genericParameters = genericParameters;
    }

    public TypeInfo[] getGenericParameters() {
        return genericParameters;
    }
}
