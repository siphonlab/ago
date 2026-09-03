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
package org.siphonlab.ago.runtime.db.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.opcode.Load;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.db.WorkflowRunSpace;
import org.siphonlab.ago.runtime.rdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class DereferencedAgoFrame<F extends AgoFunction, Id> extends DereferenceContextAgoFrame<Id> {

    private static final Logger logger = LoggerFactory.getLogger(DereferencedAgoFrame.class);

    private final DbSlots<Id> dbSlots;

    private List<Instance<?>> loadedScopes = new LinkedList<>();

    public DereferencedAgoFrame(DbSlots<Id> slots, AgoFunction agoFunction, DbEngine<Id> engine) {
        super(new DereferenceContextSlots<>(slots.getObjectRef(), slots), agoFunction, engine);
        this.dbSlots = slots;

        slots.setOwner(this);
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        super.setRunSpace(runSpace);
        this.dbSlots.logChangeBesideSlots(DbSlots.BesideSlotsChange.RunSpace);
    }

    @Override
    public void setParentScope(Instance<?> parentScope) {
        super.setParentScope(parentScope);
        this.dbSlots.logChangeBesideSlots(DbSlots.BesideSlotsChange.ParentScope);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        var ref = (CallFrame<?>) ObjectRefObject.toObjectRefInstance((DbEngine<?>) engine, caller);
        if(ref == null) ref = caller;

        if (ObjectRefOwner.equals(ref, this.getCaller())) return;

        super.setCaller(caller);
        this.dbSlots.logChangeBesideSlots(DbSlots.BesideSlotsChange.Caller);
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
            case Load.bindcls_scope_vCc:    {
                int dest = code[pc++];
                AgoClass scopedClass = createScopedClass(self, code[pc++], getScope(code[pc++]), pc);
                slots.setObject(dest, scopedClass);
                if(scopedClass.getAgoClass().getEmptyArgsConstructor() != null){
                    return -1;
                }
            } break;

            default:
                return super.evaluateLoad(self, slots, pc, instruction);

        }
        return pc;
    }

    @Override
    protected Instance<?> getScope(int depth) {
        if (depth == 0) return this;
        Instance<?> r = this;
        for (var i = 1; i <= depth; i++) {
            r = r.getParentScope();
            loadedScopes.add(r);
        }

        return r;
    }

    public ObjectRef<Id> getObjectRef() {
        return dbSlots.getObjectRef();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DereferencedAgoFrame) {
            return this.getObjectRef().equals(((DereferencedAgoFrame) obj).getObjectRef());
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

    @Override
    protected boolean evaluateInvoke(CallFrame<?> self, int instruction) {
        if(self.getRunSpace() instanceof WorkflowRunSpace<?> workflowRunSpace) {
            var nextFrame = this.getCallFrameAt(this.code[this.pc]);

            nextFrame.setRunSpace(workflowRunSpace);
            workflowRunSpace.saveTask(self, nextFrame, this.pc - 1);
        }
        return super.evaluateInvoke(self, instruction);
    }
}
