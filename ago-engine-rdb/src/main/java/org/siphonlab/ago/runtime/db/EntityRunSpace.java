package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.rdb.DbEngine;

/**
 * a runspace collecting changed, and flush when complete
 *
 */
public class EntityRunSpace<Id> extends RunSpace {

    private final EntityAdapter<Id> entityAdapter;

    public EntityRunSpace(DbEngine<Id> dbEngine, RunSpaceHost runSpaceHost, EntityAdapter<Id> entityAdapter) {
        super(dbEngine, runSpaceHost);
        this.entityAdapter = entityAdapter.beginTransaction();      // each entity runspace start a transaction
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush();
        }
        return b;
    }

    public void flush(){
        entityAdapter.flush();
    }
}
