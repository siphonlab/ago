package org.siphonlab.ago.classloader;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.ParameterizedClassInfo;

public record GenericTypeCodeAvatarInfo(AgoClass genericParameter, AgoClass templateClass, int index, int genericTypeCode, String name) {

    public GenericParameterInfo getGenericParameterInfo() {
        return GenericParameterInfo.extract(genericParameter);
    }

    public static GenericTypeCodeAvatarInfo extract(AgoClass genericTypeCodeAvatar){
        ParameterizedClassInfo p = (ParameterizedClassInfo) genericTypeCodeAvatar.getConcreteTypeInfo();
        assert p.getParameterizedBaseClass().getFullname().equals("lang.GenericTypeCodeAvatar");

        var args = p.getArguments();
        return new GenericTypeCodeAvatarInfo((AgoClass) args[0], (AgoClass) args[1], (Integer) args[2], (Integer) args[3], (String) args[4]);
    }
}
