package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.Union;

public abstract class Nullable implements Union {

    @Override
    public abstract boolean isNull();

    @Override
    public abstract Nullable setNull();
}
