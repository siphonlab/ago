package org.siphonlab.collection;

import java.util.Arrays;

public class IntSortedArraySet {
    private int[] elements;
    private int size;

    public IntSortedArraySet() {
        elements = new int[16];
        size = 0;
    }

    public boolean add(int value) {
        int index = Arrays.binarySearch(elements, 0, size, value);
        if (index >= 0) {
            return false;
        }
        index = -index - 1;
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, elements.length * 2);
        }
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
        return true;
    }

    // 检查元素是否存在
    public boolean contains(int value) {
        return Arrays.binarySearch(elements, 0, size, value) >= 0;
    }

    public int size() {
        return size;
    }
}