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
package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class LiteralTests {

    @Test
    public void list() throws CompilationError, IOException {
        run("literal/list.ago");
        assertTrue(Trace.outputted("1", "2", "3", "5", "7", "9", "101", "102", "103", "22", "33", "44"));
    }

    @Test
    public void list_op() throws CompilationError, IOException {
        run("literal/list_op.ago");
//        Trace.printOutput();
        Trace.outputted("[1,8,7,4]", "[ArrayList<int>|1,8,7,4,9,8,7,6]", "[1,8,7,4,9,8,7,6]", "[LinkedList<int>|1,8,7,4,9,8,7,6,1,8,7,4,9,8,7,6]", "[0,0,1,8,7,0,0,0,0,0]", "[0,0,1,8,7,1,8,7,4,9]", "[1,8,7,4,9,8,7,6,4,9]");
    }

    @Test
    public void object() throws CompilationError, IOException {
        run("literal/object.ago");
        assertTrue(Trace.outputted("name: Tom  gender: M", "name: Tom  gender: M  workNo: 1001 salary: 10200", "name: Mike  gender: M  workNo: 1003 salary: 13332"));
    }

    @Test
    public void map() throws CompilationError, IOException {
        run("literal/map.ago");
        assertTrue(Trace.outputted("HashMap<string,lang.Object>", "Tom", "Jenny", "42", "M", "51888222"));
    }


}
