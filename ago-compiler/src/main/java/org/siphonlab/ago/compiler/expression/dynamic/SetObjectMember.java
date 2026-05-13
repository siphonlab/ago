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
package org.siphonlab.ago.compiler.expression.dynamic;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;

import java.util.Objects;

public class SetObjectMember extends ExpressionInFunctionBody {

    private final Expression object;
    private final Expression index;
    private final Expression value;

    public SetObjectMember(FunctionDef ownerFunction, Expression object, Expression index, Expression value) {
        super(ownerFunction);
        this.object = object;
        this.index = index;
        this.value = value.setParent(this);
    }

    public SetObjectMember(FunctionDef ownerFunction, ObjectMember objectMember, Expression value) {
        super(ownerFunction);
        this.object = objectMember.getObject();
        this.index = objectMember.getIndexExpr();
        this.value = value;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getRoot().getAnyClass();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var r = this.visit(blockCompiler);
            r.outputToLocalVar(localVar, blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }
    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar obj = (Var.LocalVar) this.object.visit(blockCompiler);
            blockCompiler.lockRegister(obj);

            var member = this.index.visit(blockCompiler);
            if(member instanceof Literal<?>){
                member = new PipeToTempVar(ownerFunction, member).visit(blockCompiler);
            }
            blockCompiler.lockRegister(member);

            var value = this.value.visit(blockCompiler);
            if(value instanceof Literal<?>){
                value = new PipeToTempVar(ownerFunction, value).visit(blockCompiler);
            }
            blockCompiler.lockRegister(value);

            blockCompiler.getCode().setDynamicMember(obj.getVariableSlot(), ((Var.LocalVar)member).getVariableSlot(), ((Var.LocalVar)value).getVariableSlot());

            blockCompiler.releaseRegister(obj);
            blockCompiler.releaseRegister(member);
            blockCompiler.releaseRegister(value);
            return value;
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public String toString() {
        return "(SetMember %s[%s] = %s)".formatted(this.object, this.index, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SetObjectMember that = (SetObjectMember) o;
        return Objects.equals(object, that.object) && Objects.equals(index, that.index) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, index, value);
    }
}
