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
package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

public class DeferenceNativeFrame extends NativeFrame implements DeferenceCallFrame, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final RdbAdapter adapter;

    private final DeferenceFrameState state;

    public DeferenceNativeFrame(LazyJsonRefSlots slots, AgoNativeFunction agoFunction, RdbEngine engine) {
        super(engine, slots, agoFunction);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = (ObjectRefCallFrame) adapter.restoreInstance(slots.getObjectRef());
        this.state = new DeferenceFrameState(inst);
        inst.setDeferencedInstance(this);
    }

    @Override
    public ObjectRefObject toObjectRefInstance() {
        return state.getObjectRefInstance();
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
        if (ObjectRefOwner.equals(caller, this.caller)) return;
        if (this.caller != null) {
            ReferenceCounter.releaseRef(this.caller, Reason.SetCallerDrop, this);
        }
        var c = toObjectRefCallFrame(caller);
        super.setCaller(c);
        ReferenceCounter.increaseRef(c, Reason.SetCallerInstall, this);
        state.setSaveRequired();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope);
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall, this);
        state.setSaveRequired();
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        super.setRunSpace(runSpace);
        state.setSaveRequired();
    }

    public ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots) this.slots).getObjectRef();
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
    public void setPayload(Object payload) {
        super.setPayload(payload);
        ((RdbEngine)this.engine).getRdbAdapter().saveInstance(this);
    }

    public void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots)this.slots, reason);
    }

    public void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
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
