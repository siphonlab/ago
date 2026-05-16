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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Disabled;
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

@Disabled
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

    @Test
    public void taskTesting() throws Exception {
        // this testcase need run twice,
        // but the first use System.exit() to shutdown the whole server to simulate unexpected shutdown
        // it will make junit terminate, so need run it manually now
        var flag = new File("output/workflow/task_flag");
        if(!flag.exists()) {
            if (Util.parseEngine() == Util.RunEngine.TaskEngine) {
                Util.applicationId = RandomUtils.insecure().randomInt();
                if(!flag.getParentFile().exists()) flag.getParentFile().mkdirs();
                FileUtils.write(flag, String.valueOf(Util.applicationId), StandardCharsets.UTF_8);
                Util.run("workflow/task.ago");
            }
        }  else {
            Util.applicationId = Integer.parseInt(FileUtils.readFileToString(flag, StandardCharsets.UTF_8));
            flag.delete();
            Util.resumeWithTask();
            Thread.sleep(2000);
        }
    }
}
