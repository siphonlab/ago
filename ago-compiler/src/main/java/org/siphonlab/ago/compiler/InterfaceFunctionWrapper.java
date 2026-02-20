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
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.generic.*;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.ArrayList;
import java.util.List;

public class InterfaceFunctionWrapper extends FunctionDef{

    private FunctionDef interfaceFun;
    private Field field;
    private ParserRuleContext identifierContext;

    public InterfaceFunctionWrapper(ClassContainer parent, FunctionDef interfaceFun, Field field, ParserRuleContext identifierContext) throws CompilationError {
        super(interfaceFun.getName(), interfaceFun.getMethodDecl());
        this.parent = parent;
        this.setInterfaceFun(interfaceFun);
        this.field = field;
        this.identifierContext = identifierContext;
        this.setUnit(interfaceFun.getUnit());
//        this.idOfClass(interfaceFun);  it will invoke in compileBody
        assert interfaceFun.getCompilingStage().getValue() > CompilingStage.ParseFields.getValue();
    }

    private void setInterfaceFun(FunctionDef interfaceFun) {
        this.interfaceFun = interfaceFun;
        this.setInterfaces(interfaceFun.getInterfaces());
        this.setSuperClass(interfaceFun.getSuperClass());
        int modifiers = (AgoClass.OVERRIDE | AgoClass.PUBLIC | interfaceFun.modifiers) & 0xffff_fbff;   // remove `abstract` modifier
        this.setModifiers(modifiers);
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        super.resolveHierarchicalClasses();
        if (interfaceFun.isGenericTemplate()) {
            // create my template parameters and map interfaceFun to instantiation of my type args
            TypeParamsContext typeParamsContext = interfaceFun.getTypeParamsContext();
            this.shiftToTemplate();
            for (int i = 0; i < typeParamsContext.size(); i++) {
                GenericTypeCode genericTypeCode = typeParamsContext.get(i);
                this.typeParamsContext.addGenericTypeParam(genericTypeCode.getName(), genericTypeCode.getGenericTypeParameterClassDef(), genericTypeCode.getGenericTypeParameterContext());
            }
            GenericConcreteType instantiatedFun = this.getOrCreateGenericInstantiationClassDef(interfaceFun, this.typeParamsContext.createDefaultArgumentsArray(), null);
            this.registerConcreteType(instantiatedFun);
            this.setInterfaceFun((FunctionDef) instantiatedFun);

            Compiler.processClassTillStage(this.interfaceFun, this.getCompilingStage());
        }
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        Compiler.processClassTillStage(this.interfaceFun, this.getCompilingStage());

        this.setInterfaces(interfaceFun.getInterfaces());
        this.setSuperClass(interfaceFun.getSuperClass());

        this.nextCompilingStage(CompilingStage.ValidateHierarchy);

        return true;
    }

    @Override
    public void inheritsFields() throws CompilationError {
        if(this.compilingStage != CompilingStage.InheritsFields) return;

        this.interfaceFun.inheritsFields();

        for (Parameter parameter : interfaceFun.getParameters()) {
            var p = new Parameter(parameter.name, parameter.parameterContext);
            p.setType(parameter.getType());
            p.setModifiers(parameter.modifiers);
            p.setInitializer(parameter.getInitializer());
            p.setSourceLocation(parameter.getSourceLocation());
            this.addParameter(p);
        }
        this.setResultType(interfaceFun.getResultType());

        this.setCompilingStage(CompilingStage.InheritsInnerClasses);
    }

    @Override
    public void allocateSlotsForFields() throws CompilationError {
        if (this.compilingStage != CompilingStage.AllocateSlots)
            return;
        super.allocateSlotsForFields();
        this.interfaceFun.allocateSlotsForFields();
    }

    @Override
    public List<ClassDef> getInterfaces() {
        return interfaceFun.getInterfaces();        // TODO check template interface
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return interfaceFun.getInterfaceDecls();
    }

    @Override
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;

        // if interfaceFun is an instantiation, needn't compileBody, otherwise it will compileBody by itself

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        var blockCompiler = new BlockCompiler(this.unit, this, null);
        Var.Field fld = new Var.Field(new Scope(1, this.getParentClass()), field)
                    .setSourceLocation(this.getParentClass().unit.sourceLocation(field.getDeclaration()));
        List<Expression> arguments = new ArrayList<>();
        for (Parameter parameter : getParameters()) {
            var arg = Var.of(new Scope.Local(this), parameter);
            arguments.add(arg);
        }
        var invoke = new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(fld, this.interfaceFun), arguments, fld.getSourceLocation());
        Expression expr;
        if (this.getResultType() == PrimitiveClassDef.VOID) {
            expr = invoke;
            blockCompiler.compileExpressions(List.of(new ExpressionStmt(expr), new Return()));
        }  else {
            expr = new Return(invoke);
            blockCompiler.compileExpressions(List.of(expr));
        }

        this.nextCompilingStage(CompilingStage.Compiled);
    }

    public FunctionDef getInterfaceFun() {
        return interfaceFun;
    }

    public Field getWrapperField() {
        return field;
    }

    public ParserRuleContext getIdentifierContext() {
        return identifierContext;
    }

    @Override
    public void cloneTo(InstantiationArguments instantiationArguments, ClassDef instantiateClass) throws CompilationError {

        instantiateClass.setGenericSource(new GenericSource(this, instantiationArguments));
        this.putInstantiatedClassToCache(instantiationArguments, instantiateClass);

        if (instantiationArguments.getSourceTemplate() != this) {
            var args = instantiationArguments.takeFor(this);
            if (args != null) {
                this.putInstantiatedClassToCache(args, instantiateClass);
            }
        }

        instantiateClass.setUnit(this.getUnit());
        instantiateClass.setSourceLocation(this.getSourceLocation());        //TODO the source location assign to template's source location?

        if (instantiateClass instanceof FunctionDef tempFun) {
            FunctionDef targetFun = (FunctionDef) instantiateClass;
            targetFun.setNativeEntrance(tempFun.getNativeEntrance());
            targetFun.setCommonName(tempFun.getCommonName());
            targetFun.setThrowsExceptions(tempFun.getThrowsExceptions());
        }
        instantiateClass.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);      // ParseClassName and ParseGenericParams skipped, it will enter ResolveHierarchicalClasses and ParseField soon

        instantiateChildren(instantiateClass, instantiationArguments);

    }
}
