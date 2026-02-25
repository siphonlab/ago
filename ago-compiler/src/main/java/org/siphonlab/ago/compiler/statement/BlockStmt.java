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

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.List;

public class BlockStmt extends Statement{

    private final List<Statement> statements;

    public BlockStmt(FunctionDef ownerFunction, List<Statement> statements){
        super(ownerFunction);
        this.statements = statements;
        for (Statement statement : this.statements) {
            statement.setParent(this);
        }
    }

    @Override
    public Statement transformInner() throws CompilationError {
        List<Statement> statementList = this.statements;
        for (int i = 0; i < statementList.size(); i++) {
            Statement statement = statementList.get(i);
            var n = statement.transform();
            if (n != statement) {
                statementList.set(i, statement);
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        for (Statement statement : statements) {
            statement.termVisit(blockCompiler);
        }
    }

    @Override
    public BlockStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "{" + StringUtils.join(statements, '\n') + "}";
    }
}
