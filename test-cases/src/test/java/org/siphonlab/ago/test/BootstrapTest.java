package org.siphonlab.ago.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

public class BootstrapTest {

    @Test
    public void hello_world() throws IOException, CompilationError {
        Util.run("bootstrap/hello_world.ago");
        assertTrue(Trace.outputted("hello world"));
    }

    @Test
    public void add() throws IOException, CompilationError {
        Util.run("bootstrap/0.add.ago");
        assertTrue(Trace.outputted("13", "22"));
    }

    @Test
    public void Object_getClass() throws CompilationError, IOException {
        Util.run("bootstrap/base_types.ago", "Meta@<Test>.main#");
//        Util.run("bootstrap/base_types.ago", "main#");
        assertTrue(Trace.startsWith("Object@"));        // Object@hashCode
    }

    @Test
    public void inherits_sample() throws CompilationError, IOException {
        Util.run("bootstrap/1.inherits.ago");
        assertTrue(Trace.outputted("Brand: Toyota", "Year: 2022", "Color: Blue", "Doors: 4", "Toyota"));
    }

    @Test
    public void static_instance_sample() throws CompilationError, IOException {
        Util.run("bootstrap/2.static_instance.ago");
        assertTrue(Trace.outputted("这是一个类方法", "类变量的值是: 10", "这是一个类方法", "类变量的值是: 20", "这是静态内部类的方法", "静态变量的值是: 20"));
    }

    @Test
    public void pronoun_sample() throws CompilationError, IOException {
        Util.run("bootstrap/3.pronoun.ago", "my.test.Meta@<B>.main#");
        assertTrue(Trace.outputted("2", "1", "4", "2029", "2029", "4", "2033", "2", "1", "4", "2029", "2029", "5", "2034"));
    }

    @Test
    public void class_of_scope() throws CompilationError, IOException {
        Util.run("scope/1.ago");
        assertTrue(Trace.outputted("AA", "AAA"));
    }

    @Test
    public void polymorphism() throws CompilationError, IOException {
        Util.run("bootstrap/4.polymorphism.ago");
        assertTrue(Trace.outputted(
                "new 1 new0", "new 1 Abcd", "new 1 AA new", "new 1 AA new", "new 1 AA new",
                "AA set to OVERRIDE", "new 1 AA new", "AA set to abcd to aa",
                "abcd to a", "set b bb", "bb", "a", "b", "aa", "bb"));
    }

    @Test
    public void metaclass() throws CompilationError, IOException {
        Util.run("bootstrap/5.metaclass.ago");
        assertTrue(Trace.outputted("class A", "class Meta@<A>", "class Meta<<Meta>>", "class Meta<<Meta>>"));
    }

    @Test
    public void array_sample() throws CompilationError, IOException {
        Util.run("bootstrap/6.array.ago");
        assertTrue(Trace.outputted("arr1.length: 10", "1", "3", "6", "115", "1", "2", "3", "4", "5"));
    }

    @Test
    public void class_ref() throws CompilationError, IOException {
        Util.run("bootstrap/7.class_ref.ago");
        assertTrue(Trace.outputted("Cat"));
    }

    @Test
    public void box_unbox() throws CompilationError, IOException {
        Util.run("bootstrap/8.box_unbox.ago");
        assertTrue(Trace.outputtedMatch("30", "40", "Dog"));
    }

    @Test
    public void parameterized_class() throws CompilationError, IOException {
        Util.run("bootstrap/9.parameterized_class.ago");
        assertTrue(Trace.outputted("200", "广东省广州市天河区体育西路101号", "13812345678"));
    }

    @Test
    public void class_interval() throws CompilationError, IOException {
        Util.run("bootstrap/10.class_interval.ago");
        assertTrue(Trace.outputted("Cat.foo", "Animal.foo", "meow", "true", "true"));
    }

    @Test
    public void generic() throws CompilationError, IOException {
        Util.run("bootstrap/11.generictype.ago");
        assertTrue(Trace.outputted("meow", "meow"));
    }

    @Test
    public void variance() throws CompilationError, IOException {
        Util.run("bootstrap/12.variance.ago");
        // no output
    }

    @Test
    public void interface_test() throws CompilationError, IOException {
        Util.run("bootstrap/13.interface.ago");
        assertTrue(Trace.outputted("Duck says quack!", "Duck is eating.", "Duck is flying.", "Duck is swimming.", "Duck is swimming.", "Duck says quack!"));
    }

    @Test
    public void callback_test() throws CompilationError, IOException {
        Util.run("bootstrap/14.callback.ago");
        assertTrue(Trace.outputted("3", "6", "103", "106", "114", "160"));
    }

    @Test
    public void initial_block_test() throws CompilationError, IOException {
        Util.run("bootstrap/15.initial_block.ago");
        assertTrue(Trace.outputted("Tom", "22"));
    }

    @Test
    public void wrapper_test() throws CompilationError, IOException {
        Util.run("bootstrap/16.wrapper.ago");
        assertTrue(Trace.outputted("INFO: hehe"));
    }

    @Test
    public void trait_test() throws CompilationError, IOException {
        Util.run("bootstrap/17.trait.ago");
        assertTrue(Trace.outputted("INFO: I can log too", "set log level INFO", "INFO: Creating user: Alice", "DEBUG", "INFO: Detailed user creation process for Alice", "set log level DEBUG", "DEBUG: Creating user: Bob", "DEBUG", "DEBUG: Detailed user creation process for Bob"));
    }

    @Test
    public void getter_setter_test() throws CompilationError, IOException {
        Util.run("bootstrap/18.getter_setter.ago");
        assertTrue(Trace.outputted("Jack", "22", "set age to 30", "Tom", "22", "M"));
    }

    @Test
    public void boxer_test() throws CompilationError, IOException {
        Util.run("bootstrap/20.boxer.ago");
        assertTrue(Trace.outputted("name:Tom", "str:Tom", "v:Tom"));
    }

    @Test
    public void field_test() throws CompilationError, IOException {
        Util.run("bootstrap/field.ago");
        assertTrue(Trace.outputted("John", "12822223333", "street freedom", "bar", "Contact name: John",
                "Contact phone: 12822223333", "Contact address: street freedom", "Foo: bar", "not bar now"));
    }

    @Test
    public void enum_test() throws CompilationError, IOException {
        Util.run("bootstrap/21.enum.ago");
        assertTrue(Trace.outputted("Today is 2", "Wednesday's value is 2", "Thursday", "Day 4 is 4", "Status 1", "Status value is 1"));
    }

    @Test
    public void const_test() throws CompilationError, IOException {
        Util.run("bootstrap/22.const.ago");
        assertTrue(Trace.outputted("20", "20", "星期五：周末快到了，放松一下！"));
    }

    @Test @Tag("generic")
    public void varargs_test() throws CompilationError, IOException {
        Util.run("bootstrap/23.var_args.ago");
        assertTrue(Trace.outputted("f#1", "1", "2", "3", "f#2", "1.0", "2", "4", "meow", "woof"));
    }

    @Test
    public void runspace_test() throws CompilationError, IOException {
        Util.run("bootstrap/24.runspace.ago");
        assertTrue(Trace.outputted("test", "open the door", "put the elephant in", "close the door"));
    }

    @Test
    public void iterable_test() throws CompilationError, IOException {
        Util.run("bootstrap/25.iterable.ago");
        assertTrue(Trace.outputted("1", "3", "5", "7", "9", "0", "2", "4", "6", "8"));
    }

}
