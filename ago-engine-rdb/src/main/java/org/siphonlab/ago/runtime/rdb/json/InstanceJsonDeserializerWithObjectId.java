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
package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.agrona.collections.Int2ObjectHashMap;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.json.AgoJsonParser;
import org.siphonlab.ago.runtime.json.InstanceJsonDeserializer;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.RowState;
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstanceJsonDeserializerWithObjectId extends InstanceJsonDeserializer {
    public InstanceJsonDeserializerWithObjectId(RdbEngine agoEngine) {
        super(agoEngine);
    }

    @Override
    protected void readObjectId(Slots slots, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        long id = ajp.getLongValue();
        ((RdbSlots) slots).setId(id);
        ajp.nextToken();
    }

    // {"@class":"class name", [@id: ], [scope: ]}
    @Override
    protected AgoClass deserializeComplexClass(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        var token = ajp.nextToken();  // after @class
        if (token == JsonToken.VALUE_STRING) {
            String className = ajp.getValueAsString();
            ajp.nextToken();  // pass class name
            ajp.nextToken();    // pass END_OBJECT
            return agoEngine.getClass(className);
        }
        ajp.nextToken();       // [

        String className = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(className);

        long id = -1;
        Instance<?> scope = null;

        while((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            ajp.nextToken();        // skip {
            String fieldName = ajp.getValueAsString();
            if (fieldName.equals("@id")) {
                ajp.nextToken();
                id = ajp.getValueAsLong();
            } else if (fieldName.equals("scope")) {
                scope = deserializeAny(ajp, ctxt, null, creator, null, null);
            }

            token = ajp.nextToken();    // }
        }
        ajp.nextToken();    // pass END_ARRAY
        ajp.nextToken();    // pass END_OBJECT

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
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.END_OBJECT;
        ajp.nextToken();

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
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        String classname = ajp.getValueAsString();
        ajp.nextToken();
        long id = ajp.getValueAsLong();
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.END_ARRAY;
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.END_OBJECT;
        ajp.nextToken();        // PASS END_OBJECT
        return ((RdbEngine) this.agoEngine).getRdbAdapter().restoreInstance(new ObjectRef(classname,id));
    }

    @Override
    protected void deserializeSlots(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator, Slots hostSlots, Map<String, AgoSlotDef> map) throws IOException {
        if(hostSlots instanceof RdbSlots rdbSlots) {
            if (ajp.currentToken() == JsonToken.START_OBJECT) ajp.nextToken();
            RowState rowState = null;
            List<Instance<?>> usingInstances = null;
            for(var token = ajp.currentToken(); token != JsonToken.END_OBJECT; token = ajp.currentToken()) {
                assert token == JsonToken.FIELD_NAME;
                String s = ajp.getValueAsString();
                ajp.nextToken();
                if (s.equals("usingInstances")) {
                    if (ajp.currentToken() == JsonToken.VALUE_NULL) {
                        ajp.nextToken();
                        continue;
                    }
                    assert ajp.currentToken() == JsonToken.START_ARRAY;
                    usingInstances = new ArrayList<>();
                    for (var el = ajp.nextToken(); el != JsonToken.END_ARRAY; el = ajp.currentToken()) {
                        usingInstances.add(deserializeAny(ajp, ctxt, null, null, null, null));
                    }
                    ajp.nextToken();
                } else if(s.equals("scope")){
                    var scope = deserializeAny(ajp,ctxt,null,creator,null,null);
                    MutableObject<Instance<?>> boxInstanceRef = (MutableObject<Instance<?>>) ctxt.getAttribute("boxerScope");
                    boxInstanceRef.setValue(scope);
                } else if(s.equals("rowState")){
                    rowState = RowState.valueOf(ajp.getValueAsString());
                    ajp.nextToken();
                } else if (s.equals("slots")) {
                    rdbSlots.beginRestore(usingInstances, rowState);
                    super.deserializeSlots(ajp, ctxt, creator, rdbSlots, map);
                    rdbSlots.endRestore();
                }
            }
            ajp.nextToken();
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