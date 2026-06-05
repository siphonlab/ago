package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.ForkContext;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.rdb.DbEngine;

/**
 * a runspace collecting changed, and flush when complete
 *
 */
public class EntityRunSpace<Id> extends RunSpace {

    private final EntityAdapter<Id> entityAdapter;

    public EntityRunSpace(DbEngine<Id> dbEngine, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost) {
        super(dbEngine, runSpaceHost);
        this.entityAdapter = entityAdapter.beginTransaction();      // each entity runspace start a transaction
    }

    public EntityAdapter<Id> getEntityAdapter() {
        return entityAdapter;
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush();
        }
        return b;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        return super.createChildRunSpace(forkContext == null ? new ForkEntityRunSpace() : forkContext);
    }
}
