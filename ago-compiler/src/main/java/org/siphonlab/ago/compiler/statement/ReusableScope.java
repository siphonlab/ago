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

import org.apache.commons.collections4.MapUtils;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Var;

import java.util.HashMap;
import java.util.Map;

public class ReusableScope extends Statement{

    protected final Statement statement;
    private Map<Expression, Var.LocalVar> reusableTempVariables = new HashMap<>();

    private Map<Expression, Var.LocalVar> prevReusableTempVariables = null;

    public ReusableScope(FunctionDef ownerFunction, Statement statement) {
        super(ownerFunction);
        this.statement = statement;
        this.setParent(statement.getParent());
        statement.setParent(this);
    }

    public static Statement wrap(Statement statement, FunctionDef ownerFunction) {
        if(statement instanceof ReusableScope) return statement;
        return new ReusableScope(ownerFunction, statement);
    }

    protected void enterReusableScope(BlockCompiler blockCompiler) {
        prevReusableTempVariables = blockCompiler.getReusableTempVariables();
        reusableTempVariables.putAll(prevReusableTempVariables);
        blockCompiler.setReusableTempVariables(reusableTempVariables);
    }

    protected void leaveReusableScope(BlockCompiler blockCompiler){
        blockCompiler.setReusableTempVariables(prevReusableTempVariables);
        for (Expression expression : reusableTempVariables.keySet()) {
            if(!prevReusableTempVariables.containsKey(expression)){
                blockCompiler.releaseRegister(reusableTempVariables.get(expression));
            }
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        enterReusableScope(blockCompiler);
        try {
            this.statement.termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            leaveReusableScope(blockCompiler);
        }
    }

    @Override
    public String toString() {
        return statement.toString();
    }
}
