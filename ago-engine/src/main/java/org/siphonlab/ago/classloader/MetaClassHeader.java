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

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.*;

import java.util.Map;

import static org.siphonlab.ago.classloader.LoadingStage.BuildClass;

public class MetaClassHeader extends ClassHeader {
    String[] dependencies;
    ClassHeader instanceClass;

    public MetaClassHeader(String fullname, byte type, int modifiers, IoBuffer slice, AgoClassLoader classLoader) {
        super(fullname, type, modifiers, slice, classLoader);
    }

    public void setDependencies(String[] dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public boolean isInGenericTemplate(Map<String, ClassHeader> headers) {
        if(super.isInGenericTemplate(headers)) return true;
        if(this.instanceClass != null){
            return this.instanceClass.isInGenericTemplate(headers);
        }
        return false;
    }

    @Override
    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        return this.instanceClass.isAffectedBy(headers, genericTypeArguments);
    }

    public ClassHeader getInstanceClass() {
        return instanceClass;
    }

    public void setInstanceClass(ClassHeader instanceClass) {
        this.instanceClass = instanceClass;
    }

    public MetaClassHeader instantiateMetaClass(ClassHeader newParent, String fullname, Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        var inst = new MetaClassHeader(fullname, this.type, this.modifiers, this.getSlice().slice(), classLoader);
        classLoader.registerNewClass(inst);
        inst.setDependencies(this.dependencies);
        //inst.instanceClass = this.instanceClass;  set instanceClass outside
        applyInstantiation(inst, genericTypeArguments, newParent, headers);
        this.registerGenericInstantiationClass(genericTypeArguments, inst);
        return inst;
    }

    @Override
    public ClassHeader clone(ClassHeader newParent, Map<String, ClassHeader> headers) {
        if (!this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = headers.get(fullname);
        if (existed != null) return existed;

        var inst = new MetaClassHeader(fullname, this.type, this.modifiers, this.getSlice() != null ? this.getSlice().slice() : null, this.classLoader);
        inst.setClassId(headers.size());
        copyToClone(inst, headers);
        inst.parent = newParent;
        classLoader.registerNewClass(inst);
        return inst;
    }

    @Override
    public AgoClass buildClass(Map<String, ClassHeader> headers) {
        if(this.loadingStage != BuildClass) return this.agoClass;

//        for (String dependency : this.dependencies) {
//            ClassHeader dependencyHeader = headers.get(dependency);
//            assert dependencyHeader != null;
//            if(dependencyHeader.loadingStage == BuildClass) {
//                System.out.println("build dependency %s for %s".formatted(dependencyHeader.fullname, this.fullname));
//                if(dependencyHeader.buildClass(headers) == null){
//                    return null;
//                }
//            }
//        }
        return super.buildClass(headers);
    }

    @Override
    public void setLoadingStage(LoadingStage loadingStage) {
        super.setLoadingStage(loadingStage);
    }
}
