package org.siphonlab.ago.lang;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;

import java.time.Instant;
import java.util.Date;

public class IO {

    public static void print_str(NativeFrame frame, String text){
        System.err.println(text);
        frame.finishVoid();
    }

    public static void print_int(NativeFrame frame, int number){
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_long(NativeFrame frame, long number){
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_double(NativeFrame frame, double number){
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_classref(NativeFrame frame, int classRef){
        System.err.println(classRef);
        frame.finishVoid();
    }

}
