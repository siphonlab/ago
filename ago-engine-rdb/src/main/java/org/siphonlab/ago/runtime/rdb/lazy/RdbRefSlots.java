package org.siphonlab.ago.runtime.rdb.lazy;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbSlots;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class RdbRefSlots extends RdbSlots implements ObjectRefOwner {

    private ObjectRef objectRef;

    /**
     *
     * @param baseSlots
     * @param objectRef
     */
    public RdbRefSlots(Slots baseSlots, ObjectRef objectRef) {
        super(baseSlots);
        this.objectRef = objectRef;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public void restoreId(long id){
        this.objectRef = new ObjectRef(objectRef.className(), id);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        if(value == null) {
            super.setObject(slot,null);
            return;
        }
        var objectRef = ObjectRefOwner.extractObjectRef(value);
        if(value instanceof ObjectRefInstanceTrait){
            var stream = Arrays.stream(this.getObjectSlots());
            if(this.getUsingInstances() != null){
                stream = Stream.concat(stream,this.getUsingInstances().stream());
            }
            var existed = stream.filter(instance -> objectRef.equals(ObjectRefOwner.extractObjectRef(instance)) && !(instance instanceof ObjectRefInstanceTrait)).findFirst();
            if(existed.isPresent()){
                value = existed.get();
            }
            // don't check other slots
        } else {
            Instance<?>[] objectSlots = this.getObjectSlots();
            if(objectSlots != null) {
                for (int i = 0; i < objectSlots.length; i++) {
                    Instance<?> item = objectSlots[i];
                    if (item == null) continue;
                    if (objectRef.equals(ObjectRefOwner.extractObjectRef(item))) {
                        this.getBaseSlots().setObject(i, value);
                        objectSlots[i] = value;
                    }
                }
            }
            Set<Instance<?>> usingInstances = this.getUsingInstances();
            if(usingInstances != null)
                usingInstances.removeIf(item -> objectRef.equals(ObjectRefOwner.extractObjectRef(item)));
        }
        super.setObject(slot,value);
    }
}
