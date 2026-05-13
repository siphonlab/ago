package org.siphonlab.ago;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.classloader.ClassRefValue;

import java.math.BigDecimal;

import static org.siphonlab.ago.AgoFrame.stringToChar;
import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.DECIMAL_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class Conversion {
    public static Instance<?> boxEnum(Slots slots, AgoEnum agoEnum, int srcIndex, int srcTypeCode) {
        switch (srcTypeCode) {
            case INT_VALUE:
                return agoEnum.findMember(slots.getInt(srcIndex));      // call valueOf# is fine too
            case BYTE_VALUE:
                return agoEnum.findMember(slots.getByte(srcIndex));
            case SHORT_VALUE:
                return agoEnum.findMember(slots.getShort(srcIndex));
            case LONG_VALUE:
                return agoEnum.findMember(slots.getLong(srcIndex));
            case FLOAT_VALUE:
                return agoEnum.findMember(slots.getFloat(srcIndex));
            case DOUBLE_VALUE:
                return agoEnum.findMember(slots.getDouble(srcIndex));
            case DECIMAL_VALUE:
                return agoEnum.findMember(slots.getDecimal(srcIndex));
            case BOOLEAN_VALUE:
                return agoEnum.findMember(slots.getBoolean(srcIndex) ? 1 : 0);
            case CHAR_VALUE:
                return agoEnum.findMember((int) slots.getChar(srcIndex));
            case STRING_VALUE:
                return agoEnum.findMember(slots.getString(srcIndex));
        }
        return null;
    }


    // support Primitive(-Null) -> Primitive(-Null), Primitive(-Null) -> Boxer Object, Primitive -> Union
    // Boxer -> Primitive(-Null), Object -> Object(assignable)
    // Boxer -> Union, Object -> Union
    public static boolean castToAny(CallFrame<?> self, AgoFrame agoFrame, Slots slots, int targetIndex, int targetTypeCode, AgoClass targetClass,
                              int srcSlotIndex, int srcTypeCode, AgoClass srcClass) {
        if(targetTypeCode == UNION_VALUE){
            return castToUnion(self, agoFrame, slots, targetIndex, targetClass, srcSlotIndex, srcTypeCode, srcClass);
        } else if(srcTypeCode == UNION_VALUE){
            return castFromUnion(self, agoFrame, slots, targetIndex, targetTypeCode, targetClass, srcSlotIndex, srcClass);
        }
        if(isPrimitiveExcludeNull(srcTypeCode)) {   // primitive src
            if (isPrimitiveExcludeNull(targetTypeCode)) {
                // primitive to primitive;
                castPrimitiveToPrimitive(slots, targetIndex, targetTypeCode, srcSlotIndex, srcTypeCode);
            } else {
                // primitive to object
                if (targetClass instanceof AgoEnum agoEnum) {
                    var instance = boxEnum(slots, agoEnum, srcSlotIndex, srcTypeCode);
                    if (instance == null) {
                        agoFrame.raiseException(self, "lang.ClassCastException", "'%s' can't cast to '%s'".formatted(of(srcTypeCode), agoEnum.getFullname()));
                        return false;
                    }
                    slots.setObject(targetIndex, instance);
                } else {
                    var instance = agoFrame.getAgoEngine().getBoxer().boxAny(slots, srcSlotIndex, srcTypeCode);
                    if (!agoFrame.validateClassInheritance(instance.getAgoClass(), targetClass)) {
                        return false;
                    }
                    slots.setObject(targetIndex, instance);
                }
            }
        } else if(srcTypeCode == NULL_VALUE){
            if(targetTypeCode == NULL_VALUE){
                slots.setVoid(targetIndex, null);
            } else {
                agoFrame.raiseException(self, "lang.ClassCastException", "can't cast null to '%s'".formatted(of(targetTypeCode)));
                return false;
            }
        } else {
            if(isPrimitiveExcludeNull(targetTypeCode)){       //TODO for union, take primitive value
                return agoFrame.getAgoEngine().getBoxer().forceUnbox(agoFrame, agoFrame.getSlots(), self, targetIndex,slots.getObject(srcSlotIndex),targetTypeCode);
            } else {
                return castObject(agoFrame, self, slots, targetIndex,slots.getObject(srcSlotIndex),targetTypeCode,targetClass);
            }
        }
        return true;
    }

    private static boolean castFromUnion(CallFrame<?> self, AgoFrame agoFrame, Slots slots, int targetIndex, int targetTypeCode, AgoClass targetClass, int srcSlotIndex, AgoClass srcClass) {
        var unionValue = slots.getUnion(srcSlotIndex);
        var srcTypeOfUnionValue = Union.extractUnionType(unionValue).value;
        var boxer = agoFrame.getAgoEngine().getBoxer();
        if(isPrimitiveExcludeNull(targetTypeCode)){
            if(isPrimitiveExcludeNull(srcTypeOfUnionValue)){
                castPrimitiveToPrimitive(slots, targetIndex, targetTypeCode, unionValue, agoFrame);
            } else if(srcTypeOfUnionValue == NULL_VALUE || srcTypeOfUnionValue == VOID_VALUE){
                agoFrame.raiseException(self, "lang.ClassCastException", "can't cast null to '%s'".formatted(of(targetTypeCode)));
                return false;
            } else {
                var unboxed = boxer.unbox((Instance<?>) unionValue);
                if(unboxed != unionValue && unboxed != null){
                    castPrimitiveToPrimitive(slots, targetIndex, targetTypeCode, unboxed, agoFrame);
                } else {
                    agoFrame.raiseException(self, "lang.ClassCastException", "'%s' can't cast to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                    return false;
                }
            }
        } else if(targetTypeCode == NULL_VALUE || targetTypeCode == VOID_VALUE){
            if(srcTypeOfUnionValue == NULL_VALUE || srcTypeOfUnionValue == VOID_VALUE){
                // skip
            } else {
                agoFrame.raiseException(self, "lang.ClassCastException", "'%s' can't cast to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                return false;
            }
        } else {
            assert targetTypeCode == OBJECT_VALUE;
            if(isPrimitiveExcludeNull(srcTypeOfUnionValue)){
                var instance = boxer.unionToObject(unionValue);
                castObject(agoFrame, self, slots, targetIndex, instance, srcTypeOfUnionValue, targetClass);
            } else if(srcTypeOfUnionValue == NULL_VALUE || srcTypeOfUnionValue == VOID_VALUE){
                agoFrame.raiseException(self, "lang.ClassCastException", "can't cast null to '%s'".formatted(of(targetTypeCode)));
                return false;
            } else {
                castObject(agoFrame, self, slots, targetIndex, (Instance<?>) unionValue, srcTypeOfUnionValue, targetClass);
            }
        }
        return true;
    }

    private static boolean castPrimitiveToPrimitive(Slots slots, int targetIndex, int targetTypeCode, Object srcUnionValue, AgoFrame agoFrame) {
        switch (targetTypeCode){
            case INT_VALUE:     slots.setInt(targetIndex, Union.unionToInt(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case STRING_VALUE:  slots.setString(targetIndex, Union.unionToString(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case LONG_VALUE:    slots.setLong(targetIndex, Union.unionToLong(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case BOOLEAN_VALUE: slots.setBoolean(targetIndex, Union.unionToBoolean(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case DOUBLE_VALUE:  slots.setDouble(targetIndex, Union.unionToDouble(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case DECIMAL_VALUE: slots.setDecimal(targetIndex, Union.unionToDecimal(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case BYTE_VALUE:    slots.setByte(targetIndex, Union.unionToByte(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case FLOAT_VALUE:   slots.setFloat(targetIndex, Union.unionToFloat(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case CHAR_VALUE:    slots.setChar(targetIndex, Union.unionToChar(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case SHORT_VALUE:   slots.setShort(targetIndex, Union.unionToShort(srcUnionValue, agoFrame.getAgoEngine()));
                break;
            case CLASS_REF_VALUE:   slots.setClassRef(targetIndex, Union.unionToClassRef(srcUnionValue, agoFrame.getAgoEngine()));
                break;

            case OBJECT_VALUE:  {
                var instance = (Instance<?>)srcUnionValue;
                var unboxed = agoFrame.getAgoEngine().getBoxer().unbox(instance);
                if(instance == unboxed || unboxed == null){
                    return false;
                } else {
                    return castPrimitiveToPrimitive(slots, targetIndex, targetTypeCode, unboxed, agoFrame);
                }
            }
        }
        return true;
    }

    private static boolean castToUnion(CallFrame<?> self, AgoFrame agoFrame, Slots slots, int targetIndex, AgoClass targetClass, int srcSlotIndex, int srcTypeCode, AgoClass srcClass) {
        if(targetClass.type == AgoClass.TYPE_ANY_CLASS){
            slots.setUnion(targetIndex, Union.toUnionValue(agoFrame.getAgoEngine(), slots, srcSlotIndex, srcTypeCode));
            return true;
        }
        var toBaseClassOfUnion = ((NullableTypeInfo) targetClass.getConcreteTypeInfo()).getBaseClass();
        Boxer boxer = agoFrame.getAgoEngine().getBoxer();
        int unionBaseTypeCode = toBaseClassOfUnion.getTypeCode().value;
        if(isPrimitiveExcludeNull(unionBaseTypeCode)){
            if(isPrimitiveExcludeNull(srcTypeCode)){
                Object value = castPrimitiveToPrimitiveUnion(slots, unionBaseTypeCode, srcSlotIndex, srcTypeCode, agoFrame.getAgoEngine());
                if(value == null){
                    agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                    return false;
                }
                slots.setUnion(targetIndex, value);
            } else if(srcTypeCode == NULL_VALUE || srcTypeCode == VOID_VALUE){
                slots.setUnion(targetIndex, null);
            } else if(srcTypeCode == UNION_VALUE){
                var srcBaseClass = ((NullableTypeInfo)srcClass.getConcreteTypeInfo()).getBaseClass();
                final Object srcUnionValue = slots.getUnion(srcSlotIndex);
                if(srcUnionValue == null){
                    slots.setUnion(targetIndex, null);
                    return true;
                }
                Object value = switch(srcBaseClass.getTypeCode().value){
                        case INT_VALUE ->  Union.unionToInt(srcUnionValue, agoFrame.getAgoEngine());
                        case STRING_VALUE ->  Union.unionToString(srcUnionValue, agoFrame.getAgoEngine());
                        case LONG_VALUE ->  Union.unionToLong(srcUnionValue, agoFrame.getAgoEngine());
                        case BOOLEAN_VALUE ->  Union.unionToBoolean(srcUnionValue, agoFrame.getAgoEngine()) ? 1 : 0;
                        case DOUBLE_VALUE ->  Union.unionToDouble(srcUnionValue, agoFrame.getAgoEngine());
                        case DECIMAL_VALUE ->  Union.unionToDecimal(srcUnionValue, agoFrame.getAgoEngine()).intValue();
                        case BYTE_VALUE ->  Union.unionToByte(srcUnionValue, agoFrame.getAgoEngine());
                        case FLOAT_VALUE ->  Union.unionToFloat(srcUnionValue, agoFrame.getAgoEngine());
                        case CHAR_VALUE ->  Union.unionToChar(srcUnionValue, agoFrame.getAgoEngine());
                        case SHORT_VALUE ->  Union.unionToShort(srcUnionValue, agoFrame.getAgoEngine());
                        case CLASS_REF_VALUE -> Union.unionToClassRef(srcUnionValue, agoFrame.getAgoEngine());
                        case OBJECT_VALUE ->  {
                            var instance = (Instance<?>)srcUnionValue;
                            var unboxed = boxer.unbox(instance);
                            if(instance == unboxed || unboxed == null){
                                yield null;
                            } else {
                                yield unboxed;
                            }
                        }
                        default -> null;
                    };
                if(value == null){
                    agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                    return false;
                }
                slots.setUnion(targetIndex, value);
            } else if(srcTypeCode == OBJECT_VALUE){
                Instance<?> instance = slots.getObject(srcSlotIndex);
                if(instance == null){
                    agoFrame.raiseException(self, "lang.IllegalStateException", "object not initialized");
                    return false;
                }
                var unboxed = boxer.unbox(instance);
                if(instance == unboxed || unboxed == null){
                    agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                    return false;
                }
                slots.setUnion(targetIndex, unboxed);
            }
        } else if(unionBaseTypeCode == NULL_VALUE || unionBaseTypeCode == VOID_VALUE){
            if(srcTypeCode == NULL_VALUE || srcTypeCode == VOID_VALUE){
                slots.setUnion(targetIndex, null);
                return true;
            } else if(srcTypeCode == UNION_VALUE){
                var union = slots.getUnion(srcSlotIndex);
                if(union == null){
                    slots.setUnion(targetIndex, null);
                    return true;
                }
            }
            agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
            return false;
        } else {
            assert unionBaseTypeCode == OBJECT_VALUE;
            if(srcTypeCode == NULL_VALUE || srcTypeCode == VOID_VALUE) {
                slots.setUnion(targetIndex, null);
                return true;
            } else if(TypeCode.isPrimitiveExcludeNull(srcTypeCode)) {
                if (!boxer.getBoxTypes().isBoxType(toBaseClassOfUnion)) {
                    agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                    return false;
                }
                var value = boxer.boxAny(slots, srcSlotIndex, srcTypeCode, toBaseClassOfUnion, self);
                slots.setUnion(targetIndex, value);
            } else if(srcTypeCode == OBJECT_VALUE){
                if(!agoFrame.validateClassInheritance(srcClass, toBaseClassOfUnion)) return false;
                slots.setUnion(targetIndex, slots.getObject(srcSlotIndex));
            } else if(srcTypeCode == UNION_VALUE){
                var srcUnionValue = slots.getUnion(srcSlotIndex);
                if(srcUnionValue == null){
                    slots.setUnion(targetIndex, null);
                    return true;
                }
                var srcBaseClass = ((NullableTypeInfo)srcClass.getConcreteTypeInfo()).getBaseClass();
                var typeCodeOfSrcBase = srcBaseClass.getTypeCode().value;
                if(typeCodeOfSrcBase == NULL_VALUE || typeCodeOfSrcBase == VOID_VALUE) {
                    slots.setUnion(targetIndex, null);
                    return true;
                } else if(TypeCode.isPrimitiveExcludeNull(typeCodeOfSrcBase)) {
                    if (!boxer.getBoxTypes().isBoxType(toBaseClassOfUnion)) {
                        agoFrame.raiseException(self, "lang.ClassCastException", "can't cast '%s' to '%s'".formatted(srcClass.getFullname(), targetClass.getFullname()));
                        return false;
                    }
                    if(srcUnionValue instanceof ClassRefValue classRefValue){
                        srcUnionValue = boxer.boxClassRef(classRefValue);
                    }
                    var value = boxer.boxAny(((Instance<?>)srcUnionValue).getSlots(), 0, typeCodeOfSrcBase, toBaseClassOfUnion, self);
                    slots.setUnion(targetIndex, value);
                } else if(typeCodeOfSrcBase == OBJECT_VALUE) {
                    if (!agoFrame.validateClassInheritance(srcClass, toBaseClassOfUnion)) return false;
                    slots.setUnion(targetIndex, (Instance<?>)srcUnionValue);
                }
            }
        }
        return true;
    }

    private static void castPrimitiveToPrimitive(Slots slots, int targetIndex, int targetTypeCode, int srcIndex, int srcTypeCode) {
        switch (targetTypeCode){
            case INT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setInt(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setInt(targetIndex, Integer.parseInt(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setInt(targetIndex, (int)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setInt(targetIndex, slots.getBoolean(srcIndex)? 1 : 0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setInt(targetIndex, (int)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setInt(targetIndex, slots.getDecimal(srcIndex).intValue());
                        return;
                    case BYTE_VALUE:
                        slots.setInt(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setInt(targetIndex, (int)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setInt(targetIndex, (int)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE: // Convert short to int
                        slots.setInt(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case STRING_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getInt(srcIndex)));
                        return;
                    case STRING_VALUE:
                        slots.setString(targetIndex, slots.getString(srcIndex));
                        return;
                    case LONG_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getLong(srcIndex)));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getBoolean(srcIndex)));
                        return;
                    case DOUBLE_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getDouble(srcIndex)));
                        return;
                    case DECIMAL_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getDecimal(srcIndex)));
                        return;
                    case BYTE_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getByte(srcIndex)));
                        return;
                    case FLOAT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getFloat(srcIndex)));
                        return;
                    case CHAR_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getChar(srcIndex)));
                        return;
                    case SHORT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getShort(srcIndex)));
                        return;
                }
                break;
            case LONG_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setLong(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setLong(targetIndex, Long.parseLong(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setLong(targetIndex, slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setLong(targetIndex, slots.getBoolean(srcIndex)? 1L : 0L);
                        return;
                    case DOUBLE_VALUE:
                        slots.setLong(targetIndex, (long)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setLong(targetIndex, slots.getDecimal(srcIndex).longValue());
                        return;
                    case BYTE_VALUE:
                        slots.setLong(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setLong(targetIndex, (long)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setLong(targetIndex, (long)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setLong(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case BOOLEAN_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setBoolean(targetIndex, slots.getInt(srcIndex) != 0);
                        return;
                    case STRING_VALUE:
                        slots.setBoolean(targetIndex, StringUtils.isNotEmpty(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setBoolean(targetIndex, slots.getLong(srcIndex) != 0L);
                        return;
                    case BOOLEAN_VALUE:
                        slots.setBoolean(targetIndex, slots.getBoolean(srcIndex));
                        return;
                    case DOUBLE_VALUE:
                        slots.setBoolean(targetIndex, slots.getDouble(srcIndex) != 0.0);
                        return;
                    case DECIMAL_VALUE:
                        slots.setBoolean(targetIndex, !slots.getDecimal(srcIndex).equals(BigDecimal.ZERO));
                        return;
                    case BYTE_VALUE:
                        slots.setBoolean(targetIndex, slots.getByte(srcIndex) != 0);
                        return;
                    case FLOAT_VALUE:
                        slots.setBoolean(targetIndex, slots.getFloat(srcIndex) != 0.0f);
                        return;
                    case CHAR_VALUE:
                        slots.setBoolean(targetIndex, slots.getChar(srcIndex) != 'f');
                        return;
                    case SHORT_VALUE:
                        slots.setBoolean(targetIndex, slots.getShort(srcIndex) != 0);
                        return;
                }
                break;
            case DOUBLE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setDouble(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setDouble(targetIndex, Double.parseDouble(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setDouble(targetIndex, slots.getBoolean(srcIndex)? 1.0 : 0.0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setDouble(targetIndex, slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setDouble(targetIndex, slots.getDecimal(srcIndex).doubleValue());
                        return;
                    case BYTE_VALUE:
                        slots.setDouble(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setDouble(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case DECIMAL_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal(slots.getInt(srcIndex)));
                        return;
                    case STRING_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal(slots.getLong(srcIndex)));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setDecimal(targetIndex, slots.getBoolean(srcIndex)? BigDecimal.ONE : BigDecimal.ZERO);
                        return;
                    case DOUBLE_VALUE:
                        slots.setDecimal(targetIndex, BigDecimal.valueOf(slots.getDouble(srcIndex)));
                        return;
                    case DECIMAL_VALUE:
                        slots.setDecimal(targetIndex, slots.getDecimal(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal(slots.getByte(srcIndex)));
                        return;
                    case FLOAT_VALUE:
                        slots.setDecimal(targetIndex, BigDecimal.valueOf(slots.getFloat(srcIndex)));
                        return;
                    case CHAR_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal((int)slots.getChar(srcIndex)));
                        return;
                    case SHORT_VALUE:
                        slots.setDecimal(targetIndex, new BigDecimal(slots.getShort(srcIndex)));
                        return;
                }
                break;
            case BYTE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setByte(targetIndex, Byte.parseByte(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setByte(targetIndex, slots.getBoolean(srcIndex)? (byte)1 : (byte)0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setByte(targetIndex, slots.getDecimal(srcIndex).byteValue());
                        return;
                    case BYTE_VALUE:
                        slots.setByte(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getShort(srcIndex));
                        return;
                }
                break;
            case FLOAT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setFloat(targetIndex, Float.parseFloat(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setFloat(targetIndex, slots.getBoolean(srcIndex)? 1.0f : 0.0f);
                        return;
                    case DOUBLE_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setFloat(targetIndex, slots.getDecimal(srcIndex).floatValue());
                        return;
                    case BYTE_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setFloat(targetIndex, slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getShort(srcIndex));
                        return;
                }
                break;
            case CHAR_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setChar(targetIndex, stringToChar(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setChar(targetIndex, (char)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setChar(targetIndex, slots.getBoolean(srcIndex)? 't' : 'f');
                        return;
                    case DOUBLE_VALUE:
                        slots.setChar(targetIndex, (char)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setChar(targetIndex, (char) slots.getDecimal(srcIndex).intValue());
                        return;
                    case BYTE_VALUE:
                        slots.setChar(targetIndex, (char)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setChar(targetIndex, slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getShort(srcIndex));
                        return;
                }
                break;
            case SHORT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setShort(targetIndex, (short)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setShort(targetIndex, Short.parseShort(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setShort(targetIndex, (short)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setShort(targetIndex, slots.getBoolean(srcIndex)? (short)1 : (short)0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setShort(targetIndex, (short)slots.getDouble(srcIndex));
                        return;
                    case DECIMAL_VALUE:
                        slots.setShort(targetIndex, slots.getDecimal(srcIndex).shortValue());
                        return;
                    case BYTE_VALUE:
                        slots.setShort(targetIndex, (short)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setShort(targetIndex, (short)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setShort(targetIndex, (short)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setShort(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
        }
    }

    public static boolean castObject(AgoFrame agoFrame, CallFrame<?> self, Slots slots, int index, Instance<?> object, int typeCode, AgoClass expectedClass) {
        if(object == null){
            agoFrame.raiseException(self, "lang.ClassCastException", "can't cast null to '%s'".formatted(expectedClass.getFullname()));
            return true;
        }
        if(!agoFrame.validateClassInheritance(object.getAgoClass(), expectedClass)) return false;
        slots.setObject(index, object);
        return true;
    }

    private static Object castPrimitiveToPrimitiveUnion(Slots slots, int targetTypeCode, int srcIndex, int srcTypeCode, AgoEngine engine) {
        switch (targetTypeCode) {
            case INT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return slots.getInt(srcIndex);
                    case STRING_VALUE:  return Integer.parseInt(slots.getString(srcIndex));
                    case LONG_VALUE:    return (int)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? 1 : 0;
                    case DOUBLE_VALUE:  return (int)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).intValue();
                    case BYTE_VALUE:    return (int)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (int)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (int)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (int)slots.getShort(srcIndex);
                }
                break;

            case STRING_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return String.valueOf(slots.getInt(srcIndex));
                    case STRING_VALUE:  return slots.getString(srcIndex);
                    case LONG_VALUE:    return String.valueOf(slots.getLong(srcIndex));
                    case BOOLEAN_VALUE: return String.valueOf(slots.getBoolean(srcIndex));
                    case DOUBLE_VALUE:  return String.valueOf(slots.getDouble(srcIndex));
                    case DECIMAL_VALUE: return String.valueOf(slots.getDecimal(srcIndex));
                    case BYTE_VALUE:    return String.valueOf(slots.getByte(srcIndex));
                    case FLOAT_VALUE:   return String.valueOf(slots.getFloat(srcIndex));
                    case CHAR_VALUE:    return String.valueOf(slots.getChar(srcIndex));
                    case SHORT_VALUE:   return String.valueOf(slots.getShort(srcIndex));
                    case CLASS_REF_VALUE: return engine.getClass(slots.getClassRef(srcIndex)).getFullname();
                }
                break;

            case LONG_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (long)slots.getInt(srcIndex);
                    case STRING_VALUE:  return Long.parseLong(slots.getString(srcIndex));
                    case LONG_VALUE:    return slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? 1L : 0L;
                    case DOUBLE_VALUE:  return (long)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).longValue();
                    case BYTE_VALUE:    return (long)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (long)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (long)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (long)slots.getShort(srcIndex);
                }
                break;

            case BOOLEAN_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return slots.getInt(srcIndex) != 0;
                    case STRING_VALUE:  return StringUtils.isNotEmpty(slots.getString(srcIndex));
                    case LONG_VALUE:    return slots.getLong(srcIndex) != 0L;
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex);
                    case DOUBLE_VALUE:  return slots.getDouble(srcIndex) != 0.0;
                    case DECIMAL_VALUE: return !slots.getDecimal(srcIndex).equals(BigDecimal.ZERO);
                    case BYTE_VALUE:    return slots.getByte(srcIndex) != 0;
                    case FLOAT_VALUE:   return slots.getFloat(srcIndex) != 0.0f;
                    case CHAR_VALUE:    return slots.getChar(srcIndex) != 'f';
                    case SHORT_VALUE:   return slots.getShort(srcIndex) != 0;
                }
                break;

            case DOUBLE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (double)slots.getInt(srcIndex);
                    case STRING_VALUE:  return Double.parseDouble(slots.getString(srcIndex));
                    case LONG_VALUE:    return (double)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? 1.0 : 0.0;
                    case DOUBLE_VALUE:  return slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).doubleValue();
                    case BYTE_VALUE:    return (double)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (double)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (double)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (double)slots.getShort(srcIndex);
                }
                break;

            case DECIMAL_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return new BigDecimal(slots.getInt(srcIndex));
                    case STRING_VALUE:  return new BigDecimal(slots.getString(srcIndex));
                    case LONG_VALUE:    return new BigDecimal(slots.getLong(srcIndex));
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? BigDecimal.ONE : BigDecimal.ZERO;
                    case DOUBLE_VALUE:  return BigDecimal.valueOf(slots.getDouble(srcIndex));
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex);
                    case BYTE_VALUE:    return new BigDecimal(slots.getByte(srcIndex));
                    case FLOAT_VALUE:   return BigDecimal.valueOf(slots.getFloat(srcIndex));
                    case CHAR_VALUE:    return new BigDecimal((int)slots.getChar(srcIndex));
                    case SHORT_VALUE:   return new BigDecimal(slots.getShort(srcIndex));
                }
                break;

            case BYTE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (byte)slots.getInt(srcIndex);
                    case STRING_VALUE:  return Byte.parseByte(slots.getString(srcIndex));
                    case LONG_VALUE:    return (byte)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? (byte)1 : (byte)0;
                    case DOUBLE_VALUE:  return (byte)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).byteValue();
                    case BYTE_VALUE:    return slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (byte)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (byte)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (byte)slots.getShort(srcIndex);
                }
                break;

            case FLOAT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (float)slots.getInt(srcIndex);
                    case STRING_VALUE:  return Float.parseFloat(slots.getString(srcIndex));
                    case LONG_VALUE:    return (float)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? 1.0f : 0.0f;
                    case DOUBLE_VALUE:  return (float)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).floatValue();
                    case BYTE_VALUE:    return (float)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (float)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (float)slots.getShort(srcIndex);
                }
                break;

            case CHAR_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (char)slots.getInt(srcIndex);
                    case STRING_VALUE:  return stringToChar(slots.getString(srcIndex));
                    case LONG_VALUE:    return (char)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? 't' : 'f';
                    case DOUBLE_VALUE:  return (char)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return (char)slots.getDecimal(srcIndex).intValue();
                    case BYTE_VALUE:    return (char)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (char)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return slots.getChar(srcIndex);
                    case SHORT_VALUE:   return (char)slots.getShort(srcIndex);
                }
                break;

            case SHORT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:     return (short)slots.getInt(srcIndex);
                    case STRING_VALUE:  return Short.parseShort(slots.getString(srcIndex));
                    case LONG_VALUE:    return (short)slots.getLong(srcIndex);
                    case BOOLEAN_VALUE: return slots.getBoolean(srcIndex) ? (short)1 : (short)0;
                    case DOUBLE_VALUE:  return (short)slots.getDouble(srcIndex);
                    case DECIMAL_VALUE: return slots.getDecimal(srcIndex).shortValue();
                    case BYTE_VALUE:    return (short)slots.getByte(srcIndex);
                    case FLOAT_VALUE:   return (short)slots.getFloat(srcIndex);
                    case CHAR_VALUE:    return (short)slots.getChar(srcIndex);
                    case SHORT_VALUE:   return slots.getShort(srcIndex);
                }
                break;

            case CLASS_REF_VALUE:
                switch(srcTypeCode){
                    case CLASS_REF_VALUE:     return new ClassRefValue(engine.getClass(slots.getClassRef(srcIndex)).getFullname());
                    case NULL_VALUE:          return new ClassRefValue("null");
                }
                break;
        }

        return null;
    }

}
