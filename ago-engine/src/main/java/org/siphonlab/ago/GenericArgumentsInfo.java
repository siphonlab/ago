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
package org.siphonlab.ago;

public class GenericArgumentsInfo extends ConcreteTypeInfo{
    private AgoClass templateClass;

    private TypeInfo[] arguments;

    public GenericArgumentsInfo(AgoClass templateClass, TypeInfo[] arguments) {
        super();
        this.templateClass = templateClass;
        this.arguments = arguments;
    }

    public AgoClass getTemplateClass() {
        return templateClass;
    }

    public void setTemplateClass(AgoClass templateClass) {
        this.templateClass = templateClass;
    }

    public TypeInfo[] getArguments() {
        return arguments;
    }

    public void setArguments(TypeInfo[] arguments) {
        this.arguments = arguments;
    }
}
