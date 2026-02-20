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
