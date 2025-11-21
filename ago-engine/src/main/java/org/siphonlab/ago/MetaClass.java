package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class MetaClass extends AgoClass{

    private AgoClass instanceClass;

    private MetaClass(AgoClassLoader classLoader) {
        super(classLoader, null, "<Meta>", "<Meta>");
        this.type = AgoClass.TYPE_METACLASS;
    }

    public static MetaClass createTheMeta(AgoClassLoader classLoader){
        return new MetaClass(classLoader);
    }

    public MetaClass(AgoClassLoader classLoader, MetaClass metaClass, String name) {
        super(classLoader, metaClass, name, name);
        this.type = AgoClass.TYPE_METACLASS;
    }

    public void setInstanceClass(AgoClass instanceClass) {
        this.instanceClass = instanceClass;
    }

    public AgoClass getInstanceClass() {
        return instanceClass;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetaClass m && m.instanceClass.equals(this);
    }

    @Override
    public boolean isInGenericTemplate() {
        return this.instanceClass != null && (this.instanceClass.isGenericTemplate() || this.instanceClass.isInGenericTemplate());
    }

    @Override
    public MetaClass cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        throw new UnsupportedOperationException("MetaClass cannot bind scope");
    }

}
