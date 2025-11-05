package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.*
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceableInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static LazyJsonAgoEngine.toObjectRefCallFrame;

@CompileStatic
public class DeferenceAgoFrame extends AgoFrame implements ReferenceableInstance, ObjectRefOwner{

    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame)

    private final RdbAdapter adapter;

    public DeferenceAgoFrame(LazyJsonRefSlots slots, AgoFunction agoFunction, RdbEngine engine) {
        super(slots, agoFunction, engine);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();
    }

    @Override
    protected CallFrame<?> getCallFrameAt(int slot) {
        var inst = slots.getObject(slot);
        if(inst instanceof ObjectRefCallFrame){
            inst.bindCallFrame(this);
            return (CallFrame<?>) inst.doDeference();
        }
        return (CallFrame<?>) inst;
    }

    @Override
    void setCaller(CallFrame<?> caller) {
        if (!Objects.equals(caller, this.caller)) {
            CallFrame c = toObjectRefCallFrame(caller, this)
            super.setCaller(c)
        } else if(caller instanceof ObjectRefInstanceTrait){
            super.setCaller((CallFrame)caller)
        }
    }

    ObjectRef getObjectRef(){
        return ((LazyJsonRefSlots) this.slots).objectRef
    }

    @Override
    ObjectRefInstanceTrait toObjectRefInstance() {
        if(logger.isDebugEnabled()) logger.debug("%s convert to objectref instance %s".formatted(this, ((LazyJsonRefSlots) this.slots).objectRef))
        return new ObjectRefCallFrame<AgoFunction>(this.agoClass, this.getObjectRef(), (DereferenceAdapter) this.adapter)
    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof DeferenceAgoFrame){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else if(obj instanceof ObjectRefInstanceTrait){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else {
            return false
        }
    }
}
