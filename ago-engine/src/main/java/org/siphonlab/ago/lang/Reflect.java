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
package org.siphonlab.ago.lang;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.opcode.Box;
import org.siphonlab.ago.runtime.UnionArrayInstance;

import java.util.Collection;
import java.util.List;

public class Reflect {


    public static void Class_forName(NativeFrame frame, String name){
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = engine.getClass(name);
        if(agoClass == null){
            frame.finishUnion(null);
        } else {
            frame.finishUnion(engine.getBoxer().boxClassRef(agoClass.getClassId()));
        }
    }

    private static AgoClass getClassFromClassRef(NativeFrame frame) {
        return Boxer.getClassFromClassRef(frame.getParentScope());
    }

    public static void getProperties(NativeFrame frame, boolean includePrivate){
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = getClassFromClassRef(frame);

        Collection<Property> properties = agoClass.getPropertyMap().values();
        if(!includePrivate) {
            properties = properties.stream().filter(p -> (p.isReadable() && p.getVisibilityForRead() == Visibility.Public) || p.getVisibilityForWrite() == Visibility.Public).toList();
        }
        var arr = engine.createObjectArray(frame.getAgoClass().getResultClass(), properties.size());
        int i = 0;
        for (Property property : properties) {
            var inst = engine.createNativeInstance(null, engine.getClass("lang.PropertyDesc"), frame);
            inst.setNativePayload(property);
            arr.value[i++] = inst;
        }
        frame.finishObject(arr);
    }

