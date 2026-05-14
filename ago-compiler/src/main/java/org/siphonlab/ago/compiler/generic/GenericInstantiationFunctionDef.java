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
package org.siphonlab.ago.compiler.generic;


import org.antlr.v4.runtime.ParserRuleContext;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import static org.siphonlab.ago.compiler.generic.GenericInstantiate.composeName;

public class GenericInstantiationFunctionDef extends FunctionDef implements GenericConcreteType {
    private final static Logger LOGGER = LoggerFactory.getLogger(GenericInstantiationFunctionDef.class);

    private final FunctionDef templateClass;
    private final InstantiationArguments instantiationArguments;

    private AgoParser.TypeArgsListContext typeArgsListContext;

    public GenericInstantiationFunctionDef(FunctionDef templateClass, ClassContainer parent, InstantiationArguments instantiationArguments) throws CompilationError {
        super(templateClass.getRoot(), composeName(templateClass, instantiationArguments.takeFor(templateClass)), templateClass.getMethodDecl());
        this.templateClass = templateClass;
        this.instantiationArguments = instantiationArguments;
        this.setGenericSource(new GenericSource(templateClass, instantiationArguments, instantiationArguments.takeFor(templateClass)));
        this.setClassType(templateClass.getClassType());
        templateClass.cloneTo(instantiationArguments, this, parent);
        if(templateClass.getCompilingStage() == CompilingStage.Compiled || templateClass.getCompilingStage() == CompilingStage.CompileMethodBody){
            GenericInstantiate.syncCompilingStage(this, templateClass.getCompilingStage());
        }
    }

    @Override
    public void validateNewFunction(FunctionDef newFun) throws SyntaxError {
        //nothing to do
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        super.inheritsChildClasses();
        this.setCompilingStage(CompilingStage.AllocateSlots);
    }

    public ClassRefLiteral[] getTypeArguments() {
        return this.instantiationArguments.takeFor(this.templateClass);
    }

    @Override
    public void setTypeArgsListAst(AgoParser.TypeArgsListContext typeArguments) {
        this.typeArgsListContext = typeArguments;
    }

    @Override
    public AgoParser.TypeArgsListContext getTypeArgsListAst() {
        return this.typeArgsListContext;
    }

    @Override
    public ClassDef getTemplateClass() {
        return templateClass;
    }

    @Override
    public List<ClassDef> getConcreteDependencyClasses() {
        return GenericInstantiate.getConcreteDependencyClasses(this);
    }

    //    @Override
//    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
//        var newArgs = this.instantiationArguments.applyIntermediate(arguments);
//        return this.templateClass.instantiate(newArgs, returnExisted);
//    }

    @Override
    public void registerConcreteType(ConcreteType concreteType) {
        if(concreteType == this) return;
        if (this.getParentClass() != null) {
            this.getParentClass().registerConcreteType(concreteType);
            return;
        }
        var stack = new Stack<ConcreteType>();
        stack.addAll(this.getConcreteTypes().values());
        while(!stack.isEmpty()){
            var value = stack.pop();
            if(this.getConcreteTypes().containsKey(value.getFullname()) || value == this) continue;
            super.registerConcreteType(value);

            ClassDef c = (ClassDef) value;
            Collection<ConcreteType> cts = c.getConcreteTypes().values();
            for (ConcreteType ct : cts) {
                if(!getConcreteTypes().containsKey(ct.getFullname()) && ct != this){
                    stack.add(ct);
                }
            }
        }

        super.registerConcreteType(concreteType);
    }

    @Override
    public void allocateSlotsForFields() throws CompilationError {
        super.allocateSlotsForFields();
        this.setCompilingStage(CompilingStage.Compiled);
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        if(super.isAffectedByTypeArguments(instantiationArguments)) return true; //TODO already instantiated?
        for (ClassRefLiteral typeArgument : this.getTypeArguments()) {
            if(typeArgument.getClassDefValue().isAffectedByTypeArguments(instantiationArguments)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void acceptRegisterConcreteType(ClassDef hostClass) {
        //
    }

    public InstantiationArguments getInstantiationArguments() {
        return this.instantiationArguments;
    }

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return templateClass.getGenericTypeParametersContextAST();
    }

    @Override
    public ParserRuleContext getDeclarationAst() {
        return templateClass.getDeclarationAst();
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        return this.templateClass.getClassBody();
    }

    @Override
    public ParserRuleContext getDeclarationName() {
        return this.getTemplateClass().getDeclarationName();
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl() {
        return this.templateClass.getBaseTypeDecl();
    }

    @Override
    public AgoParser.MethodDeclarationContext getMethodDecl() {
        return templateClass.getMethodDecl();
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return this.getTemplateClass().getInterfaceDecls();
    }

}
