package org.siphonlab.ago.classloader;

public class SwitchTableDesc {
    public int id;
    byte type;      // 1 dense, 2 sparse
    int[] data;
}
