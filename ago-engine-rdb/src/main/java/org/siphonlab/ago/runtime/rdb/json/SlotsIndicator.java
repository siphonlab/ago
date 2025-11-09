package org.siphonlab.ago.runtime.rdb.json;

import org.siphonlab.ago.Instance;

// wrap an instance and indicate that should stringify with SlotsJsonSerializer
public class SlotsIndicator {
    private final Instance<?> instance;

    public SlotsIndicator(Instance<?> instance) {this.instance = instance;}

    public Instance<?> getInstance() {
        return instance;
    }
}
