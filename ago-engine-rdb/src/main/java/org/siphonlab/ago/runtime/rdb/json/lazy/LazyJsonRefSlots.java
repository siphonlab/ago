package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
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
        if (old instanceof ReferenceCounter rc) {
            rc.releaseRef(ReferenceCounter.Reason.SetSlotDrop);
        }

        // always set ObjectRefInstance, don't use DeferenceObject, that will break reference count
        // when saveInstance, it releases ref of DeferenceObject
        // however when restore, it increases ref of ObjectRefInstance
        if(value instanceof DeferenceObject deferenceObject){
            value = (Instance<?>) deferenceObject.toObjectRefInstance();
        }
        if (value instanceof ReferenceCounter referenceCounter) {
            referenceCounter.increaseRef(ReferenceCounter.Reason.SetSlotInstall);
        }

        super.setObject(slot, value);
    }


    @Override
    public void setId(long id) {
        this.restoreId(id);
    }

    @Override
    public void setInt(int slot, int value) {
        logger.info("%s setInt %d, %d".formatted(this.getObjectRef(), slot, value));
        super.setInt(slot, value);
    }

    @Override
    public int getInt(int slot) {
        var r = super.getInt(slot);
        logger.info("%s getInt %d, got %d".formatted(this.getObjectRef(), slot, r));
        return r;
    }
}
