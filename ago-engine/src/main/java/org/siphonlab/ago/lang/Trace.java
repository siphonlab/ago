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


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.native_.NativeFrame;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class Trace {

    private static List<String> lines = new CopyOnWriteArrayList<>();

    public static void print_str(NativeFrame frame, Object text){
        if(text == null){
            lines.add("null");
            System.err.println("null");
        } else {
            lines.addAll(IOUtils.readLines(new StringReader((String)text)));
            System.err.println(text);
        }
        frame.finishVoid();
    }

    public static void print_int(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_long(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_double(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_decimal(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_float(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_byte(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_short(NativeFrame frame, Object number){
        lines.add(String.valueOf(number));
        System.err.println(number);
        frame.finishVoid();
    }

    public static void print_char(NativeFrame frame, Object c){
        lines.add(String.valueOf(c));
        System.err.println(c);
        frame.finishVoid();
    }

    public static void print_classref(NativeFrame frame, Object classRef){
        if(classRef == null) {
            print_str(frame, "null");
            return;
        }
        AgoClass agoClass = frame.getAgoEngine().getClass((Integer) classRef);
        lines.add(String.valueOf( agoClass));
        System.err.println(agoClass);
        frame.finishVoid();
    }

    public static void print_any(NativeFrame frame, Object value){
        lines.add(String.valueOf(value));
        System.err.println(value);
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

    public static void clear() {
        lines.clear();
    }
}
