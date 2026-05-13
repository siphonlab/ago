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
package org.siphonlab.ago.compiler.expression.invoke;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Cast;
import org.siphonlab.ago.compiler.expression.CastStrategy;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.generic.*;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.*;
import java.util.function.Consumer;

public class FunctionInvocationResolver {

    private final FunctionDef ownerFunction;
    private final FunctionDef method;
    private final Collection<FunctionDef> candidates;
    private final List<Expression> arguments;
    private final SourceLocation sourceLocation;

    public FunctionInvocationResolver(FunctionDef ownerFunction, FunctionDef method, Collection<FunctionDef> candidates, List<Expression> arguments, SourceLocation sourceLocation){
        this.ownerFunction = ownerFunction;
        this.method = method;
        this.candidates = candidates != null && !candidates.isEmpty() ? candidates : null;
        assert this.candidates == null || this.candidates.contains(method);
        this.arguments = arguments;
        this.sourceLocation = sourceLocation;
    }

    public ResolveResult resolve(Consumer<ResolveResult> acceptor) throws CompilationError {
        if(this.candidates == null){
            ResolveResult resolveResult = resolve(method, arguments);
            if(resolveResult.error != null){
                throwError(resolveResult);
            } else if(resolveResult.score == 0){
                throw new ResolveError("arguments not match", sourceLocation);
            }
            return resolveResult;
        } else {
            List<ResolveResult> resolveResults = new ArrayList<>();
            int matchCount = 0;
            for (FunctionDef functionDef : candidates) {
                ResolveResult resolveResult = resolve(functionDef, arguments);
                resolveResults.add(resolveResult);
                if(resolveResult.error == null && resolveResult.score > 0) {
                    acceptor.accept(resolveResult);
                }
                if(resolveResult.error == null && resolveResult.score > 0) {
                    matchCount ++;
                }
            }
            var matched = resolveResults.stream()
                    .filter(r -> r.error == null && r.score > 0);
            if(matchCount == 0) {
                throwError(resolveResults.getFirst());
            }

            if(matchCount == 1){
                return matched.findFirst().get();
            } else {
                var ls = matched.sorted((r1, r2) -> Double.compare(r2.score , r1.score)).toList();
                var first = ls.getFirst();
                var scnd = ls.get(1);
                if(first.score == scnd.score){
                    throw new ResolveError( "multi functions compatible for invocation '%s' '%s'".formatted(first.functionDef.getFullname(), scnd.functionDef.getFullname()), sourceLocation);
                }
                return first;
            }
        }
    }

    private void throwError(ResolveResult errorResolveResult) throws CompilationError {
        if(errorResolveResult.paramIndex == -1){
            throw errorResolveResult.error;
        } else {
            errorResolveResult.error.setSourceLocation(this.arguments.get(errorResolveResult.paramIndex).getSourceLocation());
            throw errorResolveResult.error;
        }
    }

