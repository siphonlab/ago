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

import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DECIMAL_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;
import static org.siphonlab.ago.TypeCode.UNION_VALUE;

public class DynamicOp {

    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;
    public static final int WRITE_RESULT_WITH_SETTER = 2;
    public static final int RESULT_WITH_GETTER = 3;

    private final AgoFrame frame;
    private final AgoEngine engine;

    public DynamicOp(AgoFrame frame){
        this.frame = frame;
        this.engine = frame.getAgoEngine();
    }

    private int result;

    public int getResult() {
        return result;
    }

    public Object readMember(CallFrame<?> self, Instance<?> object, String memberName, int dest){
        if(object == null) {
            frame.raiseException(self, "lang.NullPointerException", "read '%s' from null".formatted(memberName));
            this.result = RESULT_EXCEPTION;
            return null;
        }
        String possibleError = null;
        AgoClass agoClass = object.getAgoClass();
        var property = agoClass.getPropertyMap().get(memberName);
        if(property != null){
            if(property.isReadable() && property.getVisibilityForRead() == Visibility.Public) {
                if(property instanceof Property.FieldProperty fieldProperty){
                    AgoField agoField = fieldProperty.getAgoField();
                    this.result = RESULT_OK;
                    return Union.toUnionValue(self.getAgoEngine(), object.getSlots(), agoField.getSlotIndex(), agoField.getTypeCode().value);
                } else if(property instanceof Property.AttributeProperty attributeProperty){
                    // invoke getter
                    object.invokeMethod(self, AgoFrame.REENTER_INVOKE_GETTER, dest, attributeProperty.getGetter());
                    this.result = RESULT_WITH_GETTER;
                    return null;
                } else {
                    throw new IllegalStateException("only these too properties");
                }
            } else {
                possibleError = "property '%s' is not visible or readable for '%s'".formatted(property.getName(), frame.getAgoClass().getFullname());
            }
        }
        AgoFunction method;
        if(memberName.contains("#")){
            method = agoClass.findMethod(memberName);
        } else {
            method = agoClass.findMethod(memberName + '#');
        }
        if(method != null){
            if(Modifier.isPublic(method.getModifiers())) {
                return engine.createScopedClass(self, method.getClassId(), object);
            } else {
                possibleError = "method '%s' is not visible for '%s'".formatted(method.getName(), frame.getAgoClass().getFullname());
            }
        }

        var child = agoClass.findChild(memberName);
        if(child != null){
            if(Modifier.isPublic(child.getModifiers())) {
                return engine.createScopedClass(self, child.getClassId(), object);
            } else {
                possibleError = "class '%s' is not visible for '%s'".formatted(child.getName(), frame.getAgoClass().getFullname());
            }
        }
        if(possibleError != null){
            frame.raiseException(self, "lang.IllegalAccessException", possibleError);
        } else {
            frame.raiseException(self, "lang.NoSuchMemberException", "'%s' not found in '%s'".formatted(memberName, agoClass.getFullname()));
        }
        this.result = RESULT_EXCEPTION;
        return null;
    }

    boolean containsMember(Instance<?> object, String memberName) {
        AgoClass agoClass = object.getAgoClass();
        var prop = agoClass.getPropertyMap().get(memberName);
        if(prop != null){
            if(prop.getVisibilityForRead() == Visibility.Public || prop.getVisibilityForWrite() == Visibility.Public) {
                return true;
            }
        }
        AgoFunction method;
        if(memberName.contains("#")){
            method = agoClass.findMethod(memberName);
        } else {
            method = agoClass.findMethod(memberName + '#');
        }
        if(method != null){
            if(Modifier.isPublic(method.getModifiers())) {
                return true;
            }
        }
        var child = agoClass.findChild(memberName);
        if(child != null){
            return Modifier.isPublic(child.getModifiers());
        }
        return false;
    }

