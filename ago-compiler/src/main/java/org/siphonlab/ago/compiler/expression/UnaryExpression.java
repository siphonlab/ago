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
import org.siphonlab.ago.compiler.exception.CompilationError;

public abstract class UnaryExpression extends ExpressionBase{
    protected final Expression value;

    public UnaryExpression(Expression value) throws CompilationError {
        this.value = value.transform();
        this.value.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return value.inferType();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.value.termVisit(blockCompiler);
    }
}
