package org.siphonlab.ago.runtime.rdb.reactive.semischema;

import org.agrona.concurrent.SnowflakeIdGenerator;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public class PGJsonSlotsCreatorFactory implements SlotsCreatorFactory {

    private SemiSchemaPGAdapter adapter;

    public PGJsonSlotsCreatorFactory(SemiSchemaPGAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        return new SlotsCreator() {
            @Override
            public Slots create() {
                long id = adapter.nextId();
                ObjectRef objectRef = new ObjectRef(agoClass.getFullname(), id);
                var r = new JsonRefSlots(objectRef, adapter.getSlotsAdapter(), agoClass.getSlotDefs());
                r.setSaved(false);
                return r;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return DefaultSlotsCreatorFactory.typeOf(agoClass.getSlotDefs()[slotIndex].getTypeCode());
            }
        };
    }

    public void setAdapter(SemiSchemaPGAdapter adapter) {
        this.adapter = adapter;
    }
}

