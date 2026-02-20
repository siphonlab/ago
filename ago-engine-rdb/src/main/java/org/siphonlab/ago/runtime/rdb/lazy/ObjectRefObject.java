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

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
/**
 * lazy instance
 */
public interface ObjectRefObject {
    static final Logger logger = LoggerFactory.getLogger(ObjectRefObject.class);

    ObjectRef getObjectRef();

    Instance getDeferencedInstance();

    Instance<?> deference();

    default Instance<?> deference(Instance<?> deferencedInstance,
                                  DereferenceAdapter dereferenceAdapter,
                                  ObjectRef objectRef) {
        if (deferencedInstance != null)
            return deferencedInstance;

        if (logger.isDebugEnabled()) logger.debug(getObjectRef() + " expand deference");
        Instance<?> r = dereferenceAdapter.dereference(objectRef);
        setDeferencedInstance(r);
        return r;
    }

    default void foldBy(Map<CallFrame<?>, ExpandableObject<?>> expanders, CallFrame<?> expander) {
        expanders.remove(expander);
        if (logger.isDebugEnabled()) logger.debug(expander + " quit, " + getObjectRef() + " has %s expanders".formatted(expanders.size()));
        tryFold();
    }

    void tryFold();

    public default void tryFold(Map<CallFrame<?>, ExpandableObject<?>> expanders, ObjectRefObject self) {
        if (expanders.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug(getObjectRef() + " fold");
            //TODO DOUBT
            ReferenceCounter.releaseDeferenceSlotsAndContext(self.getDeferencedInstance());
            self.setDeferencedInstance(null);
        }

    }

    ExpandableObject<?> expandFor(ObjectRefCallFrame<?> expander, boolean alreadyDeferenced);

    ExpandableObject<?> expandFor(ObjectRefCallFrame<?> expander);

    default ExpandableObject<?> expandFor(ExpandableObject<?> expander){
        if (expander instanceof ExpandableCallFrame<?> callFrame) {
            return expandFor(callFrame.getObjectRefInstance());
        } else {
            return expandFor(expander.getExpander());
        }
    }

    default ExpandableObject<?> expandFor(CallFrame<?> expander){
        if(expander instanceof EntranceCallFrame<?> entranceCallFrame){
            expander = entranceCallFrame.getInner();
        }
        if(expander instanceof ObjectRefCallFrame<?> objectRefCallFrame){
            return expandFor(objectRefCallFrame);
        } else if(expander instanceof ExpandableCallFrame<?> expandableCallFrame) {
            return expandFor((ExpandableObject<?>) expandableCallFrame);
        } else if(expander instanceof DeferenceCallFrame deferenceCallFrame){
            return expandFor((ObjectRefCallFrame<?>) deferenceCallFrame.toObjectRefInstance());
        } else {
            throw new IllegalArgumentException("unexpected CallFrame type " + expander);
        }
    }

    Instance<?> dereferenceForExpander(ObjectRefCallFrame<?> expander, ExpandableObject<?> expandableObject);

    default Instance dereferenceForExpander(Map<CallFrame<?>, ExpandableObject<?>> expanders, CallFrame expander, ExpandableObject<?> expandableObject) {
        expanders.put(expander, expandableObject);
        if (logger.isDebugEnabled()) logger.debug("%s expand for %s, now there are %d expanders".formatted(getObjectRef(), expander, expanders.size()));
        return deference();
    }

    void setDeferencedInstance(Instance inst);


    /**
     * Now, when ResultSlots setObject(ObjectRefInstance), it won't increase ref count
     * so it maybe removed in cache since the ref count = 0, and the follow runspace can access it via ResultSlots!
     */
    void fixCache();

    void addExpander(CallFrame<?> callFrame, ExpandableObject<?> expandableObject);
}
