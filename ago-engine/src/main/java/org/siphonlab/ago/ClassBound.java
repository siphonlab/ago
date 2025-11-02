package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public interface ClassBound {
    // class bound,
    public static boolean isClassBound(AgoClass classInterval){
        AgoClassLoader classLoader = classInterval.getClassLoader();
        return classInterval.getConcreteTypeInfo() instanceof ParameterizedClassInfo p &&
                (p.getParameterizedBaseClass().equals(classLoader.getClassIntervalClass())
                || p.getParameterizedBaseClass().equals(classLoader.getScopedClassIntervalClass())
                || p.getParameterizedBaseClass().equals(classLoader.getGenericTypeParameterClass()));
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
