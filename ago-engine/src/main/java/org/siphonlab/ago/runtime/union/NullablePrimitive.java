package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.Union;

public abstract class NullablePrimitive implements Union {

    protected final boolean isNull;

    protected NullablePrimitive(boolean isNull) {
        this.isNull = isNull;
    }

    public abstract NullablePrimitive setNull();

    @Override
    public boolean isNull() {
        return isNull;
    }

}
