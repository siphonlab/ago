package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.RunSpaceHost;

/**
 * a runspace collecting changed, and flush when complete
 *
 */
public class EntityRunSpace<Id> extends RunSpace {

    private final EntityAdapter<Id> entityAdapter;

    public EntityRunSpace(AgoEngine agoEngine, RunSpaceHost runSpaceHost, EntityAdapter<Id> entityAdapter) {
        super(agoEngine, runSpaceHost);
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
