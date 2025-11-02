package org.siphonlab.ago;

public class DenseSwitchTable extends SwitchTable{
    private final int[] data;
    public DenseSwitchTable(int[] data) {
        this.data = data;
    }

    @Override
    public int resolve(int key) {
        int p = key - data[0] + 1;
        return p < data.length ? data[p] : data[data.length - 1];
    }

    public int[] getData() {
        return data;
    }
}
