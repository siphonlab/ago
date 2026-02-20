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
package org.siphonlab.ago.classloader;

public class MethodDesc{
    private final String name;
    private final String fullname;
    private ClassHeader functionClassHeader;
    private int methodIndex = 1;

    public MethodDesc(String name, String fullname) {
        this.name = name;
        this.fullname = fullname;
    }

    public ClassHeader getFunctionClassHeader() {
        return functionClassHeader;
    }

    public void setFunctionClassHeader(ClassHeader functionClassHeader) {
        this.functionClassHeader = functionClassHeader;
    }

    public int getMethodIndex() {
        return methodIndex;
    }

    public void setMethodIndex(int methodIndex) {
        this.methodIndex = methodIndex;
    }

    public String getName() {
        return name;
    }

    public String getFullname() {
        return fullname;
    }

    @Override
    public String toString() {
        return "(MethodDesc %d %s %s)".formatted(methodIndex, name, fullname);
    }
}
