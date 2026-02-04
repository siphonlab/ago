package org.siphonlab.ago;

public class ParameterizedClassInfo extends ConcreteTypeInfo{
    private AgoClass parameterizedBaseClass;
    private AgoFunction parameterizedConstructor;
    private Object[] arguments;

    public ParameterizedClassInfo(AgoClass parameterizedBaseClass, AgoFunction parameterizedConstructor, Object[] arguments) {
        super();
        this.parameterizedBaseClass = parameterizedBaseClass;
        this.parameterizedConstructor = parameterizedConstructor;
        this.arguments = arguments;
    }


    public AgoClass getParameterizedBaseClass() {
        return parameterizedBaseClass;
    }

    public void setParameterizedBaseClass(AgoClass parameterizedBaseClass) {
        this.parameterizedBaseClass = parameterizedBaseClass;
    }

    public AgoFunction getParameterizedConstructor() {
        return parameterizedConstructor;
    }

    public void setParameterizedConstructor(AgoFunction parameterizedConstructor) {
        this.parameterizedConstructor = parameterizedConstructor;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }
}
