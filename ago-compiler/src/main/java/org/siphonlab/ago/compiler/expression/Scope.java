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
package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;

import java.util.Collection;
import java.util.Objects;

public class Scope extends ExpressionBase implements MaybeFunction{

    private int depth;

    private ClassDef classDef;

    private boolean isPronoun;

    private NamePathResolver.PronounType pronounType;
    private ClassDef originalClass;

    public Scope(int depth, ClassDef classDef) {
        this.depth = depth;
        if(classDef.isTrait()){
            this.classDef = new TraitDefInScope(classDef);
        } else {
            this.classDef = classDef;
        }
        assert classDef != null;
    }

    @Override
    public Expression transform() throws CompilationError {
        if(classDef == null){
            throw new CompilationError("no class found at scope depth %s".formatted(depth), this.getSourceLocation());
        }
        return super.transform();
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(localVar instanceof Var.ReusingLocalVar reusingLocalVar && reusingLocalVar.isOutputted())
            return;

        blockCompiler.enter(this);
        blockCompiler.getCode().loadScope(this.depth, localVar.getVariableSlot());
        blockCompiler.leave(this);

        if (localVar instanceof Var.ReusingLocalVar reusingLocalVar){
             reusingLocalVar.setOutputted(true);
        }
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return classDef;
    }

    // for `super`, need provide the originalClass
    public Scope fromPronoun(NamePathResolver.PronounType pronounType, ClassDef originalClass) {
        isPronoun = true;
        this.pronounType = pronounType;
        this.originalClass = originalClass == null ? classDef : originalClass;
        return this;
    }

    public Scope fromPronoun(NamePathResolver.PronounType pronounType) {
        return fromPronoun(pronounType, null);
    }

    public NamePathResolver.PronounType getPronounType() {
        return pronounType;
    }

    public Scope getParentScope(){
        ClassDef parentClass = this.getClassDef().getParentClass();
        return parentClass == null ? null : new Scope(this.depth + 1, parentClass);
    }

    public boolean isPronoun() {
        return isPronoun;
    }

    @Override
    public String toString() {
        return "(Scope %s %s)".formatted(depth, classDef.getFullname());
    }

    @Override
    public Scope setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean isFunction() {
        return isPronoun;
    }

    @Override
    public FunctionDef getFunction() {
        if(isPronoun){
            return this.classDef.getConstructor();
        }
        return null;
    }

    Collection<FunctionDef> candidates;
    @Override
    public void setCandidates(Collection<FunctionDef> candidates) {
        this.candidates = candidates;
    }

    @Override
    public Collection<FunctionDef> getCandidates() {
        return this.candidates == null ? this.classDef.getConstructors() : this.candidates;
    }

    @Override
    public Expression getScopeOfFunction() {
        return this;
    }

    public ClassDef getOriginalClass() {
        return originalClass;
    }

    public static class Local extends Scope{

        public Local(ClassDef classDef) {
            super(0, classDef);
        }

        @Override
        public String toString() {
            return "(Local)";
        }

        @Override
        public Local setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Scope scope)) return false;
        return depth == scope.depth && classDef == scope.classDef;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Scope.class, depth, classDef);
    }
}
