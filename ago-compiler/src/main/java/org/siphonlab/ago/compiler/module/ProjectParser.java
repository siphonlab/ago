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
import org.siphonlab.ago.module.ModuleParser;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ProjectParser extends ModuleParser {

    private File baseDirectory;

    public ProjectParser() {}

    public Project parse(String filename) throws IOException {
        return (Project) super.parse(filename);
    }

    public Project parse(File file) throws IOException {
        this.baseDirectory = file.getParentFile();
        return (Project) super.parse(file);
    }

    public Project parse(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(inputStream);
        var project = new Project();
        Map<String, String> module = (Map<String, String>) map.get("module");
        project.setName(module.get("name"));
        project.setVersion(new Semver(module.get("version")));
        project.setAuthor(module.get("author"));
        project.setLicense(module.get("license"));

        Map<String,String> repository = (Map<String, String>) map.get("repository");
        project.setRepositoryUrl(repository.get("url"));

        List<String> units = (List<String>) map.get("units");
        processUnits(project, units);

        return project;
    }

    // for directory, it's files, for zip, it's zip entry
    protected void processUnits(Project project, List<String> files) throws IOException {
        if(files != null) {
            UnitSource[] arr = new UnitSource[files.size()];
            int i = 0;
            for (String s : files) {
                UnitSource unitSource = new UnitSource(s, new FileReader(new File(baseDirectory, s), StandardCharsets.UTF_8));
                arr[i++]= (unitSource);
            }
            project.loadUnits(arr);
        }
    }

}
