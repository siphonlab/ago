package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.SlotsCreator;

public interface DbSlotsCreator<Id> extends SlotsCreator {
    DbSlots<Id> create(ObjectRef<Id> objectRef);

    static <Id> Slots create(AgoClass agoClass, ObjectRef<Id> objectRef) {
        SlotsCreator slotsCreator = agoClass.getSlotsCreator();
        if(slotsCreator instanceof DbSlotsCreator){
            DbSlotsCreator<Id> dbSlotsCreator = (DbSlotsCreator<Id>) slotsCreator;
            return dbSlotsCreator.create(objectRef);
        } else {
            return slotsCreator.create();
        }
    }
}
