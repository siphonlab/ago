package org.siphonlab.ago.runtime.rdb;

import org.agrona.collections.Int2ObjectHashMap;
import org.siphonlab.ago.*;

import java.util.HashMap;
import java.util.Map;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.STRING;

public abstract class TypeMapping {
    protected final BoxTypes boxTypes;
    protected ClassManager classManager;
    // primitive typecode -> RdbType
    protected Int2ObjectHashMap<RdbType> typeMap = new Int2ObjectHashMap<>();
    // more detailed db types, i..e VarChar::(length), BigInt ...
    // maybe need multiple columns, but now not found
    protected Map<AgoClass, RdbType> parameterizedDbTypes = new HashMap<>();
    protected Map<AgoClass, RdbType> typeMappingCache = new HashMap<>();

    protected RdbType idRdbType;

    protected TypeMapping(BoxTypes boxTypes) {
        this.boxTypes = boxTypes;
    }

    public void setIdRdbType(RdbType idRdbType) {
        this.idRdbType = idRdbType;
    }

    protected abstract void initTypeMap(ClassManager rdbEngine);

    // return at least one typename for one type, allow multi types, for maybe need multi columns for one object field
    public RdbType mapType(TypeCode typeCode, AgoClass agoClass) {
        RdbType types = typeMappingCache.get(agoClass);
        if (types != null) return types;
        if (typeCode == OBJECT || typeCode == UNION) {
            var r = mapObjectType(agoClass);
            typeMappingCache.put(agoClass, r);
            return r;
        } else {
            return typeMap.get(typeCode.value);
        }
    }

    protected RdbType mapObjectType(AgoClass agoClass) {
        var existed = parameterizedDbTypes.get(agoClass);
        if (existed != null) return existed;
        for (AgoClass k : parameterizedDbTypes.keySet()) {
            if (agoClass.isThatOrDerivedFrom(k)) {
                return mapStandardType(parameterizedDbTypes.get(k), agoClass);
            }
        }

        if (agoClass instanceof MetaClass) {
            // if slot is MetaClass, it means the value is Class, we store it with STRING
            return mapType(STRING, null);
        }
        if (boxTypes.isBoxType(agoClass)) {
            TypeCode unboxType = boxTypes.getUnboxType(agoClass);
            return mapType(unboxType, null);
        } else {
            var idType = idRdbType.clone();
            var classNameType = mapType(STRING, null).clone();
            return idType.chain(classNameType);
        }
    }

    protected abstract RdbType mapStandardType(RdbType standardType, AgoClass agoClass);
}
