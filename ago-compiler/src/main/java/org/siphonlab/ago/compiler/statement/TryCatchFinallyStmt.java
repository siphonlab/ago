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
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.SomeInstance;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TryCatchFinallyStmt extends Statement{

    private final BlockStmt tryBlock;
    private final List<CatchCause> catchCauses;
    private final BlockStmt finallyBlock;

    private Var.LocalVar finalExit;
    private Label finalEntrance;

    public static class CatchCause{
        private final BlockStmt blockStmt;
        private final Var.LocalVar exceptionVar;
        private List<ClassDef> exceptionTypes;
        private Label handler;

        public CatchCause(List<ClassDef> exceptionTypes, BlockStmt blockStmt, Var.LocalVar exceptionVar){
            this.exceptionTypes = exceptionTypes;
            this.blockStmt = blockStmt;
            this.exceptionVar = exceptionVar;
        }

        public List<ClassDef> getExceptionTypes() {
            return exceptionTypes;
        }

        public void setExceptionTypes(List<ClassDef> exceptionTypes) {
            this.exceptionTypes = exceptionTypes;
        }

        public void setHandler(Label handler) {
            this.handler = handler;
        }

        public Label getHandler() {
            return handler;
        }
    }

    public TryCatchFinallyStmt(FunctionDef ownerFunction, BlockStmt tryBlock, List<CatchCause> catchCauses, BlockStmt finallyBlock){
        super(ownerFunction);
        this.tryBlock = tryBlock;
        tryBlock.setParent(this);
        this.catchCauses = catchCauses;
        if(catchCauses != null){
            for (CatchCause catchCause : catchCauses) {
                catchCause.blockStmt.setParent(this);
            }
        }
        this.finallyBlock = finallyBlock;
        if(finallyBlock != null) finallyBlock.setParent(this);
    }

    public Var.LocalVar getFinalExit() {
        return finalExit;
    }

    public Label getFinalEntrance() {
        return finalEntrance;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.tryBlock.transform();
        if(catchCauses != null){
            for (CatchCause catchCause : catchCauses) {
                catchCause.blockStmt.transform();
            }
        }
        if(finallyBlock != null) finallyBlock.transform();
        return this;
    }

    record CatchItem(Label begin, Label end, Label handler, List<ClassDef> exceptionTypes){}

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            FunctionDef functionDef = blockCompiler.getFunctionDef();
            CodeBuffer code = blockCompiler.getCode();
            ClassDef langException = functionDef.getRoot().getExceptionClass();

            if(this.finallyBlock != null){
                this.finalExit = blockCompiler.acquireTempVar(new IntLiteral(0));
                blockCompiler.lockRegister(finalExit);
            }
            this.finalEntrance = blockCompiler.createLabel();

            List<CatchItem> catchItems = new ArrayList<>();

            List<ClassDef> allCaughtExceptions;
            if (this.catchCauses != null) {
                allCaughtExceptions = this.catchCauses.stream().flatMap(c -> c.exceptionTypes.stream()).toList();
                blockCompiler.getHandledExceptions().addAll(allCaughtExceptions);
            } else {
                allCaughtExceptions = null;
            }

            Label begin = blockCompiler.createLabel().here();
            Label exit = blockCompiler.createLabel();
            tryBlock.termVisit(blockCompiler);

            Label end = blockCompiler.createLabel().here();

            code.jump(finalEntrance);    // no exception occurs, jump to exit

            if (allCaughtExceptions != null) {
                for (ClassDef ex : allCaughtExceptions) {
                    var r = blockCompiler.getHandledExceptions().removeLast();
                    assert ex == r;
                }
            }

            if (catchCauses != null && !catchCauses.isEmpty()) {
                for (CatchCause catchCause : catchCauses) {
                    Label catch_ = blockCompiler.createLabel().here();
                    catchItems.add(new CatchItem(begin, end, catch_, catchCause.exceptionTypes));

                    code.store_exception(catchCause.exceptionVar.getVariableSlot());
                    catchCause.blockStmt.termVisit(blockCompiler);

                    // in catch-block, the error handler is finally block
                    if (finallyBlock != null) {
                        functionDef.idOfClass(langException);
                        catchItems.add(new CatchItem(catch_, blockCompiler.createLabel().here(), finalEntrance, Collections.singletonList(langException)));
                    }

                    code.jump(finalEntrance);
                }
            } else {
                catchItems.add(new CatchItem(begin, end, finalEntrance, Collections.singletonList(langException)));
            }

            finalEntrance.here();
            if (finallyBlock != null) {
                var e = blockCompiler.acquireTempVar(new SomeInstance(ownerFunction, langException));
                code.store_exception(e.getVariableSlot());
                blockCompiler.lockRegister(e);
                finallyBlock.termVisit(blockCompiler);
                code.throw_if_exists(e.getVariableSlot());      // if there is unhandled exception, throw the exception insteadof continue the `exit`
                code.jnz(this.finalExit.getVariableSlot());

                blockCompiler.releaseRegister(finalExit);
            }
            exit.here();

            for (CatchItem item : catchItems) {
                functionDef.registerTryCatchTable(item.begin, item.end, item.handler, item.exceptionTypes);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public TryCatchFinallyStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
