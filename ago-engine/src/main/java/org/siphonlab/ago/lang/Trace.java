package org.siphonlab.ago.lang;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.native_.NativeFrame;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Trace {

    private static List<String> lines = new LinkedList<>();

    public static void print_str(NativeFrame frame, String text){
        lines.addAll(IOUtils.readLines(new StringReader(text)));

        System.err.println(text);
        frame.finishVoid();
    }

    public static void print_int(NativeFrame frame, int number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_long(NativeFrame frame, long number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_double(NativeFrame frame, double number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_float(NativeFrame frame, float number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_byte(NativeFrame frame, byte number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_short(NativeFrame frame, short number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_char(NativeFrame frame, char c){
        lines.add(String.valueOf(c));
        System.err.println(c);
        frame.finishVoid();
    }

    public static void print_classref(NativeFrame frame, int classRef){
        AgoClass agoClass = frame.getAgoEngine().getClass(classRef);
        lines.add(String.valueOf( agoClass));
        System.err.println(agoClass);
        frame.finishVoid();
    }

    public static boolean outputted(String... expected){
        if(lines.size() == expected.length){
            for (int i = 0; i < expected.length; i++) {
                if(!Objects.equals(lines.get(i), expected[i])){
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static boolean startsWith(String s) {
        return (lines.size() == 1 && lines.get(0).startsWith(s));
    }

    public static void printOutput(){
        System.out.println(lines.stream().map(s -> '"' + s + '"').toList());
    }

    public static boolean outputtedMatch(String... expected) {
        if (lines.size() == expected.length) {
            for (int i = 0; i < expected.length; i++) {
                String regex = expected[i];
                String line = lines.get(i);
                if(regex == null){
                    if (line != null && !StringUtils.isEmpty(line)) {
                        return false;
                    }
                } else if(!regex.equals(line) && !Pattern.matches(regex, lines.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

}
