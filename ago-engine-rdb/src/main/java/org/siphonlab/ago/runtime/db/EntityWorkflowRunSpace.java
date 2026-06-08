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
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityWorkflowRunSpace;
import org.siphonlab.ago.runtime.rdb.DbEngine;

import java.util.function.Consumer;

public class EntityWorkflowRunSpace<Id> extends WorkflowRunSpace<Id>{

    private final EntityAdapter<Id> entityAdapter;

    public EntityWorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, workflowAdapter, runSpaceHost);
        this.entityAdapter = entityAdapter;
    }

    public EntityWorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost, Id id) {
        super(agoEngine, workflowAdapter, runSpaceHost, id);
        this.entityAdapter = entityAdapter;
    }

    public EntityAdapter<Id> getEntityAdapter() {
        return entityAdapter;
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush(this);
        }
        return b;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        return super.createChildRunSpace(forkContext == null ? new ForkEntityWorkflowRunSpace() : forkContext);
    }


    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, objectRef, slotsInitializer);
        }
        if (!entityAdapter.isEntityClass(agoClass)) {
            return super.createInstance(parentScope, agoClass, objectRef, slotsInitializer);
        }

        Slots slots = DbSlotsCreator.create(agoClass, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);
        Instance<?> instance;
        if(agoClass.isNative()) {
            instance = new NativeInstance(slots, agoClass);
        } else {
            instance = new Instance<>(slots, agoClass);
        }
        if(parentScope != null) instance.setParentScope(parentScope);
        return instance;
    }

    @Override
    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        if(!entityAdapter.isEntityClass(agoFunction)) {
            return super.createFunctionInstance(agoFunction, parentScope, objectRef, slotsInitializer);
        }

        var slots = DbSlotsCreator.create(agoFunction, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);
        CallFrame<?> inst;
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            inst = new NativeFrame(getAgoEngine(), slots, agoNativeFunction);
        } else {
            inst = new AgoFrame(slots, agoFunction, this.getAgoEngine());
        }
        if(parentScope != null) inst.setParentScope(parentScope);
        return inst;
    }
}
