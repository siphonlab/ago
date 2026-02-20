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

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericTest {

    @Test
    public void intermedia_test() throws CompilationError, IOException {
        run("generic/intermedia.ago");
        assertTrue(Trace.outputted("Samoyed"));
    }

    @Test
    public void metaclass_test() throws CompilationError, IOException {
        run("generic/metaclass.ago");
        assertTrue(Trace.outputted("Samoyed"));
    }

    @Test
    public void tuple_test() throws CompilationError, IOException {
        run("generic/tuple.ago");
        assertTrue(Trace.outputted("CatLeft", "meow", "woof", "CatRight", "woof", "meow", "CatDog", "meow", "woof", "DogCat", "woof", "meow", "Last", "meow", "woof"));
    }

    @Test
    public void triple_test() throws CompilationError, IOException {
        run("generic/triple.ago");
        assertTrue(Trace.outputted("Triple", "woof", "meow", "tweet", "DogXX", "woof", "meow", "tweet", "XDogX", "meow", "woof", "tweet", "XXDog", "meow", "tweet", "woof", "CatXDog", "meow", "tweet", "woof", "CatBirdDog", "meow", "tweet", "woof"));
    }

    @Test
    public void inner_test() throws CompilationError, IOException {
        run("generic/inner.ago");
        assertTrue(Trace.outputted("meow", "meow", "tweet", "meow", "meow", "tweet"));
    }

    @Test
    public void inner2_test() throws CompilationError, IOException {
        run("generic/inner2.ago");
        assertTrue(Trace.outputted("meow", "tweet", "meow", "tweet"));
    }

    @Test
    public void class_constructor_test() throws CompilationError, IOException {
        run("generic/class_constructor.ago");
        assertTrue(Trace.outputted("Samoyed", "Samoyed", "Samoyed"));
    }

    @Test
    public void function_test() throws CompilationError, IOException {
        run("generic/function.ago");
        assertTrue(Trace.outputted("1", "woof", "Samoyed"));
    }

    @Test
    public void numbers_test() throws CompilationError, IOException {
        run("generic/numbers.ago");
        assertTrue(Trace.outputted("3", "10.1", "6.28", "4", "1", "-8", "now i is 0", "eq false", "neq true", "gt false", "ge false", "lt true", "le true", "now i is 1", "eq true", "neq false", "gt false", "ge true", "lt false", "le true"));
    }

}
