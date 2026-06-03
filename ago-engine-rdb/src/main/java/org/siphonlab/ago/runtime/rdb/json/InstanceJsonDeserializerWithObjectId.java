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
import org.apache.commons.lang3.mutable.MutableObject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.WorkflowAdapter;
import org.siphonlab.ago.runtime.json.AgoJsonParser;
import org.siphonlab.ago.runtime.json.InstanceJsonDeserializer;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.rdb.RowState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InstanceJsonDeserializerWithObjectId<Id> extends InstanceJsonDeserializer<Id> {

    public InstanceJsonDeserializerWithObjectId(DbEngine<Id> agoEngine, Object sampleId) {
        super(agoEngine, sampleId);
    }


    @Override
    protected void readObjectId(Slots slots, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        DbSlots<Id> dbSlots = (DbSlots<Id>) slots;
        dbSlots.setObjectRef(ObjectRef.create(dbSlots.getObjectRef().className(), readId(ajp)));
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

        Id id = null;
        Instance<?> scope = null;

        while((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            ajp.nextToken();        // skip {
            String fieldName = ajp.getValueAsString();
            if (fieldName.equals("@id")) {
                ajp.nextToken();
                id = readId(ajp);
            } else if (fieldName.equals("scope")) {
                scope = deserializeAny(ajp, ctxt, null, creator, null, null);
            }

            token = ajp.nextToken();    // }
        }
        ajp.nextToken();    // pass END_ARRAY
        ajp.nextToken();    // pass END_OBJECT

        if (Objects.equals(((DbSlots) baseClass.getSlots()).getObjectRef().id(), id)) return baseClass;

        if (id == null) throw new IllegalStateException("class id not found");
        return ((WorkflowAdapter) ((DbEngine) this.agoEngine).getDbAdapter()).loadScopedAgoClass(baseClass, id);
    }

    //{"@classref": [classname, id, [parentScope]]}
    protected Instance<?> deserializeClassRef(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        assert ajp.nextToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        String classname = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(classname);
        JsonToken token;
        AgoClass result = null;
        Object id = null;
        Instance<?> scope;
        while ((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NUMBER_INT) {      // id
                id = readId(ajp);
            } else if (token == JsonToken.START_OBJECT) {
                scope = deserializeAny(ajp, ctxt, null, creator, null, null);
            }
        }
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.END_OBJECT;
        ajp.nextToken();

        if (Objects.equals(((DbSlots<Id>) baseClass.getSlots()).getObjectRef().id(), id)) return baseClass;

        if(id == null) throw new IllegalStateException("class id not found");
        return ((WorkflowAdapter)((DbEngine)this.agoEngine).getDbAdapter()).loadScopedAgoClass(baseClass, id);
    }

    @Override
    protected AgoClass deserializeClassRef(AgoClass baseClass, Id id) {
        if (((DbSlots) baseClass.getSlots()).getObjectRef() == id) return baseClass;
        return ((WorkflowAdapter)((DbEngine)this.agoEngine).getDbAdapter()).loadScopedAgoClass(baseClass, id);
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
        return ((DbEngine) this.agoEngine).getDbAdapter().getById(ObjectRef.create(classname,id));
    }

    @Override
    protected void deserializeSlots(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator, Slots hostSlots, Map<String, AgoSlotDef> map) throws IOException {
        if(hostSlots instanceof DbSlots dbSlots) {
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
                    dbSlots.beginRestore(usingInstances, rowState);
                    super.deserializeSlots(ajp, ctxt, creator, dbSlots, map);
                    dbSlots.endRestore();
                }
            }
            ajp.nextToken();
        } else {
            super.deserializeSlots(ajp, ctxt, creator, hostSlots, map);
        }
    }

    @Override
    protected Instance<?> acceptObject(Instance<?> instance) {
        var slots = (DbSlots<?>) instance.getSlots();
        slots.setRowState(RowState.Unchanged);
        return super.acceptObject(instance);
    }
}