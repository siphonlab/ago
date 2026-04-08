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
package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.classloader.ClassRefValue;

import java.io.IOException;

public class ClassRefValueSerializer extends JsonSerializer<ClassRefValue> {

    @Override
    public void serialize(ClassRefValue classRefValue, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(classRefValue.className());
    }
}
