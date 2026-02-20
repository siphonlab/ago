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
package org.siphonlab.ago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.channel.nio.NioEventLoopGroup;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.classloader.ClassRefValue;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.Executors;

import static org.siphonlab.ago.TypeCode.*;

public class AgoEngine implements ClassManager{

    private final static Logger LOGGER = LoggerFactory.getLogger(AgoEngine.class);
    protected RunSpaceHost runSpaceHost;

    private Map<String, AgoClass> classByName = new HashMap<>();
    private AgoClass[] classes = null;
    private String[] strings = null;       // merged const strings

    private byte[][] blobs;
    private Boxer boxer;
    private AgoClass scopedClassIntervalClass;
    protected AgoClass PRIMITIVE_TYPE;
    protected AgoClass PRIMITIVE_NUMBER_TYPE;

    private RunSpace runSpace;
    private BoxTypes boxTypes;

    protected ObjectMapper defaultObjectMapper;
    protected Map<AgoJsonConfig, ObjectMapper> jsonObjectMappers = new HashMap<>();

    private AgoClass runSpaceClass;

    private LangClasses langClasses;

    public String toString(int i){
        return strings[i];
    }

    //TODO support multiple class loader, each class loader has its meta
    private MetaClass theMata;

    public static class MetaClassCreatingTask{
        AgoClass target;
        MetaClass metaClass;
        AgoFunction constructor;
        Object[] arguments;

        public AgoClass getTarget() {
            return target;
        }

        public void setTarget(AgoClass target) {
            this.target = target;
        }

        public MetaClass getMetaClass() {
            return metaClass;
        }

        public void setMetaClass(MetaClass metaClass) {
            this.metaClass = metaClass;
        }

        public AgoFunction getConstructor() {
            return constructor;
        }

        public void setConstructor(AgoFunction constructor) {
            Objects.requireNonNull(constructor);
            this.constructor = constructor;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public void setArguments(Object[] arguments) {
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return "(ApplyMetaConstructor %s %s on %s)".formatted(metaClass.getFullname(), constructor.getFullname(), target.getFullname());
        }
    }

    public AgoEngine(){
        this(new NettyEventLoopRunSpaceHost(new NioEventLoopGroup(0, Executors.newCachedThreadPool())));
    }

    public AgoEngine(RunSpaceHost runSpaceHost){
        this.runSpaceHost = runSpaceHost;
    }

    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return createRunSpaceInner(runSpaceHost);
    }

    protected RunSpace createRunSpaceInner(RunSpaceHost runSpaceHost) {
        return new RunSpace(this, runSpaceHost);
    }


    public void load(AgoClassLoader classLoader){
        this.theMata = classLoader.getTheMeta();

        this.runSpace = createRunSpace(this.runSpaceHost);

        this.classes = classLoader.getClasses().toArray(new AgoClass[0]);
        this.strings = classLoader.getStrings().toArray(new String[0]);
        this.classByName = classLoader.getClassByName();
        this.blobs = classLoader.getBlobs().toArray(new byte[0][]);

        this.langClasses = classLoader.getLangClasses();

        this.boxer = classLoader.createBoxer(this);
        this.scopedClassIntervalClass = this.getScopedClassIntervalClass();
        this.PRIMITIVE_TYPE = langClasses.getPrimitiveClass();
        this.PRIMITIVE_NUMBER_TYPE = langClasses.getPrimitiveNumberClass();

        this.boxTypes = classLoader.getBoxTypes();
        this.runSpaceClass = langClasses.getRunSpaceClass();

        // applyMetaClasses(classLoader.getMetaClassCreationQueue()); // TODO applyMetaClasses will change the slots info, however, we need jsonObjectMapper for dump slots
        applyMetaClasses(classLoader.getMetaClassCreationQueue());
    }

    public AgoClass getRunSpaceClass() {
        return runSpaceClass;
    }

    public BoxTypes getBoxTypes() {
        return boxTypes;
    }

    protected ObjectMapper createDefaultObjectMapper(){
        var r = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializer(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());

        module.addDeserializer(Instance.class, new InstanceJsonDeserializer(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));
        r.registerModule(module);
        return r;
    }

    public ObjectMapper getObjectMapper(AgoJsonConfig agoJsonConfig) {
        if(defaultObjectMapper == null){
            defaultObjectMapper = createDefaultObjectMapper();
        }
        return this.jsonObjectMappers.computeIfAbsent(agoJsonConfig,c ->{
            return defaultObjectMapper.copyWith(new AgoJsonParserFactory(agoJsonConfig));
        });
    }

