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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.ParameterizedClassInfo;
import org.siphonlab.ago.Variance;

public record GenericParameterInfo(AgoClass lBound, AgoClass uBound, Variance variance) {

    public static GenericParameterInfo extract(AgoClass genericParameter) {
        ParameterizedClassInfo p = (ParameterizedClassInfo) genericParameter.getConcreteTypeInfo();
        assert p.getParameterizedBaseClass().getFullname().equals("lang.GenericTypeParameter");

        var args = p.getArguments();
        return new GenericParameterInfo((AgoClass) args[0], (AgoClass) args[1], Variance.of((Byte) args[2]));

    }
}
