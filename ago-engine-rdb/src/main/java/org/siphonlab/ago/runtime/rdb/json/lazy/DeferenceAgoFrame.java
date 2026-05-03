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
package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.opcode.Load;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.siphonlab.ago.runtime.rdb.task.TaskRunSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class DeferenceAgoFrame extends AgoFrame implements DeferenceCallFrame, ObjectRefOwner {
    public DeferenceAgoFrame(LazyJsonRefSlots slots, AgoFunction agoFunction, RdbEngine engine) {
        super(slots, agoFunction, engine);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = (ObjectRefCallFrame) adapter.restoreInstance(getObjectRef());
        this.state = new DeferenceFrameState(inst);
        inst.setDeferencedInstance(this);
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        super.setRunSpace(runSpace);
        this.state.setSaveRequired();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope);
        ReferenceCounter.increaseRef(parentScope, ReferenceCounter.Reason.SetParentInstall, this);
        this.state.setSaveRequired();
    }


    @Override
    public void setCaller(CallFrame<?> caller) {
        CallFrame c = LazyJsonAgoEngine.toObjectRefCallFrame(caller);
        if (ObjectRefOwner.equals(caller, this.getCaller())) return;

        if (this.getCaller() != null) {
            ReferenceCounter.releaseRef(this.getCaller(), ReferenceCounter.Reason.SetCallerDrop, this);
        }

        super.setCaller(c);
        ReferenceCounter.increaseRef(c, ReferenceCounter.Reason.SetCallerInstall, this);
        this.state.setSaveRequired();
    }

    protected int evaluateLoad(CallFrame<?> self, Slots slots, int pc, int instruction) {
        switch (instruction) {
            case Load.loadscope_v:
                slots.setObject(code[pc++], getScope(1));
                break;
            case Load.loadscope_vc:
                slots.setObject(code[pc++], getScope(code[pc++]));
                break;

            case Load.loadcls_scope_vc: {
                int target = code[pc++];
                int offset = code[pc++];
                slots.setObject(target, getScope(offset).getAgoClass());
                break;
            }
            case Load.loadcls_scope_v:
                slots.setObject(code[pc++], getScope(1).getAgoClass());
                break;
            case Load.loadcls_vo:
                slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass());
                break;
            case Load.loadcls_vC:
                slots.setObject(code[pc++], engine.getClass(code[pc++]));
                break;

            case Load.loadcls2_scope_vc: {
                int target = code[pc++];
                int offset = code[pc++];
                switch (offset) {
                    case 0:
                        slots.setObject(target, this.agoClass.getAgoClass());
                        break;
                    default:
                        slots.setObject(target, this.getScope(offset).getAgoClass().getAgoClass());
                        break;
                }
                break;
            }
            case Load.loadcls2_scope_v:
                slots.setObject(code[pc++], getScope(1).getAgoClass().getAgoClass());
                break;
            case Load.loadcls2_vo:
                slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass().getAgoClass());
                break;

            case Load.bindcls_vCo:          {
                int dest = code[pc++];
                AgoClass scopedClass = createScopedClass(self, code[pc++], slots.getObject(code[pc++]), pc);
                slots.setObject(dest, scopedClass);
                if(scopedClass.getAgoClass().getEmptyArgsConstructor() != null){        // will invoke constructor soon
                    return -1;
                }
            } break;
            case Load.bindcls_scope_vCc:    {
                int dest = code[pc++];
                AgoClass scopedClass = createScopedClass(self, code[pc++], getScope(code[pc++]), pc);
                slots.setObject(dest, scopedClass);
                if(scopedClass.getAgoClass().getEmptyArgsConstructor() != null){
                    return -1;
                }
            } break;

        }
        return pc;
    }

    @Override
    protected Instance<?> getScope(int depth) {
        if (depth == 0) return this;
        Instance<?> r = this;
        for (Integer i = 1; i <= depth; i++) {
            r = r.getParentScope();
            loadedScopes.add(r);
            ReferenceCounter.increaseRef(r, ReferenceCounter.Reason.LoadScope, this);
        }

        return r;
    }

    public boolean isSaveRequired() {
        return state.isSaveRequired();
    }

    public void markSaved() {
        state.markSaved();
    }

    public ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots)slots).getObjectRef();
    }

    @Override
    public ObjectRefObject toObjectRefInstance() {
        return this.state.getObjectRefInstance();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeferenceAgoFrame) {
            return this.getObjectRef().equals(((DeferenceAgoFrame) obj).getObjectRef());
        } else if (obj instanceof ObjectRefObject) {
            return this.getObjectRef().equals(((ObjectRefObject) obj).getObjectRef());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "(DeferenceAgoFrame %s)".formatted(this.getObjectRef());
    }

    public void releaseSlotsDeference(ReferenceCounter.Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots)this.getSlots(), reason);
        for (Instance scope : this.loadedScopes) {
            ReferenceCounter.releaseDeferenceAndContext(scope, ReferenceCounter.Reason.UnloadScope);
        }

    }

    public void increaseSlotsDeference(ReferenceCounter.Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots)(this.getSlots()), reason);
    }

    @Override
    public DeferenceObjectState getDeferenceObjectState() {
        return state;
    }

    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame.class);
    private final RdbAdapter adapter;
    private final DeferenceFrameState state;
    private List<Instance> loadedScopes = new LinkedList<Instance>();

    @Override
    public DeferenceFrameState getDeferenceFrameState() {
        return state;
    }


    @Override
    protected boolean evaluateInvoke(CallFrame<?> self, int instruction) {
        var runspace = (TaskRunSpace) this.runSpace;
        var nextFrame = this.getCallFrameAt(this.code[this.pc]);

        nextFrame.setRunSpace(self.getRunSpace());
        runspace.saveTask(self, nextFrame, this.pc - 1);

        return super.evaluateInvoke(self, instruction);
    }
}
