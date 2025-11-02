package org.siphonlab.ago.runtime.rdb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.json.InstanceJsonDeserializer;
import org.siphonlab.ago.runtime.json.InstanceJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.commons.dbcp2.Utils.closeQuietly;

public class RdbEngine extends AgoEngine {

    public final static Logger LOGGER = LoggerFactory.getLogger(RdbEngine.class);

    RdbAdapter rdbAdapter;

    public RdbEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(runSpaceHost);
        this.rdbAdapter = rdbAdapter;
    }

    public RdbEngine(RdbAdapter rdbAdapter) {
        super();
        this.rdbAdapter = rdbAdapter;
    }

    protected ObjectMapper createJsonObjectMapper(AgoClassLoader classLoader, boolean writeType, boolean writeId, boolean serializeObjectAsReference, boolean serializeSlots) {
        var r = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        BoxTypes boxTypes = new BoxTypes(classLoader);
        module.addSerializer(Instance.class, new InstanceJsonSerializerWithObjectId(this, boxTypes, false, serializeSlots));
        module.addDeserializer(Instance.class, new JsonDeserializerWithObjectId(this, boxTypes, serializeObjectAsReference, serializeSlots));

        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));

        r.registerModule(module);

        return r.copyWith(new AgoJsonFactory(writeType, writeId, serializeObjectAsReference));
    }


    static class InstanceJsonSerializerWithObjectId extends InstanceJsonSerializer {

        public InstanceJsonSerializerWithObjectId(AgoEngine agoEngine, BoxTypes boxTypes, boolean writeType, boolean serializeSlots) {
            super(agoEngine, boxTypes, writeType, serializeSlots);
        }

        @Override
        public void writeObjectId(Instance<?> instance, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            RdbSlots slots = (RdbSlots) instance.getSlots();
            gen.writeNumberField("@id", slots.getId());
        }
    }

    static class JsonDeserializerWithObjectId extends InstanceJsonDeserializer {
        public JsonDeserializerWithObjectId(AgoEngine agoEngine, BoxTypes boxTypes, boolean serializeObjectAsReference, boolean serializeSlots) {
            super(agoEngine, boxTypes, serializeObjectAsReference, serializeSlots);
        }

        @Override
        protected void readObjectId (Instance<?> currObject, JsonParser p, CallFrame<?> callFrame) throws IOException {
            long id = p.getLongValue();
            ((RdbSlots) currObject.getSlots()).setId(id);
        }

        @Override
        protected Instance<?> acceptObject(Instance<?> instance) {
            RdbSlots slots = (RdbSlots) instance.getSlots();
            slots.setRowState(RowState.Unchanged);
            return super.acceptObject(instance);
        }
    }

    public RdbAdapter getRdbAdapter() {
        return rdbAdapter;
    }

    @Override
    protected AgoRunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return new RdbAgoSpace(this, runSpaceHost);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace) {
//        if(!boxTypes.isBoxType(agoClass)) {
//            ((RdbAgoSpace)runSpace).collectInstance(inst);
//        }
        return super.createInstance(parentScope, agoClass, creator, runSpace);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createFunctionInstance(parentScope, agoFunction, caller, creator, runSpace);
//        ((RdbAgoSpace) runSpace).collectInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, int classId, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, classId, creator);
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
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
