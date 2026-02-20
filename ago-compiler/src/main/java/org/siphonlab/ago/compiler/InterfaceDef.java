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
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InterfaceDef extends ClassDef{

    private final static Logger LOGGER = LoggerFactory.getLogger(InterfaceDef.class);

    private final AgoParser.InterfaceDeclarationContext interfaceDeclaration;

    public InterfaceDef(String name, AgoParser.InterfaceDeclarationContext interfaceDeclaration) {
        super(name);
        this.interfaceDeclaration = interfaceDeclaration;
        this.classType = AgoClass.TYPE_INTERFACE;
        this.modifiers |= AgoClass.ABSTRACT;
    }

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return interfaceDeclaration.genericTypeParameters();
    }

    @Override
    public ParserRuleContext getDeclarationAst() {
        return interfaceDeclaration;
    }

    public AgoParser.InterfaceDeclarationContext getInterfaceDeclaration() {
        return interfaceDeclaration;
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        return interfaceDeclaration.classBody();
    }

    @Override
    public ParserRuleContext getDeclarationName() {
        return interfaceDeclaration.interfaceName;
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl() {
        return null;
    }

    public AgoParser.DeclarationTypeContext getPermitTypeDecl(){
        var permitsTypeContext = interfaceDeclaration.permitsType();
        return permitsTypeContext != null ? permitsTypeContext.declarationType() : null;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if (this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;
        super.resolveHierarchicalClasses();
        if (this.getInterfaces() != null) {
            for (ClassDef superInterface : this.getInterfaces()) {
                if (superInterface.getCompilingStage() == CompilingStage.ResolveHierarchicalClasses) {
                    superInterface.resolveHierarchicalClasses();
                }
            }
        }
        this.unit.resolvePermitClass(this,this.getPermitTypeDecl());
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        var implementsPhraseContext = interfaceDeclaration.extendsInterfaces();
        return implementsPhraseContext != null? implementsPhraseContext.interfaceList().interfaceItem() : null;
    }

    @Override
    public String toString() {
        if(this.compilingStage != CompilingStage.Compiled){
            return "(Interface %s %s)".formatted(this.getFullname(), this.compilingStage);
        }
        return "(Interface %s)".formatted(this.getFullname());
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        if (this.compilingStage != CompilingStage.InheritsInnerClasses) return;
        for(var superInterface : this.getInterfaces()){
            if (superInterface != null) {
                if (superInterface.compilingStage == CompilingStage.InheritsInnerClasses)
                    superInterface.inheritsChildClasses();

                var classes = superInterface.getUniqueChildren();
                inheritsChildClasses(classes);
            }
        }
        this.nextCompilingStage(CompilingStage.ValidateMembers);
    }

    @Override
    public void inheritsFields() {
        if(this.getCompilingStage() != CompilingStage.InheritsFields) return;
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherits fields".formatted(this));
        this.setCompilingStage(CompilingStage.ValidateNewFunctions);
    }

    @Override
    public void verifyMembers() throws SyntaxError {
        super.verifyMembers();

        for (ClassDef child : this.getDirectChildren()) {
            if(!child.isPublic()){
                throw unit.syntaxError(child.getDeclarationName(), "child class inside interface must be public");  // TODO source location of visibility
            }
        }
    }

    public InterfaceDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var clone = new InterfaceDef(name, this.interfaceDeclaration);
        this.cloneTo(instantiationArguments, clone);
        return clone;
    }

    @Override
    void instantiateHierarchy() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        var templ = this.getTemplateClass();
        var instantiationArguments = this.getGenericSource().instantiationArguments();
        List<ClassDef> list = new ArrayList<>();
        for (ClassDef i : templ.getInterfaces()) {
            ClassDef instantiate = i.instantiate(instantiationArguments, null);
            list.add(instantiate);
        }
        this.setInterfaces(list);
        this.setSuperClass(templ.getSuperClass().instantiate(instantiationArguments, null));       // TODO and parameterized superclass

        this.setPermitClass(templ.getPermitClass().instantiate(instantiationArguments, null));

        this.resolveMetaclass();

        this.setCompilingStage(CompilingStage.ParseFields);     // bypass ValidateHierarchy
    }

    @Override
    public void verifyFunctionsImplemented(ClassDef abstractClassDef, ParserRuleContext extendsAbstractClassDecl) throws ResolveError {
        // interface needn't implement functions
    }
}
