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
package org.siphonlab.ago.runtime.rdb.lazy;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceFrameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectRefCallFrame<F extends AgoFunction> extends CallFrame<F> implements ObjectRefObject, ObjectRefOwner, ReferenceCounter {

    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);

    final ObjectRef objectRef;
    final DereferenceAdapter dereferenceAdapter;
    final Map<CallFrame<?>, ExpandableObject<?>> expanders = new HashMap<>();

    Instance<?> deferencedInstance;

    private AtomicInteger referenceCounter = new AtomicInteger(0);
    private Instance deferencedCallFrame;

    public ObjectRefCallFrame(F agoClass, final ObjectRef objectRef, DereferenceAdapter dereferenceAdapter, final RowState rowState) {
        super(DefaultGroovyMethods.with(agoClass.createSlots(), new Closure<Slots>(null, null) {
            public Slots doCall(Slots it) {
                if (it instanceof RdbSlots) {
                    ((RdbSlots) it).setRowState(rowState);
                    ((RdbSlots) it).setId(objectRef.id());
                }
                return it;
            }

            public Slots doCall() {
                return doCall(null);
            }

        }), agoClass);
        this.objectRef = objectRef;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public Instance<?> deference() {
        if(deferencedCallFrame != null) return deferencedCallFrame;

        deference(deferencedInstance, this.dereferenceAdapter, this.objectRef);
        return deferencedCallFrame;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public Instance getDeferencedInstance() {
        return deferencedInstance;
    }

    public Instance getDeferencedCallFrame() {
        return deferencedCallFrame;
    }

    public void foldBy(CallFrame<?> expander) {
        foldBy(expanders, expander);
    }

    public void tryFold() {
        tryFold(expanders, this);
    }

    public Instance<?> dereferenceForExpander(ObjectRefCallFrame<?> expander, ExpandableObject<?> expandableObject) {
        dereferenceForExpander(expanders, expander, expandableObject);
        return deferencedInstance;
    }

    public void setDeferencedInstance(Instance inst) {
        if(inst == null){
            this.deferencedCallFrame = null;
            this.deferencedInstance = null;
            return;
        }

        this.deferencedInstance = inst;
        if (inst instanceof DeferenceCallFrame r) {
            DeferenceFrameState state = r.getDeferenceFrameState();
            if (state.isEntrance()) {
                inst = new EntranceCallFrame<>(this.expandFor(this));
            } else if (state.isAsyncEntrance()) {
                inst = new AsyncEntranceCallFrame<>(this.expandFor(this));
            }
            this.deferencedCallFrame = inst;
        } else {
            this.deferencedCallFrame = inst;
        }
    }

    @Override
    public Slots getSlots() {
        return deferencedInstance.getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deferencedInstance.getParentScope();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        deferencedInstance.setParentScope(parentScope);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        recomposeAsCallFrame().setCaller(caller);
    }

    public CallFrame recomposeAsCallFrame() {
        if(deferencedInstance == null){
            this.deference();
        }
        return (CallFrame) deferencedInstance;
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return recomposeAsCallFrame().resolveSourceLocation();
    }

    @Override
    public boolean handleException(Instance<?> exception) {
        return recomposeAsCallFrame().handleException(exception);
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        recomposeAsCallFrame().setRunSpace(runSpace);
    }

    @Override
    public RunSpace getRunSpace() {
        RunSpace r = super.getRunSpace();
        if (r != null) return ((RunSpace) (r));
        return recomposeAsCallFrame().getRunSpace();
    }

    @Override
    public String toString() {
        return "(ObjectRefCallFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void run() {
        recomposeAsCallFrame().run();
    }

    @Override
    public void run(CallFrame<?> self) {
        CallFrame r = recomposeAsCallFrame();
        if (self.equals(this)) {
            r.run(r);
        } else {
            r.run(self);
        }
    }

    @Override
    public CallFrame<?> getCaller() {
        return recomposeAsCallFrame().getCaller();
    }

    @Override
    public int hashCode() {
        return getObjectRef().hashCode();
    }


    @Override
    public ExpandableCallFrame<?> expandFor(ObjectRefCallFrame<?> expander, boolean alreadyDeferenced) {
        var existed = expanders.get(expander);
        if(existed != null) return (ExpandableCallFrame<?>) existed;
        ExpandableCallFrame<?> callFrame = new ExpandableCallFrame<>(this, expander, alreadyDeferenced);
        expanders.put(expander, callFrame);
        return callFrame;
    }

    public ExpandableCallFrame<?> expandFor(ObjectRefCallFrame<?> expander) {
        return expandFor(expander, this.deferencedCallFrame != null);
    }

    @Override
    public int getRefCount() {
        return referenceCounter.get();
    }

    @Override
    public void increaseRef(Reason reason) {
        int r = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s inc ref got %d for %s".formatted(this, r, reason));
    }

    @Override
    public int releaseRef(Reason reason) {
        int r = referenceCounter.decrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s release ref got %d for %s".formatted(this, r, reason));
        if (r == 0) {
            dereferenceAdapter.release(this.getObjectRef());
        }

        return r;
    }

    @Override
    public void fixCache() {
        this.dereferenceAdapter.repair(this.objectRef, this);
    }

    public void addExpander(CallFrame<?> callFrame, ExpandableObject<?> expandableObject) {
        this.expanders.put(callFrame, expandableObject);
    }

    @Override
    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }
}
