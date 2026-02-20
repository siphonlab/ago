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

import java.util.List;

public class TraitDef extends ClassDef{

    private final static Logger LOGGER = LoggerFactory.getLogger(TraitDef.class);

    private final AgoParser.TraitDeclarationContext traitDeclaration;

    public TraitDef(String name, AgoParser.TraitDeclarationContext traitDeclaration) {
        super(name);
        this.traitDeclaration = traitDeclaration;
        this.classType = AgoClass.TYPE_TRAIT;
    }

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return traitDeclaration.genericTypeParameters();
    }

    @Override
    public ParserRuleContext getDeclarationAst() {
        return traitDeclaration;
    }

    public AgoParser.TraitDeclarationContext getTraitDeclaration() {
        return traitDeclaration;
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        return traitDeclaration.classBody();
    }

    @Override
    public ParserRuleContext getDeclarationName() {
        return traitDeclaration.className;
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl() {
        return traitDeclaration.extendsPhrase() != null ? traitDeclaration.extendsPhrase().baseType : null;
    }

    public AgoParser.DeclarationTypeContext getPermitTypeDecl(){
        var permitsTypeContext = traitDeclaration.permitsType();
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
        var implementsPhraseContext = traitDeclaration.implementsPhrase();
        return implementsPhraseContext != null? implementsPhraseContext.interfaceList().interfaceItem() : null;
    }

    @Override
    public String toString() {
        if(this.compilingStage != CompilingStage.Compiled){
            return "(Trait %s %s)".formatted(this.getFullname(), this.compilingStage);
        }
        return "(Trait %s)".formatted(this.getFullname());
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.InheritsFields);
            return false;
        }
        if (!super.parseFields()) return false;

        if(this.permitClass != null && this.permitClass != getRoot().getObjectClass()){
            if(this.fieldForPermitClass == null){
                String fldName = "@permit_" + (permitClass.getGenericSource() == null? permitClass.getName(): permitClass.getGenericSource().originalTemplate().getName());
                for(var i = 0; ;i++) {
                    String s = fldName + "_" + i;
                    if (this.fields.containsKey(s)) {
                        fldName = s;
                    } else {
                        break;
                    }
                }
                var field = new Field(this, fldName, null);
                field.setType(this.permitClass);
                field.setModifiers(AgoClass.PRIVATE);
                field.setDeclaration(this.getPermitTypeDecl());
                //TODO field.setSourceLocation(); permit declaration
                this.addField(field);
                this.fieldForPermitClass = field;
            }
        }
        return true;
    }

    @Override
    public void allocateSlotsForFields() throws CompilationError {
        super.allocateSlotsForFields();
    }

    @Override
    public void verifyMembers() throws SyntaxError {
        super.verifyMembers();

        if(this.constructor != null){
            if(this.getConstructors().size() > 1){
                throw unit.syntaxError(this.getConstructors().stream().filter(c -> c!=this.constructor).findFirst().get().getDeclarationAst(),
                        "trait can only have one constructor");
            }
            if(!this.constructor.getParameters().isEmpty()){
                throw unit.syntaxError(this.constructor.getConstructorDeclaration().formalParameters(), "the constructor for trait cannot have parameters");
            }
        }
    }

    public TraitDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var clone = new TraitDef(name, this.traitDeclaration);
        this.cloneTo(instantiationArguments, clone);
        return clone;
    }

    @Override
    void instantiateHierarchy() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        super.instantiateHierarchy();

        var templ = this.getTemplateClass();
        var instantiationArguments = this.getGenericSource().instantiationArguments();
        this.setPermitClass(templ.getPermitClass().instantiate(instantiationArguments, null));

        this.setCompilingStage(CompilingStage.ParseFields);     // bypass ValidateHierarchy
    }



}
