package org.siphonlab.ago.classloader;

public class MethodDesc{
    private final String name;
    private final String fullname;
    private ClassHeader functionClassHeader;
    private int methodIndex = 1;

    public MethodDesc(String name, String fullname) {
        this.name = name;
        this.fullname = fullname;
    }

    public ClassHeader getFunctionClassHeader() {
        return functionClassHeader;
    }

    public void setFunctionClassHeader(ClassHeader functionClassHeader) {
        this.functionClassHeader = functionClassHeader;
    }

    public int getMethodIndex() {
        return methodIndex;
    }

    public void setMethodIndex(int methodIndex) {
        this.methodIndex = methodIndex;
    }

    public String getName() {
        return name;
    }

    public String getFullname() {
        return fullname;
    }

    @Override
    public String toString() {
        return "(MethodDesc %d %s %s)".formatted(methodIndex, name, fullname);
    }
}
