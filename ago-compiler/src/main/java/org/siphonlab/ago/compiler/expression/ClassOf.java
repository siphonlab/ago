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
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.util.Collection;
import java.util.Objects;

public abstract class ClassOf extends ExpressionBase{

    protected int metaLevel = 1;

    public static Expression create(Expression object) throws CompilationError {
        object = object.transform();
        if(object instanceof Literal<?> literal) {
            return new ConstClass(literal.getClassDef());
        } else if(object instanceof ConstClass constClass){
            return new ConstClass(constClass.inferType());
        } else if(object instanceof ClassOf.ClassOfScope classOfScope) {
            return new ClassOfScope(classOfScope.scope, classOfScope.metaLevel + 1);
        } else if(object instanceof ClassOfInstance classOfInstance){
            return new ClassOfInstance(classOfInstance.expression, classOfInstance.metaLevel + 1);
        } else if(object instanceof Scope scope){
            return new ClassOfScope(scope).transformInner();
        } else {
            return new ClassOfInstance(object).transformInner();
        }
    }

    public int getMetaLevel() {
        return metaLevel;
    }

    public ClassDef getClassDef(ClassDef classDef) {
        if(metaLevel == 1) {
            return classDef;
        } else if(metaLevel == 2){
            return classDef.getMetaClassDef();
        } else if(metaLevel == 3){
            return classDef.getMetaClassDef().getMetaClassDef();
        } else {
            throw new UnsupportedOperationException("Unexpected metalevel: %s".formatted(metaLevel));
        }
    }

    public static class ClassOfScope extends ClassOf implements MaybeFunction{

        private final Scope scope;

        protected ClassOfScope(Scope scope) {
            this.scope = scope;
            scope.setParent(this);
        }

        public ClassOfScope(Scope scope, int level) {
            this(scope);
            this.metaLevel = level;
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return getClassDef().getMetaClassDef();
        }

        public ClassDef getClassDef() {
            return this.getClassDef(scope.getClassDef());
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if (localVar instanceof Var.ReusingLocalVar reusingLocalVar && reusingLocalVar.isOutputted())
                return;

            try {
                blockCompiler.enter(this);

                if (metaLevel == 1) {
                    blockCompiler.getCode().loadClassOfScope(scope.getDepth(), localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().loadMetaClassOfScope(scope.getDepth(), metaLevel, localVar.getVariableSlot());
                }
            } finally {
                blockCompiler.leave(this);
            }
            if (localVar instanceof Var.ReusingLocalVar reusingLocalVar) {
                reusingLocalVar.setOutputted(true);
            }
        }

        @Override
        protected ClassOfScope transformInner() throws CompilationError {
            return this;
        }

        @Override
        public String toString() {
            if(metaLevel > 1){
                return "(ClassOfScope %s lv:%s %s)".formatted(scope.getDepth(), metaLevel, this.getClassDef());
            } else {
                return "(ClassOfScope %s)".formatted(scope.getDepth());
            }
        }

        @Override
        public ClassOfScope setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean isFunction() {
            return scope.getClassDef() instanceof FunctionDef;
        }

        @Override
        public FunctionDef getFunction() {
            return isFunction() ? (FunctionDef)scope.getClassDef() : null;
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

        public Scope getScope() {
            return scope;
        }

        @Override
        public Scope getScopeOfFunction() {
            return scope.getParentScope();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClassOfScope that = (ClassOfScope) o;
            return Objects.equals(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ClassOfScope.class, scope);
        }
    }

    public static class ClassOfInstance extends ClassOf{

        private final Expression expression;

        protected ClassOfInstance(Expression expression) {
            try {
                this.expression = expression.transform();
            } catch (CompilationError compilationError) {
                throw new RuntimeException(compilationError);
            }
            this.expression.setParent(this);
        }

        protected ClassOfInstance(Expression expression, int level) {
            this.expression = expression;
            this.metaLevel = level;
        }

        @Override
        public Expression transformInner() {
            if(expression instanceof ClassOfInstance classOfInstance){  // meta2
                return new ClassOfInstance(expression, classOfInstance.metaLevel + 1);
            } else if(expression instanceof ClassOfScope classOfScope){     // meta2
                return new ClassOfScope(classOfScope.scope, classOfScope.metaLevel + 1);
            } else if(expression instanceof ConstClass constClass){
                return new ConstClass(constClass.getClassDef().getMetaClassDef());
            }
            return this;
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return getClassDef().getMetaClassDef();
        }

        public ClassDef getClassDef() throws CompilationError {
            return super.getClassDef(expression.inferType());
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                Var.LocalVar term = (Var.LocalVar) expression.visit(blockCompiler);
                blockCompiler.getCode().loadClassOfInstance(this.metaLevel, localVar.getVariableSlot(), term.getVariableSlot());
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public String toString() {
            if(metaLevel > 1){
                return "(ClassOf %s lv:%s)".formatted(expression, metaLevel);
            } else {
                return "(ClassOf %s)".formatted(expression);
            }
        }

        @Override
        public ClassOfInstance setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ClassOfInstance that)) return false;
            return Objects.equals(expression, that.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(expression);
        }
    }

    public static class ClassOfScopedClassInterval extends ClassOf{
        private final Expression scopedClassIntervalInstance;
        private final MetaClassDef metaClassDef;

        public ClassOfScopedClassInterval(Expression scopedClassIntervalInstance, MetaClassDef metaClassDef) {
            this.scopedClassIntervalInstance = scopedClassIntervalInstance.setParent(this);
            this.metaClassDef = metaClassDef;
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return metaClassDef;
        }

        public ClassDef getClassDef() throws CompilationError {
            ScopedClassIntervalClassDef s = (ScopedClassIntervalClassDef) scopedClassIntervalInstance.inferType();
            return s.getLBoundClass();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            // just extract stored BoundClass from slots, needn't support reusable
            try {
                blockCompiler.enter(this);

                var classDef = blockCompiler.getFunctionDef();
                ClassDef classInterval = classDef.getRoot().getScopedClassInterval();
                if (!this.scopedClassIntervalInstance.inferType().isDeriveFrom(classInterval)) {
                    throw new TypeMismatchError("a ClassInterval expression expected", this.getSourceLocation());
                }

                var fld = classInterval.getVariable("boxedClass");
                Var.LocalVar instance = (Var.LocalVar) scopedClassIntervalInstance.visit(blockCompiler);
                blockCompiler.getCode().castScopedClassIntervalToBoundClass(localVar.getVariableSlot(), instance.getVariableSlot(), fld.getSlot());

            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }

        }

        @Override
        public ClassOfScopedClassInterval setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public ClassOfScopedClassInterval setParent(Expression expression) {
            super.setParent(expression);
            return this;
        }

        @Override
        public String toString() {
            return "(ClassOfScopedClassInterval %s as %s)".formatted(scopedClassIntervalInstance, metaClassDef);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ClassOfScopedClassInterval that)) return false;
            return Objects.equals(scopedClassIntervalInstance, that.scopedClassIntervalInstance) && Objects.equals(metaClassDef, that.metaClassDef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scopedClassIntervalInstance, metaClassDef);
        }
    }
}
