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
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

public class NullableValue extends ExpressionInFunctionBody{

    private final Expression nullableExpression;
    private Var.LocalVar nonNullValueReceiver;
    private Var.LocalVar outputted;

    public NullableValue(FunctionDef ownerFunction, Expression nullableExpression, Var.LocalVar nonNullValueReceiver) throws CompilationError {
        super(ownerFunction);
        this.setParent(nullableExpression.getParent());
        nullableExpression.setParent(this);
        this.setSourceLocation(nullableExpression.getSourceLocation());
        this.nullableExpression = nullableExpression.transform();
        this.nonNullValueReceiver = nonNullValueReceiver;
        if(!(nullableExpression.inferType() instanceof NullableClassDef)){
            throw new IllegalArgumentException("an nullable expression expected");
        }
    }

    public NullableValue(FunctionDef ownerFunction, Expression nullableExpression) throws CompilationError {
        this(ownerFunction, nullableExpression, null);
    }

    public void releaseResult(BlockCompiler blockCompiler) {
        blockCompiler.releaseRegister(outputted);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return nullableExpression.inferType();
    }


    @Override
    public NullableValue setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(outputted != null){
            if(localVar == outputted){
                return;
            } else {
                blockCompiler.releaseRegister(outputted);
                ownerFunction.assign(localVar, outputted).termVisit(blockCompiler);
            }
        } else {
            nullableExpression.outputToLocalVar(localVar, blockCompiler);
        }

        outputted = localVar;
        blockCompiler.lockRegister(outputted);
    }

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        if(outputted != null){
            return outputted;
        }
        outputted = (Var.LocalVar) nullableExpression.visit(blockCompiler);
        blockCompiler.lockRegister(outputted);
        return outputted;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NullableValue that)) return false;
        return Objects.equals(nullableExpression, that.nullableExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nullableExpression);
    }

    @Override
    public String toString() {
        return "(NullableValue %s)".formatted(nullableExpression);
    }

    public IsNotNull isNotNull(){
        return new IsNotNull(ownerFunction);
    }

    public IsNull isNull(){
        return new IsNull(ownerFunction);
    }

    public NonNullValue nonNullValue(){
        return new NonNullValue(ownerFunction);
    }

    public NonNullPlaceHolder nonNullPlaceHolder(){
        return new NonNullPlaceHolder(ownerFunction);
    }

    public boolean hasReceiver() {
        return this.nonNullValueReceiver != null && this.nonNullValueReceiver.variable.getSlot() != null;
    }

    public class IsNotNull extends ExpressionInFunctionBody{

        public IsNotNull(FunctionDef ownerFunction) {
            super(ownerFunction);
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return getRoot().BOOLEAN();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if(outputted == null){
                NullableValue.this.visit(blockCompiler);
            }
            blockCompiler.getCode().notEqualsNull(localVar.getVariableSlot(), outputted.getVariableSlot());
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            if(outputted == null){
                NullableValue.this.visit(blockCompiler);
            }
            return (Var.LocalVar) super.visit(blockCompiler);
        }

        @Override
        public String toString() {
            return "(%s != null)".formatted(nullableExpression);
        }
    }

    public class IsNull extends ExpressionInFunctionBody{

        public IsNull(FunctionDef ownerFunction) {
            super(ownerFunction);
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return getRoot().BOOLEAN();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if(outputted == null){
                NullableValue.this.visit(blockCompiler);
            }
            blockCompiler.getCode().equalsNull(localVar.getVariableSlot(), outputted.getVariableSlot());
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            if(outputted == null){
                NullableValue.this.visit(blockCompiler);
            }
            return (Var.LocalVar) super.visit(blockCompiler);
        }

        @Override
        public String toString() {
            return "(%s == null)".formatted(nullableExpression);
        }
    }

    public class NonNullValue extends ExpressionInFunctionBody{

        public NonNullValue(FunctionDef ownerFunction) {
            super(ownerFunction);
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            var n = (NullableClassDef) NullableValue.this.inferType();
            return n.getBaseClass();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if(outputted == null){
                NullableValue.this.visit(blockCompiler);
            }
            if(nonNullValueReceiver != null){
                ownerFunction.cast(outputted, inferType()).transform().outputToLocalVar(nonNullValueReceiver, blockCompiler);
                ownerFunction.assign(localVar, nonNullValueReceiver).termVisit(blockCompiler);
            } else {
                nonNullValueReceiver = localVar;
            }
            ownerFunction.cast(outputted, inferType()).transform().outputToLocalVar(localVar, blockCompiler);
            blockCompiler.releaseRegister(outputted);
        }

        public NullableValue getNullableValue(){
            return NullableValue.this;
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            if(nonNullValueReceiver != null){
                outputToLocalVar(nonNullValueReceiver, blockCompiler);
                return nonNullValueReceiver;
            } else {
                return (Var.LocalVar) super.visit(blockCompiler);
            }
        }

        @Override
        public String toString() {
            return "(NonNullValue %s)".formatted(nullableExpression);
        }
    }

    public class NonNullPlaceHolder extends ExpressionInFunctionBody{
        public NonNullPlaceHolder(FunctionDef ownerFunction) {
            super(ownerFunction);
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            var n = (NullableClassDef) NullableValue.this.inferType();
            return n.getBaseClass();
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            throw new UnsupportedOperationException("NullableCondition is a placeholder class");
        }

        public NullableValue getNullableValue(){
            return NullableValue.this;
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            throw new UnsupportedOperationException("NullableCondition is a placeholder class");
        }

        @Override
        public String toString() {
            return "(NullableCondition %s)".formatted(nullableExpression);
        }
    }
}
