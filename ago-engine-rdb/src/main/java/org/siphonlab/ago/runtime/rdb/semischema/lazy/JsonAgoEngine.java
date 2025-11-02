package org.siphonlab.ago.runtime.rdb.semischema.lazy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait;
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceableInstance;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.siphonlab.ago.runtime.stateful.RunningState;
import org.siphonlab.ago.runtime.stateful.StatefulAgoFrame;

import java.util.function.Consumer;

public class JsonAgoEngine extends PersistentRdbEngine {

    public JsonAgoEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
        rdbAdapter.setClassManager(this);
    }

    @Override
    protected ObjectMapper createJsonObjectMapper(AgoClassLoader classLoader) {
        return createJsonObjectMapper(classLoader, false, false, true,false);
    }

    protected ObjectMapper createJsonSlotsMapper(AgoClassLoader classLoader) {
        return createJsonObjectMapper(classLoader, false, false, true, true);
    }


    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator, AgoRunSpace runSpace) {
        return createFunctionInstance(agoFunction,parentScope, caller, creator, null);
    }

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, CallFrame<?> caller, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        JsonRefSlots slots = (JsonRefSlots) agoFunction.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);
        CallFrame<?> inst;
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            inst = new DeferenceNativeFrame(slots, agoNativeFunction, this);
        } else {
            inst = new DeferenceAgoFrame(slots, agoFunction, this);
        }
        if (parentScope != null)
            inst.setParentScope(parentScope);  // not sure parentScope need restore to ObjectRefInstance too
        // restore DeferenceInstance to ObjectRefInstance
        // it cut caller chain so that only running CallFrame living in the memory
        inst.setCaller(toObjectRefCallFrame(caller,inst));
        inst.setCreator(toObjectRefCallFrame(creator, inst));
        saveInstance(inst);
        return inst;
    }

    public static CallFrame<?> toObjectRefCallFrame(CallFrame<?> callFrame, CallFrame<?> instanceCallFrame){
        if (callFrame instanceof ReferenceableInstance) {
            ObjectRefInstanceTrait r = ((ReferenceableInstance) callFrame).toObjectRefInstance();
            r.bindCallFrame(instanceCallFrame);
            return (CallFrame<?>) r;
        }
        return callFrame;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        return createInstance(parentScope,agoClass, creator, runSpace, null);
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace, Consumer<Slots> slotsInitializer) {
        var inst = super.createInstance(parentScope,agoClass, creator, runSpace);
        if(inst.getSlots() instanceof JsonRefSlots jsonRefSlots && slotsInitializer != null){
            slotsInitializer.accept(jsonRefSlots);
        }
        return inst;
    }


    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        return inst;
    }

    public void resume(){
        StatefulAgoFrame resumeFrame = (StatefulAgoFrame) this.createFunctionInstance(null, (AgoFunction) this.getClass("@resume#"), null, null, this.getRunSpace());
        resumeFrame.setRunningState(RunningState.DONE);
        CallFrame<?>[] callFrames = this.getRdbAdapter().loadResumableCallFrames(resumeFrame);
        for (CallFrame<?> callFrame : callFrames) {
            callFrame.getRunSpace().spawn(callFrame);   //TODO resume in original runspace
        }
    }
}
