package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.TypeCode;

import java.io.IOException;

public class ResultSlotsSerializer extends JsonSerializer<ResultSlots> {

    @Override
    public void serialize(ResultSlots resultSlots, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("type", resultSlots.getDataType());
        gen.writeFieldName("value");

        switch (resultSlots.getDataType()) {
            case TypeCode.INT_VALUE:
                gen.writeNumber(resultSlots.getIntValue());
                break;
            case TypeCode.LONG_VALUE:
                gen.writeNumber(resultSlots.getLongValue());
                break;
            case TypeCode.SHORT_VALUE:
                gen.writeNumber(resultSlots.getShortValue());
                break;
            case TypeCode.BYTE_VALUE:
                gen.writeNumber(resultSlots.getByteValue());
                break;
            case TypeCode.FLOAT_VALUE:
                gen.writeNumber(resultSlots.getFloatValue());
                break;
            case TypeCode.DOUBLE_VALUE:
                gen.writeNumber(resultSlots.getDoubleValue());
                break;
            case TypeCode.BOOLEAN_VALUE:
                gen.writeBoolean(resultSlots.getBooleanValue());
                break;
            case TypeCode.STRING_VALUE:
                gen.writeString(resultSlots.getStringValue());
                break;
            case TypeCode.CHAR_VALUE:
                gen.writeString(String.valueOf(resultSlots.getCharValue()));
                break;
            case TypeCode.VOID_VALUE:
            case TypeCode.NULL_VALUE:
                gen.writeNull();
                break;

            case TypeCode.CLASS_REF_VALUE:
                gen.writeObject(resultSlots.getClassRefValue());
                break;

            case TypeCode.OBJECT_VALUE:
                gen.writeObject(resultSlots.getObjectValue());
                break;
        }

        gen.writeEndObject();
    }
}
