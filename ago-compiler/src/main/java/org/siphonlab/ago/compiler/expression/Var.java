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

import java.util.Objects;

public abstract class Var extends ExpressionBase implements Assign.Assignee {

    public final Variable variable;

    protected Var(Variable variable) {
        assert variable != null;
        this.variable = variable;
    }

    public static Var of(Expression scopeExpr, Variable variable) throws CompilationError {
        scopeExpr = scopeExpr.transform();
        if(scopeExpr instanceof Scope scope && scope.getDepth() == 0){
            return new Var.LocalVar(variable, LocalVar.VarMode.Existed);
        }
        if(scopeExpr instanceof LocalVar localVar){
            return new Field(localVar, variable);
        } else {
            return new Field(scopeExpr, variable);
        }
    }

    @Override
    public abstract Var transformInner() throws CompilationError;

    @Override
    public Var transform() throws CompilationError {
        return (Var)super.transform();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return variable.getType();
    }

    @Override
    public Var setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public static class LocalVar extends Var implements LocalVarResultExpression, TermExpression{

        public enum VarMode{
            Existed,
            ToDeclare,
            Temp;
        }

        public final VarMode varMode;

        public LocalVar(Variable variable, VarMode varMode) {
            super(variable);
            this.varMode = varMode;
        }

        @Override
        public void outputToLocalVar(LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            if(localVar == this || this.variable == localVar.variable) return;
            blockCompiler.enter(this);

            blockCompiler.getCode().assign(localVar.variable.getSlot(), localVar.variable.getType().getTypeCode(), this.variable.getSlot());
            blockCompiler.leave(this);
        }

        @Override
        public LocalVar transformInner() {
            return this;
        }

        @Override
        public LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
//           后面的语句要从 localVariables 检索该变量, 因此不在 visit 中创建
//            if(this.varMode == VarMode.ToDeclare){
//                FunctionDef functionDef = blockCompiler.getFunctionDef();
//                functionDef.addLocalVariable(variable);
//            }
            return this;
        }

        public SlotDef getVariableSlot(){
            return variable.getSlot();
        }

        @Override
        public String toString() {
            return variable.toString();
        }

        @Override
        public LocalVar setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LocalVar v2 && v2.variable == this.variable;
        }
    }

    public static class ReusingLocalVar extends LocalVar{

        private boolean outputted = false;

        public ReusingLocalVar(Variable variable, VarMode varMode) {
            super(variable, varMode);
        }

        public boolean isOutputted() {
            return outputted;
        }

        public void setOutputted(boolean outputted) {
            this.outputted = outputted;
        }
    }

    // allow base is LocalVar, PipeToTempVar, and Scope
    public static class Field extends Var {

        Expression instance;

        LocalVar baseVar;

        boolean simplified = false;

        protected Field(LocalVar instance, Variable variable) {
            super(variable);
            assert instance != null;
            assert variable != null;
            this.instance = instance;
            this.baseVar = instance;
        }

        public Field(Expression instance, Variable variable) throws CompilationError {
            super(variable);
            this.instance = instance.transform();
        }

        @Override
        public Var transformInner() throws CompilationError {
//TODO cast to trait and read its field
//            if(!(this.instance instanceof Scope || this.instance instanceof ReusingLocalVar) && this.instance.inferType().isTrait()){
//                throw new CompilationError("cannot access field of trait outside of trait or its permit class", this.getSourceLocation());
//            }
            if(this.instance instanceof Scope scope && scope.getDepth() == 0){      // LocalVar
                return new LocalVar(variable, LocalVar.VarMode.Existed);
            } else {
                if (!(this.instance instanceof LocalVarResultExpression)) {
                    this.instance = new PipeToTempVar(instance);
                }
            }
            return this;
        }

        public boolean isEnumValue(){
            if(instance instanceof ConstClass c && c.getClassDef().isEnum()) {
                if (variable.getType() == c.getClassDef()) {
                    return true;
                }
            }
            return false;
        }

        public void simplify(BlockCompiler blockCompiler) throws CompilationError {
            if(simplified) return;

            if(instance instanceof LocalVarResultExpression localVarResultExpression){
                simplified = true;
                this.baseVar = localVarResultExpression.visit(blockCompiler);
            }
        }

        public LocalVar getBaseVar() {
            return baseVar;
        }

        @Override
        public void outputToLocalVar(LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                this.simplify(blockCompiler);
                blockCompiler.getCode().assign(localVar.getVariableSlot(), inferType().getTypeCode(), getBaseVar().getVariableSlot(), variable.getSlot());
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }

        }

        @Override
        public String toString() {
            return "(VarOfInstance %s %s)".formatted(instance, variable.toString());
        }

        @Override
        public Field setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Field f2 && Objects.equals(f2.baseVar, this.baseVar) && Objects.equals(f2.variable, this.variable);
        }
    }

}

