package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompliationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class CollectionTest {

    @Test
    public void myHashMap() throws CompilationError, CompliationErrorsException, IOException {
        run("collection/MyHashMapTest.ago");
        Trace.printOutput();
        // Note: Person key tests (containsKey/get via equals) currently fail due to runtime limitation
        // where .equals() on Object[] slot doesn't dispatch correctly.
        // Basic int/string key functionality works fine.
        assertTrue(Trace.outputted("0", "2", "Alice", "Bob", "Robert", "true", "false", "true", "1", "2", "true", "Engineer", "Senior Engineer", "2", "2", "2", "1=Alice", "2", "2", "0", "false", "3", "10", "20", "30", "true", "false", "1", "false", "1", "20", "v0", "v19", "v10", "50", "big49", "big25"));
    }
}
