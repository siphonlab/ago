package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.TypeCode;

import java.io.IOException;

public class ResultSlotsDeserializer extends JsonDeserializer<ResultSlots> {

    private final AgoEngine agoEngine;

    public ResultSlotsDeserializer(AgoEngine agoEngine){
        this.agoEngine = agoEngine;
    }

    @Override
    public ResultSlots deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        ResultSlots resultSlots = new ResultSlots();
        assert p.nextToken() == JsonToken.START_OBJECT;
        p.nextToken();
        var dataType = p.getIntValue();

        assert p.nextToken() == JsonToken.FIELD_NAME;   // "value":
        p.nextToken();
        switch (dataType){
            case TypeCode.INT_VALUE:
                resultSlots.setIntValue(p.getIntValue());
                break;
            case TypeCode.LONG_VALUE:
                resultSlots.setLongValue(p.getLongValue());
                break;
            case TypeCode.SHORT_VALUE:
                resultSlots.setShortValue(p.getShortValue());
                break;
            case TypeCode.BYTE_VALUE:
                resultSlots.setByteValue(p.getByteValue());
                break;
            case TypeCode.FLOAT_VALUE:
                resultSlots.setFloatValue(p.getFloatValue());
                break;
            case TypeCode.DOUBLE_VALUE:
                resultSlots.setDoubleValue(p.getDoubleValue());
                break;
            case TypeCode.BOOLEAN_VALUE:
                resultSlots.setBooleanValue(p.getBooleanValue());
                break;
            case TypeCode.STRING_VALUE:
                resultSlots.setStringValue(p.getValueAsString());
                break;
            case TypeCode.CHAR_VALUE:
                resultSlots.setCharValue(p.getValueAsString().charAt(0));
                break;
            case TypeCode.VOID_VALUE:
            case TypeCode.NULL_VALUE:

                break;

            case TypeCode.CLASS_REF_VALUE:
                resultSlots.setClassRefValue(agoEngine.getClass(p.getValueAsString()));
                break;

            case TypeCode.OBJECT_VALUE:
                resultSlots.setObjectValue(ctxt.readValue(p, Instance.class));
                break;
        }
        assert p.nextToken() == JsonToken.END_OBJECT;
        p.nextToken();
        return resultSlots;
    }
}
