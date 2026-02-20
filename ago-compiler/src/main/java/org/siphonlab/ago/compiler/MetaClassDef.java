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
import org.siphonlab.ago.compiler.generic.GenericTypeCode;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

import static java.lang.String.format;
import static org.siphonlab.ago.AgoClass.TYPE_METACLASS;

public class MetaClassDef extends ClassDef{

    private final ClassDef instanceClassDef;
    private final AgoParser.MetaclassDeclarationContext metaclassDeclaration;

    // depth, default is 1, metaclass of metaclass is 2, the max value is 2
    private final int metaLevel;

    public MetaClassDef(ClassDef instanceClassDef, int metaLevel, AgoParser.MetaclassDeclarationContext metaclassDeclaration) {
        super(format("Meta@<%s>", instanceClassDef.getFullnameWithoutPackage()));
        this.instanceClassDef = instanceClassDef;
        this.metaclassDeclaration = metaclassDeclaration;
        this.metaLevel = metaLevel;
        this.unit = instanceClassDef.unit;
        this.classType = TYPE_METACLASS;
        this.modifiers = instanceClassDef.getVisibility();
//        if(instanceClassDef.isAbstract()) this.modifiers |= AgoClass.ABSTRACT;
        if(instanceClassDef.isFinal()) this.modifiers |= AgoClass.FINAL;
    }

    public ClassDef getInstanceClassDef() {
        return instanceClassDef;
    }

    @Override
    public boolean addDependency(ClassDef dependency) {
        return super.addDependency(dependency);
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        return this.metaclassDeclaration == null? null : this.metaclassDeclaration.classBody();
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        if(this.instanceClassDef.isEnum()){
            return;
        }
        super.inheritsChildClasses();
    }

    @Override
    public ParserRuleContext getDeclarationAst() {
        return metaclassDeclaration;
    }

    @Override
    public void setSuperClass(ClassDef superClass) {
        super.setSuperClass(superClass);
        if(superClass instanceof MetaClassDef metaClassDefSuper) {
            if (metaClassDefSuper.getInstanceClassDef() instanceof ConcreteType concreteType) {
                this.dependencies.remove(superClass);
            }
        } else {
            this.dependencies.remove(superClass);       // lang.Class
        }
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;
        resolveMetaclass();
        this.nextCompilingStage(CompilingStage.ParseFields);
    }

    public MetaClassDef resolveMetaclass() throws CompilationError {
        if(this.getMetaLevel() == 2)
            return null;
        return super.resolveMetaclass();
    }

    @Override
    public GenericTypeCode findGenericType(String genericTypeName) {
        return instanceClassDef.findGenericType(genericTypeName);
    }

    @Override
    public ParserRuleContext getDeclarationName() {
        return null;
    }

    public int getMetaLevel() {
        return metaLevel;
    }

    @Override
    public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
        return this.getInstanceClassDef().isAffectedByTemplate(instantiationArguments);
    }

    public MetaClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var instanceClass = this.instanceClassDef.getCachedInstantiatedClass(instantiationArguments);
        var clone = new MetaClassDef(instanceClass, metaLevel, metaclassDeclaration);
        this.getParent().addChild(clone);
        super.cloneTo(instantiationArguments, clone);
        return clone;
    }

    @Override
    public MetaClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return (MetaClassDef) super.instantiate(arguments, returnExisted);
    }

    @Override
    public boolean isGenericInstantiateRequiredForNew() {
        return this.instanceClassDef.isGenericInstantiateRequiredForNew();
    }

    public boolean isInGenericInstantiation(){
        return this.instanceClassDef.isInGenericInstantiation();
    }

    public boolean isInGenericTemplate(){
        return this.instanceClassDef.isInGenericTemplate();
    }

}
