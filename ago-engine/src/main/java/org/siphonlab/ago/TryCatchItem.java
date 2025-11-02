package org.siphonlab.ago;

public class TryCatchItem {
    public final  int begin;
    public final int end;
    public final  int handler;
    public final AgoClass[] exceptionClasses;

    public TryCatchItem(int begin, int end, int handler, AgoClass[] exceptionClasses) {
        this.begin = begin;
        this.end = end;
        this.handler = handler;
        this.exceptionClasses = exceptionClasses;
    }

    public int resolve(int pc, AgoClass exceptionClass){
        if(pc >= begin && pc <= handler){
            for (AgoClass agoClass : exceptionClasses) {
                if(agoClass == exceptionClass) return handler;
                if(exceptionClass.isThatOrDerivedFrom(agoClass)){
                    return handler;
                }
            }
        }
        return -1;
    }
}
