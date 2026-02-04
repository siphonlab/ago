package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public interface ClassBound {
    // class bound,
    public static boolean isClassBound(AgoClass classInterval){
        var langClasses = classInterval.getClassLoader().getLangClasses();
        return classInterval.getConcreteTypeInfo() instanceof ParameterizedClassInfo p &&
                (p.getParameterizedBaseClass().equals(langClasses.getClassIntervalClass())
                || p.getParameterizedBaseClass().equals(langClasses.getScopedClassIntervalClass())
                || p.getParameterizedBaseClass().equals(langClasses.getGenericTypeParameterClass()));
    }

    public static AgoClass getLBound(AgoClass classInterval){
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) classInterval.getConcreteTypeInfo();
        return (AgoClass) parameterizedClassInfo.getArguments()[0];
    }
    public static AgoClass getUBound(AgoClass classInterval){
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) classInterval.getConcreteTypeInfo();
        return (AgoClass) parameterizedClassInfo.getArguments()[1];
    }

    // SharedGenericTypeParameterClassDef
    static Variance getVariance(AgoClass sharedGenericTypeParameter) {
        ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) sharedGenericTypeParameter.getConcreteTypeInfo();
        return Variance.of((Byte)parameterizedClassInfo.getArguments()[2]);
    }
}
