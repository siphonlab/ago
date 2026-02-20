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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;

public class ExpandableInstance<T extends AgoClass> extends Instance<T> implements ObjectRefOwner, ReferenceCounter, ExpandableObject<T> {

    private boolean expanded = false;
    private final ObjectRefInstance<T> objectRefInstance;
    private final ObjectRefCallFrame<?> expander;

    private Instance<?> deferenceObject;

    public ExpandableInstance(ObjectRefInstance<T> objectRefInstance, ObjectRefCallFrame<?> expander, boolean alreadyDereferenced) {
        super(new ExpandableSlots(), objectRefInstance.getAgoClass());
        ExpandableSlots expandableSlots = (ExpandableSlots) slots;
        expandableSlots.setOwner(this);
        this.objectRefInstance = objectRefInstance;
        this.expander = expander;
        if(alreadyDereferenced){
            this.deferenceObject = objectRefInstance.getDeferencedInstance();
            this.expanded = true;
            objectRefInstance.addExpander(expander, this);
            ((ExpandableSlots) this.getSlots()).setInnerSlots(deferenceObject.getSlots());
        }
    }

    @Override
    public Slots getSlots() {
        expand();       //TODO or expand when invoke slots.getX/setX
        return this.slots;
    }

    public Instance<?> expand() {
        if (!expanded) {
            expanded = true;
            deferenceObject = objectRefInstance.dereferenceForExpander(this.expander, this);
            ((ExpandableSlots) this.getSlots()).setInnerSlots(deferenceObject.getSlots());
        }
        return deferenceObject;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public Instance<?> getExpandedInstance() {
        return deferenceObject;
    }

    @Override
    public ExpandableObject<?> expandFor(ExpandableObject<?> expander) {
        CallFrame<?> expanderCallFrame;
        if (expander instanceof ExpandableCallFrame<?> callFrame) {
            expanderCallFrame = callFrame;
        } else {
            expanderCallFrame = expander.getExpander();
        }
        if (ObjectRefOwner.equals(this.expander, expanderCallFrame)) {
            return this;
        }
        return this.objectRefInstance.expandFor(expanderCallFrame);
    }

    @Override
    public void fold(){
        if(this.expanded){
            this.expanded = false;
            objectRefInstance.foldBy(expander);
            this.deferenceObject = null;
            ExpandableSlots expandableSlots = (ExpandableSlots) this.slots;
            if (expandableSlots.getInnerSlots() != null) {
                expandableSlots.setInnerSlots(null);
            }
        }
    }

    @Override
    public void setParentScope(Instance parentScope) {
        // when this as parentScope of another instance, it won't expand
        expand().setParentScope(parentScope);
    }

    @Override
    public Instance getParentScope() {
        return expand().getParentScope();
    }

    @Override
    public String toString() {
        return "(ExpandableInstance %s %s)".formatted(this.getObjectRef(), this.expander);
    }

    @Override
    public ObjectRef getObjectRef() {
        return objectRefInstance.getObjectRef();
    }

    public Instance<T> getObjectRefInstance() {
        return objectRefInstance;
    }

    @Override
    public ObjectRefCallFrame<?> getExpander() {
        return expander;
    }

    @Override
    public void increaseRef(Reason reason) {
        objectRefInstance.increaseRef(reason);
    }

    @Override
    public int releaseRef(Reason reason) {
        return objectRefInstance.releaseRef(reason);
    }

    @Override
    public int getRefCount() {
        return objectRefInstance.getRefCount();
    }

    @Override
    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }
}
