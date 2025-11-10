package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.siphonlab.ago.Slots;

import java.io.IOException;

public class SlotsJsonDeserializer extends JsonDeserializer<Slots> {

    @Override
    public Slots deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
