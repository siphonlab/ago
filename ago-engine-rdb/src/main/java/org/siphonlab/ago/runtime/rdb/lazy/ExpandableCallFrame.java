package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandableCallFrame<T extends AgoFunction> extends CallFrame<T>
                implements ObjectRefOwner, ReferenceCounter, ExpandableObject<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExpandableCallFrame.class);

    private boolean expanded = false;
    private final ObjectRefCallFrame<T> objectRefInstance;
    private final CallFrame<?> expander;

    private CallFrame<?> deferenceObject;

    public ExpandableCallFrame(ObjectRefCallFrame<T> objectRefInstance, CallFrame<?> expander, boolean alreadyDereferenced) {
        super(new ExpandableSlots(), objectRefInstance.getAgoClass());
        ExpandableSlots expandableSlots = (ExpandableSlots) slots;
        expandableSlots.setOwner(this);
        this.objectRefInstance = objectRefInstance;
        this.expander = expander;
        if (alreadyDereferenced) {
            this.deferenceObject = (CallFrame<?>) objectRefInstance.getDeferencedInstance();
            assert this.getObjectRef().equals(ObjectRefOwner.extractObjectRef(deferenceObject));
            this.expanded = true;
        }
    }

    public CallFrame<?> expand(){
        if (!expanded) {
            expanded = true;
            deferenceObject = (CallFrame<?>) objectRefInstance.dereferenceForExpander(this.expander);
            ((ExpandableSlots)this.getSlots()).setInnerSlots(deferenceObject.getSlots());
        }
        return deferenceObject;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public CallFrame<?> getExpandedInstance() {
        return deferenceObject;
    }

    @Override
    public Slots getSlots() {
        expand();       //TODO or expand when invoke slots.getX/setX
        return this.slots;
    }


    public void fold(){
        if(this.expanded){
            objectRefInstance.foldBy(expander);
            this.deferenceObject = null;
            this.expanded = false;
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
    public void setCreator(CallFrame<?> creator) {
        expand().setCreator(creator);
    }

    @Override
    public CallFrame<?> getCreator() {
        return expand().getCreator();
    }

    @Override
    public void setRunSpace(AgoRunSpace runSpace) {
        if(logger.isDebugEnabled()) logger.debug("%s set runspace %s".formatted(this, runSpace));
        expand().setRunSpace(runSpace);
    }

    @Override
    public AgoRunSpace getRunSpace() {
        return expand().getRunSpace();
    }

    @Override
    public String toString() {
        return "(ExpandableCallFrame %s %s)".formatted(this.getObjectRef(), this.expander);
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return expand().getCreator().resolveSourceLocation();
    }

    @Override
    public void run(CallFrame<?> self) {
        expand().run(this);
    }

    @Override
    public ObjectRef getObjectRef() {
        return objectRefInstance.getObjectRef();
    }

    public Instance<T> getObjectRefInstance() {
        return objectRefInstance;
    }

    public CallFrame<?> getExpander() {
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
}
