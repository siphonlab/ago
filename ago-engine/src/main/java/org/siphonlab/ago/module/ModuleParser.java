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
package org.siphonlab.ago.module;

import org.semver4j.Semver;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ModuleParser {


    public Module parse(String filename) throws IOException {
        return parse(new File(filename));
    }

    public Module parse(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(inputStream);
        var module = new Module();
        Map<String, String> info = (Map<String, String>) map.get("module");
        module.setName(info.get("name"));
        module.setVersion(new Semver(info.get("version")));
        module.setAuthor(info.get("author"));
        module.setLicense(info.get("license"));

        Map<String,String> repository = (Map<String, String>) map.get("repository");
        module.setRepositoryUrl(repository.get("url"));

        List<String> units = (List<String>) map.get("units");
        processUnits(module, units);

        return module;
    }

    // for directory, it's files, for zip, it's zip entry
    protected void processUnits(Module module, List<String> files) throws IOException {
        //
    }

    public Module parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }
}