    public String jsonStringify(Instance<?> instance, AgoJsonConfig agoJsonConfig) throws JsonProcessingException {
        return getObjectMapper(agoJsonConfig).writeValueAsString(instance);
    }

    public Instance<?> jsonDeserialize(Reader reader, boolean deserializeSlots) throws IOException {
        return jsonDeserialize(null,null,reader,deserializeSlots);
    }

    public Instance<?> jsonDeserialize(AgoClass agoClass, CallFrame<?> callFrame, Reader reader, boolean deserializeSlots) throws IOException {
        AgoJsonConfig agoJsonConfig = new AgoJsonConfig();
        agoJsonConfig.setWriteSlots(deserializeSlots);
        if(agoClass != null || callFrame != null){  // unable to cache
            return defaultObjectMapper.copyWith(new AgoJsonParserFactory(agoJsonConfig, agoClass, callFrame))
                    .readValue(reader, Instance.class);
        } else {
            return getObjectMapper(agoJsonConfig).readValue(reader, Instance.class);
        }
    }

    private void applyMetaClasses(List<MetaClassCreatingTask> metaClassCreationQueue) {
        for (AgoClass agoClass : this.classes) {
            agoClass.initSlots();
        }

        while(!metaClassCreationQueue.isEmpty()){
            var item = metaClassCreationQueue.removeFirst();
            if(LOGGER.isDebugEnabled()) LOGGER.debug("apply meta class %s".formatted(item));
            var constructor = item.constructor;
            CallFrame<?> frame = createFunctionInstance(item.target, constructor, null, null);
            Object[] arguments = item.arguments;
            for (int i = 0; i < arguments.length; i++) {
                Object argument = arguments[i];
                var p = constructor.getParameters()[i];
                switch (p.getTypeCode().value){
                    case INT_VALUE:       frame.getSlots().setInt(i, (Integer) argument); break;
                    case STRING_VALUE:    frame.getSlots().setString(i, (String) argument); break;
                    case LONG_VALUE:      frame.getSlots().setLong(i, (Long)argument); break;
                    case BOOLEAN_VALUE:   frame.getSlots().setBoolean(i, (Boolean)argument); break;
                    case DOUBLE_VALUE:    frame.getSlots().setDouble(i, (Double) argument); break;
                    case BYTE_VALUE:      frame.getSlots().setByte(i, (Byte) argument); break;
                    case FLOAT_VALUE:     frame.getSlots().setFloat(i, (Float) argument); break;
                    case CHAR_VALUE:      frame.getSlots().setChar(i, (Character)argument); break;
                    case SHORT_VALUE:     frame.getSlots().setShort(i, (Short) argument); break;
                    case CLASS_REF_VALUE: frame.getSlots().setClassRef(i, classByName.get(((ClassRefValue) argument).className()).getClassId()); break;
                    default:
                        if(p.getAgoClass() instanceof AgoEnum agoEnum){
                            var enumValue = agoEnum.findMember(argument);
                            assert enumValue != null;
                            frame.getSlots().setObject(i,enumValue);
                            break;
                        }
                        throw new RuntimeException("unexpected type for meta class constructor");
                }
            }
            this.runSpace.awaitTillComplete(frame);
        }
    }

    public void run(String functionName){
        var agoClass = classByName.get(functionName);
        if(agoClass instanceof AgoFunction fun) {
            var frame = createFunctionInstance(null, fun, null, null);
            this.runSpace.awaitTillComplete(frame);
        } else {
            throw new RuntimeException(functionName + " is not function");
        }
    }

    public void run(String className, String functionName){
        var agoClass = classByName.get(className + "." + functionName);
        if(agoClass instanceof AgoFunction fun) {
            var frame = createFunctionInstance(classByName.get(className), fun, null, null);
            runSpace.awaitTillComplete(frame);
        } else {
            throw new RuntimeException(functionName + " is not function");
        }
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator){
        if(LOGGER.isDebugEnabled()) LOGGER.debug("create instance of " + agoFunction);
        CallFrame<?> result;
        if (agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            result = new NativeFrame(this, agoNativeFunction.createSlots(), agoNativeFunction);
        } else {
            result = new AgoFrame(agoFunction.createSlots(), agoFunction, this);
        }
        if(parentScope != null) result.setParentScope(parentScope);
        return result;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        if (agoClass instanceof AgoFunction fun)
            return createFunctionInstance(parentScope, fun, creator, creator);

        var instance = new Instance<>(agoClass.createSlots(), agoClass);
        if(parentScope != null) instance.setParentScope(parentScope);
        return instance;
    }

