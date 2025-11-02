package org.siphonlab.ago.runtime.rdb.lazy

import groovy.transform.CompileStatic
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
public trait ReferenceInstanceTrait {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceInstanceTrait)

    private CallFrame boundCallFrame;       // to instantiate Boxer

    private List<InstanceUser> references = new LinkedList<>();

    public void bindCallFrame(CallFrame<?> callFrame) {
        this.boundCallFrame = callFrame;
    }

    public CallFrame getBoundCallFrame() {
        return boundCallFrame;
    }

    public void addReference(Slots owner, int slot) {
        this.references.add(new InstanceUser(owner, slot));
    }

    public Instance<?> deference() {
        var inst = doDeference();
        for (InstanceUser user : this.references) {
            user.owner().setObject(user.slot(), inst);
        }
        this.references.clear();
        return inst;
    }

    public abstract Instance<?> doDeference();


}
