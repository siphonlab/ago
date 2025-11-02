package org.siphonlab.ago.runtime.stateful;

import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.reactive.RunningState;

public interface StatefulCallFrame {

    RunningState getRunningState();

    void setRunningState(RunningState runningState);

    Slots getSlots();
}
