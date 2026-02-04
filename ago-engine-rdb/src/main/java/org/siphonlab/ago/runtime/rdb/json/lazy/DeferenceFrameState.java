package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;

// share state of DeferenceAgoFrame and DeferenceNativeFrame
public class DeferenceFrameState extends DeferenceObjectState {

    // for objRefFrame.deference(), to deference to EntranceFrame
    boolean isEntrance = false;
    boolean isAsyncEntrance = false;

    public DeferenceFrameState(ObjectRefCallFrame objectRefInstance) {super(objectRefInstance);}


    public boolean isEntrance() {
        return isEntrance;
    }

    public boolean isAsyncEntrance() {
        return isAsyncEntrance;
    }

    public void setEntrance(boolean entrance) {
        isEntrance = entrance;
    }

    public void setAsyncEntrance(boolean asyncEntrance) {
        isAsyncEntrance = asyncEntrance;
    }
}
