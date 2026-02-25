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
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

@Deprecated
public class NullAsObject implements LiteralResultExpression{

    private final NullLiteral nullLiteral;
    private final ClassDef type;

    public NullAsObject(NullLiteral nullLiteral, ClassDef type){
        this.nullLiteral = nullLiteral;
        this.type = type;
        this.setParent(nullLiteral.getParent());
        nullLiteral.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return type;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().assignLiteral(localVar.getVariableSlot(), this.nullLiteral);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {

    }

    @Override
    public Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError {
        return this.nullLiteral;
    }

    @Override
    public Expression transform() throws CompilationError {
        return null;
    }

    public NullLiteral getNullLiteral() {
        return nullLiteral;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public NullAsObject setSourceLocation(SourceLocation sourceLocation) {
//        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public Expression setParent(Expression expression) {
        return null;
    }

    @Override
    public Expression getParent() {
        return null;
    }
}
