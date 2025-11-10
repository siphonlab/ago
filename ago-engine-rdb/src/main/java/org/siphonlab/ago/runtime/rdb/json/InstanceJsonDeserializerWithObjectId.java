package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.json.AgoJsonParser;
import org.siphonlab.ago.runtime.json.InstanceJsonDeserializer;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.RowState;

import java.io.IOException;
import java.util.Map;

public class InstanceJsonDeserializerWithObjectId extends InstanceJsonDeserializer {
    public InstanceJsonDeserializerWithObjectId(RdbEngine agoEngine) {
        super(agoEngine);
    }

    @Override
    protected void readObjectId(Slots slots, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        long id = ajp.getLongValue();
        ((RdbSlots) slots).setId(id);
    }

    // {"@class":"class name", [@id: ], [scope: ]}
    @Override
    protected AgoClass deserializeClass(AgoClass baseClass, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        var token = ajp.nextToken();
        long id = -1;
        Instance<?> scope = null;
        while ((token = ajp.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.FIELD_NAME) {
                if (ajp.getValueAsString().equals("@id")) {
                    id = ajp.getValueAsLong();
                } else if (ajp.getValueAsString().equals("scope")) {
                    scope = deserializeAny(ajp, ctxt, null, creator, null, null);
                }
            }
        }
        if (((RdbSlots) baseClass.getSlots()).getId() == id) return baseClass;

        if (id == -1) throw new IllegalStateException("class id not found");
        return ((RdbEngine) this.agoEngine).getRdbAdapter().loadScopedAgoClass(baseClass, id);
    }

    //{"@classref": [classname, id, [parentScope]]}
    protected Instance<?> deserializeClassRef(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        assert ajp.nextToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        String classname = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(classname);
        JsonToken token;
        AgoClass result = null;
        long id = -1;
        Instance<?> scope;
        while ((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NUMBER_INT) {      // id
                id = ajp.getValueAsLong();
            } else if (token == JsonToken.START_OBJECT) {
                scope = deserializeAny(ajp, ctxt, null, creator, null, null);
            }
        }
        assert ajp.nextToken() == JsonToken.END_OBJECT;

        if (((RdbSlots) baseClass.getSlots()).getId() == id) return baseClass;

        if(id ==-1) throw new IllegalStateException("class id not found");
        return  ((RdbEngine)this.agoEngine).getRdbAdapter().loadScopedAgoClass(baseClass, id);
    }

    @Override
    protected AgoClass deserializeClassRef(AgoClass baseClass, long id) {
        if (((RdbSlots) baseClass.getSlots()).getId() == id) return baseClass;
        return ((RdbEngine) this.agoEngine).getRdbAdapter().loadScopedAgoClass(baseClass, id);
    }

    // {"@objectref": [classname, id]}
    @Override
    protected Instance<?> deserializeObjectRef(AgoJsonParser ajp, DeserializationContext ctxt) throws IOException {
        assert ajp.nextToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        String classname = ajp.getValueAsString();
        ajp.nextToken();
        long id = ajp.getValueAsLong();
        assert ajp.nextToken() == JsonToken.END_ARRAY;
        assert ajp.nextToken() == JsonToken.END_OBJECT;
        return ((RdbEngine) this.agoEngine).getRdbAdapter().restoreInstance(new ObjectRef(classname,id));
    }

    @Override
    protected void deserializeSlots(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator, Slots hostSlots, Map<String, AgoSlotDef> map) throws IOException {
        if(hostSlots instanceof RdbSlots rdbSlots) {
            super.deserializeSlots(ajp, ctxt, creator, rdbSlots.getBaseSlots(), map);
        } else {
            super.deserializeSlots(ajp, ctxt, creator, hostSlots, map);
        }
    }

    @Override
    protected Instance<?> acceptObject(Instance<?> instance) {
        RdbSlots slots = (RdbSlots) instance.getSlots();
        slots.setRowState(RowState.Unchanged);
        return super.acceptObject(instance);
    }
}