package org.siphonlab.ago.runtime.db;

import java.util.Objects;

public class StringObjectRef extends ObjectRef<String>{
    private final String id;

    public StringObjectRef(String className, String id) {
        super(className);
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StringObjectRef) obj;
        return Objects.equals(this.className, that.className) &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, id);
    }

}
