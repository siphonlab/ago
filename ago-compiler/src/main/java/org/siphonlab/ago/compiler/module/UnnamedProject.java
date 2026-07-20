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
package org.siphonlab.ago.compiler.module;

import org.semver4j.Semver;
import org.siphonlab.ago.compiler.UnitSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UnnamedProject extends Project {

    public UnnamedProject(File... sources) throws IOException {
        this.setName("unnamed-" + UUID.randomUUID());
        this.setVersion(new Semver("0.1.0"));
        this.setRepositoryUrl("<local>");
        List<UnitSource> list = new ArrayList<>();
        for (File f : sources) {
            UnitSource unitSource = new UnitSource(f.getName(), new FileReader(f));
            list.add(unitSource);
        }
        this.appendUnits(list.toArray(new UnitSource[0]));
    }


}
