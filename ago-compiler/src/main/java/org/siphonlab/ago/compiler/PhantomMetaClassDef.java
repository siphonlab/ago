package org.siphonlab.ago.compiler;

public class PhantomMetaClassDef extends MetaClassDef{

    private Root root;

    public PhantomMetaClassDef(ClassDef instanceClassDef) {
        super(instanceClassDef, instanceClassDef instanceof MetaClassDef ? 1 : 2, null);
        this.setRoot(instanceClassDef.getRoot());
    }

    public void setRoot(Root root){
        this.root = root;
    }

    @Override
    public Root getRoot() {
        return root;
    }
}
