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

    private List<InstanceUser> references = new LinkedList<>();

    private Instance deferenced = null;

    public void addReference(Slots owner, int slot) {
        this.references.add(new InstanceUser(owner, slot));
    }

    public Instance<?> deference() {
        if(deferenced) return deferenced;

        var inst = doDeference();
        for (InstanceUser user : this.references) {
            user.owner().setObject(user.slot(), inst);
        }
        this.references.clear();
        return deferenced = inst;
    }

    public abstract Instance<?> doDeference();


}
