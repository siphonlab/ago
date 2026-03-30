package org.siphonlab.ago.classloader;

import org.siphonlab.ago.Variance;

public class SharedGenericTypeParameterClassHeader extends ParameterizedClassHeader{

    public final String lBoundClassName;
    public final String uBoundClassName;
    public final Variance variance;

    public SharedGenericTypeParameterClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.lBoundClassName = ((ClassRefValue) arguments[0]).className();
        this.uBoundClassName = ((ClassRefValue) arguments[1]).className();
        variance = Variance.of((Byte) arguments[2]);
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

    public static String composeName(String lBound, String uBound, Variance variance){
        StringBuilder sb = switch (variance){
            case Invariance ->  new StringBuilder();
            case Covariance ->   new StringBuilder("+");
            case Contravariance ->   new StringBuilder("-");
        };
        sb.append("[").append(composeNameOfClassInClassInterval(lBound)).append('~').append(composeNameOfClassInClassInterval(uBound)).append(']');
        return sb.toString();
    }

    @Override
    public boolean isGenericTerminated() {
        return false;
    }
}
