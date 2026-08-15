package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class CollectionTest {

    @Test
    public void myHashMap() throws CompilationError, CompilationErrorsException, IOException {
        run("collection/MyHashMapTest.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("0", "2", "Alice", "Bob", "Robert", "true", "false", "true", "1", "2", "true", "Engineer", "Senior Engineer", "2", "2", "2", "1=Alice", "2", "2", "0", "false", "3", "10", "20", "30", "true", "false", "1", "false", "1", "20", "v0", "v19", "v10", "50", "big49", "big25", "caught: key not found: 2222", "big0"));
    }

    @Test
    public void myArrayList() throws CompilationError, CompilationErrorsException, IOException {
        run("collection/MyArrayListTest.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("0", "3", "Alice", "Bob", "Charlie", "true", "true", "false", "Robert", "true", "2", "Robert", "Alice", "Robert", "3", "20", "true", "2", "100", "99", "50", "3", "z", "0", "3", "2", "caught: index 999 out of bounds for size 2"));
    }

    @Test
    public void myLinkedList() throws CompilationError, CompilationErrorsException, IOException {
        run("collection/MyLinkedListTest.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("0", "3", "Alice", "Bob", "Charlie", "true", "true", "false", "Robert", "true", "2", "Robert", "Alice", "Robert", "3", "20", "true", "2", "100", "99", "50", "3", "z", "0", "3", "2", "caught: index 999 out of bounds for size 2"));
    }

    @Test
    public void linkedHashMap() throws CompilationError, CompilationErrorsException, IOException {
        run("collection/LinkedHashMapTest.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("0", "3", "Alice", "Bob", "Charlie", "Robert", "3=Alice", "1=Robert", "2=Charlie", "true", "false", "true", "2", "3=Alice", "2=Charlie", "2", "true", "Engineer", "Senior Engineer", "2", "2", "3", "3", "3", "0", "false", "3", "z=30", "x=10", "y=20", "true", "false", "2", "false", "1", "20", "v0", "v19", "v10", "50", "big49", "big25", "0", "caught: key not found: 2222", "big0"));
    }

    @Test
    public void nativeList() throws CompilationError, CompilationErrorsException, IOException {
        run("collection/NativeListTest.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("0", "3", "Alice", "Bob", "Charlie", "true", "true", "false", "Robert", "true", "2", "Robert", "Alice", "Robert", "3", "20", "true", "true", "2", "100", "99", "50", "0", "3", "2", "false"));
    }
}
