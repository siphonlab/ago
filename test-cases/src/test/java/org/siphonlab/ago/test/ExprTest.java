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

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExprTest {

    @Test
    public void self_op_test() throws CompilationError, IOException {
        run("expr/self_expr.ago");
        assertTrue(Trace.outputted(
                "i=3 j=2",
                "i=4 j=4",
                "age=23 j=22",
                "age=24 j=24",
                "salary=2001 j=2000",
                "salary=2002 j=2002",
                "salary=4004 j=4004",
                "fav[1]=3 j=2",
                "fav[1]=4 j=4",
                "i=1 j=2",
                "i=0 j=0",
                "age=21 j=22",
                "age=20 j=20",
                "salary=1999 j=2000",
                "salary=1998 j=1998",
                "salary=999 j=999",
                "fav[1]=1 j=2",
                "fav[1]=0 j=0"));
    }

    @Test
    public void logic_op_test() throws CompilationError, IOException {
        run("expr/logic_op.ago");
        assertTrue(Trace.outputted(
                // test or
                "foo got null",
                "bar got animal",
                "bark",

                "bar got animal",
                "bark",

                "foo got null",
                "bar got animal",
                "bark",

                // test and
                "foo got null",
                "null",

                "bar got animal",
                "foo got null",
                "null",

                "foo got null",
                "null",

                // test and literal
                "true",
                "false",
                "false",
                "false",
                // test or literal
                "true",
                "true",
                "true",
                "false",

                // andFun
                "invoke f",
                "false",
                "false",
                "invoke f",
                "false",
                "invoke t",
                "invoke f",
                "false",

                // testComposite
                "invoke t",
                "invoke f",
                "false",
                "invoke t",
                "true",
                "invoke t",
                "invoke f",
                "false",
                "invoke t",
                "true",
                "invoke f",
                "false",
                "invoke t",
                "true",
                "false",
                "invoke t",
                "true"));
    }

    @Test
    public void generic_cond() throws CompilationError, IOException {
        run("expr/generic_cond.ago");
        assertTrue(Trace.outputted("true", "true", "true", "false"));
    }

    @Test
    public void bit_op_test() throws CompilationError, IOException {
        run("expr/bit_op.ago");
        assertTrue(Trace.outputted("t.a  band t.b = 12",
                "t.a  bor t.b = 61",
                "t.a  bxor t.b = 49",
                "bnot t.a  = -61",
                "t.a  << 2 = 240",
                "t.a  >> 2 = 15",
                "t.a  >>> 2 = 15",
                "negative = -60",
                "negative >> 2 = -15",
                "negative >>> 2 = 1073741809",
                "",
                "初始 t.result[1] = 60",
                "t.result[1] band= t.b = 12",
                "t.result[1] bor= t.b = 61",
                "t.result[1] bxor= t.b = 49",
                "t.result[1] <<= 2 = 240",
                "t.result[1] >>= 2 = 15",
                "t.result[1] >>>= 2 = 15",
                "",
                "交换前: t.a  = 60, t.b = 13",
                "交换后: t.a  = 13, t.b = 60"));
    }

    @Test
    public void if_else_test() throws CompilationError, IOException {
        run("expr/if_else.ago");
        assertTrue(Trace.outputted("42 是 偶数",
                "成绩 85 对应等级: 及格",
                "用户名称: 匿名用户",
                "最终价格: 80.0",
                "您已成年，欢迎使用系统！",
                "有效索引: 30",
                "状态: 默认消息"));
    }

    @Test
    public void instanceof_test() throws CompilationError, IOException {
        run("expr/instanceof.ago");
        assertTrue(Trace.outputted("圆的面积: 78.539815", "矩形的面积: 24.0", "未知形状"));
    }

    @Test
    public void generic_instanceof_test() throws CompilationError, IOException {
        run("expr/generic_instanceof.ago");
        assertTrue(Trace.outputted("20"));
    }

    @Test
    public void chain_creator() throws CompilationError, IOException {
        run("expr/chain_creator.ago");
        assertTrue(Trace.outputted("a dog woof", "a dog woof", "Dog name George", "George woof", "a dog yum", "Dog name George", "George yum"));
    }

    @Test
    public void high_prior_cast() throws CompilationError, IOException {
        run("expr/high_prior_cast.ago");
//        assertTrue(Trace.outputted("woof"));
        Trace.printOutput();
    }

}
