package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.TypeCode;

import java.util.Objects;

public class LongObjectRef extends ObjectRef<Long>{
    private final long id;

    public LongObjectRef(String className, long id) {
        super(className);
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    public long longId(){
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LongObjectRef) obj;
        return Objects.equals(this.className, that.className) &&
                this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, id);
    }

}
