package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

public record InstanceUser(Slots owner, int slot) {
}
