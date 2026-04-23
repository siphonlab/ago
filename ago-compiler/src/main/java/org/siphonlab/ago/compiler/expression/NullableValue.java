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
        this.nullableExpression = nullableExpression;
        this.nonNullValueReceiver = nonNullValueReceiver;
        if(!(nullableExpression.inferType() instanceof NullableClassDef)){
            throw new IllegalArgumentException("an nullable expression expected");
        }
        this.setParent(nullableExpression.getParent());
        nullableExpression.setParent(this);
    }

    public NullableValue(FunctionDef ownerFunction, Expression nullableExpression) throws CompilationError {
        this(ownerFunction, nullableExpression, null);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return nullableExpression.getSourceLocation();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return nullableExpression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        nullableExpression.outputToLocalVar(localVar, blockCompiler);
        outputted = localVar;
    }

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        outputted = (Var.LocalVar) nullableExpression.visit(blockCompiler);
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

    public IsNotNull isNotNull(){
        return new IsNotNull(ownerFunction);
    }

    public IsNull isNull(){
        return new IsNull(ownerFunction);
    }

    public NonNullValue nonNullValue(){
        return new NonNullValue(ownerFunction);
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
            new Equals(ownerFunction, outputted, getRoot().nullLiteral(), Equals.Type.NotEquals).transform()
                    .outputToLocalVar(localVar, blockCompiler);
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            return (Var.LocalVar) super.visit(blockCompiler);
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
            new Equals(ownerFunction, outputted, getRoot().nullLiteral(), Equals.Type.Equals).transform()
                    .outputToLocalVar(localVar, blockCompiler);
        }

        @Override
        public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
            return (Var.LocalVar) super.visit(blockCompiler);
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
            if(nonNullValueReceiver != null){
                assert localVar == nonNullValueReceiver;
            } else {
                nonNullValueReceiver = localVar;
            }
            ownerFunction.cast(outputted, inferType()).transform().outputToLocalVar(localVar, blockCompiler);
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
    }
}
