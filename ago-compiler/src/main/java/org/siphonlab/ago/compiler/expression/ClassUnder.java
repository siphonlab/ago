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


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Collection;
import java.util.Objects;

public abstract class ClassUnder extends ExpressionBase implements MaybeFunction{

    public final Expression object;
    public final ClassDef classDef;

    protected ClassUnder(Expression object, ClassDef classDef) throws CompilationError {
        this.object = object.transform();
        this.object.setParent(this);
        this.classDef = classDef;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        var m = classDef.getMetaClassDef();
        if(m == null){
            return new PhantomMetaClassDef(classDef);
        }
        return m;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public static ClassUnder create(Expression scopeExpr, ClassDef subclass) throws CompilationError {
        scopeExpr = scopeExpr.transform();
        if(scopeExpr instanceof Scope scope){
            return new ClassUnderScope(scope, subclass);
        } else {
            return new ClassUnderInstance(scopeExpr, subclass);
        }
    }

    @Override
    public boolean isFunction() {
        return classDef instanceof FunctionDef;
    }

    @Override
    public FunctionDef getFunction() {
        return isFunction() ? (FunctionDef)classDef : null;
    }
    private Collection<FunctionDef> candidateFunctions;
    @Override
    public void setCandidates(Collection<FunctionDef> candidates) {
        this.candidateFunctions = candidates;
    }

    @Override
    public Collection<FunctionDef> getCandidates() {
        return candidateFunctions;
    }

    @Override
    public Expression getScopeOfFunction() {
        return this.object;
    }

    public Expression getScope(){
        return this.object;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClassUnder that)) return false;
        return Objects.equals(object, that.object) && Objects.equals(classDef, that.classDef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, classDef);
    }

    public static class ClassUnderScope extends ClassUnder {

        public ClassUnderScope(Scope object, ClassDef classDef) throws CompilationError {
            super(object, classDef);
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if (localVar instanceof Var.ReusingLocalVar reusingLocalVar && reusingLocalVar.isOutputted())
                return;

            blockCompiler.getCode().bindClassUnderScope(localVar.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(this.getClassDef()),  ((Scope)this.getScope()).getDepth());

            if (localVar instanceof Var.ReusingLocalVar reusingLocalVar) {
                reusingLocalVar.setOutputted(true);
            }

        }

        @Override
        public String toString() {
            return "(ClassUnderScope %s %s)".formatted(((Scope)object).getDepth(), classDef);
        }

        @Override
        public ClassUnderScope setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClassUnderScope that = (ClassUnderScope) o;
            return Objects.equals(this.object, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ClassUnderScope.class, this.object);
        }
    }

    public static class ClassUnderInstance extends ClassUnder{

        protected ClassUnderInstance(Expression object, ClassDef classDef) throws CompilationError {
            super(object, classDef);
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                Var.LocalVar obj = (Var.LocalVar) object.visit(blockCompiler);
                blockCompiler.getCode().bindClassUnderInstance(localVar.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(this.getClassDef()), obj.getVariableSlot());
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public String toString() {
            return "(ClassUnder %s %s)".formatted(object, classDef);
        }

        @Override
        public ClassUnderInstance setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

    }

}
