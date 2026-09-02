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
package org.siphonlab.ago.runtime.db.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectRefInstance<T extends AgoClass, Id> extends Instance<T> implements ObjectRefObject<Id> {
    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);

    DbAdapter<Id> dereferenceAdapter;

    ObjectRef<Id> objectRef;
    RunSpace runSpace;

    private Instance<?> deferencedInstance;

    public ObjectRefInstance(T agoClass, ObjectRef<Id> objectRef) {
        super(agoClass);
        this.objectRef = objectRef;
    }

    @Override
    public void bindDereferenceContext(RunSpace runSpace, DbAdapter<Id> dereferenceAdapter) {
        this.runSpace = runSpace;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public Instance<?> dereference() {
        return dereference(deferencedInstance, this.dereferenceAdapter, this.objectRef, runSpace);
    }

    @Override
    public Slots getSlots() {
        return dereference().getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return dereference().getParentScope();
    }

    @Override
    public void setParentScope(Instance<?> parentScope) {
        dereference().setParentScope(parentScope);
    }

    public int hashCode() {
        return getObjectRef().hashCode();
    }

    @Override
    public String toString() {
        return "(ObjectRefInstance %s)".formatted(this.getObjectRef());
    }

    public ObjectRef<Id> getObjectRef() {
        return objectRef;
    }

    public Instance<?> getDeferencedInstance() {
        return deferencedInstance;
    }


    public void setDereferencedInstance(Instance<?> inst) {
        this.deferencedInstance = inst;
    }

    @Override
    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }
}

