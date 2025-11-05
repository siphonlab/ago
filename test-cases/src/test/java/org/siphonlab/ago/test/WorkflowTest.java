package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.io.IOException;

public class WorkflowTest {
    @Test
    public void restartAndResume() throws CompilationError, IOException {
        if (Util.parseEngine() == Util.RunEngine.PGJsonLazyEngine) {
            Util.run("workflow/restart_resume.ago");
        }
    }
}
