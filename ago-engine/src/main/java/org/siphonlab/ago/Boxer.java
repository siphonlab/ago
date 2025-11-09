package org.siphonlab.ago;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.NULL_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class Boxer {
    private final BoxTypes boxTypes;

    private final AgoClass INTEGER;
    private final AgoClass LONG;
    private final AgoClass BYTE;
    private final AgoClass CHAR;
    private final AgoClass SHORT;
    private final AgoClass CLASS_REF;
    private final AgoClass STRING;
    private final AgoClass BOOLEAN;
    private final AgoClass FLOAT;
    private final AgoClass DOUBLE;

    private AgoEngine engine;

    public Boxer(BoxTypes boxTypes, AgoClass INTEGER, AgoClass LONG, AgoClass BYTE, AgoClass CHAR, AgoClass SHORT,
                 AgoClass CLASS_REF, AgoClass STRING, AgoClass BOOLEAN, AgoClass FLOAT, AgoClass DOUBLE) {
        this.boxTypes = boxTypes;
        this.INTEGER = INTEGER;
        this.LONG = LONG;
        this.BYTE = BYTE;
        this.CHAR = CHAR;
        this.SHORT = SHORT;
        this.CLASS_REF = CLASS_REF;
        this.STRING = STRING;
        this.BOOLEAN = BOOLEAN;
        this.FLOAT = FLOAT;
        this.DOUBLE = DOUBLE;
    }

    public boolean isNarrowBoxType(AgoClass agoClass){
        return agoClass == INTEGER ||
                agoClass == LONG ||
                agoClass == BYTE ||
                agoClass == CHAR ||
                agoClass == SHORT ||
                agoClass == CLASS_REF ||
                agoClass == STRING ||
                agoClass == BOOLEAN ||
                agoClass == FLOAT ||
                agoClass == DOUBLE;
    }

    public void setEngine(AgoEngine engine) {
        this.engine = engine;
    }

    public Instance<?> boxInt(int i) {
        var instance = new Instance<>(INTEGER.createSlots(), INTEGER);
        instance.slots.setInt(0, i);
        return instance;
    }

    public Instance<?> boxLong(long l) {
        var instance = new Instance<>(LONG.createSlots(), LONG);
        instance.slots.setLong(0, l);
        return instance;
    }

    public Instance<?> boxByte(byte b) {
        var instance = new Instance<>(BYTE.createSlots(), BYTE);
        instance.slots.setByte(0, b);
        return instance;
    }

    public Instance<?> boxChar(char c) {
        var instance = new Instance<>(CHAR.createSlots(), CHAR);
        instance.slots.setChar(0, c);
        return instance;
    }

    public Instance<?> boxShort(short i) {
        var instance = new Instance<>(SHORT.createSlots(), SHORT);
        instance.slots.setShort(0, i);
        return instance;
    }

    public Instance<?> boxClassRef(int classRef) {
        var instance = new Instance<>(CLASS_REF.createSlots(), CLASS_REF);
        instance.slots.setClassRef(0, classRef);
        instance.slots.setObject(1, engine.getClass(classRef));
        return instance;
    }

    public Instance<?> boxString(String s) {
        var instance = new Instance<>(STRING.createSlots(), STRING);
        instance.slots.setString(0, s);
        return instance;
    }

    public Instance<?> boxBoolean(boolean b) {
        var instance = new Instance<>(BOOLEAN.createSlots(), BOOLEAN);
        instance.slots.setBoolean(0, b);
        return instance;
    }

    public Instance<?> boxFloat(float f) {
        var instance = new Instance<>(FLOAT.createSlots(), FLOAT);
        instance.slots.setFloat(0, f);
        return instance;
    }

    public Instance<?> boxDouble(double d) {
        var instance = new Instance<>(DOUBLE.createSlots(), DOUBLE);
        instance.slots.setDouble(0, d);
        return instance;
    }

    public Instance<?> boxClassRef(int classRef, AgoClass classRefClass) {
        var instance = new Instance<>(classRefClass.createSlots(), classRefClass);
        instance.slots.setClassRef(0, classRef);
        instance.slots.setObject(1, engine.getClass(classRef));
        return instance;
    }

    public Instance<?> boxAny(Class<?> clazz, Slots slots, int slotIndex) {
        if(clazz == int.class){
            return boxInt(slots.getInt(slotIndex));
        }
        if (clazz == double.class) {
            return boxDouble(slots.getDouble(slotIndex));
        }
        if (clazz == String.class) {
            return boxString(slots.getString(slotIndex));
        }
        if (clazz == long.class) {
            return boxLong(slots.getLong(slotIndex));
        }
        if (clazz == boolean.class) {
            return boxBoolean(slots.getBoolean(slotIndex));
        }
        if (clazz == short.class) {
            return boxShort(slots.getShort(slotIndex));
        }
        if (clazz == byte.class) {
            return boxByte(slots.getByte(slotIndex));
        }
        if (clazz == float.class) {
            return boxFloat(slots.getFloat(slotIndex));
        }
        if (clazz == char.class) {
            return boxChar(slots.getChar(slotIndex));
        }
        if(AgoClass.class.isAssignableFrom(clazz)){
            return engine.getClass(slots.getClassRef(slotIndex));
        }
        if(Instance.class.isAssignableFrom(clazz)){
            return (Instance<?>) slots.getObject(slotIndex);
        }
        throw new UnsupportedOperationException("'%s' is not primitive type".formatted(clazz));
    }

    private boolean validateBoxType(AgoFrame agoFrame, AgoClass agoClass, int typeCode){
        if(agoClass instanceof AgoEnum agoEnum){
            if(agoEnum.getBasePrimitiveType().value != typeCode){
                agoFrame.raiseException("lang.ClassCastException", "illegal cast from '%s' to '%s'".formatted(of(typeCode), agoClass.getFullname()));
                return false;
            }
            return true;
        }
        return switch (typeCode) {
            case INT_VALUE -> agoClass.isThatOrDerivedFrom(this.INTEGER);
            case STRING_VALUE -> agoClass.isThatOrDerivedFrom(this.STRING);
            case LONG_VALUE -> agoClass.isThatOrDerivedFrom(this.LONG);
            case BOOLEAN_VALUE -> agoClass.isThatOrDerivedFrom(this.BOOLEAN);
            case DOUBLE_VALUE -> agoClass.isThatOrDerivedFrom(this.DOUBLE);
            case BYTE_VALUE -> agoClass.isThatOrDerivedFrom(this.BYTE);
            case FLOAT_VALUE -> agoClass.isThatOrDerivedFrom(this.FLOAT);
            case CHAR_VALUE -> agoClass.isThatOrDerivedFrom(this.CHAR);
            case SHORT_VALUE -> agoClass.isThatOrDerivedFrom(this.SHORT);
            case CLASS_REF_VALUE -> agoClass.isThatOrDerivedFrom(this.CLASS_REF);
            default -> false;
        };
    }

    public boolean forceUnbox(AgoFrame agoFrame, int receiverIndex, Instance<?> object, int typeCode) {
        Slots slots = agoFrame.getSlots();
        if(object == null) {
            agoFrame.raiseException("NullPointerException","unbox to '%s' meet null".formatted(TypeCode.of(typeCode)));
            return false;
        }
        if(!validateBoxType(agoFrame, object.getAgoClass(), typeCode)){
            return false;
        }
        switch (typeCode){
            case INT_VALUE:
                slots.setInt(receiverIndex,object.slots.getInt(0));
                break;
            case BOOLEAN_VALUE:
                slots.setBoolean(receiverIndex, object.slots.getBoolean(0));
                break;
            case CHAR_VALUE:
                slots.setChar(receiverIndex, object.slots.getChar(0));
                break;
            case FLOAT_VALUE:
                slots.setFloat(receiverIndex, object.slots.getFloat(0));
                break;
            case DOUBLE_VALUE:
                slots.setDouble(receiverIndex, object.slots.getDouble(0));
                break;
            case BYTE_VALUE:
                slots.setByte(receiverIndex, object.slots.getByte(0));
                break;
            case SHORT_VALUE:
                slots.setShort(receiverIndex, object.slots.getShort(0));
                break;
            case LONG_VALUE:
                slots.setLong(receiverIndex, object.slots.getLong(0));
                break;
            case OBJECT_VALUE:
                slots.setObject(receiverIndex, object.slots.getObject(0));
                break;
            case NULL_VALUE:
                slots.setObject(receiverIndex, null);
            case STRING_VALUE:
                slots.setString(receiverIndex, object.slots.getString(0));
                break;
            case CLASS_REF_VALUE:
                slots.setInt(receiverIndex, object.slots.getInt(0));
        }
        return true;
    }

    public Instance<?> boxAny(Slots slots, int slotIndex, int typeCode) {
        switch (typeCode) {
            case INT_VALUE:
                return boxInt(slots.getInt(slotIndex));
            case STRING_VALUE:
                return boxString(slots.getString(slotIndex));
            case LONG_VALUE:
                return boxLong(slots.getLong(slotIndex));
            case BOOLEAN_VALUE:
                return boxBoolean(slots.getBoolean(slotIndex));
            case DOUBLE_VALUE:
                return boxDouble(slots.getDouble(slotIndex));
            case BYTE_VALUE:
                return boxByte(slots.getByte(slotIndex));
            case FLOAT_VALUE:
                return boxFloat(slots.getFloat(slotIndex));
            case CHAR_VALUE:
                return boxChar(slots.getChar(slotIndex));
            case SHORT_VALUE:
                return boxShort(slots.getShort(slotIndex));
            case CLASS_REF_VALUE:
                return boxClassRef(slots.getClassRef(slotIndex));
        }
        return null;
    }

    public Instance<?> boxInt(CallFrame<?> callFrame, AgoClass agoClass, int value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setInt(0, value);        // TODO invoke constructor?
        return instance;
    }

    public Instance<?> boxLong(CallFrame<?> callFrame, AgoClass agoClass, long value) {
        Instance<?> instance = engine.createInstance(agoClass, callFrame);
        instance.slots.setLong(0, value);
        return instance;
    }

    public Instance<?> boxDouble(CallFrame<?> callFrame, AgoClass agoClass, double value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setDouble(0, value);
        return instance;
    }

    public Instance<?> boxBoolean(CallFrame<?> callFrame, AgoClass agoClass, boolean value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setBoolean(0, value);
        return instance;
    }

    public Instance<?> boxString(CallFrame<?> callFrame, AgoClass agoClass, String value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setString(0, value);
        return instance;
    }

    public Instance<?> boxChar(CallFrame<?> callFrame, AgoClass agoClass, char value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setChar(0, value);
        return instance;
    }

    public Instance<?> boxShort(CallFrame<?> callFrame, AgoClass agoClass, short value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setShort(0, value);
        return instance;
    }

    public Instance<?> boxByte(CallFrame<?> callFrame, AgoClass agoClass, byte value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setByte(0, value);
        return instance;
    }

    public Instance<?> boxFloat(CallFrame<?> callFrame, AgoClass agoClass, float value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setFloat(0, value);
        return instance;
    }

    public Instance<?> boxClassRef(CallFrame<?> callFrame, AgoClass agoClass, AgoClass value) {
        Instance<?> instance = engine.createInstance(agoClass,callFrame);
        instance.slots.setClassRef(0, value.classId);
        return instance;
    }

    public Object unbox(Instance<?> instance){
        return boxTypes.unbox(engine,instance);
    }
}
