package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.native_.AgoNativeFunction;

import java.util.Map;
import java.util.Objects;

public class AgoEnum extends AgoClass{

    private TypeCode basePrimitiveType;
    private Map<String, Object> enumValues;

    protected AgoEnum(AgoClassLoader classLoader, String fullname, String name) {
        super(classLoader, fullname, name);
        this.type = AgoClass.TYPE_ENUM;
    }

    public AgoEnum(AgoClassLoader classLoader, MetaClass metaClass, String fullname, String name) {
        super(classLoader, metaClass, fullname, name);
        this.type = AgoClass.TYPE_ENUM;
    }

    public void setBasePrimitiveType(TypeCode basePrimitiveType) {
        this.basePrimitiveType = basePrimitiveType;
    }

    public TypeCode getBasePrimitiveType() {
        return basePrimitiveType;
    }

    public void setEnumValues(Map<String, Object> enumValues) {
        this.enumValues = enumValues;
    }

    public Map<String, Object> getEnumValues() {
        return enumValues;
    }

    public Instance<?> findMember(Object value) {
        for (AgoField field : agoClass.getFields()) {
            if(Objects.equals(field.getConstLiteralValue(), value)){
                return slots.getObject(field.getSlotIndex());
            }
        }
        return null;
    }

    public Instance<?> findMember(String value) {
        return slots.getObject(agoClass.findField(value).getSlotIndex());
    }

    @Override
    public AgoEnum withScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoEnum(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

    @Override
    protected void copyTo(AgoClass cls) {
        var copy = (AgoEnum) cls;
        super.copyTo(cls);
        copy.setBasePrimitiveType(this.basePrimitiveType);
        copy.setEnumValues(this.enumValues);
    }

}
