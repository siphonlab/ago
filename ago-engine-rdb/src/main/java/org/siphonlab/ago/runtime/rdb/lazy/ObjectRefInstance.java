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

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectRefInstance<T extends AgoClass> extends Instance<T> implements ObjectRefObject, ObjectRefOwner, ReferenceCounter {
    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);
    private final AtomicInteger referenceCounter = new AtomicInteger(0);
    private final DereferenceAdapter dereferenceAdapter;

    final ObjectRef objectRef;
    final Map<CallFrame<?>, ExpandableObject<?>> expanders = new HashMap<>();
    Instance<?> deferencedInstance;

    public ObjectRefInstance(T agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
        super(agoClass);
        this.dereferenceAdapter = dereferenceAdapter;
        this.objectRef = objectRef;
    }

    @Override
    public Instance<?> deference() {
        return deference(deferencedInstance, this.dereferenceAdapter, this.objectRef);
    }

    @Override
    public Slots getSlots() {
        return deference().getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deference().getParentScope();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        deference().setParentScope(parentScope);
    }

    public int hashCode() {
        return getObjectRef().hashCode();
    }

    @Override
    public String toString() {
        return "(ObjectRefInstance %s)".formatted(this.getObjectRef());
    }

    @Override
    public int getRefCount() {
        return referenceCounter.get();
    }

    @Override
    public ExpandableInstance<?> expandFor(ObjectRefCallFrame<?> expander, boolean alreadyDeferenced) {
        var existed = expanders.get(expander);
        if (existed != null) return (ExpandableInstance<?>) existed;
        ExpandableInstance<?> instance = new ExpandableInstance<>(this, expander, alreadyDeferenced);
        expanders.put(expander, instance);
        return instance;
    }

    @Override
    public ExpandableObject<?> expandFor(ObjectRefCallFrame<?> expander) {
        return expandFor(expander, this.deferencedInstance != null);
    }

    @Override
    public void increaseRef(Reason reason) {
        int cnt = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s inc ref got %d for %s".formatted(this, cnt, reason));
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

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public Instance getDeferencedInstance() {
        return deferencedInstance;
    }


    public void foldBy(CallFrame<?> expander) {
        foldBy(expanders,expander);
    }

    public void tryFold() {
        tryFold(expanders,this);
    }


    @Override
    public Instance<?> dereferenceForExpander(ObjectRefCallFrame<?> expander, ExpandableObject<?> expandableObject) {
        return dereferenceForExpander(expanders, expander, expandableObject);
    }

    public void setDeferencedInstance(Instance inst) {
        this.deferencedInstance = inst;
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

