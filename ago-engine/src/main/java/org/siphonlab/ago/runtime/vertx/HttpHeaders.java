package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.MultiMap;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpHeaders {

    private static MultiMap get(NativeFrame frame){
        return (MultiMap) frame.getParentScope().getNativePayload();
    }

    public static void getAll(NativeFrame frame, String name){
        var list = get(frame).getAll(name);
        if(list == null || list.isEmpty()){
            frame.finishObject(null);
        } else {
            var listClz = frame.getAgoEngine().getClass("lang.collection.ArrayList");
            Instance<?> agoList = frame.getAgoEngine().createNativeInstance(null, listClz, frame.getRunSpace());
            agoList.setNativePayload(list);
            frame.finishObject(agoList);
        }
    }

    public static void contains(NativeFrame frame, String name){
        frame.finishBoolean(get(frame).contains(name));
    }

    public static void size_get(NativeFrame frame){
        frame.finishInt(get(frame).size());
    }

}
