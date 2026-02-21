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

import org.junit.jupiter.api.Disabled;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.parseEngine;
import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ControlTest{

    @Test
    public void if_test() throws CompilationError, IOException {
        run("control/if.ago");
        assertTrue(Trace.outputted("success", "failed"));
    }

    @Test
    public void while_test() throws CompilationError, IOException {
        run("control/while.ago");
        assertTrue(Trace.outputted("sum: 5050"));
    }

    @Test
    public void break_label_test() throws CompilationError, IOException {
        run("control/break_label.ago");
        assertTrue(Trace.outputted("world", "0, 0", "0, 1", "0, 2", "1, 0"));
    }

    @Test
    public void switch_test() throws CompilationError, IOException {
        run("control/switch.ago");
        assertTrue(Trace.outputted("星期四", "星期三"));
    }

    @Test
    public void for_performance_test() throws CompilationError, IOException {
        if(parseEngine().equals("vertx")) {
            run("control/for_performance.ago");
            assertTrue(Trace.outputtedMatch("start", "end at ", "\\d+"));
        }
    }

    @Test
    public void try_catch_test() throws CompilationError, IOException {
        run("control/try_catch.ago");
        assertTrue(Trace.outputted("捕获到自定义异常：值 150 超过允许的最大值 100", "finally 块：总是会执行"));
    }

    @Test @Disabled
    public void try_catch2_test() throws CompilationError, IOException {
        run("control/try_catch2.ago");
        assertTrue(Trace.outputted("捕获到自定义异常：值 150 超过允许的最大值 100", "finally 块：总是会执行", "finally output line 24", "yet another finally block", "outer finally output line 28"));
    }

    @Test
    public void continue_test() throws CompilationError, IOException {
        run("control/continue_label.ago");
        assertTrue(Trace.outputted(
                "test 1", "1", "3", "5", "7", "9", "10", "12", "14", "16", "18",
                "test 2", "0, 0", "0, 1", "0, 2", "1, 0", "2, 0", "2, 1", "2, 2"));
    }

    @Test
    public void for_loop_test() throws CompilationError, IOException {
        run("control/for_loop.ago");
        assertTrue(Trace.outputted("test 1", "0", "1", "2",
                "test 2", "3", "2", "1", "0",
                "test 3", "-1", "0", "1", "2", "3", "4", "5",
                "test 4", "10"));
    }

    @Test
    public void switch_enum() throws CompilationError, IOException {
        run("control/switch_enum.ago");
        assertTrue(Trace.outputted("星期五：周末快到了，放松一下！"));
    }

    @Test
    public void with_test() throws CompilationError, IOException {
        run("control/with.ago");
        assertTrue(Trace.outputted("John", "20", "Tom", "21"));
    }

    @Test
    public void via_test() throws CompilationError, IOException {
        run("control/via.ago");
        assertTrue(Trace.outputted("open file sample.txt", "read file content from sample.txt", "close file sample.txt"));
    }

}
