package org.siphonlab.ago.runtime.rdb;

import org.agrona.concurrent.SnowflakeIdGenerator;
import org.siphonlab.ago.*;

public class RdbSlotsCreatorFactory implements SlotsCreatorFactory {

    private final DefaultSlotsCreatorFactory baseSlotFactory;
    private final RdbAdapter rdbAdapter;

    public RdbSlotsCreatorFactory(RdbAdapter rdbAdapter){
        this.rdbAdapter = rdbAdapter;
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var creator = baseSlotFactory.generateSlotsCreator(agoClass);
        return new SlotsCreator() {
            @Override
            public Slots create() {
                var baseSlots = (creator == null)? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
                var slots = new RdbSlots(baseSlots);
                slots.setId(rdbAdapter.idGenerator.nextId());
                return slots;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return creator.getSlotType(slotIndex);
            }
        };
    }

}