    public int writeMember(CallFrame<?> self, Instance<?> object, String memberName, Object value){
        if(object == null) {
            frame.raiseException(self, "lang.NullPointerException", "set '%s' of null".formatted(memberName));
            return RESULT_EXCEPTION;
        }
        AgoClass agoClass = object.getAgoClass();
        String possibleError = null;
        var property = agoClass.getPropertyMap().get(memberName);
        if(property != null){
            if(property.isWritable() && property.getVisibilityForWrite() == Visibility.Public) {
                if(property instanceof Property.FieldProperty fieldProperty){
                    AgoVariable fld = fieldProperty.getAgoField();
                    AgoClass unionClass = frame.getAgoEngine().getLangClasses().getAnyClass();
                    if(Conversion.castFromUnion(self, frame, object.getSlots(), fld.getSlotIndex(), fld.getTypeCode().value, agoClass, value, unionClass)){
                        return RESULT_OK;
                    } else {
                        return RESULT_EXCEPTION;
                    }
                } else if(property instanceof Property.AttributeProperty attributeProperty){
                    var setter = engine.createFunctionInstance(object, attributeProperty.getSetter(), self, self);
                    AgoClass targetType = setter.getAgoClass();
                    AgoVariable fld = setter.getAgoClass().getParameters()[0];
                    AgoClass unionClass = frame.getAgoEngine().getLangClasses().getAnyClass();
                    if(Conversion.castFromUnion(self, frame, setter.getSlots(), fld.getSlotIndex(), fld.getTypeCode().value, targetType, value, unionClass)){
                        setter.setRunSpace(frame.getRunSpace());
                        setter.setCaller(frame);
                        frame.getRunSpace().setCurrCallFrame(setter);
                        return WRITE_RESULT_WITH_SETTER;
                    }
                }
            } else {
                possibleError = "property '%s' is not visible or not writable for '%s'".formatted(property.getName(), frame.getAgoClass().getFullname());
            }
        }
        if(possibleError != null){
            frame.raiseException(self, "lang.IllegalAccessException", possibleError);
        } else {
            frame.raiseException(self, "lang.NoSuchMemberException", "'%s' not found in '%s'".formatted(memberName, agoClass.getFullname()));
        }
        return RESULT_EXCEPTION;
    }

    CallFrame<?> createOrGetDynamicCallFrame(CallFrame<?> self, Instance<?> obj){
        if(obj == null) {
            frame.raiseException(self, "lang.NullPointerException", "an call frame or a function expected");
            return null;
        }
        if(obj instanceof CallFrame<?> f) return f;

        AgoClass agoClass = obj.getAgoClass();
        if(engine.getLangClasses().getScopedClassIntervalClass().isThatOrSuperOfThat(agoClass)) {
            return (CallFrame<?>) engine.createInstanceFromScopedClassInterval(obj, self);
        } else if(obj instanceof AgoFunction f){
            return (CallFrame<?>) engine.createInstanceFromScopedClass(f, self, frame.getRunSpace());
        } else {
            frame.raiseException(self, "lang.ClassCastException", "'%s' is not a function".formatted(agoClass.getFullname()));
            return null;
        }
    }

    Instance<?> createDynamicInstance(CallFrame<?> self, Instance<?> creator, Instance<?> tupleArguments){
        Instance<?> instance;
        AgoClass agoClass = creator.getAgoClass();
        if(engine.getLangClasses().getScopedClassIntervalClass().isThatOrSuperOfThat(agoClass)) {
            instance = engine.createInstanceFromScopedClassInterval(creator, self);
        } else if(creator instanceof AgoFunction f){
            instance = engine.createInstanceFromScopedClass(f, self, frame.getRunSpace());
        } else {
            frame.raiseException(self, "lang.ClassCastException", "'%s' is not a function".formatted(agoClass.getFullname()));
            return null;
        }
        if(!instance.getAgoClass().isFunction()){
            throw new UnsupportedOperationException("need to find out the constructor match arguments");
        }

        if(tupleArguments == null) return instance;

        var argSlots = tupleArguments.getSlots();
        for (AgoSlotDef slotDef : tupleArguments.getAgoClass().getSlotDefs()) {
            int index = slotDef.getIndex();
            switch (slotDef.getTypeCode().value){
                case INT_VALUE: instance.getSlots().setInt(index, argSlots.getInt(index)); break;
                case LONG_VALUE: instance.getSlots().setLong(index, argSlots.getLong(index)); break;
                case DOUBLE_VALUE: instance.getSlots().setDouble(index, argSlots.getDouble(index)); break;
                case DECIMAL_VALUE: instance.getSlots().setDecimal(index, argSlots.getDecimal(index)); break;
                case BOOLEAN_VALUE: instance.getSlots().setBoolean(index, argSlots.getBoolean(index)); break;
                case STRING_VALUE: instance.getSlots().setString(index, argSlots.getString(index)); break;
                case CHAR_VALUE: instance.getSlots().setChar(index, argSlots.getChar(index)); break;
                case SHORT_VALUE: instance.getSlots().setShort(index, argSlots.getShort(index)); break;
                case BYTE_VALUE: instance.getSlots().setByte(index, argSlots.getByte(index)); break;
                case FLOAT_VALUE: instance.getSlots().setFloat(index, argSlots.getFloat(index)); break;
                case CLASS_REF_VALUE: instance.getSlots().setClassRef(index, argSlots.getClassRef(index)); break;
                case OBJECT_VALUE: instance.getSlots().setObject(index, argSlots.getObject(index)); break;
                case UNION_VALUE: instance.getSlots().setUnion(index, argSlots.getUnion(index)); break;
            }
        }

        return instance;
    }
}