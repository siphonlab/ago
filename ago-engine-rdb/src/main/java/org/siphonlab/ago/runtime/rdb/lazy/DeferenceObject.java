package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceObjectState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.releaseDeferenceAndContext;

public interface DeferenceObject {
    final static Logger logger = LoggerFactory.getLogger(DeferenceObject.class);

    ObjectRefObject toObjectRefInstance();

    boolean isSaveRequired();

    void markSaved();

    public void releaseSlotsDeference(ReferenceCounter.Reason reason);
    public void increaseSlotsDeference(ReferenceCounter.Reason reason);

    default void releaseSlotsDeference(RdbSlots slots, ReferenceCounter.Reason reason) {
        if(logger.isDebugEnabled()) logger.debug("release slots of %s".formatted(slots));
        for (var p : slots.getObjectSlots()){
            if(p == null) continue;
            Instance<?> obj = p.getLeft();
            if(obj == null) continue;

            releaseDeferenceAndContext(obj, reason);

            // DON'T down to embedded objects
            // i.e. student.teacher.name = 'Jack'
            //      here student has a slot, teacher will get a slot too,
            //      every slot has business of itself

        }
    }

    default void increaseSlotsDeference(RdbSlots slots, ReferenceCounter.Reason reason) {
        for (var p : slots.getObjectSlots()) {
            if (p == null) continue;
            Instance<?> obj = p.getLeft();

            ReferenceCounter.increaseRef(obj, reason);
        }
    }

    DeferenceObjectState getDeferenceObjectState();

}
