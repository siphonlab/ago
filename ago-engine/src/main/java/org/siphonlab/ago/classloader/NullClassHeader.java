package org.siphonlab.ago.classloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoNullClass;

import static org.siphonlab.ago.classloader.LoadingStage.BuildClass;
import static org.siphonlab.ago.classloader.LoadingStage.ResolveFunctionIndex;

public class NullClassHeader extends ClassHeader{

    public NullClassHeader(AgoClassLoader classLoader) {
        super("null", AgoClass.TYPE_CLASS, AgoClass.PUBLIC | AgoClass.FINAL, null, classLoader);
        this.setLoadingStage(LoadingStage.BuildClass);
        this.slotDescs = new SlotDesc[0];
    }

    @Override
    public AgoClass buildClass() {
        if(this.loadingStage != BuildClass) return this.agoClass;
        var agoClass =  new AgoNullClass(classLoader);
        this.agoClass = agoClass;
        classLoader.getClassByName().put(this.fullname, agoClass);
        this.setLoadingStage(LoadingStage.Done);
        return agoClass;
    }
}
