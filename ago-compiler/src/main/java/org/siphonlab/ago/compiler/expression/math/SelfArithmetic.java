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
package org.siphonlab.ago.compiler.expression.math;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.CollectionElement;
import org.siphonlab.ago.opcode.arithmetic.Multiply;

import java.util.Objects;

import static org.siphonlab.ago.opcode.arithmetic.Add.KIND_ADD;
import static org.siphonlab.ago.opcode.arithmetic.Div.KIND_DIV;
import static org.siphonlab.ago.opcode.arithmetic.Mod.KIND_MOD;
import static org.siphonlab.ago.opcode.arithmetic.Subtract.KIND_SUBTRACT;

public class SelfArithmetic extends ExpressionInFunctionBody {

    private final Expression site;
    private Expression change;
    private final Type type;

    public enum Type{
        Inc(KIND_ADD),      // to slots.inc()
        Dec(KIND_SUBTRACT),
        SelfMulti(Multiply.KIND_MULTIPLY),      // just set(get() * )
        SelfDiv(KIND_DIV),
        SelfMod(KIND_MOD),

        IncPost(KIND_ADD),      // i++
        DecPost(KIND_SUBTRACT),
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }

    public SelfArithmetic(FunctionDef ownerFunction, Expression site, Expression change, Type type) throws CompilationError {
        super(ownerFunction);
        this.site = site.transform().setParent(this);
        this.change = ownerFunction.cast(change.setParent(this), site.inferType()).transform();
        this.type = type;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.site.inferType();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if (!(site instanceof Assign.Assignee)) {
            throw new SyntaxError("bad target for inc operation", this.site.getSourceLocation());
        }
        ClassDef type = this.change.inferType();
        if(type.getTypeCode() == TypeCode.OBJECT && type.isPrimitiveOrBoxed()){
            return new SelfArithmetic(ownerFunction, this.site, ownerFunction.unbox(this.change), this.type).setSourceLocation(this.getSourceLocation()).transform();
        }
        if(!type.getTypeCode().isNumber()){
            throw new TypeMismatchError("number required", this.change.getSourceLocation());
        }
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (site instanceof Var.Field field) {
                field.simplify(blockCompiler);
                blockCompiler.lockRegister(field.getBaseVar());
                switch (this.type) {
                    case Inc:
                        incField(blockCompiler, field, change);
                        if (localVar != null)
                            ownerFunction.assign(localVar, field).termVisit(blockCompiler);
                        break;
                    case Dec:
                        incField(blockCompiler, field, new Neg(ownerFunction, change));
                        if (localVar != null)
                            ownerFunction.assign(localVar, field).termVisit(blockCompiler);
                        break;
                    case SelfMulti:
                    case SelfDiv:
                    case SelfMod:
                        var temp = localVar != null ? localVar : blockCompiler.acquireTempVar(this);
                        new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), field, change).setSourceLocation(this.sourceLocation).outputToLocalVar(temp, blockCompiler);
                        ownerFunction.assign(field, temp).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                        break;
                    case IncPost:
                        if (localVar != null)
                            ownerFunction.assign(localVar, field).termVisit(blockCompiler);
                        incField(blockCompiler, field, this.change);
                        break;
                    case DecPost:
                        if (localVar != null)
                            ownerFunction.assign(localVar, field).termVisit(blockCompiler);
                        incField(blockCompiler, field, new Neg(ownerFunction, this.change));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + this.type);
                }
                blockCompiler.releaseRegister(field.getBaseVar());
            } else if (site instanceof Var.LocalVar var) {
                ArithmeticExpr expr = new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), var, change).setSourceLocation(this.sourceLocation);
                switch (this.type) {
                    case Inc:
                    case Dec:
                    case SelfMulti:
                    case SelfDiv:
                    case SelfMod:
                        expr.outputToLocalVar(var, blockCompiler);
                        if (localVar != null && localVar != var) {
                            ownerFunction.assign(localVar, var).termVisit(blockCompiler);
                        }
                        break;
                    case DecPost:
                    case IncPost:
                        if (localVar != null && localVar != var)
                            ownerFunction.assign(localVar, var).termVisit(blockCompiler);
                        expr.outputToLocalVar(var, blockCompiler);
                        break;
                }
            } else if (site instanceof CollectionElement collectionElement) {
                ArithmeticExpr expr;
                Var.LocalVar arr = null;
                TermExpression index = null;
                switch (this.type) {
                    case Inc:
                    case Dec:
                    case SelfMulti:
                    case SelfDiv:
                    case SelfMod:
                        TermExpression old = collectionElement.visit(blockCompiler);
                        arr = collectionElement.getProcessedCollection();
                        blockCompiler.lockRegister(arr);
                        index = collectionElement.getProcessedIndex();
                        blockCompiler.lockRegister(index);

                        expr = new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), old, change).setSourceLocation(this.sourceLocation);
                        var v = collectionElement.toPutElement(arr, index, expr, ownerFunction).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                        if (localVar != null) {
                            ownerFunction.assign(localVar, v).termVisit(blockCompiler);
                        }
                        break;
                    case IncPost:
                    case DecPost:
                        Var.LocalVar temp = localVar != null ? localVar : blockCompiler.acquireTempVar(collectionElement);

                        collectionElement.outputToLocalVar(temp, blockCompiler);
                        arr = collectionElement.getProcessedCollection();
                        blockCompiler.lockRegister(arr);
                        index = collectionElement.getProcessedIndex();
                        blockCompiler.lockRegister(index);

                        expr = new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), temp, change).setSourceLocation(this.sourceLocation);
                        collectionElement.toPutElement(arr, index, expr, ownerFunction).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                        break;
                }

                if (arr != null)
                    blockCompiler.releaseRegister(arr);
                if (index != null)
                    blockCompiler.releaseRegister(index);

            } else if (site instanceof Attribute attribute) {
                TermExpression r;
                switch (this.type) {
                    case Inc:
                    case Dec:
                    case SelfMulti:
                    case SelfDiv:
                    case SelfMod:
                        var got = attribute.visit(blockCompiler);
                        blockCompiler.lockRegister(attribute.getProcessedScope());
                        r = new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), got, change).setSourceLocation(this.sourceLocation).visit(blockCompiler);
                        attribute.setValue(ownerFunction, r).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                        blockCompiler.releaseRegister(attribute.getProcessedScope());
                        if (localVar != null)
                            ownerFunction.assign(localVar, r).termVisit(blockCompiler);
                        break;
                    case IncPost:
                    case DecPost:
                        Var.LocalVar temp = localVar != null ? localVar : blockCompiler.acquireTempVar(attribute);
                        attribute.outputToLocalVar(temp, blockCompiler);
                        r = new ArithmeticExpr(ownerFunction, ArithmeticExpr.Type.of(this.type.op), localVar, change).setSourceLocation(this.sourceLocation).visit(blockCompiler);
                        attribute.setValue(ownerFunction, r).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                        blockCompiler.releaseRegister(attribute.getProcessedScope());
                        break;
                }

            } else {
                throw new UnsupportedOperationException("not supported yet");
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private void incField(BlockCompiler blockCompiler, Var.Field simplifiedField, Expression value) throws CompilationError {
        TermExpression v = value.visit(blockCompiler);
        if(v instanceof Literal<?> literal) {
            blockCompiler.getCode().inc(simplifiedField.getBaseVar().getVariableSlot(), simplifiedField.variable.getSlot(), literal);
        } else {
            blockCompiler.getCode().inc(simplifiedField.getBaseVar().getVariableSlot(), simplifiedField.variable.getSlot(), ((Var.LocalVar) v).getVariableSlot());
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.outputToLocalVar(null, blockCompiler);
    }

    @Override
    public SelfArithmetic setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(this.type, this.site, this.change);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SelfArithmetic that)) return false;
        return Objects.equals(site, that.site) && Objects.equals(change, that.change) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, change, type);
    }
}
