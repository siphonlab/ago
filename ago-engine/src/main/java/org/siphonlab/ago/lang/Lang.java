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

import org.jspecify.annotations.NonNull;
import org.siphonlab.ago.*;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.AgoArrayInstance;
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
        AgoClass agoClass = (AgoClass) frame.getParentScope().getSlots().getObject(1);
        frame.finishString(agoClass.getFullname());
    }

    public static void Class_getName( NativeFrame frame){
        var scope = frame.getParentScope();
        frame.finishString(((AgoClass) scope).getName());
    }

    public static void ClassRef_getName(NativeFrame frame){
        AgoClass agoClass = (AgoClass) frame.getParentScope().getSlots().getObject(1);
        frame.finishString(agoClass.getFullname());
    }

    public static void Object_getClass( NativeFrame frame){
        Instance<?> object = frame.getParentScope();
        AgoEngine engine = frame.getAgoEngine();
        var scopedClassRef = engine.getBoxer().boxClassRef(frame, engine.getLangClasses().getScopedClassRefClass(), object.getAgoClass(), object.getParentScope());
        frame.finishObject(scopedClassRef);
    }

    public static void Throwable_fillStackTrace(NativeFrame callFrame){
        var scope = callFrame.getParentScope();
        var agoEngine = callFrame.getAgoEngine();

        CallFrame<?> creator = callFrame.getCaller().getCaller();   // new# -> caller
        AgoClass StackTraceElementClass = agoEngine.getClass("lang.StackTraceElement");
        AgoField functionName = StackTraceElementClass.findField("functionName");
        AgoField fileName = StackTraceElementClass.findField("fileName");
        AgoField lineNumber = StackTraceElementClass.findField("lineNumber");
        AgoField column = StackTraceElementClass.findField("column");
        AgoField length = StackTraceElementClass.findField("length");

        List<Instance<?>> stackElements = fillAgoStackTrace(callFrame, creator, agoEngine, StackTraceElementClass, functionName, fileName, lineNumber, column, length);

        AgoClass arrClass = agoEngine.getClass("[lang.StackTraceElement;");
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

    private static @NonNull List<Instance<?>> fillAgoStackTrace(NativeFrame callFrame, CallFrame<?> creator, AgoEngine agoEngine, AgoClass StackTraceElementClass, AgoField functionName, AgoField fileName, AgoField lineNumber, AgoField column, AgoField length) {
        List<Instance<?>> stackElements = new ArrayList<>();
        for(var c = creator; c!= null && c.getAgoClass() != null; c = c.getCaller()) {
            var inst = agoEngine.createInstance(StackTraceElementClass, callFrame.getRunSpace());
            //     fun new(field functionName as string, field fileName as string, field lineNumber as int, field column as int, field length as int){
            SourceLocation sourceLocation = c.resolveSourceLocation();
            inst.getSlots().setString(functionName.getSlotIndex(), c.getAgoClass().getFullname());
            inst.getSlots().setString(fileName.getSlotIndex(), sourceLocation.getFilename());
            inst.getSlots().setInt(lineNumber.getSlotIndex(), sourceLocation.getLine());
            inst.getSlots().setInt(column.getSlotIndex(), sourceLocation.getColumn());
            inst.getSlots().setInt(length.getSlotIndex(), sourceLocation.getLength());
            stackElements.add(inst);
        }
        return stackElements;
    }

    public static void Throwable_fillStackTraceFromJavaException(NativeFrame callFrame) {
        var scope = callFrame.getParentScope();
        var javaException = (java.lang.Exception) scope.getNativePayload();
        var agoEngine = callFrame.getAgoEngine();

        CallFrame<?> creator = callFrame.getCaller().getCaller();   // new# -> caller
        AgoClass StackTraceElementClass = agoEngine.getClass("lang.StackTraceElement");
        AgoField functionName = StackTraceElementClass.findField("functionName");
        AgoField fileName = StackTraceElementClass.findField("fileName");
        AgoField lineNumber = StackTraceElementClass.findField("lineNumber");
        AgoField column = StackTraceElementClass.findField("column");
        AgoField length = StackTraceElementClass.findField("length");
        List<Instance<?>> stackElements = fillAgoStackTrace(callFrame, creator, agoEngine, StackTraceElementClass, functionName, fileName, lineNumber, column, length);

        var c = callFrame.resolveSourceLocation();
        for (var trace : javaException.getStackTrace()) {
            var inst = agoEngine.createInstance(StackTraceElementClass, callFrame.getRunSpace());
            //     fun new(field functionName as string, field fileName as string, field lineNumber as int, field column as int, field length as int){

            inst.getSlots().setString(functionName.getSlotIndex(), trace.getMethodName());
            inst.getSlots().setString(fileName.getSlotIndex(), trace.getFileName());
            inst.getSlots().setInt(lineNumber.getSlotIndex(), trace.getLineNumber());
            inst.getSlots().setInt(column.getSlotIndex(), c.getColumn());
            inst.getSlots().setInt(length.getSlotIndex(), c.getLength());
            stackElements.add(inst);
        }
        AgoClass arrClass = agoEngine.getClass("[lang.StackTraceElement;");
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

    public static void arrayCopy(NativeFrame frame, Instance<?> source, int sourcePos, Instance<?> destination, int destPos, int length){
        var srcArray = ((AgoArrayInstance)source).getArray();
        var destArray = ((AgoArrayInstance)destination).getArray();
        System.arraycopy(srcArray, sourcePos, destArray, destPos, length);
        frame.finishVoid();
    }

    // --- String instance methods (receiver is parent scope) ---

    public static void String_length(NativeFrame frame){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishInt(s.length());
    }

    public static void String_charAt(NativeFrame frame, int index){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishChar(s.charAt(index));
    }

    public static void String_substring_begin(NativeFrame frame, int beginIndex){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.substring(beginIndex));
    }

    public static void String_substring_begin_end(NativeFrame frame, int beginIndex, int endIndex){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.substring(beginIndex, endIndex));
    }

    public static void String_contains(NativeFrame frame, String target){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishBoolean(s.contains(target));
    }

    public static void String_startsWith(NativeFrame frame, String prefix){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishBoolean(s.startsWith(prefix));
    }

    public static void String_endsWith(NativeFrame frame, String suffix){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishBoolean(s.endsWith(suffix));
    }

    public static void String_indexOf_char(NativeFrame frame, char ch){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishInt(s.indexOf(ch));
    }

    public static void String_indexOf_str(NativeFrame frame, String str){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishInt(s.indexOf(str));
    }

    public static void String_lastIndexOf_char(NativeFrame frame, char ch){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishInt(s.lastIndexOf(ch));
    }

    public static void String_lastIndexOf_str(NativeFrame frame, String str){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishInt(s.lastIndexOf(str));
    }

    public static void String_trim(NativeFrame frame){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.trim());
    }

    public static void String_toLowerCase(NativeFrame frame){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.toLowerCase());
    }

    public static void String_toUpperCase(NativeFrame frame){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.toUpperCase());
    }

    public static void String_replace_char(NativeFrame frame, char oldChar, char newChar){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.replace(oldChar, newChar));
    }

    public static void String_replace_str(NativeFrame frame, String oldStr, String newStr){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishString(s.replace(oldStr, newStr));
    }

    public static void String_split(NativeFrame frame, String separator){
        String s = frame.getParentScope().getSlots().getString(0);
        var parts = s.split(separator, -1);
        AgoEngine engine = frame.getAgoEngine();
        var arrayInst = engine.createStringArray(frame.getAgoClass().getResultClass(), parts.length);
        for (int i = 0; i < parts.length; i++) {
            arrayInst.value[i] = parts[i];
        }
        frame.finishObject(arrayInst);
    }

    public static void String_equalsIgnoreCase(NativeFrame frame, String other){
        String s = frame.getParentScope().getSlots().getString(0);
        frame.finishBoolean(s.equalsIgnoreCase(other));
    }
}
