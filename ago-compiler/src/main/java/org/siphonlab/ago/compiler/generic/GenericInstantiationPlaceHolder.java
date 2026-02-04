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

    public ClassDef resolve(List<Expression> arguments) throws CompilationError {
        List<FunctionDef> constructors = templateClass.getConstructors();
        var resolver = new FunctionInvocationResolver(templateClass.getConstructor(),
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
