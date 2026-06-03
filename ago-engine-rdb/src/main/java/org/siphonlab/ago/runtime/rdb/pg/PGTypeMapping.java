package org.siphonlab.ago.runtime.rdb.pg;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.RdbType;
import org.siphonlab.ago.runtime.rdb.TypeMapping;

import java.sql.Types;

import static org.siphonlab.ago.TypeCode.STRING;

public class PGTypeMapping extends TypeMapping {

    public PGTypeMapping(BoxTypes boxTypes) {
        super(boxTypes);
    }

    @Override
    protected void initTypeMap(ClassManager rdbEngine) {
        typeMap.put(TypeCode.INT_VALUE, new RdbType(TypeCode.INT, Types.INTEGER, "integer"));
        typeMap.put(TypeCode.LONG_VALUE, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint"));
        typeMap.put(TypeCode.FLOAT_VALUE, new RdbType(TypeCode.FLOAT, Types.FLOAT, "float"));
        typeMap.put(TypeCode.DOUBLE_VALUE, new RdbType(TypeCode.DOUBLE, Types.DOUBLE, "double"));
        typeMap.put(TypeCode.DECIMAL_VALUE, new RdbType(TypeCode.DECIMAL, Types.DECIMAL, "decimal"));
        typeMap.put(TypeCode.BOOLEAN_VALUE, new RdbType(TypeCode.BOOLEAN, Types.BOOLEAN, "boolean"));
        typeMap.put(TypeCode.STRING_VALUE, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar"));
        typeMap.put(TypeCode.BYTE_VALUE, new RdbType(TypeCode.BYTE, Types.TINYINT, "smallint"));    // no byte type in PG
        typeMap.put(TypeCode.SHORT_VALUE, new RdbType(TypeCode.SHORT, Types.SMALLINT, "smallint"));
        typeMap.put(TypeCode.CHAR_VALUE, new RdbType(TypeCode.CHAR, Types.CHAR, "char"));
        typeMap.put(TypeCode.CLASS_REF_VALUE, new RdbType(TypeCode.CLASS_REF, Types.VARCHAR, "varchar(1024)"));
        typeMap.put(TypeCode.UNION_VALUE, new RdbType(TypeCode.UNION, Types.JAVA_OBJECT, "jsonb"));
        typeMap.put(TypeCode.OBJECT_VALUE, new RdbType(TypeCode.OBJECT, Types.JAVA_OBJECT, "jsonb"));

        AgoClass agoClass = classManager.getClass("VarChar");
        if (agoClass != null) parameterizedDbTypes.put(agoClass, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar", agoClass));

        agoClass = classManager.getClass("BigInt");
        if (agoClass != null)
            parameterizedDbTypes.put(agoClass, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint", agoClass));
    }

    // standardType is parameterizable
    protected RdbType mapStandardType(RdbType standardType, AgoClass agoClass) {
        if (standardType.getTypeCode() == STRING && agoClass.getParameterizedBaseClass() == standardType.getAgoClass()) {
            ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) agoClass.getConcreteTypeInfo();
            Object length = parameterizedClassInfo.getArguments()[0];
            String typename = "varchar(%s)".formatted(length);
            return new RdbType(standardType.getTypeCode(), standardType.getSqlType(), typename);
        }
        return standardType;
    }

}
