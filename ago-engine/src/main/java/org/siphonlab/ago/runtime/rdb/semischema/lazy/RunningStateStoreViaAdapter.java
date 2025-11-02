package org.siphonlab.ago.runtime.rdb.semischema.lazy;

import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.stateful.RunningStateStore;
import org.siphonlab.ago.runtime.stateful.StatefulCallFrame;

public class RunningStateStoreViaAdapter extends RunningStateStore {

    private final RdbAdapter rdbAdapter;

    public RunningStateStoreViaAdapter(RdbAdapter rdbAdapter){
        this.rdbAdapter = rdbAdapter;
    }

    @Override
    public void saveState(StatefulCallFrame callFrame) {
        this.rdbAdapter.saveCallFrameState(callFrame);
    }
}
