package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots;
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceInstanceTrait;
import org.siphonlab.ago.runtime.rdb.json.JsonRefSlots;

public class LazyJsonRefSlots extends RdbRefSlots implements JsonRefSlots {

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

    @Override
    public void setObject(int slot, Instance<?> value) {
        super.setObject(slot, value);
        if (value instanceof ReferenceInstanceTrait recomposeInstance) {
            recomposeInstance.addReference(this.baseSlots, slot);
        }
    }

    @Override
    public Instance<?> getObject(int slot) {
        Instance<?> value = super.getObject(slot);
        if (value instanceof ReferenceInstanceTrait recomposeInstance) {
            recomposeInstance.addReference(this.baseSlots, slot);
        }
        return value;
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
    public void setId(long id) {
        this.restoreId(id);
    }


}
