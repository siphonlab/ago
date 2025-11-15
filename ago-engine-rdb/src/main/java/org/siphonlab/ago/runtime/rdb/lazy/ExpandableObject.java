package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;

public interface ExpandableObject<T extends AgoClass> {
    Instance<?> expand();

    void fold();

    CallFrame<?> getExpander();

    Instance<T> getObjectRefInstance();

    boolean isExpanded();

    Instance<?> getExpandedInstance();
}