    public static void getMethods(NativeFrame frame, boolean includePrivate){
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = getClassFromClassRef(frame);

        var methods = agoClass.getMethods();
        List<Instance<?>> ls = new java.util.ArrayList<>(methods.length);
        for (AgoFunction method : methods) {
            if(method != null && (includePrivate || method.isPublic())){
                ls.add(engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getClassRefClass(), method));
            }
        }
        var arr = engine.createObjectArray(frame.getAgoClass().getResultClass(), ls.size());
        ls.toArray(arr.value);
        frame.finishObject(arr);
    }

    public static void getChildren(NativeFrame frame, boolean includePrivate){
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = getClassFromClassRef(frame);

        var children = agoClass.getChildren();
        List<Instance<?>> ls = new java.util.ArrayList<>(children.length);
        for (var child : children) {
            if(child != null && (includePrivate || child.isPublic())){
                ls.add(engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getClassRefClass(), child));
            }
        }
        var arr = engine.createObjectArray(frame.getAgoClass().getResultClass(), ls.size());
        ls.toArray(arr.value);
        frame.finishObject(arr);
    }

    public static void getProperty(NativeFrame frame, String name, boolean includePrivate){
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = getClassFromClassRef(frame);

        var prop = agoClass.getPropertyMap().get(name);
        if(prop == null) {
            frame.finishUnion(null);
            return;
        }
        if(!includePrivate && prop.getVisibilityForRead() != Visibility.Public){
            frame.finishUnion(null);
            return;
        }

        var inst = engine.createNativeInstance(null, engine.getClass("lang.PropertyDesc"), frame);
        inst.setNativePayload(prop);
        frame.finishUnion(inst);
    }

    public static void getMethod(NativeFrame frame, String name, boolean includePrivate){
        AgoClass agoClass = getClassFromClassRef(frame);

        var method = agoClass.findMethod(name);
        if(method == null) {
            frame.finishUnion(null);
            return;
        }
        if(!includePrivate && !method.isPublic()){
            frame.finishUnion(null);
            return;
        }

        AgoEngine engine = frame.getAgoEngine();
        frame.finishUnion(engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getScopedClassRefClass(), method));
    }

    public static void getChild(NativeFrame frame, String name, boolean includePrivate){
        AgoClass agoClass = getClassFromClassRef(frame);

        var child = agoClass.findChild(name);
        if(child == null) {
            frame.finishUnion(null);
            return;
        }
        if(!includePrivate && !child.isPublic()){
            frame.finishUnion(null);
            return;
        }

        AgoEngine engine = frame.getAgoEngine();
        frame.finishUnion(engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getScopedClassRefClass(), child));
    }

    public static void Property_getName(NativeFrame frame){
        Property propertyDesc = (Property) frame.getParentScope().getNativePayload();
        frame.finishString(propertyDesc.getName());
    }

    public static void Property_isReadable(NativeFrame frame){
        Property propertyDesc = (Property) frame.getParentScope().getNativePayload();
        frame.finishBoolean(propertyDesc.isReadable());
    }

    public static void Property_isWritable(NativeFrame frame){
        Property propertyDesc = (Property) frame.getParentScope().getNativePayload();
        frame.finishBoolean(propertyDesc.isWritable());
    }

    public static void Property_getKind(NativeFrame frame){
        Property propertyDesc = (Property) frame.getParentScope().getNativePayload();

        AgoEngine engine = frame.getAgoEngine();
        AgoEnum propertyKindEnum = (AgoEnum) engine.getClass("lang.PropertyKind");
        if(propertyDesc instanceof Property.FieldProperty){
            frame.finishObject(propertyKindEnum.findMember("Field"));
        } else {
            frame.finishObject(propertyKindEnum.findMember("Attribute"));
        }
    }

    public static void Property_getType(NativeFrame frame){
        Property propertyDesc = (Property) frame.getParentScope().getNativePayload();
        AgoEngine engine = frame.getAgoEngine();
        frame.finishObject(engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getClassRefClass(), propertyDesc.getType()));
    }

    public static void Property_getValue(NativeFrame frame, Instance<?> object, String propName){
        if(frame.getReenterState() == NativeFrame.REENTER_INVOKE_GETTER){
            var inst = frame.getRunSpace().getResultSlots().castAnyToObject(frame.getAgoEngine().getBoxer());
            frame.finishUnion(inst);
            return;
        }
        var property = object.getAgoClass().getPropertyMap().get(propName);
        if(property == null){
            frame.raiseException(frame.self(), "NoSuchPropertyException", "'%s' not found in '%s'".formatted(propName, object.getAgoClass().getFullname()));
            return;
        }
        getPropertyValue(frame, object, property);
    }

    public static void Property_getValue(NativeFrame frame, Instance<?> object, Instance<?> propertyInst){
        Property property = (Property) propertyInst.getNativePayload();
        if(property.getOwnerClass() != object.getAgoClass()){
            frame.raiseException(frame.self(), "NoSuchPropertyException", "the owner of '%s' is not '%s'".formatted(property.getName(), object.getAgoClass().getFullname()));
            return;
        }
        getPropertyValue(frame, object, property);
    }

    public static void getPropertyValue(NativeFrame frame, Instance<?> object, Property property){
        if(!property.isReadable()){
            frame.raiseException(frame.self(), "IllegalAccessException", "'%s' is not readable".formatted(property.getName()));
            return;
        }
        var engine = frame.getAgoEngine();
        if(property instanceof Property.FieldProperty fieldProperty){
            AgoField agoField = fieldProperty.getAgoField();
            var r = engine.getBoxer().boxAny(object.getSlots(), agoField.getSlotIndex(), agoField.getTypeCode().value);
            frame.finishUnion(r);
        } else if(property instanceof Property.AttributeProperty attributeProperty){
            var getter = attributeProperty.getGetter();
            object.invokeMethod(frame, NativeFrame.REENTER_INVOKE_GETTER, 0, getter);
        } else {
            throw new IllegalStateException("unknown property type " + property);
        }
    }

    public static void Property_setValue(NativeFrame frame, Instance<?> object, String propName, Object value){
        if(frame.getReenterState() == NativeFrame.REENTER_INVOKE_SETTER){
            frame.finishVoid();
            return;
        }
        var property = object.getAgoClass().getPropertyMap().get(propName);
        if(property == null){
            frame.raiseException(frame.self(), "NoSuchPropertyException", "'%s' not found in '%s'".formatted(propName, object.getAgoClass().getFullname()));
            return;
        }
        setPropertyValue(frame, object, property, value);
    }

    public static void Property_setValue(NativeFrame frame, Instance<?> object, Instance<?> propertyInst, Object value){
        Property property = (Property) propertyInst.getNativePayload();
        if(property.getOwnerClass() != object.getAgoClass()){
            frame.raiseException(frame.self(), "NoSuchPropertyException", "the owner of '%s' is not '%s'".formatted(property.getName(), object.getAgoClass().getFullname()));
            return;
        }
        setPropertyValue(frame, object, property, value);
    }

    public static void setPropertyValue(NativeFrame frame, Instance<?> object, Property property, Object value){
        if(!property.isWritable()){
            frame.raiseException(frame.self(), "IllegalAccessException", "'%s' is not writable".formatted(property.getName()));
            return;
        }
        var engine = frame.getAgoEngine();
        if(property instanceof Property.FieldProperty fieldProperty){
            AgoField agoField = fieldProperty.getAgoField();
            DynamicOp.setSlot(frame, frame.self(), object, value, agoField, object.getAgoClass());
            frame.finishVoid();
        } else if(property instanceof Property.AttributeProperty attributeProperty){
            var method = attributeProperty.getSetter();
            var setter = engine.createFunctionInstance(object, method, frame.self(), frame.self());
            DynamicOp.setSlot(frame, frame.self(), setter, value, setter.getAgoClass().getParameters()[0], setter.getAgoClass());

            frame.invokeFrame(setter, NativeFrame.REENTER_INVOKE_SETTER);
        } else {
            throw new IllegalStateException("unknown property type " + property);
        }
    }

    // arguments type is Object? ..., values are boxed as Instance
    // method is a ClassRef object
    public static void Method_invoke(NativeFrame frame, Instance<?> object, Instance<?> method, Instance<?> arguments){
        if(frame.getReenterState() == NativeFrame.REENTER_INVOKE_FUNCTION){
            var inst = frame.getRunSpace().getResultSlots().castAnyToObject(frame.getAgoEngine().getBoxer());
            frame.finishUnion(inst);
            return;
        }

        AgoEngine engine = frame.getAgoEngine();
        UnionArrayInstance arr = (UnionArrayInstance) arguments;
        AgoFunction fun = method instanceof AgoFunction f ? f : (AgoFunction) Boxer.getClassFromClassRef(method);
        var toInvoke = engine.createFunctionInstance(object, fun, frame.self(), frame.self());
        AgoParameter[] parameters = fun.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            AgoParameter parameter = parameters[i];
            DynamicOp.setSlot(frame, frame.self(), toInvoke, arr.value[i], parameter, fun);
        }
        frame.invokeFrame(toInvoke, NativeFrame.REENTER_INVOKE_FUNCTION);
    }

    public static void Method_invoke(NativeFrame frame, Instance<?> object, String method, Instance<?> arguments){
        var fun = object.getAgoClass().findMethod(method);
        if(fun == null){
            frame.raiseException(frame.self(), "NoSuchMethodException", "'%s' not found in '%s'".formatted(method, object.getAgoClass().getFullname()));
            return;
        }
        Method_invoke(frame, object, fun, arguments);
    }

    public static void createInstance(NativeFrame frame){
        createInstance(frame, null, null, null);
    }

    public static void createInstance(NativeFrame frame, Instance<?> scope){
        createInstance(frame, scope, null, null);
    }

    public static void createInstance(NativeFrame frame, Instance<?> constructor, Instance<?> arguments){
        createInstance(frame, null, constructor, arguments);
    }

    public static void createInstance(NativeFrame frame, Instance<?> scope, Instance<?> constructor, Instance<?> arguments){
        if(frame.getReenterState() == NativeFrame.REENTER_CREATE_INSTANCE){
            Object nativePayload = frame.getNativePayload();
            frame.finishObject((Instance<?>) nativePayload);
            return;
        }
        AgoEngine engine = frame.getAgoEngine();
        AgoClass agoClass = getClassFromClassRef(frame);

        Instance<?> result;
        if(agoClass.isNative()) {
            result = engine.createNativeInstance(scope, agoClass, frame.self());
        } else {
            result = engine.createInstance(scope, agoClass, frame.self());
        }
        if(constructor != null){
            AgoFunction fun = constructor instanceof AgoFunction f ? f : (AgoFunction) Boxer.getClassFromClassRef(constructor);
            var toInvoke = engine.createFunctionInstance(result, fun, frame.self(), frame.self());
            UnionArrayInstance arr = (UnionArrayInstance) arguments;
            AgoParameter[] parameters = fun.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                AgoParameter parameter = parameters[i];
                DynamicOp.setSlot(frame, frame.self(), toInvoke, arr.value[i], parameter, fun);
            }
            frame.setNativePayload(result);
            frame.invokeFrame(toInvoke, NativeFrame.REENTER_CREATE_INSTANCE);
        } else {
            frame.finishObject(result);
        }
    }


}
