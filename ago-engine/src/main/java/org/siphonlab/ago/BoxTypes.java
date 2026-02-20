/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN;
import static org.siphonlab.ago.TypeCode.BYTE;
import static org.siphonlab.ago.TypeCode.CHAR;
import static org.siphonlab.ago.TypeCode.DOUBLE;
import static org.siphonlab.ago.TypeCode.FLOAT;
import static org.siphonlab.ago.TypeCode.INT;
import static org.siphonlab.ago.TypeCode.LONG;
import static org.siphonlab.ago.TypeCode.SHORT;
import static org.siphonlab.ago.TypeCode.STRING;

public class BoxTypes {

    private final Map<TypeCode, AgoClass> mapTypeCodeToClass;

    Map<AgoClass, TypeCode> boxTypes = new HashMap<>();
    Map<AgoClass, TypeCode> boxTypesInterfaces = new HashMap<>();
    Map<AgoClass, TypeCode> cache = new HashMap<>();

    public BoxTypes(AgoClassLoader agoClassLoader){
        Map<String, AgoClass> classByName = agoClassLoader.getClassByName();
        AgoClass t;
        t = classByName.get("lang.Integer");
        if(t != null) boxTypes.put(t, INT);
        t = classByName.get("lang.String");
        if(t != null) boxTypes.put(t, STRING);
        t = classByName.get("lang.Long");
        if(t != null) boxTypes.put(t, LONG);
        t = classByName.get("lang.Boolean");
        if(t != null) boxTypes.put(t, BOOLEAN);
        t = classByName.get("lang.Float");
        if(t != null) boxTypes.put(t, FLOAT);
        t = classByName.get("lang.Double");
        if(t != null) boxTypes.put(t, DOUBLE);
        t = classByName.get("lang.Character");
        if(t != null) boxTypes.put(t, CHAR);
        t = classByName.get("lang.Byte");
        if(t != null) boxTypes.put(t, BYTE);
        t = classByName.get("lang.Short");
        if(t != null) boxTypes.put(t, SHORT);
        t = classByName.get("lang.ClassRef");
        if (t != null) boxTypes.put(t, CLASS_REF);

        this.mapTypeCodeToClass = boxTypes.entrySet().stream().collect(Collectors.toMap(e -> e.getValue(), e-> e.getKey()));

        t = classByName.get("lang.Boxer<int>");
        if(t != null) boxTypesInterfaces.put(t, INT);
        t = classByName.get("lang.Boxer<string>");
        if(t != null) boxTypesInterfaces.put(t, STRING);
        t = classByName.get("lang.Boxer<long>");
        if(t != null) boxTypesInterfaces.put(t, LONG);
        t = classByName.get("lang.Boxer<float>");
        if(t != null) boxTypesInterfaces.put(t, FLOAT);
        t = classByName.get("lang.Boxer<double>");
        if(t != null) boxTypesInterfaces.put(t, DOUBLE);
        t = classByName.get("lang.Boxer<boolean>");
        if(t != null) boxTypesInterfaces.put(t, BOOLEAN);
        t = classByName.get("lang.Boxer<char>");
        if(t != null) boxTypesInterfaces.put(t, CHAR);
        t = classByName.get("lang.Boxer<byte>");
        if(t != null) boxTypesInterfaces.put(t, BYTE);
        t = classByName.get("lang.Boxer<short>");
        if(t != null) boxTypesInterfaces.put(t, SHORT);
        t = classByName.get("lang.Boxer<classref>");
        if (t != null) boxTypesInterfaces.put(t, CLASS_REF);

        t = classByName.get("lang.Enum<int>");
        if (t != null) boxTypesInterfaces.put(t, INT);
        t = classByName.get("lang.Enum<byte>");
        if (t != null) boxTypesInterfaces.put(t, BYTE);
        t = classByName.get("lang.Enum<short>");
        if (t != null) boxTypesInterfaces.put(t, SHORT);
        t = classByName.get("lang.Enum<long>");
        if (t != null) boxTypesInterfaces.put(t, LONG);

        t = classByName.get("lang.IntEnum");
        if (t != null) boxTypesInterfaces.put(t, INT);
        t = classByName.get("lang.ByteEnum");
        if (t != null) boxTypesInterfaces.put(t, BYTE);
        t = classByName.get("lang.ShortEnum");
        if (t != null) boxTypesInterfaces.put(t, SHORT);
        t = classByName.get("lang.LongEnum");
        if (t != null) boxTypesInterfaces.put(t, LONG);
    }

    public boolean isBoxType(AgoClass agoClass) {
        if(agoClass == null || agoClass instanceof MetaClass) return false;
        TypeCode unboxType = getUnboxType(agoClass);
        return unboxType != null && unboxType != VOID;
    }

    public boolean isBoxTypeOrWithin(AgoClass agoClass){
        var r = this.isBoxType(agoClass);
        if(r) return true;

        if(agoClass.getParent() != null) return isBoxType(agoClass.getParent());

        return false;
    }

    public TypeCode getUnboxType(AgoClass agoClass) {
        TypeCode typeCode = cache.get(agoClass);
        if (typeCode != null) {
            if(typeCode == VOID) return null;
            return typeCode;
        }

        var t = boxTypes.get(agoClass);
        if (t != null) {
            cache.put(agoClass, t);
            return t;
        }
        t = boxTypesInterfaces.get(agoClass);
        if (t != null)
            return t;

        // we only handle single field type
        if(agoClass.getSlotDefs() != null && agoClass.getSlotDefs().length == 1 && agoClass.getInterfaces() != null) {
            for (AgoClass anInterface : agoClass.getInterfaces()) {
                t = getUnboxType(anInterface);
                if (t != null) {
                    cache.put(agoClass, t);
                    return t;
                }
            }
        }
        if (agoClass.getSuperClass() != null && agoClass.getSuperClass() != agoClass) {
            t = getUnboxType(agoClass.getSuperClass());
            if (t != null) {
                cache.put(agoClass, t);
                return t;
            }
        }
        cache.put(agoClass, VOID);
        return null;
    }

    public AgoClass getBoxType(TypeCode typeCode) {
        return mapTypeCodeToClass.get(typeCode);
    }

    public Object unbox(ClassManager classManager, Instance<?> instance) {
        if (instance == null) return null;
        var unboxType = getUnboxType(instance.getAgoClass());
        if (unboxType == null) return instance;

        Slots slots = instance.getSlots();
        switch (unboxType.getValue()) {
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE:
                return slots.getObject(0);
            case INT_VALUE:
                return slots.getInt(0);
            case BYTE_VALUE:
                return slots.getByte(0);
            case SHORT_VALUE:
                return slots.getShort(0);
            case LONG_VALUE:
                return slots.getLong(0);
            case FLOAT_VALUE:
                return slots.getFloat(0);
            case DOUBLE_VALUE:
                return slots.getDouble(0);
            case BOOLEAN_VALUE:
                return slots.getBoolean(0);
            case CHAR_VALUE:
                return slots.getChar(0);
            case STRING_VALUE:
                return slots.getString(0);
            case CLASS_REF_VALUE:
                return classManager.getClass(slots.getClassRef(0));
            default:
                throw new UnsupportedOperationException("unexpected data type " + unboxType);
        }
    }
}
