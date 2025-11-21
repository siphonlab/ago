package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectRefInstance<T extends AgoClass> extends Instance<T> implements ObjectRefObject, ObjectRefOwner, ReferenceCounter {
    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);
    private final AtomicInteger referenceCounter = new AtomicInteger(0);
    private final DereferenceAdapter dereferenceAdapter;

    final ObjectRef objectRef;
    final Set<CallFrame> expanders = new HashSet<>();
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

    public ExpandableInstance createExpander(CallFrame expander, boolean alreadyDeferenced) {
        return new ExpandableInstance(this, expander, alreadyDeferenced);
    }

    @Override
    public void increaseRef(Reason reason) {
        if (objectRef.className().equals("my.test.A")) {
            times++;
        }
        int cnt = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s inc ref got %d for %s".formatted(this, cnt, reason));
    }

    private static int times = 0;
    @Override
    public int releaseRef(Reason reason) {
        if(objectRef.className().equals("my.test.A")){
            times ++;
        }
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

    public Instance dereferenceForExpander(CallFrame expander) {
        return dereferenceForExpander(expanders,expander);
    }

    public void setDeferencedInstance(Instance inst) {
        this.deferencedInstance = inst;
    }

    @Override
    public void fixCache() {
        this.dereferenceAdapter.repair(this.objectRef, this);
    }

}

