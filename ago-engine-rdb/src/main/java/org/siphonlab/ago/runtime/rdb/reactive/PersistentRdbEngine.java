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
package org.siphonlab.ago.runtime.rdb.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import groovy.sql.GroovyRowResult;
import org.agrona.collections.Long2ObjectHashMap;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.mina.util.IdentityHashSet;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbRunSpace;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.json.lazy.JsonAgoClassLoader;

import java.util.Map;
import java.util.Set;


/**
 * full persistent engine.
 * all objects store in db, only function instances and runspace live in memory, and the `setObject` result (by load_xx, move_o_xx), stay in memory.
 * however in memory they are just a `(table-name, id)` key, instead of real data
 * and for `setInt`, `setDouble`, they are all stay in db, and all commands works as sql, i.e. `jump_if_i` transforms to `update function_inst set pc = x where cond = `
 */
public class PersistentRdbEngine extends RdbEngine {

    protected Long2ObjectHashMap<RunSpace> runspaces = new Long2ObjectHashMap<>();

    public PersistentRdbEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
    }

    public PersistentRdbEngine(RdbAdapter rdbAdapter){
        super(rdbAdapter);
    }

    @Override
    public void load(AgoClassLoader classLoader) {
        boolean loadFromDb = (classLoader instanceof JsonAgoClassLoader);
        if(!loadFromDb) {
            getRdbAdapter().saveStrings(classLoader.getStrings());
            getRdbAdapter().saveBlobs(classLoader.getBlobs());
        }

        classLoader.getTheMeta().setSlotsCreator(classLoader.getSlotsCreatorFactory().generateSlotsCreator(classLoader.getTheMeta()));
        for (AgoClass agoClass : classLoader.getClasses()) {
            if (agoClass.getAgoClass() != null) {
                MetaClass metaClass = agoClass.getAgoClass();
                if (metaClass.getSlotsCreator() == null) {
                    metaClass.setSlotsCreator(classLoader.getSlotsCreatorFactory().generateSlotsCreator(classLoader.getTheMeta()));
                }
            }
            agoClass.initSlots();
            assert agoClass.getSlots() != null && !(agoClass.getSlots() instanceof AgoClass.TraceOwnerSlots);

            if(!loadFromDb) saveInstance(agoClass);
        }

        // here is parentScope, creator, slots of class
        if(loadFromDb){
            JsonAgoClassLoader jsonAgoClassLoader = (JsonAgoClassLoader) classLoader;
            for (Map.Entry<String, GroovyRowResult> entry : jsonAgoClassLoader.getRowsByClassName().entrySet()) {
                var agoClass = this.getClass(entry.getKey());
                try {
                    restoreClassStates(agoClass, entry.getValue());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        super.load(classLoader);
    }

    protected void restoreClassStates(AgoClass agoClass, GroovyRowResult row) throws JsonProcessingException {
        throw new NotImplementedException();
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = super.createInstance(parentScope, agoClass, creator);
        saveInstance(inst);
        return inst;
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        var inst = super.createFunctionInstance(parentScope, agoFunction, caller, creator);
        saveInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, RunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        saveInstance(inst);
        return inst;
    }

    public void resume(){
        throw new NotImplementedException();
    }

    @Override
    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        var r = super.createRunSpace(runSpaceHost);
        if(r instanceof RdbRunSpace rdbAgoRunSpace){
            this.runspaces.put(rdbAgoRunSpace.getId(), rdbAgoRunSpace);
        }
        return r;
    }

    public void releaseRunSpace(long id) {
        this.runspaces.remove(id);
    }

    public RunSpace getRunSpace(long id) {
        return this.runspaces.get(id);
    }

    Set<RunSpace> getRunSpaces(){
        return new IdentityHashSet<>(this.runspaces.values());
    }
}
