package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoTrait extends AgoClass{

    protected AgoTrait(AgoClassLoader classLoader, String fullname, String name) {
        super(classLoader, fullname, name);
        this.type = AgoClass.TYPE_TRAIT;
    }

    public AgoTrait(AgoClassLoader classLoader,MetaClass metaClass, String fullname, String name) {
        super(classLoader, metaClass, fullname, name);
        this.type = AgoClass.TYPE_TRAIT;
    }

    @Override
    public AgoTrait cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoTrait(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

}
