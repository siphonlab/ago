package org.siphonlab.ago.classloader;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.ParameterizedClassInfo;
import org.siphonlab.ago.Variance;

public record GenericParameterInfo(AgoClass lBound, AgoClass uBound, Variance variance) {

    public static GenericParameterInfo extract(AgoClass genericParameter) {
        ParameterizedClassInfo p = (ParameterizedClassInfo) genericParameter.getConcreteTypeInfo();
        assert p.getParameterizedBaseClass().getFullname().equals("lang.GenericTypeParameter");

        var args = p.getArguments();
        return new GenericParameterInfo((AgoClass) args[0], (AgoClass) args[1], Variance.of((Byte) args[2]));

    }
}
