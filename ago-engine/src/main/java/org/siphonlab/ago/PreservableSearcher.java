package org.siphonlab.ago;

import java.util.*;
import java.util.function.Predicate;

public class PreservableSearcher<E> {

    private List<E> list;
    private int pos = 0;

    public PreservableSearcher(List<E> list) {
        this.list = list;
    }

    public E search(Predicate<? super E> predicate) {
        int size = this.list.size();
        if (size == 0) {
            return null;
        }

        int startPos = pos;

        for (int i = pos; i < size; i++) {
            E e = this.list.get(i);
            if (predicate.test(e)) {
                pos = i;
                return e;
            }
        }

        // from 0 → startPos-1
        if (startPos > 0) {
            for (int i = 0; i < startPos; i++) {
                E e = this.list.get(i);
                if (predicate.test(e)) {
                    pos = i;
                    return e;
                }
            }
        }

        return null;
    }

}
