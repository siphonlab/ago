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
package org.siphonlab.ago.runtime.rdb;

import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.IdGenerator;
import org.siphonlab.ago.*;

import java.sql.Types;
import java.util.List;
import java.util.Map;

public class H2Adapter extends RdbAdapter{

    public H2Adapter(BoxTypes boxTypes, ClassManager classManager, IdGenerator idGenerator) {
        super(boxTypes, classManager, idGenerator);
    }

    @Override
    public RdbType idType() {
        return mapType(TypeCode.LONG, null);
    }

    @Override
    protected void initTypeMap(Int2ObjectHashMap<RdbType> typeMap, Map<AgoClass, RdbType> standardDbTypes, ClassManager classManager) {
        typeMap.put(TypeCode.INT_VALUE, new RdbType(TypeCode.INT, Types.INTEGER, "integer"));
        typeMap.put(TypeCode.LONG_VALUE, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint"));
        typeMap.put(TypeCode.FLOAT_VALUE, new RdbType(TypeCode.FLOAT, Types.FLOAT, "float"));
        typeMap.put(TypeCode.DOUBLE_VALUE, new RdbType(TypeCode.DOUBLE, Types.DOUBLE, "double"));
        typeMap.put(TypeCode.BOOLEAN_VALUE, new RdbType(TypeCode.BOOLEAN, Types.BOOLEAN, "boolean"));
        typeMap.put(TypeCode.STRING_VALUE, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar"));
        typeMap.put(TypeCode.BYTE_VALUE, new RdbType(TypeCode.BYTE, Types.TINYINT, "tinyint"));
        typeMap.put(TypeCode.SHORT_VALUE, new RdbType(TypeCode.SHORT, Types.SMALLINT, "smallint"));
        typeMap.put(TypeCode.CHAR_VALUE, new RdbType(TypeCode.CHAR, Types.CHAR, "char"));
        typeMap.put(TypeCode.CLASS_REF_VALUE, new RdbType(TypeCode.CLASS_REF, Types.VARCHAR, "varchar(1024)"));

        AgoClass agoClass = classManager.getClass("VarChar");
        if(agoClass != null) standardDbTypes.put(agoClass, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar", agoClass));

        agoClass = classManager.getClass("BigInt");
        if (agoClass != null)
            standardDbTypes.put(agoClass, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint", agoClass));

    }

    @Override
    public void saveStrings(List<String> strings) {
        //TODO
    }

    @Override
    public void saveBlobs(List<byte[]> blobs) {
        //TODO
    }

}
