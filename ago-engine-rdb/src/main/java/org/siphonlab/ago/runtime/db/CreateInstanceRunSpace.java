package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.lazy.DereferenceContextSlots;

import java.util.function.Consumer;

public interface CreateInstanceRunSpace<Id> {
    Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer);

    CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer);

    Instance<?> createArrayInstance(AgoClass arrayType, int length, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer);

    void updateDeferenceContext(Instance<?> instance, DereferenceContextSlots<Id> slots);
}
