package org.siphonlab.ago.lang;

import org.siphonlab.ago.native_.NativeFrame;

public class StringBuilder {
    /*
        fun new() native "org.siphonlab.ago.lang.StringBuilder.create"

    fun new#str(s as string) native "org.siphonlab.ago.lang.StringBuilder.create";

    fun append#str(s as string) as StringBuilder
        native "org.siphonlab.ago.lang.StringBuilder.append";

    fun append#obj(obj as Object) {
        return append(obj.toString())
    }

    fun toString() native "org.siphonlab.ago.lang.StringBuilder.toString"

     */
    public static void create(NativeFrame frame){
        frame.getParentScope().setNativePayload(new java.lang.StringBuilder());
        frame.finishVoid();
    }

    public static void create(NativeFrame frame, String s){
        frame.getParentScope().setNativePayload(new java.lang.StringBuilder(s));
        frame.finishVoid();
    }

    public static void append(NativeFrame frame, String s){
        java.lang.StringBuilder sb = (java.lang.StringBuilder) frame.getParentScope().getNativePayload();
        sb.append(s);
        frame.finishObject(frame.getParentScope());
    }

    public static void toString(NativeFrame frame){
        java.lang.StringBuilder sb = (java.lang.StringBuilder) frame.getParentScope().getNativePayload();
        frame.finishString(sb.toString());
    }
}
