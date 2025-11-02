package org.siphonlab.ago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.classloader.ClassRefValue;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.json.AgoJsonParserFactory;
import org.siphonlab.ago.runtime.rdb.AgoJsonFactory;
import org.siphonlab.ago.runtime.*;
import org.siphonlab.ago.runtime.json.InstanceJsonDeserializer;
import org.siphonlab.ago.runtime.json.InstanceJsonSerializer;
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

    private final AgoRunSpace runSpace;
    private ObjectMapper jsonObjectMapper;
    private BoxTypes boxTypes;
    private ObjectMapper jsonSlotsMapper;

    private AgoClass runSpaceClass;

    public String toString(int i){
        return strings[i];
    }

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
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(0, Executors.newCachedThreadPool());
        this(new NettyEventLoopRunSpaceHost(eventLoopGroup));
    }

    public AgoEngine(RunSpaceHost runSpaceHost){
        this.runSpaceHost = runSpaceHost;
        runSpace = createRunSpace(this.runSpaceHost);
    }

    protected AgoRunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return new AgoRunSpace(this, runSpaceHost);
    }

    public void load(AgoClassLoader classLoader){
        this.classes = classLoader.getClasses().toArray(new AgoClass[0]);
        this.strings = classLoader.getStrings().toArray(new String[0]);
        this.classByName = classLoader.getClassByName();
        this.blobs = classLoader.getBlobs().toArray(new byte[0][]);
        this.boxer = classLoader.createBoxer(this);
        this.scopedClassIntervalClass = this.classByName.get("lang.ScopedClassInterval");
        this.PRIMITIVE_TYPE = this.classByName.get("lang.Primitive");
        this.PRIMITIVE_NUMBER_TYPE = this.classByName.get("lang.PrimitiveNumber");

        this.boxTypes = classLoader.getBoxTypes();
        this.runSpaceClass = classLoader.getRunSpaceClass();

        // applyMetaClasses(classLoader.getMetaClassCreationQueue()); // TODO applyMetaClasses will change the slots info, however, we need jsonObjectMapper for dump slots
        this.jsonObjectMapper = createJsonObjectMapper(classLoader);
        this.jsonSlotsMapper = createJsonSlotsMapper(classLoader);

        applyMetaClasses(classLoader.getMetaClassCreationQueue());
    }

    public AgoClass getRunSpaceClass() {
        return runSpaceClass;
    }

    public BoxTypes getBoxTypes() {
        return boxTypes;
    }

    protected ObjectMapper createJsonObjectMapper(AgoClassLoader classLoader) {
        return createJsonObjectMapper(classLoader, false,true, false, false);
    }

    protected ObjectMapper createJsonSlotsMapper(AgoClassLoader classLoader) {
        return createJsonObjectMapper(classLoader, false, true, false, true);
    }

    protected ObjectMapper createJsonObjectMapper(AgoClassLoader classLoader, boolean writeType, boolean writeId, boolean serializeObjectAsReference, boolean serializeSlots) {
        var r = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        BoxTypes boxTypes = classLoader.getBoxTypes();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializer(this, boxTypes, false, serializeSlots);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addDeserializer(Instance.class, new InstanceJsonDeserializer(this, boxTypes, serializeObjectAsReference, serializeSlots));

        r.registerModule(module);

        return r.copyWith(new AgoJsonFactory(writeType, writeId, serializeObjectAsReference));
    }

    public String jsonStringify(Instance<?> instance, boolean serializeSlots) throws JsonProcessingException {
        if(serializeSlots){
            return jsonSlotsMapper.writeValueAsString(instance);
        } else {
            return getJsonObjectMapper().writeValueAsString(instance);
        }
    }

    public Instance<?> jsonDeserialize(AgoClass agoClass, CallFrame<?> callFrame, Reader reader, boolean serializeSlots) throws IOException {
        ObjectMapper mapper = serializeSlots ? jsonSlotsMapper : getJsonObjectMapper();
        return mapper.copyWith(new AgoJsonParserFactory(agoClass, callFrame)).readValue(reader, Instance.class);
    }

    public ObjectMapper getJsonObjectMapper() {
        return jsonObjectMapper;
    }

    private void applyMetaClasses(List<MetaClassCreatingTask> metaClassCreationQueue) {
        for (AgoClass agoClass : this.classes) {
            agoClass.initSlots();
        }

        while(!metaClassCreationQueue.isEmpty()){
            var item = metaClassCreationQueue.removeFirst();
            if(LOGGER.isDebugEnabled()) LOGGER.debug("apply meta class %s".formatted(item));
            var constructor = item.constructor;
            AgoFrame frame = (AgoFrame) createFunctionInstance(item.target, constructor, null, null, runSpace);
            Object[] arguments = item.arguments;
            for (int i = 0; i < arguments.length; i++) {
                Object argument = arguments[i];
                var p = constructor.getParameters()[i];
                switch (p.getTypeCode().value){
                    case INT_VALUE:       frame.slots.setInt(i, (Integer) argument); break;
                    case STRING_VALUE:    frame.slots.setString(i, (String) argument); break;
                    case LONG_VALUE:      frame.slots.setLong(i, (Long)argument); break;
                    case BOOLEAN_VALUE:   frame.slots.setBoolean(i, (Boolean)argument); break;
                    case DOUBLE_VALUE:    frame.slots.setDouble(i, (Double) argument); break;
                    case BYTE_VALUE:      frame.slots.setByte(i, (Byte) argument); break;
                    case FLOAT_VALUE:     frame.slots.setFloat(i, (Float) argument); break;
                    case CHAR_VALUE:      frame.slots.setChar(i, (Character)argument); break;
                    case SHORT_VALUE:     frame.slots.setShort(i, (Short) argument); break;
                    case CLASS_REF_VALUE: frame.slots.setClassRef(i, classByName.get(((ClassRefValue) argument).className()).getClassId()); break;
                    default:
                        if(p.getAgoClass() instanceof AgoEnum agoEnum){
                            var enumValue = agoEnum.findMember(argument);
                            assert enumValue != null;
                            frame.slots.setObject(i,enumValue);
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
            var frame = createFunctionInstance(null, fun, null, null , runSpace);
            this.runSpace.awaitTillComplete(frame);
        } else {
            throw new RuntimeException(functionName + " is not function");
        }
    }

    public void run(String className, String functionName){
        var agoClass = classByName.get(className + "." + functionName);
        if(agoClass instanceof AgoFunction fun) {
            var frame = createFunctionInstance(classByName.get(className), fun, null, null , runSpace);
            runSpace.awaitTillComplete(frame);
        } else {
            throw new RuntimeException(functionName + " is not function");
        }
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator, AgoRunSpace runSpace){
        if(LOGGER.isDebugEnabled()) LOGGER.debug("create instance of " + agoFunction);
        CallFrame<?> result;
        if (!(agoFunction instanceof AgoNativeFunction agoNativeFunction)) {
            result = new AgoFrame(agoFunction.createSlots(), agoFunction, this);
        } else {
//            result = new NativeFrame(this, runSpace, agoFunction.createSlots(), agoNativeFunction);
            result = agoFunction.createCallFrame(this, runSpace);
        }
        if(parentScope != null) result.setParentScope(parentScope);
        result.setCaller(caller);
        result.setCreator(creator);
        return result;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        if (agoClass instanceof AgoFunction fun)
            return createFunctionInstance(parentScope, fun, creator, creator, runSpace);

        var instance = new Instance<>(agoClass.createSlots(), agoClass);
        if(parentScope != null) instance.setParentScope(parentScope);
        instance.setCreator(creator);
        return instance;
    }

    public Instance<?> createInstance(AgoClass agoClass, CallFrame<?> creator){
        return createInstance(null, agoClass, creator, creator.getRunSpace());
    }

    public AgoClass getClass(int classId) {
        return classes[classId];
    }

    public Instance<?> createInstance(Instance<?> parentScope, int classId, CallFrame<?> creator, AgoRunSpace runSpace) {
        return createInstance(parentScope,classes[classId], creator , runSpace);
    }

    public Instance<?> createNativeInstance(Instance<?> parentScope, int classId, CallFrame<?> creator) {
        return createNativeInstance(parentScope, classes[classId], creator);
    }

    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        if (agoClass instanceof AgoNativeFunction agoNativeFunction) {
            return createFunctionInstance(parentScope, agoNativeFunction, creator, creator, creator.getRunSpace());
        }
        var instance = new NativeInstance(agoClass.createSlots(), agoClass);
        if (parentScope != null) instance.setParentScope(parentScope);
        instance.setCreator(creator);
        return instance;
    }

    public Instance<?> createScopedClass(CallFrame<?> caller, int classId, Instance<?> parentScope) {
        var c = classes[classId].withScope(parentScope);
        c.setCreator(caller);

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

    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace){
        // after getClass(scopedClass.classId), the ScopedClass restore to the original class
        return createInstance(scopedClass.getParentScope(), getClass(scopedClass.classId), creator, runSpace);
    }

    public AgoRunSpace getRunSpace() {
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

    public boolean validateClassInheritance(AgoFrame agoFrame, AgoClass sampleClass, AgoClass expectedClass) {
        if (!sampleClass.isThatOrDerivedFrom(expectedClass)) {
            agoFrame.raiseException("lang.ClassCastException", "illegal cast from '%s' to '%s'".formatted(expectedClass.getFullname(), sampleClass.getFullname()));
            return false;
        }
        return true;
    }



}
