package org.siphonlab.ago.runtime.rdb.reactive;

import org.agrona.collections.Long2ObjectHashMap;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.mina.util.IdentityHashSet;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbAgoRunSpace;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.json.lazy.JsonAgoClassLoader;

import java.util.HashSet;
import java.util.Set;


/**
 * full persistent engine.
 * all objects store in db, only function instances and runspace live in memory, and the `setObject` result (by load_xx, move_o_xx), stay in memory.
 * however in memory they are just a `(table-name, id)` key, instead of real data
 * and for `setInt`, `setDouble`, they are all stay in db, and all commands works as sql, i.e. `jump_if_i` transforms to `update function_inst set pc = x where cond = `
 */
public class PersistentRdbEngine extends RdbEngine {

    protected Long2ObjectHashMap<AgoRunSpace> runspaces = new Long2ObjectHashMap<>();

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

        super.load(classLoader);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstance(parentScope, agoClass, creator, runSpace);
        saveInstance(inst);
        return inst;
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createFunctionInstance(parentScope, agoFunction, caller, creator, runSpace);
        saveInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        saveInstance(inst);
        return inst;
    }

    public void resume(){
        throw new NotImplementedException();
    }

    public void releaseRunSpace(long id) {
        this.runspaces.remove(id);
    }

    public AgoRunSpace getRunSpace(long id) {
        return this.runspaces.get(id);
    }

    Set<AgoRunSpace> getRunSpaces(){
        return new IdentityHashSet<>(this.runspaces.values());
    }
}
