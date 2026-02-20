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

import org.siphonlab.ago.compiler.expression.Scope;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.List;

public class GetterFunction extends FunctionDef{

    private Field field;
    private final AgoParser.GetterContext getterContext;

    public GetterFunction(Field field, AgoParser.GetterContext getterContext) throws SyntaxError {
        super(field.getName() + "#get", null);
        this.field = field;
        this.getterContext = getterContext;
        this.setUnit(field.getOwnerClass().unit);
        //this.setGenericSource(getter.getGenericSource());
        int visibility = Compiler.commonVisibility(unit, getterContext.commonVisiblility(), Compiler.ModifierTarget.Method);
        this.setModifiers(visibility | AgoClass.GETTER);
        this.setSourceLocation(unit.sourceLocation(getterContext));
        this.setCompilingStage(CompilingStage.ParseFields);
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return null;
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.InheritsFields);
            return true;
        }

        this.setResultType(field.getType());
        this.createFunctionInterface();
        this.createFieldsOfTrait();

        this.setCompilingStage(CompilingStage.ValidateNewFunctions);
        return true;
    }

    @Override
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        var blockCompiler = new BlockCompiler(this.unit, this, null);
        Var.Field fld = new Var.Field(new Scope(1, this.getParentClass()), field)
                    .setSourceLocation(this.getParentClass().unit.sourceLocation(field.getDeclaration()));

        blockCompiler.compileExpressions(List.of(new Return(fld).setSourceLocation(unit.sourceLocation(getterContext))));

        this.nextCompilingStage(CompilingStage.Compiled);
    }
}
