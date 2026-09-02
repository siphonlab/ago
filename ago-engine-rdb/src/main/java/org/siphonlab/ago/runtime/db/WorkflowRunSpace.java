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
package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.*;
import org.siphonlab.ago.runtime.db.lazy.*;
import org.siphonlab.ago.runtime.db.sdk.ForkWorkflowRunSpace;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.siphonlab.ago.runtime.db.task.WorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.siphonlab.ago.TypeCode.*;

public class WorkflowRunSpace<Id> extends RunSpace implements CreateInstanceRunSpace<Id>{
    private final static Logger LOGGER = LoggerFactory.getLogger(WorkflowRunSpace.class);

    protected final WorkflowAdapter<Id> workflowAdapter;
    public final Id id;

    public WorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, RunSpaceHost runSpaceHost) {
        this(agoEngine, workflowAdapter, runSpaceHost, workflowAdapter.nextId());
    }

    public WorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, RunSpaceHost runSpaceHost, Id id) {
        super(agoEngine, runSpaceHost);
        this.id = id;
        this.workflowAdapter = workflowAdapter;
    }

    public Id getId() {
        return id;
    }

    private boolean isEntranceOrTask(CallFrame<?> frame) {
        if (frame == null) {
            return false;
        }
        if (frame instanceof EntranceCallFrame<?>) {
            return true;
        }
        if(frame instanceof ObjectRefCallFrame<?,?> objectRefCallFrame){
            frame = objectRefCallFrame.dereference();
        }
        return frame.getAgoClass().isThatOrDerivedFrom(agoEngine.getLangClasses().getTaskInterface());
    }

    public WorkflowAdapter<Id> getWorkflowAdapter() {
        return workflowAdapter;
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (Exception e) {
            LOGGER.error("%s failed: %s".formatted(this, e.getMessage()), e);
            if(this.currCallFrame != null){
                currCallFrame.raiseJavaException(currCallFrame, e, false);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected boolean tryComplete() {
        boolean r = super.tryComplete();
        if(r && agoEngine instanceof WorkflowEngine workflowEngine){
            workflowEngine.releaseRunSpace(this.getId());
        }
        return r;
    }

    @Override
    public Object awaitTillComplete(CallFrame<?> frame) {
        workflowAdapter.saveInstance(frame);
        return super.awaitTillComplete(frame);
    }

    @Override
    protected void addPausingParent(RunSpace parent) {
        super.addPausingParent(parent);
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    protected boolean removePausingParent(RunSpace parent) {
        var r = super.removePausingParent(parent);
        workflowAdapter.updateRunSpace(this);
        return r;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        var r = super.createChildRunSpace(forkContext == null ? new ForkWorkflowRunSpace(): forkContext);
        workflowAdapter.updateRunSpace(this);        // add forkedRunSpace
        return r;
    }

    @Override
    protected void removeForkedSpace(RunSpace forkedRunSpace) {
        super.removeForkedSpace(forkedRunSpace);
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    public void setRunningState(byte runningState) {
        if(this.runningState == runningState) return;
        super.setRunningState(runningState);
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    public void setCurrCallFrame(CallFrame<?> currCallFrame) {
        if (ObjectRefOwner.equals(this.currCallFrame, currCallFrame)) return;
        super.setCurrCallFrame(currCallFrame);
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    public void fork(CallFrame<?> frame) {
        super.fork(frame);
        workflowAdapter.saveInstance(new CallFrameWithRunningState<>(frame, frame.getRunSpace().getRunningState()));
    }

    @Override
    public void spawn(CallFrame<?> frame) {
        super.spawn(frame);
    }

    @Override
    public void fork(CallFrame<?> frame, ForkContext forkContext) {
        if(frame instanceof ObjectRefCallFrame objectRefCallFrame) {
            frame = objectRefCallFrame.dereference();
        }

        var curRunSpace = (WorkflowRunSpace<?>) frame.getRunSpace();
        var nextRunSpace = (WorkflowRunSpace<?>) this.createChildRunSpace(forkContext);
        frame.setRunSpace(nextRunSpace);

        if (forkContext == null) {
            LOGGER.info("{} fork {} got {}", this, nextRunSpace, this.forkedSpaces.size());
        }
        else {
            LOGGER.info("{} fork {} via {}, got {}", this, nextRunSpace, forkContext, forkedSpaces.size());
        }

        var transactionAdapter = this.workflowAdapter.beginTransaction();       // a transit adapter
        transactionAdapter.updateRunSpace(curRunSpace, curRunSpace.getCurrentCallFrame());
        transactionAdapter.updateRunSpace(nextRunSpace, frame);
        transactionAdapter.saveInstance(frame);
        transactionAdapter.updateCallFrameRunningState(new CallFrameWithRunningState<>(frame.getCaller(), curRunSpace.getRunningState()));
        try {
            transactionAdapter.commitTransaction();
        } catch (Exception e) {
            throw new CommitFailedException(e);
        } finally {
            transactionAdapter.close();
        }

        nextRunSpace.start(new EntranceCallFrame<>(frame));
    }

    @Override
    public Future<?> startAsync(CallFrame<?> frame) {
        if(frame instanceof ObjectRefCallFrame<?,?> objectRefCallFrame) {
            frame = objectRefCallFrame.dereference();
        }
        workflowAdapter.saveFrameAndRunspace(frame);
        return super.startAsync(frame);
    }

    @Override
    protected void setException(Instance<?> exception) {
        super.setException(exception);
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    public void resumeByAcceptResult() {
        super.resumeByAcceptResult();
        workflowAdapter.updateRunSpace(this);
    }

    @Override
    public void interrupt() {
        CallFrame<?> callFrame = this.currCallFrame;
        super.interrupt();
        workflowAdapter.saveInstance(new CallFrameWithRunningState<>(callFrame, RunningState.INTERRUPTED));
    }

    public void resumeByRestore() {
        if (this.getRunningState() == RunningState.RUNNING || this.getRunningState() == RunningState.PENDING) {  // only works for RUNNING
            setRunningState(RunningState.PENDING);
            runSpaceHost.execute(this);
        }
    }

    public void restore(byte runningState, CallFrame<?> currCallFrame, RunSpace parent,
                        List<RunSpace> forkedRunspaces, List<RunSpace> pausingParents,
                        Instance<?> exception, ResultSlots resultSlots) {
        this.runningState = runningState;
        this.currCallFrame = currCallFrame;
        this.parent = parent;
        if(forkedRunspaces != null) this.forkedSpaces.addAll(forkedRunspaces);
        if(pausingParents != null) this.pausingParents.addAll(pausingParents);
        this.exception = exception;
        if(resultSlots != null) this.resultSlots = resultSlots;
    }

    public void saveTask(CallFrame<?> prev, CallFrame<?> cur, int pc) {
        if (cur == null) {
            return ;
        }
        if (isEntranceOrTask(cur)) {
            var t =this.workflowAdapter.beginTransaction();
            LOGGER.debug("saving task instances {}", prev);
            this.workflowAdapter.saveCallChainIncludeCurrent(prev);
            t.updateCallFrameRunningState(new CallFrameWithRunningState<>(prev, prev.getRunSpace().getRunningState(), pc));
        }
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, objectRef, slotsInitializer);
        }

        var slots = DbSlotsCreator.create(agoClass, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        if(!(slots instanceof DbSlots<?>)){   // box types use default slots
            return new Instance<>(slots, agoClass);
        }

        DbAdapter<Id> adapter = this.workflowAdapter;

        Instance<?> inst;
        if(agoClass.isNative()){
            inst = new DeferenceNativeInstance((DbSlots) slots, agoClass, (DbEngine<Id>) this.agoEngine, adapter, this);
        } else {
            inst = new DeferenceInstance((DbSlots) slots, agoClass, adapter, (DbEngine<Id>) this.agoEngine, this);
        }
        if (parentScope != null) inst.setParentScope(parentScope);

        DeferenceObject deferenceObject = (DeferenceObject) inst;
        deferenceObject.markSaved();

        return inst;
    }

    @Override
    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        DbSlots<Id> slots = (DbSlots<Id>) DbSlotsCreator.create(agoFunction, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);    // may change slots rowstate -> none
        CallFrame<?> inst;
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            inst = new DereferencedNativeFrame<>(slots, agoNativeFunction, (DbEngine<Id>) getAgoEngine(), this);
        } else {
            inst = new DereferencedAgoFrame<>(slots, agoFunction, (DbEngine<Id>) getAgoEngine(), this);
        }
        if (parentScope != null)
            inst.setParentScope(parentScope);  // not sure parentScope need restore to ObjectRefInstance too

        // restore DeferenceInstance to ObjectRefInstance
        // it cut off caller chain so that only running CallFrame living in the memory
        DeferenceObject<Id> deferenceObject = (DeferenceObject<Id>) inst;
        deferenceObject.markSaved();       // avoid instance marked as saveRequired
        return inst;
    }

    @Override
    public Instance<?> createArrayInstance(AgoClass arrayType, int length, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        var slots = DbSlotsCreator.create(arrayType, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        if(!(slots instanceof DbSlots<?>)) {
            return createArrayInstanceDefault(arrayType, length);
        }

        AgoClass elementType = arrayType.getElementClassOfArray();
        int typeCodeValue = elementType.getTypeCode().value;

        Instance<?> inst;
        switch (typeCodeValue) {
            case INT_VALUE, CLASS_REF_VALUE:
                inst = new IntArrayInstance(slots, arrayType, length);
                break;
            case BYTE_VALUE:
                inst = new ByteArrayInstance(slots, arrayType, length);
                break;
            case BOOLEAN_VALUE:
                inst = new BooleanArrayInstance( slots, arrayType, length);
                break;
            case CHAR_VALUE:
                inst = new CharArrayInstance( slots, arrayType, length);
                break;
            case DOUBLE_VALUE:
                inst = new DoubleArrayInstance( slots, arrayType, length);
                break;
            case FLOAT_VALUE:
                inst = new FloatArrayInstance( slots, arrayType, length);
                break;
            case LONG_VALUE:
                inst = new LongArrayInstance( slots, arrayType, length);
                break;
            case OBJECT_VALUE:
                inst = new ObjectArrayInstance( slots, arrayType, length);
                break;
            case UNION_VALUE:
                inst = new UnionArrayInstance( slots, arrayType, length);
                break;
            case DECIMAL_VALUE:
                inst = new DecimalArrayInstance( slots, arrayType, length);
                break;
            case SHORT_VALUE:
                inst = new ShortArrayInstance( slots, arrayType, length);
                break;
            case STRING_VALUE:
                inst = new StringArrayInstance( slots, arrayType, length);
                break;
            default:
                return createArrayInstanceDefault(arrayType, length);
        }

        if (inst instanceof DeferenceObject) {
            ((DeferenceObject) inst).markSaved();
        }
        return inst;
    }

    @Override
    public void updateDeferenceContext(Instance<?> instance, DereferenceContextSlots<Id> slots) {
        slots.bindContext(this, workflowAdapter);
    }

    private Instance<?> createArrayInstanceDefault(AgoClass arrayType, int length) {
        AgoClass elementType = arrayType.getElementClassOfArray();
        int typeCodeValue = elementType.getTypeCode().value;
        switch (typeCodeValue) {
            case INT_VALUE: return new IntArrayInstance(arrayType.createSlots(), arrayType, length);
            case BYTE_VALUE: return new ByteArrayInstance(arrayType.createSlots(), arrayType, length);
            case BOOLEAN_VALUE: return new BooleanArrayInstance(arrayType.createSlots(), arrayType, length);
            case CHAR_VALUE: return new CharArrayInstance(arrayType.createSlots(), arrayType, length);
            case DOUBLE_VALUE: return new DoubleArrayInstance(arrayType.createSlots(), arrayType, length);
            case FLOAT_VALUE: return new FloatArrayInstance(arrayType.createSlots(), arrayType, length);
            case LONG_VALUE: return new LongArrayInstance(arrayType.createSlots(), arrayType, length);
            case OBJECT_VALUE: return new ObjectArrayInstance(arrayType.createSlots(), arrayType, length);
            case UNION_VALUE: return new UnionArrayInstance(arrayType.createSlots(), arrayType, length);
            case DECIMAL_VALUE: return new DecimalArrayInstance(arrayType.createSlots(), arrayType, length);
            case SHORT_VALUE: return new ShortArrayInstance(arrayType.createSlots(), arrayType, length);
            case STRING_VALUE: return new StringArrayInstance(arrayType.createSlots(), arrayType, length);
            case CLASS_REF_VALUE: return new IntArrayInstance(arrayType.createSlots(), arrayType, length);
            default: throw new IllegalArgumentException("Unknown element type: " + elementType.getTypeCode());
        }
    }
}
