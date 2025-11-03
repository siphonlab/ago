package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public class ReactiveJsonSlotsCreatorFactory implements SlotsCreatorFactory {

    private ReactiveJsonPGAdapter adapter;

    public ReactiveJsonSlotsCreatorFactory(ReactiveJsonPGAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        return new SlotsCreator() {
            @Override
            public Slots create() {
                long id = adapter.nextId();
                ObjectRef objectRef = new ObjectRef(agoClass.getFullname(), id);
                var r = new ReactiveJsonRefSlots(objectRef, adapter.getSlotsAdapter(), agoClass.getSlotDefs());
                r.setSaved(false);
                return r;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return DefaultSlotsCreatorFactory.typeOf(agoClass.getSlotDefs()[slotIndex].getTypeCode());
            }
        };
    }

    public void setAdapter(ReactiveJsonPGAdapter adapter) {
        this.adapter = adapter;
    }
}

