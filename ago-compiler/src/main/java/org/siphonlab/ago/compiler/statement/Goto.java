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
package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class Goto extends Statement{

    private final Label label;

    public Goto(FunctionDef ownerFunction, Label label) throws CompilationError {
        super(ownerFunction);
        this.label = label;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.enter(this);
        blockCompiler.getCode().jump(label);
        blockCompiler.leave(this);
    }

    @Override
    public Goto setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "goto %s".formatted(label);
    }
}
