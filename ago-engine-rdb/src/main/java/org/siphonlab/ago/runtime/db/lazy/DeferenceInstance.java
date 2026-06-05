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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeferenceInstance<T extends AgoClass, Id> extends Instance<T> implements DeferenceObject<Id>, ObjectRefOwner {
    private static final Logger logger = LoggerFactory.getLogger(DeferenceInstance.class);

    private final DbAdapter<Id> adapter;

    private final DeferenceObjectState state;

    public DeferenceInstance(DbSlots<Id> slots, T agoClass, DbAdapter<Id> adapter) {
        super(slots, agoClass);

        slots.setOwner(this);
        this.adapter = adapter;

        ObjectRefInstance<T, Id> inst = (ObjectRefInstance<T, Id>) adapter.getById(getObjectRef());
        inst.setDeferencedInstance(this);
        this.state = new DeferenceObjectState(inst);
    }

    @Override
    public ObjectRef<Id> getObjectRef() {
        return ((DbSlots<Id>)slots).getObjectRef();
    }

    @Override
    public void setParentScope(Instance<?> parentScope) {
        super.setParentScope(parentScope);
        state.setSaveRequired();
    }

    @Override
    public String toString() {
        return "(DeferenceInstance %s)".formatted(this.getObjectRef());
    }

    @Override
    public ObjectRefInstance<T, Id> toObjectRefInstance() {
        return (ObjectRefInstance<T, Id>) state.getObjectRefObject();
    }

    public boolean isSaveRequired() {
        return state.isSaveRequired();
    }

    public void markSaved() {
        state.markSaved();
    }


    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }

    @Override
    public DeferenceObjectState getDeferenceObjectState() {
        return state;
    }

}
