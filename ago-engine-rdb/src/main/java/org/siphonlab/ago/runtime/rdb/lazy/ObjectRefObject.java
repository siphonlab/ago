package org.siphonlab.ago.runtime.rdb.lazy;

import groovy.lang.GString;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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

    default void foldBy(Set<CallFrame> expanders, CallFrame<?> expander) {
        expanders.remove(expander);
        if (logger.isDebugEnabled()) logger.debug(expander + " quit, " + getObjectRef() + " has %s expanders".formatted(expanders.size()));
        tryFold();
    }

    void tryFold();

    public default void tryFold(Set<CallFrame> expanders, ObjectRefObject self) {
        if (expanders.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug(getObjectRef() + " fold");
            //TODO DOUBT
            ReferenceCounter.releaseDeferenceSlotsAndContext(self.getDeferencedInstance());
            self.setDeferencedInstance(null);
        }

    }

    ExpandableObject expandFor(CallFrame expander, boolean alreadyDeferenced);

    Instance dereferenceForExpander(CallFrame expander);

    default Instance dereferenceForExpander(Set<CallFrame> expanders, CallFrame expander) {
        if (logger.isDebugEnabled()) logger.debug(String.valueOf(getObjectRef()) + " expand for " + String.valueOf(expander));
        expanders.add(expander);
        return deference();
    }

    void setDeferencedInstance(Instance inst);


    /**
     * Now, when ResultSlots setObject(ObjectRefInstance), it won't increase ref count
     * so it maybe removed in cache since the ref count = 0, and the follow runspace can access it via ResultSlots!
     */
    void fixCache();
}
