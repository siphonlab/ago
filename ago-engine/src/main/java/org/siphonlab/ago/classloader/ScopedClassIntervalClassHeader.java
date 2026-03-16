package org.siphonlab.ago.classloader;

public class ScopedClassIntervalClassHeader extends ParameterizedClassHeader{

    private final String lBoundClassName;
    private final String uBoundClassName;

    public ScopedClassIntervalClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.lBoundClassName = ((ClassRefValue) arguments[0]).className();
        this.uBoundClassName = ((ClassRefValue) arguments[1]).className();
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        throw new UnsupportedOperationException("TOOD");
    }

    public ClassHeader getLBound(){
        return classLoader.getClassHeader(lBoundClassName);
    }

    public ClassHeader getUBound(){
        return classLoader.getClassHeader(uBoundClassName);
    }

    public static String composeName(String lBound, String uBound){
        return '[' + composeNameOfClassInClassInterval(lBound) + '~' + composeNameOfClassInClassInterval(uBound) + ']';
    }
}
