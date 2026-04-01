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

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoNullClass;

import static org.siphonlab.ago.classloader.LoadingStage.BuildClass;
import static org.siphonlab.ago.classloader.LoadingStage.ResolveFunctionIndex;

public class NullClassHeader extends ClassHeader{

    public NullClassHeader(AgoClassLoader classLoader) {
        super("null", AgoClass.TYPE_CLASS, AgoClass.PUBLIC | AgoClass.FINAL, null, classLoader);
        this.setLoadingStage(LoadingStage.BuildClass);
        this.slotDescs = new SlotDesc[0];
    }

    @Override
    public AgoClass buildClass() {
        if(this.loadingStage != BuildClass) return this.agoClass;
        var agoClass =  new AgoNullClass(classLoader);
        this.agoClass = agoClass;
        classLoader.getClassByName().put(this.fullname, agoClass);
        this.setLoadingStage(LoadingStage.Done);
        return agoClass;
    }
}
