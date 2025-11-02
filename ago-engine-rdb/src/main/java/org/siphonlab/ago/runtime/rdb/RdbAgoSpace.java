package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;

public class RdbAgoSpace extends AgoRunSpace {


    public RdbAgoSpace(AgoEngine agoEngine, RunSpaceHost runSpaceHost) {
        super(agoEngine, runSpaceHost);
    }

    @Override
    public void run() {
        for (var cf = this.currCallFrame; cf != null; cf = this.currCallFrame) {
            cf.run();

            save(cf);     // save after function run

            if (this.currCallFrame == cf) {       // not set new CallFrame
                this.currCallFrame = null;
                return;
            }
        }
    }

    @Override
    public Object awaitTillComplete(CallFrame<?> frame) {
        if(frame.getRunSpace() instanceof RdbAgoSpace rdbAgoSpace){
            rdbAgoSpace.save(frame);
        }
        return super.awaitTillComplete(frame);
    }

    protected void save(Instance<?> instance){
        ((RdbEngine)getAgoEngine()).saveInstance(instance);
    }

}
