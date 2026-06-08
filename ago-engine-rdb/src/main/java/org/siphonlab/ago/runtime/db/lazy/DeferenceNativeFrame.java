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
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeferenceNativeFrame<F extends AgoFunction, Id> extends NativeFrame implements DeferenceCallFrame<F, Id>, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final DbAdapter<Id> adapter;

    private final DeferenceFrameState state;

    public DeferenceNativeFrame(DbSlots<Id> slots, AgoNativeFunction agoFunction, DbEngine<Id> engine, RunSpace runSpace) {
        super(engine, slots, agoFunction);
        this.adapter = engine.getDbAdapter();

        var inst = (ObjectRefCallFrame<F, Id>) engine.createObjectRefInstance(this.getObjectRef(), runSpace);
        this.state = new DeferenceFrameState(inst);
        inst.setDeferencedInstance(this);
    }

    @Override
    public ObjectRefCallFrame<F, Id> toObjectRefInstance() {
        return (ObjectRefCallFrame<F, Id>) state.getObjectRefObject();
    }

    @Override
    public boolean isSaveRequired() {
        return state.isSaveRequired();
    }

    @Override
    public void markSaved() {
        state.markSaved();
    }

    @Override
    public String toString() {
        return "(DeferenceNativeFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        if(caller instanceof DeferenceCallFrame deferenceCallFrame){
            caller = deferenceCallFrame.toObjectRefInstance();
        }
        if (ObjectRefOwner.equals(caller, this.getCaller())) return;

        super.setCaller(caller);
        this.state.setSaveRequired();
    }

    @Override
    public void setParentScope(Instance<?> parentScope) {
        super.setParentScope(parentScope);
        state.setSaveRequired();
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        super.setRunSpace(runSpace);
        state.setSaveRequired();
    }

    public ObjectRef<Id> getObjectRef() {
        return ((DbSlots<Id>)slots).getObjectRef();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeferenceAgoFrame deferenceAgoFrame) {
            return this.getObjectRef().equals(deferenceAgoFrame.getObjectRef());
        } else if (obj instanceof ObjectRefObject objectRefObject) {
            return this.getObjectRef().equals(objectRefObject.getObjectRef());
        } else {
            return false;
        }
    }

    @Override
    public void setNativePayload(Object payload) {
        super.setNativePayload(payload);
        // ((RdbEngine)this.engine).getRdbAdapter().saveInstance(this);
    }

    @Override
    public DeferenceObjectState getDeferenceObjectState() {
        return state;
    }

    @Override
    public DeferenceFrameState getDeferenceFrameState() {
        return state;
    }
}
