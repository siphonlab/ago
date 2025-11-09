package org.siphonlab.ago.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class WorkflowTest {

    @Test
    public void restartAndResume() throws Exception {
        // this testcase need run twice,
        // but the first use System.exit() to shutdown the whole server to simulate unexpected shutdown
        // it will make junit terminate, so need run it manually now
        var flag = new File("output/workflow/restart_resume_flag");
        if(!flag.exists()) {
            if (Util.parseEngine() == Util.RunEngine.PGJsonLazyEngine) {
                Util.applicationId = RandomUtils.insecure().randomInt();
                if(!flag.getParentFile().exists()) flag.getParentFile().mkdirs();
                FileUtils.write(flag, String.valueOf(Util.applicationId), StandardCharsets.UTF_8);
                Util.run("workflow/restart_resume.ago");
            }
        }  else {
            Util.applicationId = Integer.parseInt(FileUtils.readFileToString(flag, StandardCharsets.UTF_8));
            flag.delete();
            Util.resumeWithPGJsonLazy();
            Thread.sleep(2000);
        }
    }



}
