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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.*;

import java.util.*;

public class ClassContainer extends Namespace<ClassDef>{

    public ClassContainer(String name) {
        super(name);
    }

    public ClassContainer(String name, Namespace parent) {
        super(name, parent);
    }

    public void validateNewFunction(FunctionDef newFun) throws CompilationError {
        if(newFun.getCompilingStage().lt(CompilingStage.InheritsFields)) {
            Compiler.processClassTillStage(newFun, CompilingStage.InheritsFields);
        }
        var unit = newFun.getUnit();
        var existed = getSameSignatureFunction(newFun);
        if (existed == null) {
            if (newFun.isOverride()) {
                getSameSignatureFunction(newFun);
                throw unit.syntaxError(newFun.getDeclarationAst(), "not found function to override");
            }
            return;
        }

        if (!existed.getName().equals(newFun.getName())) {
            throw unit.syntaxError(newFun.getDeclarationName(), "duplicated function with different name, '%s' and '%s'".formatted(existed.getName(), newFun.getName()));
        }
        if (!(newFun instanceof ConstructorDef)) {
            if (existed.getParent() == this) {
                throw unit.syntaxError(newFun.getDeclarationAst(), "duplicated function '%s' found".formatted(newFun.getName()));
            } else {
                if (newFun.getResultType() != existed.getResultType() && !existed.getResultType().isThatOrSuperOfThat(newFun.getResultType())) {
                    throw unit.syntaxError(newFun.getDeclarationAst(), "function result type changed from '%s' to '%s'".formatted(existed.getResultType(), newFun.getResultType()));
                }
                if (!newFun.isOverride()) {
                    throw unit.syntaxError(newFun.getDeclarationAst(), "'override' required");
                }
            }
        }
    }

    /**
     * find function to override/overload
     * @param newFun
     * @return
     */
    public FunctionDef getSameSignatureFunction(FunctionDef newFun){
        var children = this.getChildren(newFun.getCommonName());
        for (ClassDef c : children) {
            if(c == newFun) continue;
            if(c instanceof FunctionDef existed){
                if(newFun.isSameSignatureWith(existed)) return existed;
            }
        }
        return null;
    }

    public List<ClassDef> getDirectChildren(){
        return this.getUniqueChildren().stream().filter(c -> c.getParent() == this && !(c instanceof ConcreteType)).toList();
    }

    public ParameterizedClassDef getOrCreateParameterizedClass(ClassDef baseClassDef, ConstructorDef constructorDef, Literal<?>[] arguments, MutableBoolean returnExisted) throws CompilationError {
        String className = ParameterizedClassDef.composeName(baseClassDef, arguments);
        var existed = this.getChild(className);
        if(existed != null) {
            if(returnExisted != null) returnExisted.setTrue();
            return (ParameterizedClassDef) existed;
        }

        var pc = new ParameterizedClassDef(baseClassDef, constructorDef, arguments);
        this.addChild(pc);

        return pc;
    }

    public ClassIntervalClassDef getOrCreateClassInterval(ClassDef baseClassDef, ConstructorDef constructorDef, Literal<?>[] arguments, MutableBoolean returnExisted) throws CompilationError {
        String className = ParameterizedClassDef.composeName(baseClassDef, arguments);
        var existed = this.getChild(className);
        if(existed != null) {
            if(returnExisted != null) returnExisted.setTrue();
            return (ClassIntervalClassDef) existed;
        }

        var pc = new ClassIntervalClassDef(baseClassDef, constructorDef, arguments);
        this.addChild(pc);

        return pc;
    }

    public ScopedClassIntervalClassDef getOrCreateScopedClassInterval(ClassDef baseClassDef, ConstructorDef constructorDef, Literal<?>[] arguments, MutableBoolean returnExisted) throws CompilationError {
        String className = ParameterizedClassDef.composeName(baseClassDef, arguments);
        var existed = this.getChild(className);
        if(existed != null) {
            if(returnExisted != null) returnExisted.setTrue();
            return (ScopedClassIntervalClassDef) existed;
        }

        var pc = new ScopedClassIntervalClassDef(baseClassDef, constructorDef, arguments);
        this.addChild(pc);

        return pc;
    }

    public SharedGenericTypeParameterClassDef getOrCreateGenericTypeParameter(ClassDef baseClassDef, ConstructorDef constructorDef, Literal<?>[] arguments, MutableBoolean returnExisted) throws CompilationError {
        String className = ParameterizedClassDef.composeName(baseClassDef, arguments);
        var existed = this.getChild(className);
        if(existed != null) {
            if(returnExisted != null) returnExisted.setTrue();
            return (SharedGenericTypeParameterClassDef) existed;
        }

        var pc = new SharedGenericTypeParameterClassDef(baseClassDef, constructorDef, arguments);
        this.addChild(pc);

        return pc;
    }

    public GenericConcreteType getOrCreateGenericInstantiationClassDef(ClassDef templateClass, ClassRefLiteral[] typeArguments, MutableBoolean returnExisted) throws CompilationError {
        // template class is not intermediate template class
        InstantiationArguments args = new InstantiationArguments(templateClass.getGenericSource() == null ? templateClass.getTypeParamsContext() : templateClass.getGenericSource().originalTemplate().getTypeParamsContext(), typeArguments);
        return (GenericConcreteType)templateClass.instantiate(args, returnExisted);
    }


}
