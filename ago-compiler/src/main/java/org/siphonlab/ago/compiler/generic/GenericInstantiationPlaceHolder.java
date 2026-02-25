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

import org.siphonlab.ago.compiler.ClassContainer;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.FunctionInvocationResolver;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.*;

public class GenericInstantiationPlaceHolder extends ClassDef {

    private final ClassDef templateClass;
    private final SourceLocation sourceLocation;
    private final ClassDef scopeClass;

    public GenericInstantiationPlaceHolder(ClassDef templateClass, SourceLocation sourceLocation, ClassDef scopeClass) {
        super(templateClass.getFullname());
        this.templateClass = templateClass;
        this.sourceLocation = sourceLocation;
        this.scopeClass = scopeClass;

        this.setClassType(templateClass.getClassType());
        this.setUnit(templateClass.getUnit());
        this.setModifiers(templateClass.getModifiers());
        this.setSuperClass(templateClass.getSuperClass());
        this.setInterfaces(templateClass.getInterfaces());
        this.setCompilingStage(templateClass.getCompilingStage());
        this.parent = templateClass.getParent();
    }

    public ClassDef resolve(ClassRefLiteral[] args) throws CompilationError {
        var pc = ((ClassContainer) templateClass.getParent()).getOrCreateGenericInstantiationClassDef(templateClass, args, null);
        scopeClass.registerConcreteType(pc);
        scopeClass.idOfClass(templateClass);
        return (ClassDef) pc;
    }

    public ClassDef resolve(FunctionDef ownerFunction, List<Expression> arguments) throws CompilationError {
        List<FunctionDef> constructors = templateClass.getConstructors();
        var resolver = new FunctionInvocationResolver(ownerFunction, templateClass.getConstructor(),
                constructors.size() == 1 ? null : constructors,
                arguments, sourceLocation);
        TypeParamsContext paramsContext = templateClass.getTypeParamsContext();
        var r = resolver.resolve(resolveResult -> {
            if (!resolveResult.allFound(paramsContext)) {
                resolveResult.error = new ResolveError("not all generic type params provided concrete argument, expected:%d provided:'%d'"
                        .formatted(paramsContext.size(), resolveResult.providedArguments.size()), sourceLocation);
            }
        });
        ClassRefLiteral[] typeArgs = r.toTypeArgs(paramsContext);
        return resolve(typeArgs);
    }



}
