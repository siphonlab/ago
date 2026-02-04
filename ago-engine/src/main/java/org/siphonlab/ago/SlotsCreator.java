package org.siphonlab.ago;

public interface SlotsCreator {
     Slots create();
     Class<?> getSlotType(int slotIndex);
}
