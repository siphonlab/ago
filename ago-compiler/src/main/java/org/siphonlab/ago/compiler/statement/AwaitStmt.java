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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

// pause, directly exit current callframe, in some engine it will set frame running state to SUSPEND
public class AwaitStmt extends Statement{

    public AwaitStmt(FunctionDef ownerFunction) {
        super(ownerFunction);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.enter(this);
        blockCompiler.getCode().pause();
        blockCompiler.leave(this);
    }

    @Override
    public String toString() {
        return "await";
    }
}
