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

import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.ArrayList;
import java.util.Collections;

public class CastToScopedClassRef extends ExpressionBase{

    private final Expression expression;
    private final ClassDef scopedClassIntervalClassDef;

    public CastToScopedClassRef(Expression expression, ClassDef scopedClassIntervalClassDef) {
        this.expression = expression;
        this.scopedClassIntervalClassDef = scopedClassIntervalClassDef;
        this.setSourceLocation(expression.getSourceLocation());
        this.setParent(expression.getParent());
        expression.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return scopedClassIntervalClassDef;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(this.expression, this.getSourceLocation());
            Expression scope = pair.getLeft();
            ClassDef classDef = pair.getRight();

            blockCompiler.lockRegister(localVar);
            new Box(new ClassRefLiteral(classDef), this.scopedClassIntervalClassDef, Box.BoxMode.Box).outputToLocalVar(localVar, blockCompiler);

            ClassDef classInterval = blockCompiler.getFunctionDef().getRoot().getScopedClassInterval();
            if (!localVar.inferType().isDeriveFrom(classInterval)) {
                throw new TypeMismatchError("a ScopedClassInterval expression expected", this.getSourceLocation());
            }

            var fld = new Var.Field(localVar, classInterval.getVariable("scope"));

            if(scope != null) {
                Assign.to(fld, scope).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
            }

            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(C2sbr %s)".formatted(expression);
    }

}
