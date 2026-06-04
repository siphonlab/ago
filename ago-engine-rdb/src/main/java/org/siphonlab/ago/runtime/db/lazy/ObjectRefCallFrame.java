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
import org.siphonlab.ago.runtime.db.DbSlotsCreator;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectRefCallFrame<F extends AgoFunction, Id> extends CallFrame<F> implements ObjectRefObject<Id>, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);

    final ObjectRef<Id> objectRef;
    final DereferenceAdapter<Id> dereferenceAdapter;

    CallFrame<?> deferencedCallFrame;

    public ObjectRefCallFrame(F agoClass, final ObjectRef<Id> objectRef, DereferenceAdapter<Id> dereferenceAdapter, final RowState rowState) {
        super(DbSlotsCreator.create(agoClass, objectRef), agoClass);
        this.objectRef = objectRef;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public CallFrame<?> deference() {
        deference(deferencedCallFrame, this.dereferenceAdapter, this.objectRef);
        return deferencedCallFrame;
    }

    @Override
    public void setDeferencedInstance(Instance<?> inst) {
        setDeferencedInstance((CallFrame<?>) inst);
    }

    public ObjectRef<Id> getObjectRef() {
        return objectRef;
    }

    public Instance<?> getDeferencedInstance() {
        return deferencedCallFrame;
    }

    public Instance<?> getDeferencedCallFrame() {
        return deferencedCallFrame;
    }

    public void setDeferencedInstance(CallFrame<?> inst) {
        if(inst == null){
            this.deferencedCallFrame = null;
            return;
        }

        this.deferencedCallFrame = inst;
        if (inst instanceof DeferenceCallFrame r) {
            DeferenceFrameState state = r.getDeferenceFrameState();
            if (state.isEntrance()) {
                inst = new EntranceCallFrame<>(this);
            } else if (state.isAsyncEntrance()) {
                inst = new AsyncEntranceCallFrame<>(this);
            }
            this.deferencedCallFrame = inst;
        } else {
            this.deferencedCallFrame = inst;
        }
    }

    @Override
    public Slots getSlots() {
        return deferencedCallFrame.getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deferencedCallFrame.getParentScope();
    }

    @Override
    public void setParentScope(Instance<?> parentScope) {
        deferencedCallFrame.setParentScope(parentScope);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        deference().setCaller(caller);
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return deference().resolveSourceLocation();
    }

    @Override
    public boolean handleException(Instance<?> exception) {
        return deference().handleException(exception);
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        deference().setRunSpace(runSpace);
    }

    @Override
    public RunSpace getRunSpace() {
        RunSpace r = super.getRunSpace();
        if (r != null) return ((RunSpace) (r));
        return deference().getRunSpace();
    }

    @Override
    public String toString() {
        return "(ObjectRefCallFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void run() {
        deference().run();
    }

    @Override
    public void run(CallFrame<?> self) {
        CallFrame<?> r = deference();
        if (self.equals(this)) {
            r.run(r);
        } else {
            r.run(self);
        }
    }

    @Override
    public CallFrame<?> getCaller() {
        return deference().getCaller();
    }

    @Override
    public int hashCode() {
        return getObjectRef().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }
}
