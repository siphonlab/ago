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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.ObjectArrayInstance;

import java.util.ArrayList;
import java.util.List;

public class Lang {
    public static void Object_hashCode(NativeFrame frame){
        frame.finishInt(frame.getParentScope().hashCode());
    }

    public static void Integer_toHexString(NativeFrame frame, int number){
        frame.finishString(Integer.toHexString(number));
    }

    public static void Integer_toString(NativeFrame frame){
        frame.finishString(Integer.toString(frame.getParentScope().getSlots().getInt(0)));
    }

    public static void Long_toHexString( NativeFrame frame, long number) {
        frame.finishString(Long.toHexString(number));
    }

    public static void Long_toString( NativeFrame frame) {
        frame.finishString(Long.toString(frame.getParentScope().getSlots().getLong(0)));
    }


    public static void ClassRef_toString(NativeFrame frame) {
        int classRef = frame.getParentScope().getSlots().getClassRef(0);
        frame.finishString(frame.getAgoEngine().getClass(classRef).getFullname());
    }

    public static void Class_getName( NativeFrame frame){
        var scope = frame.getParentScope();
        if(scope == null) frame.finishNull();
        frame.finishString(((AgoClass) scope).getName());
    }

    public static void Object_getClass( NativeFrame frame){
        frame.finishObject(frame.getParentScope().getAgoClass());
    }

    public static void Throwable_fillStackTrace(NativeFrame callFrame){
        var scope = callFrame.getParentScope();
        var agoEngine = callFrame.getAgoEngine();

        CallFrame<?> creator = callFrame.getCaller().getCaller().getCaller();   // new# -> caller
        AgoClass StackTraceElementClass = agoEngine.getClass("lang.StackTraceElement");
        AgoField functionName = StackTraceElementClass.findField("functionName");
        AgoField fileName = StackTraceElementClass.findField("fileName");
        AgoField lineNumber = StackTraceElementClass.findField("lineNumber");
        AgoField column = StackTraceElementClass.findField("column");
        AgoField length = StackTraceElementClass.findField("length");
        List<Instance<?>> stackElements = new ArrayList<>();
        for(var c = creator; c!= null && c.getAgoClass() != null; c = c.getCaller()) {
            var inst = agoEngine.createInstance(StackTraceElementClass, callFrame);
            //     fun new(field functionName as string, field fileName as string, field lineNumber as int, field column as int, field length as int){
            SourceLocation sourceLocation = c.resolveSourceLocation();
            inst.getSlots().setString(functionName.getSlotIndex(), c.getAgoClass().getFullname());
            inst.getSlots().setString(fileName.getSlotIndex(), sourceLocation.getFilename());
            inst.getSlots().setInt(lineNumber.getSlotIndex(), sourceLocation.getLine());
            inst.getSlots().setInt(column.getSlotIndex(), sourceLocation.getColumn());
            inst.getSlots().setInt(length.getSlotIndex(), sourceLocation.getLength());
            stackElements.add(inst);
        }
        AgoClass arrClass = agoEngine.getClass("lang.[StackTraceElement");
        var arrayInst = new ObjectArrayInstance(arrClass.createSlots(), arrClass, stackElements.size());
        for (int i = 0; i < stackElements.size(); i++) {
            Instance<?> stackElement = stackElements.get(i);
            arrayInst.value[i] = stackElement;
        }
        var ThrowableClass = agoEngine.getClass("lang.Throwable");
        var stackTraceElements = ThrowableClass.findField("stackTraceElements");
        scope.getSlots().setObject(stackTraceElements.getSlotIndex(), arrayInst);

        callFrame.finishVoid();
    }

    public static void String_hashCode(NativeFrame frame, String s){
        frame.finishInt(s.hashCode());
    }

//    public static void Function_pause(NativeFrame frame){
//        var caller = (CallFrame<?>) frame.getParentScope();
//        if(caller instanceof AgoFrame agoFrame){
//            agoFrame.pause();
//        }
//        frame.finishVoid();
//    }
//
    public static void Function_notify(NativeFrame frame) {
        var caller = (CallFrame<?>) frame.getParentScope();
        caller.resume();
        frame.finishVoid();
    }
//
//    public static void Function_cancel(NativeFrame frame) {
//        System.out.println("cancel " + frame);
//        var caller = (CallFrame<?>) frame.getParentScope();
//        caller.getRunSpace().interrupt();
//        frame.finishVoid();
//    }


}
