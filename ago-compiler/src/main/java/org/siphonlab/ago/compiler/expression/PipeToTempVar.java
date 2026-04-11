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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.array.CollectionElement;

import java.util.Objects;

/**
 * Wrap OutputLocalVarExpression to a temp var
 */
public class PipeToTempVar extends ExpressionInFunctionBody implements LocalVarResultExpression{

    private final Expression baseExpression;

    private boolean visited = false;
    private Var.LocalVar tempVar;

    public PipeToTempVar(FunctionDef ownerFunction, Expression baseExpression){
        super(ownerFunction);
        this.baseExpression = baseExpression;
    }

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        if(visited){
            return tempVar;
        } else {
            if(baseExpression instanceof Var.LocalVar){
                this.tempVar = (Var.LocalVar)baseExpression;
            } else {
                this.tempVar = blockCompiler.acquireTempVar(this.baseExpression);
                baseExpression.outputToLocalVar(tempVar, blockCompiler);
            }
            visited = true;
            return tempVar;
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        baseExpression.visit(blockCompiler);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return baseExpression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var t = this.visit(blockCompiler);
        t.outputToLocalVar(localVar, blockCompiler);
    }

    @Override
    public String toString() {
        return "(PipeToTemp %s)".formatted(baseExpression);
    }

    @Override
    public PipeToTempVar setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PipeToTempVar that)) return false;
        return Objects.equals(baseExpression, that.baseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(baseExpression);
    }

    public FinallyRelease releaseAfter(Expression expression){
        if (expression instanceof LiteralResultExpression && expression instanceof TermExpression) {
            return new FinallyReleaseWithLiteralResultAndTermExpression(ownerFunction, (LiteralResultExpression) expression);
        } else if (expression instanceof LocalVarResultExpression && expression instanceof TermExpression) {
            return new FinallyReleaseWithLocalVarResultAndTermExpression(ownerFunction, (LocalVarResultExpression) expression);
        } else if (expression instanceof LiteralResultExpression) {
            return new FinallyReleaseWithLiteralExpression(ownerFunction, (LiteralResultExpression) expression);
        } else if (expression instanceof CollectionElement) {
            return new FinallyReleaseWithCollectionElement(ownerFunction, (CollectionElement) expression);
        } else if (expression instanceof Assign.Assignee) {
            return new FinallyReleaseWithAssignee(ownerFunction, (Assign.Assignee) expression);
        } else if (expression instanceof TermExpression) {
            return new FinallyReleaseWithTermExpression(ownerFunction, (TermExpression) expression);
        } else {
            return new FinallyRelease(ownerFunction, expression);
        }
    }

    /**
     * var p = new PipeToTempVar(expr1)
     * var b = p.releaseAfter(expr2);           // after expr2 evaluated, release p
     */
    public class FinallyRelease extends ExpressionInFunctionBody{

        protected Expression inner;

        private FinallyRelease(FunctionDef ownerFunction, Expression inner){
            super(ownerFunction);
            this.inner = inner;
        }

        @Override
        protected Expression transformInner() throws CompilationError {
            this.inner = this.inner.transform();
            return this;
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return inner.inferType();
        }

        @Override
        public SourceLocation getSourceLocation() {
            return inner.getSourceLocation();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            blockCompiler.lockRegister(tempVar);
            inner.outputToLocalVar(localVar, blockCompiler);
            blockCompiler.releaseRegister(PipeToTempVar.this.tempVar);
        }

        @Override
        public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
            blockCompiler.lockRegister(tempVar);
            var r = inner.visit(blockCompiler);
            blockCompiler.releaseRegister(PipeToTempVar.this.tempVar);
            return r;
        }

        @Override
        public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
            blockCompiler.lockRegister(tempVar);
            inner.termVisit(blockCompiler);
            blockCompiler.releaseRegister(PipeToTempVar.this.tempVar);
        }

        @Override
        public String toString() {
            return inner.toString();
        }

        private Expression getBaseExpression(){
            return baseExpression;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FinallyRelease that)) return false;
            return Objects.equals(inner, that.inner) && Objects.equals(baseExpression, that.getBaseExpression());
        }

        @Override
        public int hashCode() {
            return Objects.hash(inner, baseExpression);
        }
    }

    public class FinallyReleaseWithLocalVarExpression extends FinallyRelease implements LocalVarResultExpression{

        private FinallyReleaseWithLocalVarExpression(FunctionDef ownerFunction, LocalVarResultExpression inner) {
            super(ownerFunction, inner);
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            return (Var.LocalVar) super.visit(blockCompiler);
        }
    }

    public class FinallyReleaseWithLiteralExpression extends FinallyRelease implements LiteralResultExpression {
        private FinallyReleaseWithLiteralExpression(FunctionDef ownerFunction, LiteralResultExpression inner) {
            super(ownerFunction, inner);
        }

        @Override
        public Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError {
            return (Literal<?>) super.visit(blockCompiler);
        }
    }

    public class FinallyReleaseWithCollectionElement extends FinallyRelease implements CollectionElement {
        private FinallyReleaseWithCollectionElement(FunctionDef ownerFunction, CollectionElement inner) {
            super(ownerFunction, inner);
        }

        @Override
        public Var.LocalVar getProcessedCollection() {
            return ((CollectionElement)inner).getProcessedCollection();
        }

        @Override
        public TermExpression getProcessedIndex() {
            return ((CollectionElement)inner).getProcessedIndex();
        }

        @Override
        public Expression toPutElement(Expression processedCollection, TermExpression processedIndex, Expression value, FunctionDef ownerFunction) throws CompilationError {
            return ((CollectionElement)inner).toPutElement(processedCollection, processedIndex, value, ownerFunction);
        }
    }

    public class FinallyReleaseWithAssignee extends FinallyRelease implements Assign.Assignee {
        private FinallyReleaseWithAssignee(FunctionDef ownerFunction, Assign.Assignee inner) {
            super(ownerFunction, inner);
        }
    }

    public class FinallyReleaseWithTermExpression extends FinallyRelease implements TermExpression {
        private FinallyReleaseWithTermExpression(FunctionDef ownerFunction, TermExpression inner) {
            super(ownerFunction, inner);
        }
    }

    public class FinallyReleaseWithLiteralResultAndTermExpression extends FinallyReleaseWithLiteralExpression implements TermExpression {
        private FinallyReleaseWithLiteralResultAndTermExpression(FunctionDef ownerFunction, LiteralResultExpression inner) {
            super(ownerFunction, inner);
        }
    }

    public class FinallyReleaseWithLocalVarResultAndTermExpression extends FinallyReleaseWithLocalVarExpression implements TermExpression {
        private FinallyReleaseWithLocalVarResultAndTermExpression(FunctionDef ownerFunction, LocalVarResultExpression inner) {
            super(ownerFunction, inner);
        }
    }


}
