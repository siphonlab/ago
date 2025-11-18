package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.*
import org.siphonlab.ago.opcode.Load
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

@CompileStatic
public class DeferenceAgoFrame extends AgoFrame implements DeferenceObject, ObjectRefOwner{

    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame)

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefCallFrame objectRefInstance

    private boolean saveRequired = false;

    // for objRefFrame.deference(), to deference to EntranceFrame
    public boolean isEntrance = false;
    public boolean isAsyncEntrance = false;

    public DeferenceAgoFrame(LazyJsonRefSlots slots, AgoFunction agoFunction, RdbEngine engine) {
        super(slots, agoFunction, engine);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = adapter.restoreInstance(objectRef) as ObjectRefCallFrame;
        inst.setDeferencedInstance(this)
        this.objectRefInstance = inst;
    }

    private void setSaveRequired(boolean value){
        this.saveRequired = value;
    }

    @Override
    void setRunSpace(AgoRunSpace runSpace) {
        super.setRunSpace(runSpace)
        this.setSaveRequired(true)
    }

    @Override
    void setCaller(CallFrame<?> caller) {
        CallFrame c = toObjectRefCallFrame(caller);
        if (ObjectRefOwner.equals(caller, this.caller)) return;

        if(this.caller != null){
            ReferenceCounter.releaseRef(this.caller, Reason.SetCallerDrop, this)
        }
        super.setCaller(c)
        ReferenceCounter.increaseRef(c, Reason.SetCallerInstall, this);
        this.setSaveRequired(true);
    }

    protected int evaluateLoad(Slots slots, int pc, int instruction) {
        switch (instruction) {
            case Load.loadscope_v: slots.setObject(code[pc++], getScope(1)); break;
            case Load.loadscope_vc: slots.setObject(code[pc++], getScope(code[pc++])); break;

            case Load.loadcls_scope_vc: {
                int target = code[pc++];
                int offset = code[pc++];
                slots.setObject(target, getScope(offset).agoClass);
                break;
            }
            case Load.loadcls_scope_v: slots.setObject(code[pc++], getScope(1).getAgoClass()); break;
            case Load.loadcls_vo: slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass()); break;
            case Load.loadcls_vC: slots.setObject(code[pc++], engine.getClass(code[pc++])); break;

            case Load.loadcls2_scope_vc: {
                int target = code[pc++];
                int offset = code[pc++];
                switch (offset) {
                    case 0: slots.setObject(target, this.agoClass.agoClass); break;
                    default: slots.setObject(target, this.getScope(offset).agoClass.agoClass); break;
                }
                break;
            }
            case Load.loadcls2_scope_v: slots.setObject(code[pc++], getScope(1).getAgoClass().getAgoClass()); break;
            case Load.loadcls2_vo: slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass().getAgoClass()); break;

            case Load.bindcls_vCo: slots.setObject(code[pc++], engine.createScopedClass(this, code[pc++], slots.getObject(code[pc++]))); break;
            case Load.bindcls_scope_vCc: slots.setObject(code[pc++], engine.createScopedClass(this, code[pc++], getScope(code[pc++]))); break;

        }
        return pc;
    }

    private List<Instance> loadedScopes = new LinkedList<>();

    @Override
    protected Instance<?> getScope(int depth) {
        if (depth == 0) return this;
        Instance<?> r = this;
        for (var i = 1; i <= depth; i++) {
            r = r.getParentScope();
            loadedScopes.add(r);
            ReferenceCounter.increaseRef(r, Reason.LoadScope, this)
        }
        return r;
    }

    @Override
    void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope)
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall, this);
        this.setSaveRequired(true);
    }

    boolean isSaveRequired(){
        return saveRequired;
    }

    void markSaved(){
        saveRequired = false
    }

    @Override
    void setCreator(CallFrame<?> creator) {
        if(ObjectRefOwner.equals(creator, this.creator)) return;

        CallFrame c = toObjectRefCallFrame(creator);

        super.setCreator(c)
        ReferenceCounter.increaseRef(c, Reason.SetCreatorInstall, this);
        saveRequired = true;
    }

    ObjectRef getObjectRef(){
        return ((LazyJsonRefSlots) this.slots).objectRef
    }

    @Override
    ObjectRefObject toObjectRefInstance() {
        assert this.objectRefInstance != null;
        return this.objectRefInstance;
    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof DeferenceAgoFrame){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else if(obj instanceof ObjectRefObject){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else {
            return false
        }
    }

    @Override
    String toString() {
        return "(DeferenceAgoFrame %s)".formatted(this.objectRef)
    }

    void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
        for(var scope : this.loadedScopes){
            ReferenceCounter.releaseDeferenceAndContext(scope, Reason.UnloadScope);
        }
    }

    void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
    }

}
