package org.siphonlab.ago.compiler;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Package extends ClassContainer{


    private List<Consumer<ClassDef>> classDeclListeners = new ArrayList<>();

    public Package(String name) {
        super(name);
    }

    public Package(String name, Namespace parent) {
        super(name, parent);
    }

    @Override
    public void addChild(ClassDef child) {
        super.addChild(child);
        this.classDeclListeners.forEach(h -> h.accept(child));
    }

    public void addClassDeclListener(Consumer<ClassDef> handler) {
        this.classDeclListeners.add(handler);
    }

}
