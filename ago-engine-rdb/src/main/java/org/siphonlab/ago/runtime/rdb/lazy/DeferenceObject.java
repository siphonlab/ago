package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.releaseDeferenceSlotsAndContext;

public interface DeferenceObject {

    ObjectRefInstanceTrait toObjectRefInstance();

    boolean isSaveRequired();

    void markSaved();

    public void releaseSlotsDeference(ReferenceCounter.Reason reason);
    public void increaseSlotsDeference(ReferenceCounter.Reason reason);

    default void releaseSlotsDeference(RdbSlots slots, ReferenceCounter.Reason reason) {
        for (var p : slots.getObjectSlots()){
            if(p == null) continue;
            Instance<?> obj = p.getLeft();
            if(obj instanceof ReferenceCounter rc){
                rc.releaseRef(reason);
            }
            releaseDeferenceSlotsAndContext(obj);
//            if (obj instanceof ReferenceCounter rc) {
//                //  for ObjRefInstance, if down to zero, it removed from cache
//                //  for DeferenceInstance, if down to zero, the deference cache of ObjRefInstance will clean up
//                rc.releaseRef(reason);
//            }
        }
    }

    default void increaseSlotsDeference(RdbSlots slots, ReferenceCounter.Reason reason) {
        for (var p : slots.getObjectSlots()) {
            if (p == null) continue;
            Instance<?> obj = p.getLeft();
            if (obj instanceof ReferenceCounter rc) {
                rc.increaseRef(reason);
            }
        }
    }

}
