package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;

public class DeferenceObjectState {
    protected final ObjectRefObject objectRefInstance;
    protected boolean saveRequired = false;

    private ObjectRef creator;

    public DeferenceObjectState(ObjectRefObject objectRefInstance) {this.objectRefInstance = objectRefInstance;}

    public ObjectRefObject getObjectRefInstance() {
        assert (this.objectRefInstance != null);
        return objectRefInstance;
    }

    public void markSaved() {
        saveRequired = false;
    }

    public boolean isSaveRequired() {
        return saveRequired;
    }

    public void setSaveRequired() {
        this.saveRequired = true;
    }

    public ObjectRef getCreator() {
        return creator;
    }

    public void setCreator(ObjectRef creator) {
        this.creator = creator;
    }
}
