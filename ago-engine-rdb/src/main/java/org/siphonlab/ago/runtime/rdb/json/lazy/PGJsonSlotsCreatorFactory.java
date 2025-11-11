package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

import java.util.function.Supplier;

public class PGJsonSlotsCreatorFactory implements SlotsCreatorFactory {

    private LazyJsonPGAdapter adapter;

    private final DefaultSlotsCreatorFactory baseSlotFactory;

    public PGJsonSlotsCreatorFactory() {
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var creator = baseSlotFactory.generateSlotsCreator(agoClass);
        return new JsonRefSlotsCreator(creator, agoClass, () -> adapter);
    }

    public void setAdapter(LazyJsonPGAdapter adapter) {
        this.adapter = adapter;
    }


    public static class JsonRefSlotsCreator implements SlotsCreator {
        private final SlotsCreator creator;
        private final AgoClass agoClass;
        private final Supplier<LazyJsonPGAdapter> adapterSupplier;

        public JsonRefSlotsCreator(SlotsCreator creator, AgoClass agoClass, Supplier<LazyJsonPGAdapter> adapterSupplier) {
            this.creator = creator;
            this.agoClass = agoClass;
            this.adapterSupplier = adapterSupplier;
        }

        @Override
        public Slots create() {
            long id = adapterSupplier.get().nextId();
            ObjectRef objectRef = new ObjectRef(agoClass.getFullname(), id);
            return create(objectRef);
        }

        public Slots create(ObjectRef objectRef) {
            Slots baseSlots = creator == null ? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
            var r = new LazyJsonRefSlots(baseSlots, objectRef, new JsonSlotMapper(agoClass.getSlotDefs()) {
                @Override
                public String mapType(TypeCode typeCode, AgoClass agoClass) {
                    return adapterSupplier.get().mapType(typeCode, agoClass).getTypeName();
                }
            });
            if(agoClass.getSlotDefs() != null) {
                r.allocateObjectSlots(agoClass.getSlotDefs().length);
            }
            return r;
        }

        @Override
        public Class<?> getSlotType(int slotIndex) {
            return DefaultSlotsCreatorFactory.typeOf(agoClass.getSlotDefs()[slotIndex].getTypeCode());
        }
    }
}
