package org.siphonlab.ago.runtime.rdb;

import java.util.Objects;

public final class ObjectRef {
    private final String className;
    private final long id;

    public ObjectRef(String className, long id) {
        this.className = className;
        this.id = id;
    }

    public String className() {return className;}

    public long id() {return id;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ObjectRef) obj;
        return Objects.equals(this.className, that.className) &&
                this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, id);
    }

    @Override
    public String toString() {
        return "ObjectRef[" +
                "className=" + className + ", " +
                "id=" + id + ']';
    }
}
