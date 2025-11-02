package org.siphonlab.ago;

public class GenericParameterTypeInfo extends TypeInfo{

    private final String parameterName;
    private final AgoClass sharedGenericTypeParameterClass;

    public GenericParameterTypeInfo(String parameterName, AgoClass sharedGenericTypeParameterClass, TypeCode typeCode, AgoClass agoClass) {
        super(typeCode, agoClass);
        this.parameterName = parameterName;
        this.sharedGenericTypeParameterClass = sharedGenericTypeParameterClass;
    }

    public String getParameterName() {
        return parameterName;
    }

    public AgoClass getSharedGenericTypeParameterClass() {
        return sharedGenericTypeParameterClass;
    }
}
