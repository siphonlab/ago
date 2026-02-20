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
import org.siphonlab.ago.compiler.expression.array.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.Objects;

public abstract class Assign extends ExpressionBase {

    public interface Assignee extends Expression{}

    protected Var assignee;
    protected Expression value;

    public Assign(Var assignee, Expression value) throws CompilationError {
        this.assignee = assignee.transform();
        this.value = value.transform();
        this.assignee.setParent(this);
        this.value.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return value.inferType();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    public static Expression to(Assignee assignee, Expression value) throws CompilationError {
        var t = assignee.transform();

        value = value.transform();
        value = processBoundClass(assignee, value);

        if(t instanceof Var.LocalVar localVar){
            if(value instanceof LiteralResultExpression literalResultExpression){
                return new LiteralToLocalVar(localVar, literalResultExpression);
            } else {
                return new ToLocalVar(localVar, value);
            }
        } else if(t instanceof Var.Field field) {
            if (value instanceof Literal<?> literal) {
                return new LiteralToField(field, literal);
            } else {
                return new ToField(field, value);
            }
        } else if(t instanceof Attribute attribute) {
            return attribute.setValue(value);
        } else if(assignee instanceof ArrayElement arrayElement){
            return new ArrayPut(arrayElement, value);
        } else if(assignee instanceof ListElement listElement){
            return new ListPut(listElement, value);
        } else if(assignee instanceof MapValue mapValue){
            return new MapPut(mapValue, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static Expression processBoundClass(Assignee assignee, Expression expression) throws CompilationError {
        if(assignee instanceof Var.LocalVar localVar && localVar.varMode == Var.LocalVar.VarMode.Temp){
            return expression;      // register variable allow ScopeBoundClass
        }
        var assigneeType = assignee.inferType();
        return processBoundClass(assigneeType,expression,assignee.getSourceLocation());
    }

    public static Expression processBoundClass(ClassDef classRefType, Expression expression, SourceLocation classRefSourceLocation) throws CompilationError {
        if (expression instanceof ClassOf || expression instanceof ClassUnder || expression instanceof ConstClass) {
            Root root = classRefType.getRoot();
            if (classRefType.isThatOrDerivedFromThat(root.getScopedClassInterval())) {
                return new CastToScopedClassRef(expression, classRefType).transform();
            } else if (root.getScopedClassInterval().isDeriveFrom(classRefType)) {
                var p = Creator.extractScopeAndClass(expression, expression.getSourceLocation());
                var t = root.getOrCreateScopedClassInterval(p.getRight(), p.getRight(), null);
                //TODO register concrete type
                return new ForceCast(new CastToScopedClassRef(expression, t).transform(), classRefType, ForceCast.CastMode.WearClassMask);
            } else if (classRefType == PrimitiveClassDef.CLASS_REF) {
                if (expression instanceof ConstClass constClass) {
                    return new ClassRefLiteral(constClass.getClassDef());
                } else {
                    throw new TypeMismatchError("classref only accept top-level class", expression.getSourceLocation());
                }
            } else {
                throw new TypeMismatchError("class interval or classref variable required", classRefSourceLocation);
            }
//            var p = Creator.extractScopeAndClass(expression, expression.getSourceLocation());
//            return new BindScope(p.getLeft(), p.getRight()).setSourceLocation(expression.getSourceLocation()).setParent(expression.getParent());
        }
        return expression;
    }

    @Override
    public String toString() {
        return "(Assign %s %s)".formatted(assignee, value);
    }

    public static class ToLocalVar extends Assign implements LocalVarResultExpression{
        private Var.LocalVar assignee;
        public ToLocalVar(Var.LocalVar assignee, Expression value) throws CompilationError {
            super(assignee, value);
            this.assignee = assignee;
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                this.visit(blockCompiler).outputToLocalVar(localVar, blockCompiler);

            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                value.outputToLocalVar(this.assignee, blockCompiler);
                return this.assignee;
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }

        }

        @Override
        public ToLocalVar setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ToLocalVar t && Objects.equals(assignee, t.assignee) && Objects.equals(value, t.value);
        }
    }

    public static class LiteralToLocalVar extends Assign implements LiteralResultExpression{
        private final LiteralResultExpression literalResultExpression;
        private Var.LocalVar assignee;
        public LiteralToLocalVar(Var.LocalVar assignee, LiteralResultExpression value) throws CompilationError {
            super(assignee, value);
            this.literalResultExpression = value;
            this.assignee = assignee;
        }

        @Override
        public Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                var literal = literalResultExpression.visit(blockCompiler);
                literal.outputToLocalVar(assignee, blockCompiler);
                return literal;

            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                this.visit(blockCompiler).outputToLocalVar(localVar, blockCompiler);

            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public LiteralToLocalVar setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LiteralToLocalVar t && Objects.equals(assignee, t.assignee) && Objects.equals(this.literalResultExpression, t.literalResultExpression);
        }
    }

    public static class ToField extends Assign{
        private Var.Field assignee;
        public ToField(Var.Field assignee, Expression value) throws CompilationError {
            super(assignee, value);
            this.assignee = assignee;
        }

        @Override
        public ExpressionBase transformInner() {
            if(!(this.value instanceof Var.LocalVar)){
                this.value = new PipeToTempVar(this.value);
            }
            return this;
        }

        @Override
        public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                this.assignee.simplify(blockCompiler);
                blockCompiler.lockRegister(this.assignee.baseVar);
                // blockCompiler.lockRegister(this.assignee.variable);  assignee.variable belongs to assignee, not in local scope

                var v = this.value.visit(blockCompiler);
                blockCompiler.releaseRegister(this.assignee.baseVar);

                if (v instanceof Var.LocalVar localVar) {
                    blockCompiler.assign(this.assignee.baseVar, this.assignee.variable, localVar);
                } else {
                    throw new UnsupportedOperationException();
                }
                return v;
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }

        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                var r = this.visit(blockCompiler);
                blockCompiler.getCode().assign(localVar.getVariableSlot(), this.assignee.variable.getType().getTypeCode(), this.assignee.baseVar.getVariableSlot(), this.assignee.variable.getSlot());

            } catch (CompilationError e){
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public ToField setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ToField toField)) return false;
            return Objects.equals(assignee, toField.assignee) && Objects.equals(toField.value,this.value);
        }

    }

    public static class LiteralToField extends Assign implements LiteralResultExpression{
        private final LiteralResultExpression literalResultExpression;
        private Var.Field assignee;
        public LiteralToField(Var.Field assignee, LiteralResultExpression value) throws CompilationError {
            super(assignee, value);
            this.literalResultExpression = value;
            this.assignee = assignee;
        }

        @Override
        public Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                this.assignee.simplify(blockCompiler);
                blockCompiler.lockRegister(this.assignee.baseVar);
                var literal = literalResultExpression.visit(blockCompiler);
                blockCompiler.releaseRegister(this.assignee.baseVar);
                blockCompiler.getCode().assignLiteral(this.assignee.baseVar.getVariableSlot(), this.assignee.variable.getSlot(), literal);
                return literal;

            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            this.visit(blockCompiler).outputToLocalVar(localVar,blockCompiler);
        }

        @Override
        public LiteralToField setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LiteralToField toField)) return false;
            return Objects.equals(assignee, toField.assignee) && Objects.equals(toField.literalResultExpression,this.literalResultExpression);
        }
    }
}
