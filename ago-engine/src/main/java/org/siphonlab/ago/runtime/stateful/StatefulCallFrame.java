package org.siphonlab.ago.runtime.stateful;

import org.siphonlab.ago.Slots;

public interface StatefulCallFrame {

    RunningState getRunningState();

    void setRunningState(RunningState runningState);

    Slots getSlots();
}
