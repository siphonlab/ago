package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoInterface extends AgoClass{

    protected AgoInterface(AgoClassLoader classLoader, String fullname, String name) {
        super(classLoader, fullname, name);
        this.type = AgoClass.TYPE_INTERFACE;
    }

    public AgoInterface(AgoClassLoader classLoader, MetaClass metaClass, String fullname, String name) {
        super(classLoader, metaClass, fullname, name);
        this.type = AgoClass.TYPE_INTERFACE;
    }

    @Override
    public AgoInterface withScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoInterface(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

}
