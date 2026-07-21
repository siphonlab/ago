package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;

import java.util.function.Consumer;

public interface CreateInstanceRunSpace<Id> {
    Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer);

    CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer);

}
