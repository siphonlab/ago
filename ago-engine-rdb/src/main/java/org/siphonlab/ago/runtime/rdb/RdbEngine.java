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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.mutable.MutableObject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonSerializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonDeserializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.RdbSlotsJsonSerializer;
import org.siphonlab.ago.runtime.rdb.json.SlotsJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.apache.commons.dbcp2.Utils.closeQuietly;

public class RdbEngine extends AgoEngine {

    public final static Logger logger = LoggerFactory.getLogger(RdbEngine.class);

    RdbAdapter rdbAdapter;

    protected ObjectMapper dumpingObjectMapper;

    public RdbEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(runSpaceHost);
        this.rdbAdapter = rdbAdapter;
        createDumpingObjectMapper();
    }

    public RdbEngine(RdbAdapter rdbAdapter) {
        super();
        this.rdbAdapter = rdbAdapter;
        createDumpingObjectMapper();
    }


    @Override
    protected ObjectMapper createDefaultObjectMapper() {
        var r = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        BoxTypes boxTypes = getBoxTypes();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);
        return r;
    }

    protected void createDumpingObjectMapper(){
        var r = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());
        module.addSerializer(Slots.class, new SlotsJsonSerializer(this));
        module.addSerializer(RdbSlots.class, new RdbSlotsJsonSerializer(this));

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);

        //new AgoJsonConfig(AgoJsonConfig.WriteTypeMode.Always, true, AgoJsonConfig.ObjectAsReferenceMode.Always, true)
        this.dumpingObjectMapper = r.copyWith(new AgoJsonParserFactory(AgoJsonConfig.RPC_OBJECT_REF));
    }


    public RdbAdapter getRdbAdapter() {
        return rdbAdapter;
    }

    public String dumpJson(Instance<?> object) throws JsonProcessingException {
        return this.dumpingObjectMapper.writeValueAsString(object);
    }

    public String dumpJson(ResultSlots object) throws JsonProcessingException {
        return this.dumpingObjectMapper.writeValueAsString(object);
    }

    public ObjectMapper getDumpingObjectMapper() {
        return dumpingObjectMapper;
    }

    // restore dumped json to Instance
    public Instance<?> restoreJson(String json) throws IOException {
        return this.dumpingObjectMapper.readValue(new StringReader(json), Instance.class);
    }

    // restore dumped json to Instance
    public Instance<?> restoreJson(String json, CallFrame<?> creator) throws IOException {
        return dumpingObjectMapper
                .copyWith(new AgoJsonParserFactory(AgoJsonConfig.RPC_OBJECT_REF, null, creator))
                .readValue(new StringReader(json), Instance.class);
    }

    public String jsonStringifySlots(Instance<?> instance) throws JsonProcessingException {
        Slots slots = instance.getSlots();
        return dumpingObjectMapper
                .writerFor(slots.getClass())
                .withAttribute("slots_class", instance.getAgoClass())
                .withAttribute("slots_instance", instance)
                .writeValueAsString(slots);
    }

    @Override
    public AgoClass getClass(String name) {
        var r = super.getClass(name);
        if(r == null && name.equals("<Meta>")){
            return this.getTheMeta();
        }
        return r;
    }

    public String jsonStringifySlots(Slots slots, AgoClass agoClass) throws JsonProcessingException {
        return dumpingObjectMapper
                .writerFor(slots.getClass())
                .withAttribute("slots_class", agoClass)
                .writeValueAsString(slots);
    }

    public void restoreSlots(Slots slots, AgoClass agoClass, String json, MutableObject<Instance<?>> boxInstanceScope) throws JsonProcessingException {
        dumpingObjectMapper.readerFor(Instance.class)
                .withAttribute("slots_class", agoClass)
                .withAttribute("slots", slots)
                .withAttribute("boxerScope", boxInstanceScope)
                .readValue(json)
        ;
    }

    @Override
    public Instance<?> jsonDeserialize(AgoClass agoClass, CallFrame<?> callFrame, Reader reader, boolean deserializeSlots) throws IOException {
        return super.jsonDeserialize(agoClass, callFrame, reader, deserializeSlots);
    }

    @Override
    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        var r = createRunSpaceInner(runSpaceHost);
        this.rdbAdapter.saveRunSpace(r);
        return r;
    }

    @Override
    protected RdbRunSpace createRunSpaceInner(RunSpaceHost runSpaceHost) {
        return new RdbRunSpace(this, rdbAdapter, runSpaceHost);
    }

    @Override
    public AgoClass createScopedClass(CallFrame<?> caller, int classId, Instance<?> parentScope) {
        var c = this.getClass(classId).cloneWithScope(parentScope);
        if (parentScope == null) return c;

        ((RdbSlots)c.getSlots()).setId(rdbAdapter.nextId());
        this.rdbAdapter.saveInstance(c);

        AgoFunction emptyArgsConstructor = c.getAgoClass().getEmptyArgsConstructor();
        if (emptyArgsConstructor != null) {
            c.invokeMethod(caller, emptyArgsConstructor, emptyArgsConstructor);
        }
        return c;
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
//        if(!boxTypes.isBoxType(agoClass)) {
//            ((RdbAgoSpace)runSpace).collectInstance(inst);
//        }
        return super.createInstance(parentScope, agoClass, creator);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        var inst = super.createFunctionInstance(parentScope, agoFunction, caller, creator);
//        ((RdbAgoSpace) runSpace).collectInstance(inst);
        return inst;
    }

    public void saveInstance(Instance<?> instance){
        rdbAdapter.saveInstance(instance);
    }

    public ResultSetMapper fetchAll(AgoClass entityClass, CallFrame<?> callFrame) {
        var r = this.rdbAdapter.fetchAll(entityClass);
        r.setAgoEngine(this);
        r.setCreator(callFrame);
        return r;
    }

    public Instance<?> getById(AgoClass entityClass, Object id){
        var r = this.rdbAdapter.getById(entityClass, this, id);
        return r;
    }
}
