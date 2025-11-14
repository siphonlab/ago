package org.siphonlab.ago.runtime.rdb.lazy

import groovy.transform.CompileStatic
import org.siphonlab.ago.AgoRunSpace
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
public trait ReferenceInstanceTrait {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceInstanceTrait)

    private Instance deferenced = null;

    public Instance<?> deference() {
        if(deferenced) return deferenced;
        var inst = doDeference();
        return deferenced = inst;
    }

    Instance getExistedDeferenced(){
        return deferenced;
    }

    void setDeferenced(Instance deferenced){
        this.deferenced = deferenced
    }

    public abstract Instance<?> doDeference();

    public void cleanDeferencedInstance(){
        deferenced = null;
    }
}
