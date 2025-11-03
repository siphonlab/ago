package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.TypeCode;

/**
 * using an uno json to store slots
 */
public abstract class JsonSlotMapper {
    private final String[] jsonFiledNames;
    private final String[] dataTypes;

    public JsonSlotMapper(AgoSlotDef[] slotDefs){
        if (slotDefs != null) {
            this.jsonFiledNames = new String[slotDefs.length];
            this.dataTypes = new String[slotDefs.length];
            for (int i = 0; i < slotDefs.length; i++) {
                AgoSlotDef slotDef = slotDefs[i];
                jsonFiledNames[i] = composeFieldName(slotDef);
                dataTypes[i] = mapType(slotDef.getTypeCode(), slotDef.getAgoClass());
            }
        } else {
            jsonFiledNames = new String[0];
            dataTypes = new String[0];
        }
    }

    public abstract String mapType(TypeCode typeCode, AgoClass agoClass);

    public String[] getJsonFiledNames() {
        return jsonFiledNames;
    }

    public String[] getDataTypes() {
        return dataTypes;
    }

    public String getDataType(int slot) {
        return dataTypes[slot];
    }

    public String getFieldName(int slot) {
        return jsonFiledNames[slot];
    }

    private String composeFieldName(AgoSlotDef slotDef) {
        return slotDef.getName() + "_" + slotDef.getIndex();
    }
}
