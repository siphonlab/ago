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

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConstructorDef extends FunctionDef{
    private final static Logger LOGGER = LoggerFactory.getLogger(ConstructorDef.class);

    private final AgoParser.ConstructorDeclarationContext constructorDeclaration;

    public ConstructorDef(int modifiers, String name) {
        super(name, null);
        this.constructorDeclaration = null;
        this.modifiers = modifiers | AgoClass.CONSTRUCTOR;
        this.setResultType(PrimitiveClassDef.VOID);
    }

    public ConstructorDef(int modifiers, AgoParser.ConstructorDeclarationContext constructorDeclaration) {
        super(composeName(constructorDeclaration), null);
        this.constructorDeclaration = constructorDeclaration;
        this.modifiers = modifiers | AgoClass.CONSTRUCTOR;
        this.setResultType(PrimitiveClassDef.VOID);
    }


    private static String composeName(AgoParser.ConstructorDeclarationContext constructorDeclaration) {
        if(constructorDeclaration != null){
            if(constructorDeclaration.POST_IDENTIFIER() != null){
                return "new" + constructorDeclaration.POST_IDENTIFIER().getText();
            }
        }
        return "new";
    }

    public AgoParser.ConstructorDeclarationContext getConstructorDeclaration() {
        return constructorDeclaration;
    }


    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        if(!executeParseFieldsOfHierarchyClasses()) return false;

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse function fields".formatted(this));
        var constructorDeclaration = this.getConstructorDeclaration();
        unit.parseFormalParameters(this, constructorDeclaration.formalParameters());
        this.processFieldParameters();

        this.createFunctionInterface();
        this.createFieldsOfTrait();

        this.nextCompilingStage(CompilingStage.ValidateHierarchy);
        return true;
    }

    @Override
    protected void processFieldParameters() throws SyntaxError {
        ClassDef owner = this.getParentClass();
        for (Parameter parameter : this.getParameters()) {
            if(parameter.isField()){
                if(owner.getFields().containsKey(parameter.getName())){
                    // should generate assign field -> this parameter
                    if(parameter.getGetterSetter() != null){
                        throw unit.syntaxError(parameter.getGetterSetter(),"field '%s' not declared by this parameter, please put get/set behind the field declaration".formatted(parameter.getName()));
                    }
                } else {
                    // create a new field
                    var field = new Field(owner, parameter.name, null);
                    field.setType(parameter.getType());
                    field.setModifiers(AgoClass.PRIVATE);
                    field.setDeclaration(parameter.getDeclaration());
                    field.setSourceLocation(unit.sourceLocation(parameter.getDeclaration()));
                    field.setModifiers(parameter.modifiers);
                    parameter.disableGetterSetter();
                    owner.addField(field);
                }
            }
        }
    }

    @Override
    public boolean addDependency(ClassDef dependency) {
        if(this.isEmptyArgs()){
            return this.getParentClass().addDependency(dependency);
        }
        return true;    // ignore it
    }

    public ParserRuleContext getDeclarationAst(){
        return this.constructorDeclaration;
    }

    public ParserRuleContext getDeclarationName(){
        return constructorDeclaration;      // no name
    }
    
    @Override
    public ParserRuleContext getMethodBodyContext() {
    	return constructorDeclaration != null ? this.constructorDeclaration.constructorBody : null;
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return null;
    }

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return null;
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl() {
        return null;
    }

    public ConstructorDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var clone = this.constructorDeclaration != null ?
                new ConstructorDef(this.modifiers, this.constructorDeclaration):
                new ConstructorDef(this.modifiers,name);
        this.cloneTo(instantiationArguments, clone);
        return clone;
    }

    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        if(this.constructorDeclaration == null || constructorDeclaration.constructorBody == null){
            new BlockCompiler(this.unit, this, new ArrayList<>()).compile();
        } else {
            AgoParser.BlockContext constructorBody = constructorDeclaration.constructorBody;
            new BlockCompiler(this.unit, this, constructorBody.blockStatement()).compile();
        }

        this.nextCompilingStage(CompilingStage.Compiled);   // Compiled
    }


    public boolean hasFieldParameters() {
        return this.getParameters().stream().anyMatch(Variable::isField);
    }
}
