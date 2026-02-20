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

import java.util.*;

import static org.siphonlab.ago.compiler.generic.GenericInstantiate.composeName;

// generic class not like ParameterizedClassDef, it's not created by metaclass,
// therefore it doesn't extend ParameterizedClassDef
public class GenericInstantiationClassDef extends ClassDef implements GenericConcreteType {
    private final static Logger LOGGER = LoggerFactory.getLogger(GenericInstantiationClassDef.class);

    private final ClassDef templateClass;
    private final InstantiationArguments instantiationArguments;

    public GenericInstantiationClassDef(ClassDef templateClass, ClassContainer parent, InstantiationArguments instantiationArguments) throws CompilationError {
        super(composeName(templateClass, instantiationArguments.getTypeArgumentsArray()));
        this.templateClass = templateClass;
        this.instantiationArguments = instantiationArguments;
        this.setClassType(templateClass.getClassType());
        if(parent != null) parent.addChild(this);

        templateClass.cloneTo(instantiationArguments, this);
        if(templateClass.getCompilingStage() == CompilingStage.Compiled || templateClass.getCompilingStage() == CompilingStage.CompileMethodBody){
            GenericInstantiate.syncCompilingStage(this, templateClass.getCompilingStage());
        }
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if (this.compilingStage != CompilingStage.ResolveHierarchicalClasses)
            return;

        super.resolveHierarchicalClasses();

        if(this.isInterfaceOrTrait()) {
            var templ = this.getTemplateClass();
            var instantiationArguments = this.getGenericSource().instantiationArguments();
            this.setPermitClass(templ.getPermitClass().instantiate(instantiationArguments, null));

            this.setCompilingStage(CompilingStage.ParseFields);     // bypass ValidateHierarchy
        }
    }

    @Override
    public boolean instantiateFields() throws CompilationError {
        return super.instantiateFields();
    }

    @Override
    protected void instantiateFieldsForInterfacesAndTraits() throws CompilationError {
        super.instantiateFieldsForInterfacesAndTraits();
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        super.inheritsChildClasses();
        this.setCompilingStage(CompilingStage.AllocateSlots);
    }

    @Override
    public void allocateSlotsForFields() throws CompilationError {
        super.allocateSlotsForFields();
        this.setCompilingStage(CompilingStage.Compiled);
    }

    @Override
    public void validateNewFunction(FunctionDef newFun) throws SyntaxError {
        //nothing to do
    }

    public ClassRefLiteral[] getTypeArguments() {
        return this.instantiationArguments.getTypeArgumentsArray();
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
//        return (ClassDef) this.findAssignableGenericInstantiationClass(anotherClass, visited);
        return super.asThatOrSuperOfThat(anotherClass, visited);
    }

    @Override
    public ClassDef getTemplateClass() {
        return templateClass;
    }

    @Override
    public List<ClassDef> getConcreteDependencyClasses() {
        return GenericInstantiate.getConcreteDependencyClasses(this);
    }

    @Override
    public Unit getUnit() {
        return this.templateClass.getUnit();
    }

    @Override
    public void registerConcreteType(ConcreteType concreteType) {
        if(concreteType == this) return;

        var stack = new Stack<ConcreteType>();
        stack.addAll(this.getConcreteTypes().values());
        while(!stack.isEmpty()){
            var value = stack.pop();
            if(this.getConcreteTypes().containsKey(value.getFullname()) || value == this) continue;
            super.registerConcreteType(value);

            ClassDef c = (ClassDef) value;
            for (ConcreteType type : c.getConcreteTypes().values()) {
                if(!this.getConcreteTypes().containsKey(type.getFullname())){
                    stack.add(type);
                }
            }
        }

        super.registerConcreteType(concreteType);
    }

    @Override
    public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
        if(super.isAffectedByTemplate(instantiationArguments)) return true; //TODO already instantiated?
        for (ClassRefLiteral typeArgument : this.getTypeArguments()) {
            if(typeArgument.getClassDefValue().isAffectedByTemplate(instantiationArguments)){
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
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return this.getTemplateClass().getInterfaceDecls();
    }
}
