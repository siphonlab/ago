package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public interface DereferenceAdapter {

    Instance<?> dereference(ObjectRef objectRef);

    void release(ObjectRef objectRef);

    void repair(ObjectRef objectRef, ObjectRefObject objectRefObject);
}
