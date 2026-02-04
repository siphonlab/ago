package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

public interface ExpandableObject<T extends AgoClass> {
    Instance<?> expand();

    void fold();

    ObjectRefCallFrame<?> getExpander();

    Instance<T> getObjectRefInstance();

    boolean isExpanded();

    Instance<?> getExpandedInstance();

    ExpandableObject<?> expandFor(ExpandableObject<?> expander);
}
