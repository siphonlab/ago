package org.siphonlab.ago.classloader;

import java.util.Arrays;

public class TryCatchItemDesc {
    public final  int begin;
    public final int end;
    public final  int handler;
    public final int[] exceptionClasses;

    public TryCatchItemDesc(int begin, int end, int handler, int[] exceptionClasses) {
        this.begin = begin;
        this.end = end;
        this.handler = handler;
        this.exceptionClasses = exceptionClasses;
    }

}
