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
package org.siphonlab.ago.compiler;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.statement.Return;

import java.util.Collections;
import java.util.List;

public class DefaultValueFunDef extends FunctionDef{

    private final Parameter parameter;

    public DefaultValueFunDef(Root root, Parameter parameter, int index) {
        super(root, Parameter.composeDefaultValueFunctionName(parameter), null);
        this.parameter = parameter;
        this.setCompilingStage(CompilingStage.CompileMethodBody);
        this.setModifiers(AgoClass.PRIVATE);
        this.setResultType(parameter.getType());
    }

    @Override
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
        if(this.isGenericInstantiation()){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }
        var blockCompiler = new BlockCompiler(getUnit(), this, Collections.emptyList());
        var statements = List.of(new Return(this, cast(blockCompiler.expression(parameter.getDefaultValueAst()), this.getResultType()).transform()));
        blockCompiler.compileExpressions(statements.stream().map(st -> (Expression)st).toList());
        this.setCompilingStage(CompilingStage.Compiled);
    }
}
