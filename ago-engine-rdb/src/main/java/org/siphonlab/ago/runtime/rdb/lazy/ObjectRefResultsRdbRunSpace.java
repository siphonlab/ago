package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbAgoRunSpace;
import org.siphonlab.ago.runtime.rdb.RdbEngine;

public class ObjectRefResultsRdbRunSpace extends RdbAgoRunSpace {

    public ObjectRefResultsRdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, rdbAdapter, runSpaceHost);
        this.resultSlots = new LazyResultSlots();
    }

    public ObjectRefResultsRdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost, long id) {
        super(agoEngine, rdbAdapter, runSpaceHost, id);
        this.resultSlots = new LazyResultSlots();
    }

    @Override
    protected boolean tryComplete() {
        var r = super.tryComplete();
        if(r){
            ((LazyResultSlots)resultSlots).cleanObjectResult();
        }
        return r;
    }
}
