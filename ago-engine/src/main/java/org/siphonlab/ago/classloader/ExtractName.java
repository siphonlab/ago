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
package org.siphonlab.ago.classloader;

class ExtractName {
    private final String classFullName;
    private int pos = 0;
    private int depth = 0;
    private int start = 0;

    public ExtractName(String classFullName) {
        this.classFullName = classFullName;
    }

    public String extractName() {
        processHead();
        return classFullName.substring(start);
    }

//        protected String extractName() {
//            int length = classFullName.length();
//            int start = 0;
//            int depth = 0;
//            i_loop:
//            for(var i = 0; i< length; i++){
//                char c = classFullName.charAt(i);
//
//                if(c == '.'){
//                    start = i + 1;
//                }
//                for(var j = i + 1; j < length; j++){
//                    char cj = classFullName.charAt(j);
//                    if(cj == '.') {
//                        i = j - 1;
//                        continue i_loop;
//                    } else if(cj == '<'){
//                        depth ++;
//                        for(j++; j < length; j++){
//                            cj = classFullName.charAt(j);
//                            if(cj == '<') depth ++;
//                            if(cj == '>') {
//                                depth --;
//                                if(depth == 0)
//                                    break;        // skip <...>
//                            }
//                        }
//                    }
//                }
//                break;
//            }
//            return classFullName.substring(start);
//        }

    // head part, array, nullable, union, meta
    private void processHead() {
        char c = classFullName.charAt(pos++);
        if (c == '[') {       // array or class interval
            depth++;
            processArrayOrClassInterval();
            depth--;
        } else if (c == '?') {
            depth++;
            tillPair(';');
            depth--;
        } else if (c == '@' && peek() == '|') {
            pos++;
            depth++;
            tillPair(';');
            depth--;
        } else if ((c == '+' || c == '-') && peek() == '[') {     // shared generic type parameter
            pos++;
            depth++;
            tillPair(']');
            depth--;
        }         // normal identifier, till '.'
        normalId();
    }

    private void normalId() {
        while (pos < classFullName.length()) {
            char c = classFullName.charAt(pos++);
            if (c == '.') {
                if (depth == 0) start = pos;
                processHead();      // after '.' is a new head, but some cases may be excluded, leave it, i.e. Meta@<, @|, ?, [array, are all top classes
            } else if (c == '[') {
                depth++;
                tillPair(']');
                depth--;
            } else if (c == '<') {
                depth++;
                tillPair('>');
                depth--;
            } else if (c == '(') {
                depth++;
                tillPair(')');
                depth--;
            } else {
                // skip
            }
        }
    }

    private void processArrayOrClassInterval() {
        char c;
        while (pos < classFullName.length()) {
            c = classFullName.charAt(pos++);
            if (c == '~') {       // class interval
                tillPair(']');
                return;
            } else if (c == ';') {
                return;
            } else if (c == '<') {
                depth++;
                tillPairForHead('>');
                depth--;
            } else if (c == '(') {
                depth++;
                tillPairForHead(')');
                depth--;
            } else {
                // skip
            }
        }
    }

    private void tillPair(char end) {
        while (pos < classFullName.length()) {
            char c = classFullName.charAt(pos++);
            if (c == end) {
                return;
            } else if (c == '[') {
                depth++;
                tillPairForHead(']');
                depth--;
            } else if (c == '<') {
                depth++;
                tillPairForHead('>');
                depth--;
            } else if (c == '(') {
                depth++;
                tillPairForHead(')');
                depth--;
            } else if (c == ',' || c == '|' || c == '/') {
                tillPairForHead(end);
                return;
            } else {
                // skip
            }
        }
    }

    private void tillPairForHead(char end) {
        char c = classFullName.charAt(pos++);
        if (c == '[') {       // array or class interval
            depth++;
            processArrayOrClassInterval();
            depth--;
        } else if (c == '?') {
            depth++;
            tillPair(';');
            depth--;
        } else if (c == '@' && peek() == '|') {
            pos++;
            depth++;
            tillPair(';');
            depth--;
        } else if ((c == '+' || c == '-') && peek() == '[') {     // shared generic type parameter
            pos++;
            depth++;
            tillPair(']');
            depth--;
        }
        tillPair(end);
    }

    private boolean laIs(String s) {
        for (var i = 0; i < s.length(); i++) {
            var p = pos + i;
            if (p < classFullName.length()) {
                if (s.charAt(i) != classFullName.charAt(p)) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private char peek() {
        if (pos < classFullName.length()) return classFullName.charAt(pos);
        return '\0';
    }

    public static void main(String[] args) {
        String s = "lang.Function0<lang.ArrayList<lang.Value_1_lang/HashMap_17|lang/[_~_]>.ListIterator>";
        System.out.println(new ExtractName(s).extractName());

        s = "lang.ArrayIterator<lang.T_0_lang/Array_16|lang/[_~_]>.new#";
        System.out.println(new ExtractName(s).extractName());

        s = "lang.Meta@<Enum>.valueOf#";
        System.out.println(new ExtractName(s).extractName());
    }
}
