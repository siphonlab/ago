package org.siphonlab.ago;

import org.agrona.collections.Int2IntHashMap;

public class SparseSwitchTable extends SwitchTable{
    final Int2IntHashMap map = new Int2IntHashMap(-1);
    final int defaultAddress;
    public SparseSwitchTable(int[] data) {
        defaultAddress = data[0];
        for (int i = 1; i < data.length; ) {
            map.put(data[i++], data[i++]);
        }
    }
    public int resolve(int key){
        return map.getOrDefault(key,defaultAddress);
    }

    public Int2IntHashMap getMap() {
        return map;
    }
}
