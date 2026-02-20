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
import org.siphonlab.ago.compiler.Root;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;

import java.util.Arrays;
import java.util.Collections;

public class ViaStmt extends Statement{

    private final Expression viaObject;
    private final Statement statement;

    public ViaStmt(Expression viaObject, Statement statement) throws CompilationError {
        this.viaObject = viaObject.transform();
        this.statement = statement.transform();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar viaObject;
            if(this.viaObject instanceof LocalVarResultExpression localVarResultExpression){
                viaObject = localVarResultExpression.visit(blockCompiler);
            } else {
                viaObject = (Var.LocalVar) this.viaObject.visit(blockCompiler);
            }

            FunctionDef functionDef = blockCompiler.getFunctionDef();
            Root root = functionDef.getRoot();
            Expression funThis = new Scope(0, functionDef).fromPronoun(NamePathResolver.PronounType.FunThis);

            ClassUnder enterFun = (ClassUnder) ClassUnder.create(viaObject, viaObject.inferType().getChild("enter#")).setSourceLocation(viaObject.getSourceLocation()).setParent(this);
            var invokeEnterFun = new Invoke(Invoke.InvokeMode.Invoke, functionDef, enterFun, Collections.singletonList(funThis), viaObject.getSourceLocation());

            ClassUnder leaveFun = (ClassUnder) ClassUnder.create(viaObject, viaObject.inferType().getChild("exit#")).setSourceLocation(viaObject.getSourceLocation()).setParent(this);
            var invokeLeaveFun = new Invoke(Invoke.InvokeMode.Invoke, functionDef, leaveFun, Collections.singletonList(funThis), viaObject.getSourceLocation());

            TryCatchFinallyStmt tryCatchFinallyStmt = new TryCatchFinallyStmt(new BlockStmt(Arrays.asList(new ExpressionStmt(invokeEnterFun), statement)), null, new BlockStmt(Collections.singletonList(new ExpressionStmt(invokeLeaveFun))));
            tryCatchFinallyStmt.termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ViaStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "via(%s) %s".formatted(viaObject, statement);
    }
}
