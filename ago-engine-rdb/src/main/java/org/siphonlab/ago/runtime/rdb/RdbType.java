package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;

import java.util.ArrayList;
import java.util.List;

public class RdbType {
    private int sqlType;
    private String typeName;
    private TypeCode typeCode;
    private AgoClass agoClass;

    private RdbType additional;

    public RdbType(TypeCode typeCode, int sqlType, String typeName) {
        this.sqlType = sqlType;
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public RdbType(TypeCode typeCode, int sqlType, String typeName, AgoClass agoClass) {
        this(typeCode, sqlType, typeName);
        this.agoClass = agoClass;
    }

    public void setAgoClass(AgoClass agoClass) {
        this.agoClass = agoClass;
    }

    public AgoClass getAgoClass() {
        return agoClass;
    }

    @Override
    public String toString() {
        return typeName;
    }

    public int getSqlType() {
        return sqlType;
    }

    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(TypeCode typeCode) {
        this.typeCode = typeCode;
    }

    public RdbType getAdditional() {
        return additional;
    }

    public void setAdditional(RdbType additional) {
        this.additional = additional;
    }

    public RdbType chain(RdbType anotherType) {
        var t = this;
        while (t.additional != null) {
            t = t.additional;
        }
        t.setAdditional(anotherType);
        return this;
    }

    public RdbType[] toTypes(){
        if(this.additional==null) return new RdbType[]{this};
        if(this.additional.additional == null) return new RdbType[]{this, this.additional};

        List<RdbType> ls = new ArrayList<>();
        for(var t = this; t != null; t = t.additional){
            ls.add(t);
        }
        return ls.toArray(RdbType[]::new);
    }

    public RdbType clone(){
        var t = new RdbType(typeCode,sqlType,typeName);
        t.setAgoClass(t.agoClass);
        if(t.additional != null){
            t.setAdditional(t.additional.clone());
        }
        return t;
    }
}