    private ResolveResult resolve(FunctionDef functionDef, List<Expression> arguments) {
        var resolveResult = new ResolveResult(functionDef);
        if(functionDef.getParameters().size() > arguments.size()){
            if (!functionDef.getParameters().getLast().isVarArgs() || functionDef.getParameters().size() - 1 != arguments.size()) {
                resolveResult.error = new ResolveError("arguments count mismatch, %d expected, but only pass %d".formatted(functionDef.getParameters().size(), arguments.size()), sourceLocation);
                return resolveResult;
            }
        }
        List<Parameter> parameters = functionDef.getParameters();
        ClassDef[] parameterTypes = parameters.stream().map(p -> {
            if (!p.isVarArgs()) return p.getType();
            try {
                return new VarArgs((ArrayClassDef) p.getType());        // wrap varargs parameter type to a temporary class VarArgs
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }).toArray(ClassDef[]::new);

        List<ClassDef> argTypes = new ArrayList<>();
        List<ClassDef> argTypesPreserveVarArgs = new ArrayList<>();
        int argPos = 0;
        for (int p = 0; p < parameterTypes.length; p++) {
            var parameterType = parameterTypes[p];
            if(p >= arguments.size()){
                if(parameters.getLast().isVarArgs() && p == arguments.size()){
                    //
                } else {
                    resolveResult.error = new ResolveError("arguments count mismatch", sourceLocation);
                    return resolveResult;
                }
            }
            ClassDef argType = null;
            try {
                if(parameterType instanceof ClassIntervalClassDef) {     // cast class to ScopedClassInterval
                    argType = new Cast(ownerFunction, arguments.get(p), parameterType).transform().inferType();
                    argTypesPreserveVarArgs.add(argType);
                    argPos++;
                } else if(parameterType instanceof VarArgs varArgs){
                    // fold rest arguments to array
                    ClassDef eleType = null;
                    if(p >= arguments.size()){
                        eleType = varArgs.getElementType();
                    } else {
                        for (var j = p; j < arguments.size(); j++) {
                            Expression arg = arguments.get(j);
                            ClassDef t = arg.inferType();
                            if (eleType == null) {
                                eleType = indicateGenericType(varArgs.getElementType(), t, resolveResult);
                            } else {
                                try {
                                    new CastStrategy(ownerFunction, arg.getSourceLocation(), false).castTo(arg, eleType);
                                } catch (TypeMismatchError e) {
                                    resolveResult.error = e;
                                    return resolveResult;
                                }
                            }
                            argTypesPreserveVarArgs.add(t);
                            argPos++;
                        }
                    }
                    if(eleType == null || eleType.getTypeCode() == TypeCode.VOID){
                        resolveResult.error = new ResolveError("common type of params array not found", sourceLocation);
                        return resolveResult;
                    }
                    parameterTypes[p] = argType = new VarArgs(functionDef.getRoot(), eleType);
                } else {
                    argType = arguments.get(p).inferType();
                    argPos++;
                    argTypesPreserveVarArgs.add(argType);
                }
                argTypes.add(argType);
            } catch (CompilationError e){
                resolveResult.error = e;
            }
            if(argType != null) {
                if(!(argType instanceof VarArgs)) {
                    parameterTypes[p] = indicateGenericType(parameterType, argType, resolveResult);
                }
                if (!resolveResult.providedArguments.isEmpty()) {
                    // only constructor of template or template function can indicate generic type
                    if (functionDef instanceof ConstructorDef && functionDef.getParentClass().isGenericTemplate()) {
                        //
                    } else if (functionDef.isGenericTemplate()) {
                        //
                    } else {
                        resolveResult.error = new TypeMismatchError("generic type not allowed here '%s'".formatted(parameters.get(p).getName()), sourceLocation);
                    }
                }
            }
            if(resolveResult.error != null) {
                resolveResult.paramIndex = p;
                return resolveResult;
            }
        }
        if(argPos < arguments.size()){
            resolveResult.error = new ResolveError("arguments count mismatch",this.sourceLocation);
            return resolveResult;
        }
        resolveResult.score = evaluateArgumentCompatibility(functionDef, parameterTypes, argTypesPreserveVarArgs);
        return resolveResult;
    }

    private ClassDef indicateGenericType(ClassDef parameterType, ClassDef argType, ResolveResult resolveResult) {
        if(parameterType == argType){
            return parameterType;
        }
        var root = method.getRoot();
        if(parameterType instanceof GenericTypeCodeAvatarClassDef a) {
            resolveResult.regTypeArg(a, argType);
            if (resolveResult.error != null) return argType;
        } else if(parameterType instanceof VarArgs varArgs){
            throw new RuntimeException("impossible");
        } else if(parameterType instanceof ArrayClassDef arrayClassDef) {
            if (argType instanceof ArrayClassDef arrayArg) {
                var el = indicateGenericType(arrayClassDef.getElementType(), arrayArg.getElementType(), resolveResult);
                if (el != arrayClassDef.getElementType()) {     // array is invariance, so the argType is what we need
                    return argType;
                }
            }
        } else if(parameterType.isGenericType()){
            GenericSource pSource = parameterType.getGenericSource();
            ClassDef template = pSource.originalTemplate();
            ClassRefLiteral[] pArr = pSource.instantiationArguments().takeFor(template);

            for (ClassDef ac : argType.getAllAncestors(true)) {
                if(template == ac.getTemplateClass()){
                    var aArr = ac.getGenericSource().instantiationArguments().takeFor(template);
                    if(pArr.length == aArr.length) {
                        for (int i = 0; i < pArr.length; i++) {
                            ClassRefLiteral p = pArr[i];
                            ClassRefLiteral a = aArr[i];
                            if(p.getClassDefValue() == root.getAnyClass()){
                               //
                            } else {
                                // indicate more deep, i.e. List<List<R>> to List<ArrayList<Dog>>
                                indicateGenericType(p.getClassDefValue(), a.getClassDefValue(), resolveResult);
                            }
                        }
                    }
                    return ac;
                }
            }
        } else {
            if(evaluateScoreOfOneArgument(parameterType, argType) == 0){
                resolveResult.error = new ResolveError("argument type mismatch, '%s' expected, '%s' found".formatted(parameterType.getFullname(), argType.getFullname()), sourceLocation);
            }
        }
        return parameterType;
    }

    class VarArgs extends ArrayClassDef{

        private final ArrayClassDef baseArrayClass;

        public VarArgs(ArrayClassDef baseArrayClass) throws CompilationError {
            super(baseArrayClass.getRoot(), baseArrayClass.getElementType());
            this.baseArrayClass = baseArrayClass;
        }

        public VarArgs(Root root, ClassDef elementType) throws CompilationError {
            super(root, elementType);
            baseArrayClass = null;
        }


        public ArrayClassDef getBaseArrayClass() {
            return baseArrayClass;
        }
    }

    private static double evaluateArgumentCompatibility(FunctionDef functionDef, ClassDef[] parameters, List<ClassDef> arguments) {
        if(parameters.length == 0 && arguments.isEmpty()) return 1;
        if(parameters.length != arguments.size() && !(parameters[parameters.length-1] instanceof VarArgs)) return 0;
        double r = 0;
        for (int i = 0; i < parameters.length; i++) {
            var p = parameters[i];
            if (p instanceof VarArgs ar) {    // must be the last parameter
                var eleType = ar.getElementType();
                double varArgsScore = 0;
                int count = 0;
                for(; i< arguments.size(); i++){
                    var v = arguments.get(i);
                    varArgsScore += evaluateScoreOfOneArgument(eleType,v);
                    count ++;
                }
                if(count == 0){
                    r += 1;
                } else {
                    r += (varArgsScore * .98) / count;        // let int... always littler than int, int, int
                }
                break;
            } else {
                var v = arguments.get(i);
                r += evaluateScoreOfOneArgument(p, v);
            }
        }
        return r / parameters.length;
    }

    private static double evaluateScoreOfOneArgument(ClassDef p, ClassDef v) {
        if (p == v) {
            return 1;
        } else if(p.isGenericType()){       // generic type already used to resolve parameter type from argument type
            return 0.75;
        } else if (p.isPrimitiveFamily()) {
            if(v.isPrimitiveFamily() && p.getTypeCode() == v.getTypeCode()){
                return 1;
            }

            if(p.isPrimitive() && v.isPrimitive()){
                if(p.getTypeCode().isHigherThan(v.getTypeCode())) return 0.9;
            }

            if(p.isPrimitive() &&  p.isThatOrSuperOfThat(v)){
                return 0.8;
            }
        } else {    // p is object
            if (v.isPrimitiveFamily()) {
                if(v.isPrimitive()) {
                    ClassDef boxType = ((PrimitiveClassDef) v).getBoxedType();
                    if (boxType != null) {
                        if (boxType == p) {
                            return 0.8;
                        } else if (boxType.isDeriveFrom(p)) {    // Object, Number, Primitive ...
                            var distance = boxType.distanceToSuperClass(p);
                            return Math.pow(0.7, distance);
                        } else if(p.isDeriveFrom(((PrimitiveClassDef) v).getBoxerInterface())){
                            var distance = p.distanceToSuperClass(((PrimitiveClassDef) v).getBoxerInterface());
                            return Math.pow(0.7, distance);
                        }
                    }
                } else {
                    // v as Primitive, can only box to Object/Primitive
                    if (p == p.getRoot().getObjectClass()) {
                        return 0.8;
                    } else if (p == p.getRoot().getPrimitiveType()) {
                        return 0.81;
                    } else if (p == p.getRoot().getAnyClass()) {
                        return 0.7;
                    } else {
                        // r += 0;
                    }
                }
            } else if (v.getTypeCode() == TypeCode.NULL) {
                return 0.9;
            } else if (v.getTypeCode() == TypeCode.OBJECT || v.getTypeCode() == TypeCode.UNION) {
                if(p == p.getRoot().getAnyClass()) {
                    return 0.70;
                } else if(p == p.getRoot().getObjectClass()){
                    return 0.75;
                } else if(v.isDeriveFrom(p)) {
                    int d = v.distanceToSuperClass(p);
                    if(d == -1) return 0.75;
                    return Math.pow(0.999, d);
                } else if(p.isThatOrSuperOfThat(v)){
                    return 0.8;
                }
            } else if(v.isGenericType() && v instanceof GenericTypeCodeAvatarClassDef a){
                return evaluateScoreOfOneArgument(p, a.getLBoundClass()) * .72;
            }
        }
        if(p.isThatOrSuperOfThat(v)) {
            int d = v.distanceToSuperClass(p);
            if(d == -1) return 0.75;
            return Math.pow(0.999, d);
        }
        return 0;
    }

    public class ResolveResult{
        public int paramIndex = -1;
        FunctionDef functionDef;
        public Map<GenericTypeCodeAvatarClassDef, ClassDef> providedArguments = new TreeMap<>();
        public CompilationError error;
        double score;

        public ResolveResult(FunctionDef functionDef) {
            this.functionDef = functionDef;
        }

        public void regTypeArg(GenericTypeCodeAvatarClassDef genericTypeCodeAvatarClassDef, ClassDef argType){
            if(!genericTypeCodeAvatarClassDef.isThatOrSuperOfThat(argType)){       //TODO the original is SharedGenericTypeParameter
                this.error = new ResolveError("'%s' is not compatible for '%s'".formatted(argType.getFullname(), genericTypeCodeAvatarClassDef.getFullname()), sourceLocation);
            }
            var existed = providedArguments.putIfAbsent(genericTypeCodeAvatarClassDef, argType);
            if(existed == null || existed == argType){
                return;
            } else if(existed.isThatOrSuperOfThat(argType)){
                return;
            } else if(argType.isThatOrSuperOfThat(existed)){
                providedArguments.put(genericTypeCodeAvatarClassDef, existed);        // rollback
            } else {
                this.error = new ResolveError("'%s' and '%s' is not compatible for '%s'".formatted(existed.getFullname(), argType.getFullname(), genericTypeCodeAvatarClassDef.getFullname()), sourceLocation);
            }
        }

        public ClassRefLiteral[] toTypeArgs(TypeParamsContext typeParamsContext){
            ClassRefLiteral[] result = new ClassRefLiteral[typeParamsContext.size()];
            for (int i = 0; i < typeParamsContext.size(); i++) {
                result[i] = providedArguments.get(typeParamsContext.get(i)).toClassRefLiteral();
            }
            return result;
        }

        public boolean allFound(TypeParamsContext typeParamsContext) {
            for (int i = 0; i < typeParamsContext.size(); i++) {
                if(!providedArguments.containsKey(typeParamsContext.get(i))) return false;
            }
            return true;
        }
    }

}
