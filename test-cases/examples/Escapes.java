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

public class Escapes{

    void f(){
        String s1 = "-\u005ct-"; // -    -
        String s2 = "-\u005c"-"; // -"-
        String s3 = "-\u005c\-"; // -\-
        String s4 = "-\u005c'-"; // -'-
        String s5 = "\u005c"";   // -"-

        String s6 = "\u005c101"; //A
        String s7 = "\u005c043"; //#
        String s8 = "\u005c0"; // nul

        char c1 = '\u005cn'; // \n
        char c2 = '\u005c''; // \'
        char c3 = '\u005c"'; // \"
        char c4 = '\u005c\'; // \\
    }
}
