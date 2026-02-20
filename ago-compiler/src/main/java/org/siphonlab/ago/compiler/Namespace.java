/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.compiler;

import org.siphonlab.collection.DuplicatedKeyException;

import java.util.*;

public class Namespace<C extends Namespace>{

    protected Namespace parent;

    protected String name;

    public Namespace(String name) {
        this.name = name;
    }

    public Namespace(String name, Namespace parent){
        this.name = name;
        parent.addChild(this);
    }

    public String getFullname(){
        if(this.parent != null && !(this.parent instanceof Root || this.parent.getName().isEmpty())){
            return this.parent.getFullname() + "." + name;
        }
        return this.name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private NamespaceCollection<C> children = new NamespaceCollection<>(false);
    private NamespaceCollection<Namespace<?>> allDescendants = new NamespaceCollection<>(true);

    public void addChild(C child){
        if(child.parent == null) {
            child.parent = this;
        }
        this.children.add(child);

        if(child.getParent() != this) return;       // inherited child, shouldn't append to descendant

        for (var p = this; p != null; p = p.parent) {
            p.appendDescendant(child);
        }
        if(!child.getChildren().isEmpty()){
            var stack = new Stack<C>();
            stack.addAll(child.getUniqueChildren());
            while(!stack.isEmpty()){
                var c = stack.pop();
                if(this.appendDescendant(c)) {
                    for (var p = this.parent; p != null; p = p.parent) {
                        p.appendDescendant(c);
                    }
                    stack.addAll(c.getUniqueChildren());
                }
            }
        }
    }

    public boolean appendDescendant(Namespace<?> child){
        var existed = this.allDescendants.get(child.getFullname());
        if(existed != null){
            if(existed == child) return false;
            throw new DuplicatedKeyException("'%s' already existed".formatted(child.getFullname()));
        }
        this.allDescendants.add(child);
        return true;
    }

    public C getChild(String name){
        return this.children.get(name);
    }

    public <C1 extends Namespace> C1 findByFullname(String name){
        return (C1) this.allDescendants.get(name);
    }

    public NamespaceCollection<C> getChildren() {
        return children;
    }

    public Collection<C> getUniqueChildren(){
        return children.getUniqueElements();
    }

    public NamespaceCollection<Namespace<?>> getAllDescendants() {
        return allDescendants;
    }

    public Namespace getParent() {
        return parent;
    }

    public Collection<C> getChildren(String name){
        return children.search(name);
    }

    public Collection<FunctionDef> findMethods(String commonName){
        return this.getChildren().getFunctionsByCommonName(commonName);
    }

    public FunctionDef findMethod(String fixedName){
        return (FunctionDef) this.getChild(fixedName);
    }

}
