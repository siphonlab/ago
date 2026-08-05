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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // --- Integer metaclass statics ---

    public static void Integer_MIN_VALUE(NativeFrame frame){
        frame.finishInt(Integer.MIN_VALUE);
    }

    public static void Integer_MAX_VALUE(NativeFrame frame){
        frame.finishInt(Integer.MAX_VALUE);
    }

    // --- Integer instance methods (receiver at slot 0) ---

    public static void Integer_abs(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishInt(Math.abs(v));
    }

    public static void Integer_negate(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishInt(-v);
    }

    public static void Integer_clamp(NativeFrame frame, int minVal, int maxVal){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishInt(Math.max(minVal, Math.min(v, maxVal)));
    }

    public static void Integer_hexString(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishString(Integer.toHexString(v));
    }

    public static void Integer_toBinaryString(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishString(Integer.toBinaryString(v));
    }

    public static void Integer_bitCount(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishInt(Integer.bitCount(v));
    }

    public static void Integer_reverseBytes(NativeFrame frame){
        int v = frame.getParentScope().getSlots().getInt(0);
        frame.finishInt(Integer.reverseBytes(v));
    }

    // --- Long metaclass statics ---

    public static void Long_MIN_VALUE(NativeFrame frame){
        frame.finishLong(Long.MIN_VALUE);
    }

    public static void Long_MAX_VALUE(NativeFrame frame){
        frame.finishLong(Long.MAX_VALUE);
    }

    // --- Long instance methods (receiver at slot 0) ---

    public static void Long_abs(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishLong(Math.abs(v));
    }

    public static void Long_negate(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishLong(-v);
    }

    public static void Long_clamp(NativeFrame frame, long minVal, long maxVal){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishLong(Math.max(minVal, Math.min(v, maxVal)));
    }

    public static void Long_hexString(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishString(Long.toHexString(v));
    }

    public static void Long_toBinaryString(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishString(Long.toBinaryString(v));
    }

    public static void Long_bitCount(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishInt(Long.bitCount(v));
    }

    public static void Long_reverseBytes(NativeFrame frame){
        long v = frame.getParentScope().getSlots().getLong(0);
        frame.finishLong(Long.reverseBytes(v));
    }

    // --- Float toString ---

    public static void Float_toString(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishString(Float.toString(v));
    }

    // --- Float metaclass statics ---

    public static void Float_MIN_VALUE(NativeFrame frame){
        frame.finishFloat(Float.MIN_NORMAL);
    }

    public static void Float_MAX_VALUE(NativeFrame frame){
        frame.finishFloat(Float.MAX_VALUE);
    }

    public static void Float_NaN(NativeFrame frame){
        frame.finishFloat(Float.NaN);
    }

    public static void Float_POSITIVE_INFINITY(NativeFrame frame){
        frame.finishFloat(Float.POSITIVE_INFINITY);
    }

    public static void Float_NEGATIVE_INFINITY(NativeFrame frame){
        frame.finishFloat(Float.NEGATIVE_INFINITY);
    }

    // --- Float instance methods (receiver at slot 0) ---

    public static void Float_abs(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishFloat(Math.abs(v));
    }

    public static void Float_isFinite(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishBoolean(!Float.isInfinite(v) && !Float.isNaN(v));
    }

    public static void Float_isInfinite(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishBoolean(Float.isInfinite(v));
    }

    public static void Float_isNaN(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishBoolean(Float.isNaN(v));
    }

    public static void Float_roundToInt(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishInt(Math.round(v));
    }

    public static void Float_roundToLong(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishLong(Math.round(v));
    }

    public static void Float_ceil(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishFloat((float)Math.ceil(v));
    }

    public static void Float_floor(NativeFrame frame){
        float v = frame.getParentScope().getSlots().getFloat(0);
        frame.finishFloat((float)Math.floor(v));
    }

    public static void Float_round_decimals(NativeFrame frame, int decimals){
        float v = frame.getParentScope().getSlots().getFloat(0);
        double factor = Math.pow(10, decimals);
        frame.finishFloat((float)(Math.round(v * factor) / factor));
    }

    // --- Float extension methods (receiver as first param) ---

    public static void Float_abs_ext(NativeFrame frame, float v){
        frame.finishFloat(Math.abs(v));
    }

    public static void Float_isFinite_ext(NativeFrame frame, float v){
        frame.finishBoolean(!Float.isInfinite(v) && !Float.isNaN(v));
    }

    public static void Float_isInfinite_ext(NativeFrame frame, float v){
        frame.finishBoolean(Float.isInfinite(v));
    }

    public static void Float_isNaN_ext(NativeFrame frame, float v){
        frame.finishBoolean(Float.isNaN(v));
    }

    public static void Float_roundToInt_ext(NativeFrame frame, float v){
        frame.finishInt(Math.round(v));
    }

    public static void Float_roundToLong_ext(NativeFrame frame, float v){
        frame.finishLong(Math.round(v));
    }

    public static void Float_ceil_ext(NativeFrame frame, float v){
        frame.finishFloat((float)Math.ceil(v));
    }

    public static void Float_floor_ext(NativeFrame frame, float v){
        frame.finishFloat((float)Math.floor(v));
    }

    public static void Float_round_decimals_ext(NativeFrame frame, float v, int decimals){
        double factor = Math.pow(10, decimals);
        frame.finishFloat((float)(Math.round(v * factor) / factor));
    }

    public static void Float_floatToIntBits(NativeFrame frame, float v){
        frame.finishInt(Float.floatToIntBits(v));
    }

    public static void Float_hashCode_ext(NativeFrame frame, float v){
        frame.finishInt(Float.floatToIntBits(v));
    }

    // --- Double toString ---

    public static void Double_toString(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishString(Double.toString(v));
    }

    // --- Double metaclass statics ---

    public static void Double_MIN_VALUE(NativeFrame frame){
        frame.finishDouble(Double.MIN_NORMAL);
    }

    public static void Double_MAX_VALUE(NativeFrame frame){
        frame.finishDouble(Double.MAX_VALUE);
    }

    public static void Double_NaN(NativeFrame frame){
        frame.finishDouble(Double.NaN);
    }

    public static void Double_POSITIVE_INFINITY(NativeFrame frame){
        frame.finishDouble(Double.POSITIVE_INFINITY);
    }

    public static void Double_NEGATIVE_INFINITY(NativeFrame frame){
        frame.finishDouble(Double.NEGATIVE_INFINITY);
    }

    // --- Double instance methods (receiver at slot 0) ---

    public static void Double_abs(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishDouble(Math.abs(v));
    }

    public static void Double_isFinite(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishBoolean(!Double.isInfinite(v) && !Double.isNaN(v));
    }

    public static void Double_isInfinite(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishBoolean(Double.isInfinite(v));
    }

    public static void Double_isNaN(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishBoolean(Double.isNaN(v));
    }

    public static void Double_roundToInt(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishInt((int)Math.round(v));
    }

    public static void Double_roundToLong(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishLong(Math.round(v));
    }

    public static void Double_ceil(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishDouble(Math.ceil(v));
    }

    public static void Double_floor(NativeFrame frame){
        double v = frame.getParentScope().getSlots().getDouble(0);
        frame.finishDouble(Math.floor(v));
    }

    public static void Double_round_decimals(NativeFrame frame, int decimals){
        double v = frame.getParentScope().getSlots().getDouble(0);
        double factor = Math.pow(10, decimals);
        frame.finishDouble(Math.round(v * factor) / factor);
    }

    // --- Double extension methods (receiver as first param) ---

    public static void Double_abs_ext(NativeFrame frame, double v){
        frame.finishDouble(Math.abs(v));
    }

    public static void Double_isFinite_ext(NativeFrame frame, double v){
        frame.finishBoolean(!Double.isInfinite(v) && !Double.isNaN(v));
    }

    public static void Double_isInfinite_ext(NativeFrame frame, double v){
        frame.finishBoolean(Double.isInfinite(v));
    }

    public static void Double_isNaN_ext(NativeFrame frame, double v){
        frame.finishBoolean(Double.isNaN(v));
    }

    public static void Double_roundToInt_ext(NativeFrame frame, double v){
        frame.finishInt((int)Math.round(v));
    }

    public static void Double_roundToLong_ext(NativeFrame frame, double v){
        frame.finishLong(Math.round(v));
    }

    public static void Double_ceil_ext(NativeFrame frame, double v){
        frame.finishDouble(Math.ceil(v));
    }

    public static void Double_floor_ext(NativeFrame frame, double v){
        frame.finishDouble(Math.floor(v));
    }

    public static void Double_round_decimals_ext(NativeFrame frame, double v, int decimals){
        double factor = Math.pow(10, decimals);
        frame.finishDouble(Math.round(v * factor) / factor);
    }

    public static void Double_doubleToLongBits(NativeFrame frame, double v){
        frame.finishLong(Double.doubleToLongBits(v));
    }

    public static void Double_hashCode_ext(NativeFrame frame, double v){
        long bits = Double.doubleToLongBits(v);
        frame.finishInt((int)(bits ^ (bits >>> 32)));
    }

    // --- Byte toString ---

    public static void Byte_toString(NativeFrame frame){
        byte v = frame.getParentScope().getSlots().getByte(0);
        frame.finishString(Byte.toString(v));
    }

    // --- Byte metaclass statics ---

    public static void Byte_MIN_VALUE(NativeFrame frame){
        frame.finishByte(Byte.MIN_VALUE);
    }

    public static void Byte_MAX_VALUE(NativeFrame frame){
        frame.finishByte(Byte.MAX_VALUE);
    }

    // --- Byte instance methods (receiver at slot 0) ---

    public static void Byte_abs(NativeFrame frame){
        byte v = frame.getParentScope().getSlots().getByte(0);
        frame.finishByte((byte)Math.abs(v));
    }

    public static void Byte_clamp(NativeFrame frame, byte minVal, byte maxVal){
        byte v = frame.getParentScope().getSlots().getByte(0);
        frame.finishByte((byte)Math.max(minVal, Math.min(v, maxVal)));
    }

    public static void Byte_toHexString(NativeFrame frame){
        byte v = frame.getParentScope().getSlots().getByte(0);
        frame.finishString(String.format("%02x", v & 0xFF));
    }

    // --- Short toString ---

    public static void Short_toString(NativeFrame frame){
        short v = frame.getParentScope().getSlots().getShort(0);
        frame.finishString(Short.toString(v));
    }

    // --- Short metaclass statics ---

    public static void Short_MIN_VALUE(NativeFrame frame){
        frame.finishShort(Short.MIN_VALUE);
    }

    public static void Short_MAX_VALUE(NativeFrame frame){
        frame.finishShort(Short.MAX_VALUE);
    }

    // --- Short instance methods (receiver at slot 0) ---

    public static void Short_abs(NativeFrame frame){
        short v = frame.getParentScope().getSlots().getShort(0);
        frame.finishShort((short)Math.abs(v));
    }

    public static void Short_clamp(NativeFrame frame, short minVal, short maxVal){
        short v = frame.getParentScope().getSlots().getShort(0);
        frame.finishShort((short)Math.max(minVal, Math.min(v, maxVal)));
    }

    public static void Short_toHexString(NativeFrame frame){
        short v = frame.getParentScope().getSlots().getShort(0);
        frame.finishString(String.format("%04x", v & 0xFFFF));
    }

    // --- Decimal toString ---

    public static void Decimal_toString(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishString(v.toString());
    }

    // --- Decimal metaclass statics ---

    public static void Decimal_ZERO(NativeFrame frame){
        frame.finishDecimal(BigDecimal.ZERO);
    }

    // --- Decimal instance methods (receiver at slot 0) ---

    public static void Decimal_abs(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishDecimal(v.abs());
    }

    public static void Decimal_isFinite(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishBoolean(true);
    }

    public static void Decimal_isInfinite(NativeFrame frame){
        frame.finishBoolean(false);
    }

    public static void Decimal_isNaN(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishBoolean(v.compareTo(BigDecimal.ZERO) == 0 && false);
    }

    public static void Decimal_roundToInt(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishInt(v.intValue());
    }

    public static void Decimal_roundToLong(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishLong(v.longValue());
    }

    public static void Decimal_scale(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishInt(v.scale());
    }

    public static void Decimal_setScale(NativeFrame frame, int newScale){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishDecimal(v.setScale(newScale, RoundingMode.HALF_UP));
    }

    public static void Decimal_compareTo(NativeFrame frame, BigDecimal other){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishInt(v.compareTo(other));
    }

    // --- Decimal extension methods (receiver as first param) ---

    public static void Decimal_abs_ext(NativeFrame frame, BigDecimal d){
        frame.finishDecimal(d.abs());
    }

    public static void Decimal_isFinite_ext(NativeFrame frame, BigDecimal d){
        frame.finishBoolean(true);
    }

    public static void Decimal_isInfinite_ext(NativeFrame frame, BigDecimal d){
        frame.finishBoolean(false);
    }

    public static void Decimal_isNaN_ext(NativeFrame frame, BigDecimal d){
        frame.finishBoolean(d.compareTo(BigDecimal.ZERO) == 0 && false);
    }

    public static void Decimal_roundToInt_ext(NativeFrame frame, BigDecimal d){
        frame.finishInt(d.intValue());
    }

    public static void Decimal_roundToLong_ext(NativeFrame frame, BigDecimal d){
        frame.finishLong(d.longValue());
    }

    public static void Decimal_scale_ext(NativeFrame frame, BigDecimal d){
        frame.finishInt(d.scale());
    }

    public static void Decimal_setScale_ext(NativeFrame frame, BigDecimal d, int newScale){
        frame.finishDecimal(d.setScale(newScale, RoundingMode.HALF_UP));
    }

    public static void Decimal_compareTo_ext(NativeFrame frame, BigDecimal a, BigDecimal b){
        frame.finishInt(a.compareTo(b));
    }

    public static void Decimal_hashCode(NativeFrame frame){
        BigDecimal v = frame.getParentScope().getSlots().getDecimal(0);
        frame.finishInt(v.hashCode());
    }

    public static void Decimal_hashCode_ext(NativeFrame frame, BigDecimal v){
        frame.finishInt(v.hashCode());
    }

    // --- Char metaclass statics ---

    public static void Char_MIN_VALUE(NativeFrame frame){
        frame.finishChar(Character.MIN_VALUE);
    }

    public static void Char_MAX_VALUE(NativeFrame frame){
        frame.finishChar(Character.MAX_VALUE);
    }

    // --- Char instance methods (receiver at slot 0) ---

    public static void Char_isDigit(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishBoolean(Character.isDigit(v));
    }

    public static void Char_isLetter(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishBoolean(Character.isLetter(v));
    }

    public static void Char_isWhitespace(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishBoolean(Character.isWhitespace(v));
    }

    public static void Char_isUpperCase(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishBoolean(Character.isUpperCase(v));
    }

    public static void Char_isLowerCase(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishBoolean(Character.isLowerCase(v));
    }

    public static void Char_toLowerCaseChar(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishChar(Character.toLowerCase(v));
    }

    public static void Char_toUpperCaseChar(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishChar(Character.toUpperCase(v));
    }

    public static void Char_digitValue(NativeFrame frame){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishInt(Character.digit(v, 10));
    }

    public static void Char_compareTo(NativeFrame frame, char other){
        char v = frame.getParentScope().getSlots().getChar(0);
        frame.finishInt(v - other);
    }

    // --- Char extension methods (receiver as first param) ---

    public static void Char_isDigit_ext(NativeFrame frame, char c){
        frame.finishBoolean(Character.isDigit(c));
    }

    public static void Char_isLetter_ext(NativeFrame frame, char c){
        frame.finishBoolean(Character.isLetter(c));
    }

    public static void Char_isWhitespace_ext(NativeFrame frame, char c){
        frame.finishBoolean(Character.isWhitespace(c));
    }

    public static void Char_isUpperCase_ext(NativeFrame frame, char c){
        frame.finishBoolean(Character.isUpperCase(c));
    }

    public static void Char_isLowerCase_ext(NativeFrame frame, char c){
        frame.finishBoolean(Character.isLowerCase(c));
    }

    public static void Char_toLowerCaseChar_ext(NativeFrame frame, char c){
        frame.finishChar(Character.toLowerCase(c));
    }

    public static void Char_toUpperCaseChar_ext(NativeFrame frame, char c){
        frame.finishChar(Character.toUpperCase(c));
    }

    public static void Char_digitValue_ext(NativeFrame frame, char c){
        frame.finishInt(Character.digit(c, 10));
    }
}
