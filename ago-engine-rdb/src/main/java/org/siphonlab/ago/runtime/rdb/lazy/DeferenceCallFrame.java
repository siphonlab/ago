package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceFrameState;

public interface DeferenceCallFrame extends DeferenceObject{

    DeferenceFrameState getDeferenceFrameState();
}
