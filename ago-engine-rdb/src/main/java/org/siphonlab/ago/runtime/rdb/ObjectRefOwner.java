package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;

import java.util.Objects;

public interface ObjectRefOwner {
    static ObjectRef extractCreator(Instance instance) {
        if(instance instanceof DeferenceObject){
            return ((DeferenceObject) instance).getDeferenceObjectState().getCreator();
        }
        return null;
    }

    ObjectRef getObjectRef();

    static ObjectRef extractObjectRef(Instance<?> instance) {
        if (instance == null) return null;
        if (instance instanceof CallFrame<?> callFrame) {
            return extractObjectRef(callFrame);
        }
        if (instance instanceof ObjectRefOwner) {
            return ((ObjectRefOwner) instance).getObjectRef();
        } else if(instance.getSlots() instanceof ObjectRefOwner slots){
            return slots.getObjectRef();
        }
        return null;
    }

    static ObjectRef extractObjectRef(CallFrame<?> instance) {
        if(instance instanceof EntranceCallFrame<?> entranceCallFrame){
            instance = entranceCallFrame.getInner();
        }
        if (instance == null) return null;
        if (instance instanceof ObjectRefOwner) {
            return ((ObjectRefOwner) instance).getObjectRef();
        } else if (instance.getSlots() instanceof ObjectRefOwner slots) {
            return slots.getObjectRef();
        }
        return null;
    }

    static boolean equals(Instance<?> a, Instance<?> b){
        if(a == b) return true;
        if((a==null)!=(b==null)) return false;
        ObjectRef aref = extractObjectRef(a);
        ObjectRef bref = extractObjectRef(b);
        if ((aref == null) != (bref == null)) return false;
        if(aref == null) return Objects.equals(a,b);
        return Objects.equals(aref, bref);
    }
}
