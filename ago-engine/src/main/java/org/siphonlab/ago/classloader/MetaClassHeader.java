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

import java.util.Arrays;

import static org.siphonlab.ago.classloader.LoadingStage.*;

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
    public boolean isInGenericTemplate() {
        if(super.isInGenericTemplate()) return true;
        if(this.instanceClass != null){
            return this.instanceClass.isInGenericTemplate();
        }
        return false;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments) {
        return this.instanceClass.isAffectedByTypeArguments(typeArguments);
    }

    public ClassHeader getInstanceClass() {
        return instanceClass;
    }

    public void setInstanceClass(ClassHeader instanceClass) {
        this.instanceClass = instanceClass;
    }

    public MetaClassHeader instantiateMetaClass(ClassHeader newParent, String name, String fullname, InstantiationArguments instantiationArguments) {
        assert newParent == null;
        var inst = new MetaClassHeader(fullname, this.type, this.modifiers, this.getSlice().slice(), classLoader);
        inst.setName(name);
        classLoader.registerNewClass(inst);
        inst.setDependencies(this.dependencies);
        //inst.instanceClass = this.instanceClass;  set instanceClass outside
        applyInstantiation(inst, instantiationArguments, newParent);
        this.putInstantiatedClassToCache(instantiationArguments, inst);
        return inst;
    }

    @Override
    public boolean isReady() {
        if(loadingStage == ResolveHierarchicalClasses) {
            if (this.instanceClass == null) return false;
        }
        return super.isReady();
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        if (!this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = classLoader.getClassHeader(fullname);
        if (existed != null) return existed;

        var inst = new MetaClassHeader(fullname, this.type, this.modifiers, this.getSlice() != null ? this.getSlice().slice() : null, this.classLoader);
        inst.setClassId(classLoader.getHeaders().size());
        copyToClone(inst);
        inst.parent = newParent;
        classLoader.registerNewClass(inst);
        return inst;
    }

    @Override
    public AgoClass buildClass() {
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
        return super.buildClass();
    }

    @Override
    public boolean parseFields() {
        if(this.loadingStage != LoadingStage.ParseFields) return true;

        if(this.instanceClass instanceof ArrayTypeHeader){
            var base = classLoader.getClassHeader(this.superClass);
            if(base.loadingStage == LoadingStage.ParseFields){
                if(!base.parseFields()) return false;
            }

            this.fields = Arrays.stream(base.fields).filter(f -> !Modifier.isPrivate(base.modifiers)).toArray(VariableDesc[]::new);
            this.slotDescs = base.slotDescs;
            this.genericSource = base.genericSource;
//            if (this.children != null) {
//                this.setChildren(new ArrayList<>(base.children.stream().map(c -> c.clone(this, headers)).toList()));
//            }
            this.setMethods(base.methods);
            this.setLoadingStage(InstantiateFunctionFamily);
        } else {
            super.parseFields();
        }
        return true;
    }

    @Override
    public void setLoadingStage(LoadingStage loadingStage) {
        super.setLoadingStage(loadingStage);
    }
}