    public Instance<?> createInstance(AgoClass agoClass, CallFrame<?> creator){
        return createInstance(null, agoClass, creator);
    }

    public AgoClass getClass(int classId) {
        return classes[classId];
    }

    public Instance<?> createInstance(Instance<?> parentScope, int classId, CallFrame<?> creator, RunSpace runSpace) {
        return createInstance(parentScope,classes[classId], creator);
    }

    public Instance<?> createNativeInstance(Instance<?> parentScope, int classId, CallFrame<?> creator) {
        return createNativeInstance(parentScope, classes[classId], creator);
    }

    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        if (agoClass instanceof AgoNativeFunction agoNativeFunction) {
            return createFunctionInstance(parentScope, agoNativeFunction, creator, creator);
        }
        var instance = new NativeInstance(agoClass.createSlots(), agoClass);
        if (parentScope != null) instance.setParentScope(parentScope);
        return instance;
    }

    public AgoClass createScopedClass(CallFrame<?> caller, int classId, Instance<?> parentScope) {
        var c = classes[classId].cloneWithScope(parentScope);
        if(parentScope == null) return c;

        AgoFunction emptyArgsConstructor = c.getAgoClass().getEmptyArgsConstructor();
        if(emptyArgsConstructor != null){
            c.invokeMethod(caller, emptyArgsConstructor,emptyArgsConstructor);
        }
        return c;
    }

    // create ScopedClassInterval instance from scopedClass
    public Instance<?> createScopedClassRef(CallFrame<?> caller, AgoClass parameterizedScopedClassInterval, AgoClass scopedClass) {
        var scopedClassInterval = createInstance(parameterizedScopedClassInterval,caller);
        Slots slots = scopedClassInterval.getSlots();
        slots.setClassRef(0,scopedClass.getClassId());   // value
        slots.setObject(1, scopedClass);            // ScopedClass
        slots.setObject(2,scopedClassInterval.getParentScope());    // scope
        return scopedClassInterval;
    }

    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, RunSpace runSpace){
        // after getClass(scopedClass.classId), the ScopedClass restore to the original class
        return createInstance(scopedClass.getParentScope(), getClass(scopedClass.classId), creator);
    }

    public RunSpace getRunSpace() {
        return runSpace;
    }

    public IntArrayInstance createIntArray(AgoClass arrayType, int length) {
        return new IntArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public ByteArrayInstance createByteArray(AgoClass arrayType, int length) {
        return new ByteArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public BooleanArrayInstance createBooleanArray(AgoClass arrayType, int length) {
        return new BooleanArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public CharArrayInstance createCharArray(AgoClass arrayType, int length) {
        return new CharArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public DoubleArrayInstance createDoubleArray(AgoClass arrayType, int length) {
        return new DoubleArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public FloatArrayInstance createFloatArray(AgoClass arrayType, int length) {
        return new FloatArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public LongArrayInstance createLongArray(AgoClass arrayType, int length) {
        return new LongArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public ObjectArrayInstance createObjectArray(AgoClass arrayType, int length) {
        return new ObjectArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public ShortArrayInstance createShortArray(AgoClass arrayType, int length) {
        return new ShortArrayInstance(arrayType.createSlots(), arrayType, length);
    }
    public StringArrayInstance createStringArray(AgoClass arrayType, int length) {
        return new StringArrayInstance(arrayType.createSlots(), arrayType, length);
    }

    public byte[] getBlob(int index) {
        return blobs[index];
    }

    public Boxer getBoxer() {
        return boxer;
    }

    public AgoClass getScopedClassIntervalClass() {
        return scopedClassIntervalClass;
    }

    public AgoClass getClass(String name){
        return this.classByName.get(name);
    }

    @Override
    public MetaClass getTheMeta() {
        return theMata;
    }

    public boolean validateClassInheritance(AgoFrame agoFrame, AgoClass sampleClass, AgoClass expectedClass) {
        if (!sampleClass.isThatOrDerivedFrom(expectedClass)) {
            agoFrame.raiseException("lang.ClassCastException", "illegal cast from '%s' to '%s'".formatted(expectedClass.getFullname(), sampleClass.getFullname()));
            return false;
        }
        return true;
    }

    public LangClasses getLangClasses() {
        return langClasses;
    }
}
