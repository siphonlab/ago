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
import org.siphonlab.ago.compiler.exception.CompilationError;

public abstract class BiExpression extends ExpressionBase {
    public Expression left;
    public Expression right;

    protected BiExpression(Expression left, Expression right) throws CompilationError {
        this.left = left.transform();
        this.right = right.transform();
        this.left.setParent(this);
        this.right.setParent(this);
    }

    protected boolean transformed = false;
    @Override
    public Expression transformInner() throws CompilationError {
        if(transformed) return this;

        CastStrategy.UnifyTypeResult unifyTypeResult = new CastStrategy(getSourceLocation(), false).unifyTypes(left, right);
        var l = unifyTypeResult.left();
        var r = unifyTypeResult.right();

        if(unifyTypeResult.changed() || l != this.left || r != this.right){
            this.left = unifyTypeResult.left().transform();
            this.right = unifyTypeResult.right().transform();
        }

        if(unifyTypeResult.resultType().isPrimitive()){
            if(l instanceof Literal<?> literal1 || r instanceof Literal<?> literal2){
                return processLiterals();
            }
        }
        transformed = true;
        return this;

//        ClassDef t1 = left.inferType();
//        ClassDef t2 = right.inferType();
//        Expression l = left, r = right;
//        if(t1.isPrimitiveOrBoxed() && t2.isPrimitiveOrBoxed()){
//            boolean unboxed = false;
//            if(!t1.isPrimitive()) {
//                l = new Unbox(l);
//                t1 = l.inferType();
//                unboxed = true;
//            }
//            if(!t2.isPrimitive()) {
//                r = new Unbox(r);
//                t2 = r.inferType();
//                unboxed = true;
//            }
//
//            if(t1 == t2) {
//                if(unboxed){
//                    return transformUnboxed(l,r).setSourceLocation(this.getSourceLocation()).setParent(this.getParent());
//                }
//                return processLiterals();
//            }
//            if(t1 == PrimitiveClassDef.STRING && t2 == PrimitiveClassDef.INT){
//                return this;
//            }
//
//            // unify type
//            var resultType = t2.getTypeCode().isHigherThan(t1.getTypeCode()) ? unifyPrimitiveType(t1, t2) : unifyPrimitiveType(t2, t1);
//            if(resultType != t1) l = new Cast(l, resultType);
//            if(resultType != t2) r = new Cast(r, resultType);
//            this.left = l.transform();
//            this.right = r.transform();
//            return processLiterals();
//        }
//        if(t1.isGenericType() && t1.isPrimitiveFamily() && t2.isGenericType() && t2.isPrimitiveFamily()){
//            if(t1.getTypeCode() != t2.getTypeCode()){
//                throw new TypeMismatchError("generic type values must have same type",this.getSourceLocation());
//            }
//        }
//        return this;
    }

    protected abstract Expression transformUnboxed(Expression left, Expression right) throws CompilationError;

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            TermExpression left = this.left.visit(blockCompiler);
            blockCompiler.lockRegister(left);
            TermExpression right = this.right.visit(blockCompiler);
            blockCompiler.releaseRegister(left);
            if (left instanceof Literal<?> literal1 && right instanceof Literal<?> literal2) {
                return processTwoLiteralsInner(literal1, literal2);
            }

            Var.LocalVar tempVar = blockCompiler.acquireTempVar(this);
            outputToLocalVar(tempVar, left, right, blockCompiler);
            return tempVar;
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.left.termVisit(blockCompiler);
        this.right.termVisit(blockCompiler);
    }

    private void outputToLocalVar(Var.LocalVar localVar, TermExpression evaluatedLeft, TermExpression evaluatedRight, BlockCompiler blockCompiler) throws CompilationError {
        if(evaluatedLeft instanceof Literal<?> literal1){
            processLeftLiteral(localVar, literal1, (Var.LocalVar) evaluatedRight, blockCompiler);
        } else if(evaluatedRight instanceof Literal<?> literal){
            processRightLiteral(localVar, (Var.LocalVar) evaluatedLeft, literal, blockCompiler);
        } else {
            processTwoVariables(localVar, (Var.LocalVar) evaluatedLeft, (Var.LocalVar) evaluatedRight, blockCompiler);
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        TermExpression left = this.left.visit(blockCompiler);
        blockCompiler.lockRegister(left);
        TermExpression right = this.right.visit(blockCompiler);
        blockCompiler.releaseRegister(left);
        try {
            blockCompiler.enter(this);

            if (left instanceof Literal<?> literal1 && right instanceof Literal<?> literal2) {
                processTwoLiteralsInner(literal1, literal2).outputToLocalVar(localVar, blockCompiler);
            } else {
                outputToLocalVar(localVar, left, right, blockCompiler);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    protected abstract void processTwoVariables(Var.LocalVar result, Var.LocalVar left, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError;

    protected abstract void processRightLiteral(Var.LocalVar result, Var.LocalVar left, Literal<?> literal, BlockCompiler blockCompiler) throws CompilationError;

    protected abstract void processLeftLiteral(Var.LocalVar result, Literal<?> literal1, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError;

    protected Expression processLiterals() throws CompilationError {
        if(this.left instanceof Literal<?> literal1){
            if(this.right instanceof Literal<?> literal2) {
                return processTwoLiteralsInner(literal1, literal2);
            } else {
                return processLeftLiteral(literal1, this.right);
            }
        } else if(this.right instanceof Literal<?> literal2){
            return processRightLiteral(this.left, literal2);
        }
        return this;
    }

    protected abstract Expression processRightLiteral(Expression left, Literal<?> right);

    protected abstract Expression processLeftLiteral(Literal<?> left, Expression right);

    protected abstract Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError;

    @Override
    public ClassDef inferType() throws CompilationError {
        return left.inferType();        // already transformed, return left type by default, boolean expr override it to return boolean
    }

    @Override
    public BiExpression setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
