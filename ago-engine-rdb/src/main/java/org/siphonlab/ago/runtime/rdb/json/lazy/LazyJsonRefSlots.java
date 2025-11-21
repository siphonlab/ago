package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots;
import org.siphonlab.ago.runtime.rdb.json.JsonRefSlots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LazyJsonRefSlots extends RdbRefSlots implements JsonRefSlots {

    private final static Logger logger = LoggerFactory.getLogger(LazyJsonRefSlots.class);

    private final JsonSlotMapper jsonSlotMapper;
    private Instance<?> owner;
    private CallFrame callFrame;
    private boolean saved;

    public LazyJsonRefSlots(Slots baseSlots, ObjectRef objectRef, JsonSlotMapper jsonSlotMapper) {
        super(baseSlots, objectRef);
        this.jsonSlotMapper = jsonSlotMapper;
    }

    @Override
    public long getId() {
        return this.getObjectRef().id();
    }

    public JsonSlotMapper getJsonSlotMapper() {
        return jsonSlotMapper;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public Instance<?> getOwner() {
        return owner;
    }

    public void setOwner(Instance<?> owner) {
        this.owner = owner;
        if (owner instanceof CallFrame callFrame) {
            this.callFrame = callFrame;
        }
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        var old = super.getObject(slot);
        if (ObjectRefOwner.equals(old, value)) {
            return;
        }
        ReferenceCounter.releaseRef(old, ReferenceCounter.Reason.SetSlotDrop, this.owner);

        // always set ObjectRefInstance, don't use DeferenceObject, that will break reference count
        // when saveInstance, it releases ref of DeferenceObject
        // however when restore, it increases ref of ObjectRefInstance
        // TODO this is the only usage about creator, once I found some other way, I'll remove creator
        boolean alreadyExpanded = false;
        if(value instanceof DeferenceObject deferenceObject){
            value = (Instance<?>) deferenceObject.toObjectRefInstance();
            var creator = deferenceObject.getDeferenceObjectState().getCreator();
            alreadyExpanded = Objects.equals(creator, ObjectRefOwner.extractObjectRef(callFrame));
        }

        // put into result slots and take away, still will cause refcount become 0
        if (value instanceof ReferenceCounter referenceCounter) {
            if(referenceCounter.getRefCount() == 0){
                if(value instanceof ObjectRefObject objectRefObject){
                    objectRefObject.fixCache();
                }
            }
        }

        if(value instanceof ObjectRefObject && callFrame != null){
            value = (Instance<?>) ((ObjectRefObject) value).createExpander(callFrame, alreadyExpanded);
        }


        ReferenceCounter.increaseRef(value, ReferenceCounter.Reason.SetSlotInstall, this.owner);

        super.setObject(slot, value);
    }

    @Override
    public void setId(long id) {
        this.restoreId(id);
    }
}
