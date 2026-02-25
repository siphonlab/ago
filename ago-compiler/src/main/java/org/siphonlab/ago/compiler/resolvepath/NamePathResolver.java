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
package org.siphonlab.ago.compiler.resolvepath;


import org.siphonlab.ago.compiler.SourceLocation;
import org.siphonlab.ago.compiler.Package;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.ClassIntervalClassDef;
import org.siphonlab.ago.compiler.generic.GenericInstantiationPlaceHolder;
import org.siphonlab.ago.compiler.generic.GenericTypeCode;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.*;

public class NamePathResolver {
    /*
        xxx                 ->  variable field method class in scope, outer and outer, only this can expand outer

        this                ->  equals class.this
        class.this          ->  instance of start class (if the start is function, it's function's class)
        fun.this            ->  instance of start function, (if the start is class, it's the outer function)

        this.xxx            ->  field, method, subclass in start class(if the start is function, it's the outer class of function), equals class.this.xxx, not include static
        fun.this.xxx        ->  field method subclass in start function, not include static
        class.this.xxx      ->  equals this.xxx
        XXX.this.xxx        ->  field, method, subclass in XXX, XXX must be outer/self of startClass, if it's field it got an offseted MatchedVariable

        super               ->  a constructor function, only allowed in constructor, equals class.super()
        class.super         ->  equals above
        class.super.xxx     ->  method in super class, method must limited in super, cannot polymorphism down
        super.xxx           ->  equals above
        fun.super.xxx       ->  field, method, subclass in super function, start calss must be function

        xxx.yyy             ->  depends on xxx,
                                    if variable is a Class, yyy maybe static field, static method, static subclass
                                    else yyy is field, method, subclass of XXX
     */

    public enum PronounType{
        FunThis,
        ClassThis,
        FunSuper,
        ClassSuper,
        TraitThis,
        TraitSuper,
        This,
        Super;
        public boolean isSuper(){
            return this == ClassSuper || this == FunSuper || this == Super || this == TraitSuper;
        }
    }

    public enum ResolveMode{
        ForTypeName("type name"),
        ForTypeExpr("type expression"),
        ForInvokable("invocable expression"),
        ForVariable("variable/field"),
        ForValue("value")        //
        ;
        private final String s;

        ResolveMode(String s){
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private final ResolveMode resolveMode;

    private final Unit unit;
    private final ClassDef scopeClass;      // it's head.inferType() if there is head, otherwise ownerFunction
    private final Expression head;
    private final ParserRuleContext namePath;
    private final List<Id> ids;
    private int pos;

    // expression is in body of ownerFunction
    private FunctionDef ownerFunction;


    private CompilationError error;

    static class Id{
        ParserRuleContext ast;
        SourceLocation sourceLocation;

        public Id(ParserRuleContext ast, SourceLocation sourceLocation) {
            this.ast = ast;
            this.sourceLocation = sourceLocation;
        }
        public String text(){
            return ast.getText();
        }

        @Override
        public String toString() {
            return text() + " at " + sourceLocation;
        }
    }

    static class Pronoun extends Id{
        PronounType pronounType;

        public Pronoun(AgoParser.PossibleNameContext ast, PronounType pronounType, SourceLocation sourceLocation) {
            super(ast, sourceLocation);
            this.pronounType = pronounType;
        }
    }

    static class ParameterizedClass extends Id{
        private final AgoParser.TypeArgumentsContext typeArguments;
        private final AgoParser.ClassCreatorArgumentsContext classCreatorArguments;
        private final AgoParser.TypeIdentifierContext typeIdentifier;

        public ParameterizedClass(AgoParser.ParameterizedTypeContext ast,SourceLocation sourceLocation) {
            super(ast, sourceLocation);
            this.typeIdentifier = ast.typeIdentifier();
            this.typeArguments = ast.typeArguments();
            this.classCreatorArguments = ast.classCreatorArguments();
        }

        @Override
        public String text() {
            return typeIdentifier.getText();
        }
    }

    static class PrimitiveType extends Id{

        private final PrimitiveClassDef primitiveClassDef;

        public PrimitiveType(ParserRuleContext ast, PrimitiveClassDef primitiveClassDef, SourceLocation sourceLocation) {
            super(ast, sourceLocation);
            this.primitiveClassDef = primitiveClassDef;
        }
    }

    public NamePathResolver(ResolveMode resolveMode, Unit unit, ClassDef scopeClass, AgoParser.FormalNamePathContext namePath){
        this(resolveMode, unit, null, scopeClass, null, namePath, parseIds(namePath, unit));
    }

    public NamePathResolver(ResolveMode resolveMode, Unit unit, FunctionDef scopeClass, AgoParser.FormalNamePathContext namePath){
        this(resolveMode, unit, scopeClass, scopeClass, null, namePath, parseIds(namePath, unit));
    }

    public NamePathResolver(ResolveMode resolveMode, Unit unit, ClassDef scopeClass, AgoParser.IdentifierAllowPostfixContext identifierAllowPostfixContext){
        this(resolveMode, unit, null, scopeClass, null, identifierAllowPostfixContext, Collections.singletonList(new Id(identifierAllowPostfixContext, unit.sourceLocation(identifierAllowPostfixContext))));
    }

    public NamePathResolver(ResolveMode resolveMode, Unit unit, FunctionDef ownerFunction, ClassDef scopeClass, AgoParser.FormalNamePathContext formalNamePath) {
        this(resolveMode, unit, ownerFunction, scopeClass, null, formalNamePath, parseIds(formalNamePath, unit));
    }

    @Override
    public String toString() {
        return "(Resolve %s in %s)".formatted(this.namePath.getText(), this.scopeClass.getFullname());
    }

    public NamePathResolver(ResolveMode resolveMode, Unit unit, FunctionDef ownerFunction,  Expression head, AgoParser.FormalNamePathContext namePath) throws CompilationError {
        this(resolveMode, unit, ownerFunction, head.inferType(), head, namePath, parseIds(namePath, unit));
    }

    public FunctionDef getOwnerFunction() {
        return ownerFunction;
    }

    private static List<Id> parseIds(AgoParser.FormalNamePathContext namePath, Unit unit) {
        var possibleNames = namePath.possibleName();
        List<Id> ids = new ArrayList<>(possibleNames.size());
        for (AgoParser.PossibleNameContext possibleName : possibleNames) {
            if(possibleName instanceof AgoParser.NameParameterizedClassTypeContext nameParameterizedClassType){
                AgoParser.ParameterizedTypeContext parameterizedType = nameParameterizedClassType.parameterizedType();
                if(parameterizedType.typeArguments() != null || parameterizedType.classCreatorArguments() != null){
                    ids.add(new ParameterizedClass(parameterizedType, unit.sourceLocation(nameParameterizedClassType)));
                } else {
                    AgoParser.IdentifierAllowPostfixContext ast = parameterizedType.typeIdentifier().identifierAllowPostfix();
                    ids.add(new Id(ast, unit.sourceLocation(ast)));
                }
            } else if(possibleName instanceof AgoParser.NameIdentifierContext nameIdentifier){
                ids.add(new Id(nameIdentifier.identifier(), unit.sourceLocation(nameIdentifier)));
            } else if(possibleName instanceof AgoParser.NamePronounContext namePronounContext){
                AgoParser.PronounContext pronoun = namePronounContext.pronoun();
                ids.add(new Pronoun(namePronounContext, pronounType(pronoun), unit.sourceLocation(pronoun)));
            } else if(possibleName instanceof AgoParser.NamePrimitiveContext namePrimitive){
                AgoParser.PrimitiveTypeContext primitiveType = namePrimitive.primitiveType();
                ids.add(new PrimitiveType(primitiveType, PrimitiveClassDef.fromPrimitiveTypeAst(primitiveType), unit.sourceLocation(primitiveType)));
            } else {
                throw new RuntimeException();
            }
        }
        return ids;
    }

    private NamePathResolver(ResolveMode resolveMode, Unit unit, FunctionDef ownerFunction, ClassDef scopeClass, Expression head, ParserRuleContext namePath, List<Id> ids){
        this.resolveMode = resolveMode;
        this.unit = unit;
        this.ownerFunction = ownerFunction;
        this.scopeClass = scopeClass;
        this.head = head;
        this.namePath = namePath;

        assert !(scopeClass instanceof ClassIntervalClassDef);

        pos = 0;
        this.ids = ids;
    }

    public Expression resolve() throws CompilationError {
        int pronounPos = findPronounPos();

        Expression start;
        if(pronounPos > 0){
            start = resolveTypeBeforePronoun(pronounPos, ((Pronoun) ids.get(pronounPos)));
            pos = pronounPos + 1;
        } else {
            start = head;
        }

        var r = forward(start, pos);
        if (r == null) {
            if(this.error != null){
                throw this.error;
            } else {
                throw unit.resolveError(namePath, "cannot resolve '%s' as %s".formatted(namePath.getText(), resolveMode));
            }
        }
        return r;
    }

    Map<PronounType, Expression> resolvedPronounResults = new HashMap<>();
    private Expression resolvePronoun(Pronoun id) throws CompilationError {
        PronounType pronounType = id.pronounType;
        var existed = resolvedPronounResults.get(pronounType);
        if(existed != null) return existed;

        int depth = 0;
        PronounResolveResult nearestClassOrTrait = null;   // scope class is within which type class
        PronounResolveResult nearestClass = null;
        PronounResolveResult nearestTrait = null;
        PronounResolveResult nearestFun = null;
        PronounResolveResult nearestTraitWithPermitClass = null;
        for(Namespace<?> namespace = scopeClass; namespace != null; namespace = namespace.getParent(), depth++){
            if(namespace instanceof ClassDef c) {
                if(nearestClassOrTrait == null && (c.isClass() || c.isTrait())) {
                    nearestClassOrTrait = new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope);
                }
                if(nearestClass == null && c.isClass()){
                    nearestClass = new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope);
                } else if(nearestFun == null && c.isFunction()){
                    nearestFun = new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope);
                } else if(c.isTrait()){
                    if(nearestTrait == null){
                        nearestTrait = new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope);
                    }
                    if(nearestTraitWithPermitClass == null && c.getFieldForPermitClass() != null){
                        nearestTraitWithPermitClass = new PronounResolveResult(scopeClass, c, depth, c.getPermitClass(), PronounResolveResultKind.PermitField);
                    }
                    break;      // prevent access anything outside trait
                }
            }
        }

        switch (pronounType){
            case This:
                if(nearestClassOrTrait == null)
                    throw unit.resolveError(id.ast, "'this','class.this' and 'trait.this' are not allowed in top-level function");

                return nearestClassOrTrait.toExpr(id, this);
            case ClassThis:
                if(nearestClass == null && nearestTraitWithPermitClass == null)
                    throw unit.resolveError(id.ast, "no class found within scope");

                if (nearestClass != null && (nearestTraitWithPermitClass == null || nearestClass.depth < nearestTraitWithPermitClass.depth)) {
                    return nearestClass.toExpr(id, this);
                }
                return nearestTraitWithPermitClass.toExpr(id, this);
            case TraitThis:
                if(nearestTrait == null)
                    throw unit.resolveError(id.ast, "no trait found within scope");

                return nearestTrait.toExpr(id, this);

            case FunThis:
                if(nearestFun == null)
                    throw unit.resolveError(id.ast, "no function found within scope");
                return nearestFun.toExpr(id, this);

            case Super:
                if(nearestClassOrTrait == null)
                    throw unit.resolveError(id.ast, "no class or trait found within scope");
                return nearestClassOrTrait.toExpr(id, this);
            case ClassSuper:
                if(nearestClass == null)
                    throw unit.resolveError(id.ast, "no class found within scope");
                return nearestClass.toExpr(id, this);
            case TraitSuper:
                if(nearestTrait == null)
                    throw unit.resolveError(id.ast, "no trait found within scope");
                return nearestTrait.toExpr(id, this);
            case FunSuper:
                throw new UnsupportedOperationException("TODO");
        }
        return null;
    }

    private Expression resolveThisAfterClass(Pronoun idThis, ClassDef deferClass) throws CompilationError {
        int depth = 0;
        for(Namespace<?> namespace = scopeClass; namespace != null; namespace = namespace.getParent(), depth++){
            if(namespace instanceof ClassDef c) {
                if(c == deferClass){
                    return new Scope(depth, c).fromPronoun(PronounType.This).setSourceLocation(idThis.sourceLocation);
                } else if((c.isClass() || c.isFunction()) && deferClass.isTrait()){
                    var traitField = c.getFieldForTrait(deferClass);
                    if(traitField != null && this.ownerFunction != null){
                        return traitField(new Scope(depth, c).fromPronoun(PronounType.This).setSourceLocation(idThis.sourceLocation), deferClass)
                                .setSourceLocation(idThis.sourceLocation);
                    }
                } else if(c.isTrait() && deferClass.getPermitClass() == deferClass && this.ownerFunction != null){
                    return permitClassField(new Scope(depth, c).fromPronoun(PronounType.This).setSourceLocation(idThis.sourceLocation)).setSourceLocation(idThis.sourceLocation);
                }
            } else {
                break;
            }
        }
        throw unit.resolveError(idThis.ast, "cannot resolve '%s' within current scope".formatted(deferClass.getFullname()));
    }

    private Expression traitField(Scope scope, ClassDef trait) throws CompilationError {
        assert ownerFunction != null;
        var field = scope.getClassDef().getFieldForTrait(trait);
        Var.Field fld = new Var.Field(ownerFunction, scope, field)
                .setSourceLocation(scope.getClassDef().getUnit().sourceLocation(field.getDeclaration()));
        return fld;
    }

    private Expression permitClassField(Scope scope) throws CompilationError {
        var field = scope.getClassDef().getFieldForPermitClass();
        Var.Field fld = new Var.Field(ownerFunction, scope, field)
                .setSourceLocation(scope.getClassDef().getUnit().sourceLocation(field.getDeclaration()));
        return fld;
    }


    enum PronounResolveResultKind {Scope, PermitField, Trait}

    record PronounResolveResult(ClassDef scopeClass, ClassDef classDef, int depth, ClassDef fieldClass, PronounResolveResultKind pronounResolveResultKind){
        PronounResolveResult(ClassDef scopeClass, ClassDef classDef, int depth, PronounResolveResultKind pronounResolveResultKind){
            this(scopeClass, classDef, depth, null, pronounResolveResultKind);
        }
        Expression toExpr(Pronoun id, NamePathResolver resolver) throws CompilationError {
            PronounType pronounType = id.pronounType;
            ClassDef c;
            if(classDef.isGenericTemplate()){
                c = classDef.instantiate(classDef.getTypeParamsContext().createDefaultArguments(), null);
                scopeClass.registerConcreteType((ConcreteType) c);
            } else {
                c = classDef;
            }
            switch (pronounResolveResultKind()) {
                case Scope:
                    if(pronounType.isSuper()){
                        if(c.getSuperClass() == null){
                            throw resolver.unit.resolveError(id.ast, "no super class found for '%s'".formatted(c.getFullname()));
                        }

                        return new Scope(depth, c.getSuperClass()).fromPronoun(pronounType, c.getSuperClass()).setSourceLocation(id.sourceLocation);
                    } else {
                        return new Scope(depth, c).fromPronoun(pronounType).setSourceLocation(id.sourceLocation);
                    }
                case Trait:
                    return resolver.traitField(new Scope(depth, c).fromPronoun(pronounType).setSourceLocation(id.sourceLocation), fieldClass)
                            .setSourceLocation(id.sourceLocation);
                case PermitField:
                    return resolver.permitClassField(new Scope(depth, c).fromPronoun(pronounType).setSourceLocation(id.sourceLocation)).setSourceLocation(id.sourceLocation);
                default:
                    throw new RuntimeException("unexpected case");
            }
        }
    }

    private Expression resolveTypeBeforePronoun(int pronounPos, Pronoun id) throws CompilationError {
        PronounType pronounType = id.pronounType;
//        if(pronounType != PronounType.This)
//            throw unit.resolveError(id.ast,  "'%s' not allowed here".formatted(id.text()));

        List<PronounResolveResult> matched = new ArrayList<>();
        var last = ids.get(pronounPos - 1);
        int depth = 0;
        for(Namespace s = scopeClass; s != null; s = s.getParent(), depth++){
            if(s instanceof ClassDef c){
                if(c.isClass()){
                    if(pronounType == PronounType.This || pronounType == PronounType.ClassThis || pronounType == PronounType.Super || pronounType == PronounType.ClassSuper) {
                        if (isScopeClassMatch(c, last)) {
                            matched.add(new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope));
                            continue;
                        }
                    }
                } else if(c.isFunction()){
                    if(pronounType == PronounType.This || pronounType == PronounType.FunThis || pronounType == PronounType.FunSuper) {
                        if (isScopeClassMatch(c, last)) {
                            matched.add(new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope));
                            continue;
                        }
                    }
                } else if(c.isTrait()){
                    if(pronounType == PronounType.This || pronounType == PronounType.TraitThis || pronounType == PronounType.TraitSuper) {
                        if (isScopeClassMatch(c, last)) {
                            matched.add(new PronounResolveResult(scopeClass, c, depth, PronounResolveResultKind.Scope));
                            continue;
                        }
                    }
                }
                if((c.isClass() || c.isFunction()) && !c.getInterfaces().isEmpty()){
                    if(pronounType == PronounType.This || pronounType == PronounType.TraitThis) {
                        var tr = c.getInterfaces().stream().filter(itf -> itf.isTrait() && itf.getName().equals(last.text())).findFirst();
                        if(tr.isPresent()){
                            matched.add(new PronounResolveResult(scopeClass, c, depth, tr.get(), PronounResolveResultKind.Trait));
                            continue;
                        }
                    }
                }
                if(c.isTrait() && c.getPermitClass() != null){
                    if(pronounType == PronounType.This || pronounType == PronounType.ClassThis){
                        if(c.getPermitClass().getName().equals(last.text())) {
                            matched.add(new PronounResolveResult(scopeClass, c, depth, c.getPermitClass(), PronounResolveResultKind.PermitField));
                            continue;
                        }
                    }
                }
            }
        }

        if(pronounPos > 1) {
            List<Namespace<?>> parents = new ArrayList<>(matched.stream().map(PronounResolveResult::classDef).toList());

            for (int p = pronounPos - 1 - 1; p >= 0; p--) {
                var parentId = ids.get(p);
                for (int i = 0; i < parents.size(); i++) {
                    if(parents.get(i) == null || parents.get(i) instanceof Package) continue;

                    var parent = parents.get(i).getParent();

                    if(parent instanceof Package pkg){
                        List<Id> followIds = ids.subList(0, p + 1);
                        Optional<Id> firstParameterizedClass = followIds.stream().filter(c -> c instanceof ParameterizedClass).findFirst();
                        if(firstParameterizedClass.isPresent()){
                            parents.set(i, null);
                        } else if(pkg.getName().equals(StringUtils.join(followIds.stream().map(Id::text).iterator(), '.'))){
                            parents.set(i, pkg);
                        } else {
                            parents.set(i, null);
                        }
                    } else if(isScopeClassMatch(parent, parentId)){
                        parents.set(i, parent);
                    } else {
                        parents.set(i, null);
                    }
                }
            }

            List<PronounResolveResult> r = new ArrayList<>();
            for (int i = 0; i < parents.size(); i++) {
                Namespace<?> namespace = parents.get(i);
                if(namespace != null && namespace.getParent() != null){     // must be top
                    r.add(matched.get(i));
                }
            }
            matched = r;
        } else {
            if(matched.size() > 1)      // if there is B.B.B and B.B and B, we only need the nearest class B.B.B
                matched = matched.subList(0, 1);
        }

        if(matched.isEmpty()){
            throw unit.resolveError(last.ast, "nothing named '%s' for '%s' found".formatted(last.text(), id.text()));
        } else if(matched.size() > 1){
            throw unit.resolveError(last.ast, "name '%s' duplicated in scope".formatted(last.text()));
        } else {
            return matched.getFirst().toExpr(id, this);
        }
    }

    private boolean isScopeClassMatch(Namespace<?> classDef, Id id) {
        if(classDef.getName().equals(id.text())){
            return true;
        }
        if(classDef instanceof FunctionDef functionDef && functionDef.getCommonName().equals(id.text())){
            return true;
        }
        if(id instanceof ParameterizedClass){
            return false;       // cannot parameterized class like VarChar::(200) in Scope
        }
        return false;
    }

    Id currId(int pos) {
        return ids.get(pos);
    }

    Expression forward(Expression curr, int pos) {
        if(pos >= this.ids.size()) {
            return curr;
        }

        var id = currId(pos);
        try {
            return switch (curr) {
                case null -> forwardStart(id, pos);
                case ConstClass constClass -> forward(constClass, id, pos);
                case Var.LocalVar localVar -> forward(localVar, id, pos);
                case Scope scope -> forward(scope, id, pos);
                case Var.Field field -> forward(field, id, pos);
                case ClassOf.ClassOfInstance classOfInstance -> forward(classOfInstance, id, pos);
                case ClassOf.ClassOfScope classOfScope -> forward(classOfScope, id, pos);
                case ClassUnder.ClassUnderInstance classUnderInstance -> forward(classUnderInstance, id, pos);
                case ClassUnder.ClassUnderScope classOfScope -> forward(classOfScope, id, pos);
                default -> forward(curr, id, pos);
            };
        } catch (CompilationError error){
            this.error = error;
            return null;
        }
    }

    private Expression forward(Expression scope, Id id, int pos) throws CompilationError{
        if(id instanceof Pronoun pronoun){
            throw unit.syntaxError(id.ast, "'%s' not allowed here".formatted(id.text()));
        }
        if(id instanceof PrimitiveType primitiveType){
            throw unit.syntaxError(id.ast, "'%s' not allowed here".formatted(id.text()));
        }

        if(resolveMode == ResolveMode.ForTypeName){
            throw unit.syntaxError(id.ast, "cannot resolve type name from scope");
        }
        var r = resolveVariableOrClass(scope, id, pos, true);
        if(r != null){
            return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
        }
        return null;
    }

    Expression resolveVariableOrClassInScopeClass(Id id, int pos) throws CompilationError {
        var atEnd = (pos == this.ids.size()  - 1);
        switch (resolveMode){
            case ForTypeName: {
                var c = resolveClassInScopeClass(id, true, true);
                if (c != null) return c;
                break;
            }
            case ForInvokable:
            case ForTypeExpr: {
                if(atEnd || id instanceof ParameterizedClass){      // GClass<Dog>.create make it not the end
                    var c = resolveClassInScopeClass(id, true, true);
                    if (c != null) return c;
                    if(id instanceof ParameterizedClass){
                        resolveClassInScopeClass(id, true, true);
                    }
                    var v = resolveVariableInScopeClass(id);
                    if (v != null) {
                        if (isClassInterval(v) || isFunction(v)) return v;    // TODO or type variable
                        if (resolveMode == ResolveMode.ForInvokable && isFunctor(v)) return v;
                    }

                } else {
                    var v = resolveVariableInScopeClass(id);
                    if (v != null) return v;
                    var c = resolveClassInScopeClass(id, true, true);
                    if (c != null) return c;
                }
                break;
            }
            case ForVariable: {
                var v = resolveVariableInScopeClass(id);
                if (v != null) return v;    // TODO or type variable
                if (!atEnd) {
                    var c = resolveClassInScopeClass(id, true, true);
                    if (c != null) return c;
                }
                break;
            }
            case ForValue: {
                var v = resolveVariableInScopeClass(id);
                if (v != null) return v;    // TODO or type variable
                var c = resolveClassInScopeClass(id, true, true);
                if (c != null) return c;
            }
        }

        return null;
    }

    Expression resolveVariableOrClass(Expression curr, Id id, int pos, boolean allowMetaScan) throws CompilationError {
        var atEnd = (pos == this.ids.size()  - 1);
        switch (resolveMode){
            case ForTypeName: {
                var c = resolveSubClass(curr, id, allowMetaScan);
                if (c != null) return c;
                break;
            }
            case ForInvokable:
            case ForTypeExpr: {
                if (atEnd) {
                    var c = resolveSubClass(curr, id, allowMetaScan);
                    if (c != null) return c;
                    var v = resolveVariable(curr, id, allowMetaScan);
                    if (v != null) {
                        if (isClassInterval(v) || isFunction(v)) return v;    // TODO or type variable
                        if (resolveMode == ResolveMode.ForInvokable && isFunctor(v)) return v;
                    }
                } else {
                    var v = resolveVariable(curr, id, allowMetaScan);
                    if (v != null) return v;

                    var c = resolveSubClass(curr, id, allowMetaScan);
                    if (c != null) return c;
                }
                break;
            }
            case ForVariable: {
                var v = resolveVariable(curr, id, allowMetaScan);
                if (v != null) return v;    // TODO or type variable
                if (!atEnd) {
                    var c = resolveSubClass(curr, id, allowMetaScan);
                    if (c != null) return c;
                }
                break;
            }
            case ForValue: {
                var v = resolveVariable(curr, id, allowMetaScan);
                if (v != null) return v;    // TODO or type variable
                var c = resolveSubClass(curr, id, allowMetaScan);
                if (c != null) return c;
            }
        }

        return null;
    }

    private boolean isClassInterval(Expression v) throws CompilationError {
        return v.inferType().isThatOrDerivedFromThat(this.scopeClass.getRoot().getClassInterval());
    }

    private boolean isFunction(Expression v) throws CompilationError {
        return v.inferType() instanceof FunctionDef;
    }
    private boolean isFunctor(Expression v) throws CompilationError {
        ClassDef classDef = v.inferType();
        return classDef.isThatOrDerivedFromThat(scopeClass.getRoot().getFunctionBaseOfAnyClass());
    }

    // resolve from empty start
    Expression forwardStart(Id id, int pos) throws CompilationError {
        if(id instanceof Pronoun pronoun) {
            return forward(resolvePronoun(pronoun), pos + 1);
        }

        if(id instanceof PrimitiveType primitiveType){
            return new ConstClass(primitiveType.primitiveClassDef);
        }

        var r = resolveVariableOrClassInScopeClass(id, pos);
        if(r != null){
            var r2 = forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            if(r2 != null) return r2;
        }

        if(head == null) {
            var cls = tryFullname();
            if (cls != null) {
                var distance = scopeClass.distanceToOuterClass(cls);
                SourceLocation sourceLocation = unit.sourceLocation(namePath);
                if (distance != -1) {
                    return forward(new ClassOf.ClassOfScope(new Scope(distance, cls), 1).setSourceLocation(sourceLocation), this.pos);
                } else {
                    return forward(new ConstClass(cls).setSourceLocation(sourceLocation), this.pos);
                }
            }
        }
        return null;
    }

    Expression forward(ConstClass curr, Id id, int pos) throws CompilationError {
        if(id instanceof Pronoun pronoun){
            switch (pronoun.pronounType){
                case This:
                    throw unit.resolveError(id.ast,  "'%s' is outside of '%s'".formatted(curr.getClassDef().getFullname(), scopeClass.getFullname()));
                case ClassThis:
                case TraitThis:
                case FunThis:
                case Super:
                case ClassSuper:
                case FunSuper:
                case TraitSuper:
                    throw unit.resolveError(id.ast,  "'%s' not allowed here".formatted(id.text()));
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            var r = resolveVariableOrClass(curr, id, pos, true);
            if(r != null){
                return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            }
            return r;
        }
    }

    Expression forward(ClassOf.ClassOfScope curr, Id id, int pos) throws CompilationError {
        if(id instanceof Pronoun pronoun){
            switch (pronoun.pronounType){
                case This:
                    return forward(resolveThisAfterClass(pronoun, curr.getClassDef()), pos + 1);
                case ClassThis:
                case TraitThis:
                case FunThis:
                case Super:
                case ClassSuper:
                case FunSuper:
                case TraitSuper:
                    throw unit.resolveError(id.ast,  "'%s' not allowed here".formatted(id.text()));
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            var r = resolveVariableOrClass(curr, id, pos, true);
            if(r != null){
                return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            }
            return r;
        }
    }

    Expression forward(ClassOf.ClassOfInstance curr, Id id, int pos) throws CompilationError {
        if(id instanceof Pronoun pronoun){
            throw unit.resolveError(id.ast,  "'this' or 'super' not allowed here");
        } else {
            var r = resolveVariableOrClass(curr, id, pos, true);
            if(r != null){
                return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            }
            return r;
        }
    }

    Expression forward(ClassUnder.ClassUnderInstance curr, Id id, int pos) throws CompilationError {
        if(id instanceof Pronoun pronoun){
            throw unit.resolveError(id.ast,  "'this' or 'super' not allowed here");
        } else {
            var r = resolveVariableOrClass(curr, id, pos, true);
            if(r != null){
                return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            }
            return r;
        }
    }

    Expression forward(ClassUnder.ClassUnderScope curr, Id id, int pos) throws CompilationError {
        assert scopeClass.distanceToOuterClass( curr.getClassDef()) == -1;      // assert not in scope path
        if(id instanceof Pronoun pronoun){
            throw unit.resolveError(id.ast,  "'this' or 'super' not allowed here");
        } else {
            var r = resolveVariableOrClass(curr, id, pos, true);
            if(r != null){
                return forward(r.setSourceLocation(id.sourceLocation), pos + 1);
            }
            return r;
        }
    }

    private Expression resolveClassInScopeClass(Id id, boolean allowScopeScan, boolean allowMetaScan) throws CompilationError {
        if(allowMetaScan && scopeClass.getMetaClassDef() != null){
            var r = resolveSubClassInMetaClass(new ConstClass(scopeClass), id);
            if (r != null) return findNearestParameterizedInterface(r);
        }
        if(allowScopeScan){
            var genericType = scopeClass.findGenericType(id.text());
            if(genericType != null){
                return new ConstClass(genericType.getGenericCodeAvatarClassDef());
            }
            if(! scopeClass.isInterfaceOrTrait()){
                var r = resolveSubClassOfScope(new Scope.Local(scopeClass), id, allowMetaScan);
                if(r!=null) return findNearestParameterizedInterface(r);
            } else {
                try {
                    var r = resolveClassInPackage(scopeClass.getParent(), id);
                    if(r!=null) return findNearestParameterizedInterface(r);
                } catch (CompilationError e){
                    this.error = e;
                    return null;
                }
            }
        }
        return null;
    }

    private Expression findNearestParameterizedInterface(Expression r) throws ResolveError {
        if(r instanceof ConstClass constClass){
            ClassDef classDef = constClass.getClassDef();
            if(classDef.isInterfaceOrTrait()) {
                var interfaceDef = classDef;
                for (var cls = scopeClass; cls != null; cls = cls.getParentClass()) {
                    if (cls.getInterfaces() != null) {
                        var existed = cls.getInterfaces().stream().filter(i -> i instanceof ParameterizedClassDef p && p.getBaseClass() == interfaceDef).toList();
                        if (existed.size() == 1) {
                            ClassDef found = existed.get(0);
                            return new ConstClass(found).setSourceLocation(cls.getUnit().sourceLocation(cls.getInterfaceDecls().get(cls.getInterfaces().indexOf(found))));
                        } else if (existed.size() > 1) {
                            throw cls.getUnit().resolveError(cls.getDeclarationName(), "implemented interface '%s' multi times".formatted(interfaceDef.getFullname()));
                        }
                    }
                }
            }
        }
        return r;
    }

    private Expression resolveVariableInScopeClass(Id id) throws CompilationError {
        if(id instanceof PrimitiveType || id instanceof ParameterizedClass){
            this.error = new TypeMismatchError("variable expected", id.sourceLocation);
            return null;
        }

        var attribute = scopeClass.getAttribute(id.text());
        if(attribute != null && ownerFunction != null){
            return new Attribute(ownerFunction, new Scope.Local(scopeClass), attribute.getGetter(), attribute.getSetter()).setSourceLocation(id.sourceLocation);
        }

        var c = scopeClass.getVariable(id.text());
        if(c != null)
            return new Var.LocalVar(ownerFunction, c, Var.LocalVar.VarMode.Existed).setSourceLocation(id.sourceLocation);

        if(scopeClass.getMetaClassDef() != null){
            var r = resolveVariable(new ConstClass(scopeClass), id, true);
            if(r != null) return r;
        }
        if(CollectionUtils.isNotEmpty(scopeClass.getInterfaces())){
            for (ClassDef anInterface : scopeClass.getInterfaces()) {
                if(anInterface.getMetaClassDef() != null){
                    var r = resolveVariable(new ConstClass(anInterface), id, true);
                    if(r != null) return r;
                }
            }
        }
        if (!scopeClass.isTop() && !scopeClass.isInterfaceOrTrait()) {
            var r = resolveVariable(new Scope(1, scopeClass.getParentClass()), id, true);
            if (r != null) return r;
        }
        return null;
    }

    private ClassDef parameterizedClass(ClassDef classDef, Id id) throws CompilationError {
        if(id instanceof ParameterizedClass parameterizedClass){
            ClassDef r = classDef;
            if(parameterizedClass.typeArguments != null){
                r = genericTypedClass(classDef, parameterizedClass);     //G<Animal>::(1,2)
                if(r instanceof GenericInstantiationPlaceHolder && this.ids.getLast() != id){
                    throw new SyntaxError("empty type arguments template initialization must put at the last",id.sourceLocation);
                }
            }
            if(parameterizedClass.classCreatorArguments != null) {
                var placeHolder = new ParameterizedClassDef.PlaceHolder(r, parameterizedClass.classCreatorArguments, id.sourceLocation, scopeClass);
                if(classDef.getCompilingStage().getValue() < CompilingStage.AllocateSlots.getValue()){
                    classDef.getRoot().addParameterizedClassDefPlaceHolder(placeHolder);
                    return placeHolder;
                } else {
                    return placeHolder.resolve();
                }
            }
            return r;
        }
        return classDef;
    }

    private ClassDef genericTypedClass(ClassDef templateClass, ParameterizedClass parameterizedClass) throws CompilationError {
        var typeArguments = parameterizedClass.typeArguments;
        if(!templateClass.isGenericTemplate() && templateClass.getGenericSource() == null){
            throw unit.typeError(typeArguments, "'%s' is not a generic template class".formatted(templateClass));
        }
        if(typeArguments instanceof AgoParser.EmptyTypeArgsContext emptyTypeArgs){
            //indicate by constructor parameters or assignee type
            typeArguments = resolveTypeArgsListFromAssigneeAST();
        }
        if(typeArguments == null){  // resolve failed, indicate by constructor parameters
            return new GenericInstantiationPlaceHolder(templateClass, parameterizedClass.sourceLocation, scopeClass);
        } else if(typeArguments instanceof AgoParser.TypeArgsListContext typeArgs){
            var args = new ClassRefLiteral[typeArgs.typeArgument().size()];
            List<AgoParser.TypeArgumentContext> argument = typeArgs.typeArgument();
            for (int i = 0; i < argument.size(); i++) {
                AgoParser.TypeArgumentContext typeArgument = argument.get(i);
                var cls = unit.parseTypeName(scopeClass, typeArgument.declarationType().namePath(), false);
                args[i] = new ClassRefLiteral(cls);
            }
            //TODO validation arguments
//            if(templateClass.getGenericTypeParams().size() != args.length){
//                //throw new CompilationError("")
//            }
            var pc = ((ClassContainer) templateClass.getParent()).getOrCreateGenericInstantiationClassDef(templateClass, args, null);
            scopeClass.registerConcreteType(pc);
            scopeClass.idOfClass(templateClass);
            Compiler.processClassTillStage((ClassDef) pc,scopeClass.getCompilingStage());

            return (ClassDef) pc;
        } else {
            throw new RuntimeException("impossible");
        }
    }

    // resolve `Foo` of `G<Foo>` from `var v as G<Foo> = new F<>()`
    private AgoParser.TypeArgsListContext resolveTypeArgsListFromAssigneeAST() {
        if (namePath.getParent() instanceof AgoParser.DeclarationTypeContext declarationTypeContext
                && declarationTypeContext.getParent() instanceof AgoParser.NormalCreatorContext normalCreatorContext
                && normalCreatorContext.getParent() instanceof AgoParser.CreatorExprContext creator
                && creator.getParent() instanceof AgoParser.VariableInitializerContext variableInitializerContext
                && variableInitializerContext.getParent() instanceof AgoParser.LocalVariableDeclarationContext localVariableDeclarationContext) {

            AgoParser.TypeOfVariableContext typeOfVariableContext = localVariableDeclarationContext.typeOfVariable();
            if (typeOfVariableContext instanceof AgoParser.AsTypeContext asTypeContext
                    && asTypeContext.variableType() instanceof AgoParser.VarTypeNormalContext varTypeNormalContext) {

                var declNamePath = varTypeNormalContext.declarationType().namePath();
                if (declNamePath instanceof AgoParser.FormalNamePathContext declFormalNamePathContext) {
                    var possibleNames = declFormalNamePathContext.possibleName();
                    if (possibleNames.size() == 1) {
                        var possibleName = possibleNames.get(0);
                        if (possibleName instanceof AgoParser.NameParameterizedClassTypeContext nameParameterizedClassTypeContext) {
                            var args = nameParameterizedClassTypeContext.parameterizedType().typeArguments();
                            if (args instanceof AgoParser.TypeArgsListContext declArgs) {
                                return declArgs;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Expression resolveSubClassOfClassOfScope(ClassOf.ClassOfScope classOfScope, Id id, boolean allowMetaScan) throws CompilationError {
        var innerClass = classOfScope.getClassDef().getChild(id.text());
        if (innerClass != null) {
            var parameterizedClass = parameterizedClass(innerClass, id);
            if(parameterizedClass != innerClass){
                return new ClassUnder.ClassUnderScope(ownerFunction, classOfScope.getScope(), parameterizedClass).setSourceLocation(id.sourceLocation);
            }
            if(classOfScope.getMetaLevel() == 1){
                int distance = scopeClass.distanceToOuterClass(innerClass);
                if (distance != -1) {
                    var r = ClassOf.create(new Scope(distance, innerClass)).setSourceLocation(id.sourceLocation);     // 对于 ParameterizedClass， 需要考虑是否为同一个类, 判断能否重入 Scope
                    return resolveCandidateFunctions(r, classOfScope.getClassDef(), id);
                } else {
                    var r = new ClassUnder.ClassUnderScope(ownerFunction, classOfScope.getScope(), innerClass).setSourceLocation(id.sourceLocation);
                    return resolveCandidateFunctions(r, classOfScope.getClassDef(), id);
                }
            } else {
                var r = ClassUnder.create(ownerFunction, classOfScope, innerClass).setSourceLocation(id.sourceLocation);
                return resolveCandidateFunctions(r, classOfScope.getClassDef(), id);
            }
        }
        return null;
    }

    private Expression resolveSubClassOfScope(Scope scope, Id id, boolean allowMetaScan){
        try {
            var exprType = scope.getClassDef();
            // try step in
            var innerClass = exprType.getChild(id.text());
            if (innerClass != null) {
                var parameterizedClass = parameterizedClass(innerClass, id);
                if(parameterizedClass != innerClass){
                    return new ClassUnder.ClassUnderScope(ownerFunction, scope, parameterizedClass).setSourceLocation(id.sourceLocation);
                }
                int distance = scopeClass.distanceToOuterClass(innerClass);
                if (distance != -1) {
                    var r = ClassOf.create(new Scope(distance, innerClass)).setSourceLocation(id.sourceLocation);     // 对于 ParameterizedClass， 需要考虑是否为同一个类, 判断能否重入 Scope
                    return resolveCandidateFunctions(r, exprType, id);
                } else {
                    var r = new ClassUnder.ClassUnderScope(ownerFunction, scope, innerClass).setSourceLocation(id.sourceLocation);
                    return resolveCandidateFunctions(r, exprType, id);
                }
            }
            // try scope class itself
            if(!scope.isPronoun() && isScopeClassMatch(exprType, id)){
                var r = ClassOf.create(scope);
                return resolveCandidateFunctions(r, scope.getClassDef().getParent(), id);
            }
            // step in metaclass of scope class
            if(allowMetaScan) {
                Expression r = resolveSubClassInMetaClass(scope, id, false, exprType);
                if (r != null) return r;
            }
            // try above scope
            if (scope.getParentScope() == null) {
                return resolveClassInPackage(scope.getClassDef().getParent(), id);
            } else {
                return resolveSubClassOfScope(scope.getParentScope(), id, allowMetaScan);
            }
        } catch (CompilationError e){
            this.error = e;
            return null;
        }
    }

    private Expression resolveSubClassInMetaClass(Expression expression, Id id, boolean isClassInterval, ClassDef exprType) throws CompilationError {
        if(exprType.getMetaClassDef() != null){
            Expression classOf = !isClassInterval ? ClassOf.create(expression) : new ClassOf.ClassOfScopedClassInterval(expression, exprType.getMetaClassDef()).transform();
            var r = resolveSubClass(classOf, id, true);
            if(r != null)
                return resolveCandidateFunctions(r, id);
        }
        return null;
    }

    private Iterator<Pair<ClassDef, ClassDef>> metaClassesOf(ClassDef classDef){
        return new Iterator<>() {
            int currClassDepth = 0;      //
            int interfaceIndex = CollectionUtils.isNotEmpty(classDef.getInterfaces()) ? 0 : -1;    //
            ClassDef currClass = classDef;
            ClassDef currMetaClass;
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Pair<ClassDef, ClassDef> next() {
                if(currClassDepth == 2){
                    if(interfaceIndex != -1) {
                        currClass = classDef.getInterfaces().get(interfaceIndex++);
                        currClassDepth = 0;
                    } else {
                        return null;
                    }
                }
                if (currClassDepth == 0) {
                    currMetaClass = currClass.getMetaClassDef();
                } else if(currMetaClass != null) {
                    currMetaClass = currMetaClass.getMetaClassDef();
                }
                currClassDepth ++;
                if(currMetaClass == null) {
                    return next();
                }
                return Pair.of(currClass, currMetaClass);
            }
        };
    }

    private Expression resolveSubClassInMetaClass(ConstClass constClass, Id id) throws CompilationError {
        ClassDef classDef = constClass.getClassDef();

        var it = metaClassesOf(classDef);
        Pair<ClassDef, ClassDef> p;
        while((p = it.next()) != null) {
            var currClass = p.getLeft();
            var metaClassDef = p.getRight();
            ClassDef c = metaClassDef != null ? metaClassDef.getChild(id.text()) : null;
            if (c != null) {
                var parameterizedClass = parameterizedClass(c, id);
                if (parameterizedClass != c) {
                    return ClassUnder.create(ownerFunction, currClass == classDef ? constClass : new ConstClass(currClass).setSourceLocation(currClass.getUnit().sourceLocation(currClass.getDeclarationName())),
                            parameterizedClass).setSourceLocation(id.sourceLocation);
                }

                var r = ClassUnder.create(ownerFunction, constClass, c).setSourceLocation(id.sourceLocation);
                return resolveCandidateFunctions(r, metaClassDef, id);
            }
        }

        return null;
    }


    /**
     * resolve class under curr expr
     * @param expression
     * @param id
     * @param allowMetaScan
     * @return
     */
    private Expression resolveSubClass(Expression expression, Id id, boolean allowMetaScan) {
        Objects.requireNonNull(expression);
        try {
            switch (expression) {
                case ConstClass constClass: {       // Dog.bark() not allowed
                    var r = resolveSubClassInMetaClass(constClass, id);
                    if(r != null) return r;
                    break;
                }
                case ClassOf.ClassOfScope classOfScope: {
                    var r = resolveSubClassOfClassOfScope(classOfScope, id, allowMetaScan);
                    if(r != null) return r;
                    break;
                }
                case ClassUnder.ClassUnderInstance classUnderInstance:
                    if(resolveMode == ResolveMode.ForTypeName){
                        throw new ResolveError("cannot resolve in a class under instance", id.sourceLocation);
                    }
                    break;
                case Scope scope:
                    var r = resolveSubClassOfScope(scope, id, allowMetaScan);
                    return r != null ? findNearestParameterizedInterface(r) : null;
                default:
                    break;
            }

            ClassDef exprType = expression.inferType();
            boolean isClassInterval = false;
            if(exprType instanceof ClassIntervalClassDef classIntervalClassDef){
                exprType = classIntervalClassDef.getLBoundClass();
                isClassInterval = true;
            } else if(exprType instanceof GenericTypeCode.GenericCodeAvatarClassDef genericCodeAvatarClassDef){
                exprType = genericCodeAvatarClassDef.getLBoundClass();
                isClassInterval = true;
            }
            if(isClassInterval){
                if(exprType == null || exprType == scopeClass.getRoot().getAnyClass()){
                    return null;
                }
            }
            if(exprType == null || exprType instanceof PhantomMetaClassDef){    // step into child
                if (resolveMode == ResolveMode.ForTypeName){
                    if(expression instanceof ConstClass constClass){
                        var curr = constClass.getClassDef();
                        var c = curr.getChild(id.text());
                        if(c != null){
                            return resolveCandidateFunctions(new ConstClass(c).setSourceLocation(id.sourceLocation),curr, id);
                        }
                    }
                }
            } else {
                var c = exprType.getChild(id.text());
                if (c != null) {
                    var parameterizedClass = parameterizedClass(c, id);
                    if (parameterizedClass != c) {
                        return ClassUnder.create(ownerFunction, expression, parameterizedClass).setSourceLocation(id.sourceLocation);
                    }

                    var r = ClassUnder.create(ownerFunction, expression, c).setSourceLocation(id.sourceLocation);
                    return resolveCandidateFunctions(r, exprType, id);
                }
            }

            if(allowMetaScan) {
                var m = resolveSubClassInMetaClass(expression, id, isClassInterval, exprType);
                if (m != null) return m;
            }

        } catch (CompilationError error){
            this.error = error;
        }
        return null;
    }

    private Expression resolveClassInPackage(Namespace<?> pkg, Id id) throws CompilationError {
        String name = id.text();
        while(pkg != null){
            var n = pkg.getChild(name);
            if(n instanceof ClassDef c1){
                return resolveCandidateFunctions(new ConstClass(parameterizedClass(c1, id)), id);
            }
            pkg = pkg.getParent();
        }
        var c = unit.getImportedClasses().get(name);
        if(c != null) {
            return resolveCandidateFunctions(new ConstClass(parameterizedClass(c, id)), id);
        }
        var lang = unit.getRoot().getChild("lang");
        c = lang.getChild(name);
        if (c != null) {
            return resolveCandidateFunctions(new ConstClass(parameterizedClass(c, id)), id);
        }
        return null;
    }

    private Expression resolveCandidateFunctions(Expression maybeFun, Namespace<?> parent, Id id) throws CompilationError {
        if(parent == null) return maybeFun;
        if(id instanceof ParameterizedClass) return maybeFun;
        if(maybeFun instanceof MaybeFunction maybeFunction && maybeFunction.isFunction()){
            String text = id.text();
            if(!text.contains("#")){
                var set = parent.findMethods(text);
                if(set.size() > 1){
                    maybeFunction.setCandidates(set);
                }
            }
        }
        return maybeFun;
    }

    private Expression resolveCandidateFunctions(Expression expr, Id id) throws CompilationError {
        if(expr instanceof MaybeFunction maybeFunction && maybeFunction.isFunction()){
            String text = id.text();
            if(!text.contains("#")){
                var set = maybeFunction.getFunction().getParent().findMethods(text);
                if(set.size() > 1){
                    maybeFunction.setCandidates(set);
                }
            }
        }
        return expr;
    }

    private Expression resolveVariable(Expression curr, Id id, boolean allowMetaScan){
        try {
            Objects.requireNonNull(curr);

            ClassDef currType = curr.inferType();
            boolean isClassInterval = false;
            if(currType instanceof ClassIntervalClassDef classIntervalClassDef){
                currType = classIntervalClassDef.getLBoundClass();
                isClassInterval = true;
            } else if(currType instanceof GenericTypeCode.GenericCodeAvatarClassDef genericCodeAvatarClassDef){
                currType = genericCodeAvatarClassDef.getLBoundClass();
                isClassInterval = true;
            }
            if(isClassInterval){
                if(currType == null || currType == scopeClass.getRoot().getAnyClass()){
                    return null;
                }
            }
            if(currType == null) return null;

            var attribute = currType.getAttribute(id.text());
            if(attribute != null && ownerFunction != null){
                return new Attribute(ownerFunction, curr, attribute.getGetter(), attribute.getSetter()).setSourceLocation(id.sourceLocation);
            }

            Variable c = currType.getVariable(id.text());
            if (c != null) {
                var r = Var.of(ownerFunction, curr, c).setSourceLocation(id.sourceLocation);
                if(curr instanceof ConstClass cls && cls.getClassDef().isEnum()){
                    if(c.getType() == cls.getClassDef()){
                        return new EnumValue((Var.Field) r, (Field) c);
                    }
                } else if(c.getConstLiteralValue() != null){
                    return new ConstValue(c).setSourceLocation(r.getSourceLocation());
                }
                return r;
            }

            Expression r;
            if (allowMetaScan) {
                if(currType.getMetaClassDef() != null) {
                    if (!isClassInterval) {
                        r = resolveVariable(ClassOf.create(curr), id, true);
                    } else {
                        r = resolveVariable(new ClassOf.ClassOfScopedClassInterval(curr, currType.getMetaClassDef()).transform(), id, true);
                    }
                    if (r != null) return r;
                }
                if(CollectionUtils.isNotEmpty(currType.getInterfaces())){
                    List<ClassDef> interfaces = currType.getInterfaces();
                    for (int i = 0; i < interfaces.size(); i++) {
                        ClassDef currTypeInterface = interfaces.get(i);
                        AgoParser.InterfaceItemContext ast = currType.getInterfaceDecls() != null && currType.getInterfaceDecls().size() > i? currType.getInterfaceDecls().get(i) : null;
                        r = resolveVariable(new ConstClass(currTypeInterface).setSourceLocation(currType.getUnit()
                                .sourceLocation(ast)), id, true);
                        if(r != null) return r;
                    }
                }
            }

            if(curr instanceof Scope scope){
                var parentScope = scope.getParentScope();
                if(parentScope != null){
                    return resolveVariable(parentScope, id, allowMetaScan);
                }
            }
        } catch (CompilationError error){
            this.error = error;
        }
        return null;
    }

    private int findPronounPos() throws SyntaxError {
        int pronounPos = -1;
        for (int i = 0; i < ids.size(); i++) {
            var id = ids.get(i);
            if (id instanceof Pronoun p) {
                pronounPos = i;
                for (i++;i < ids.size(); i++) {
                    if(ids.get(i) instanceof Pronoun p2){
                        throw unit.syntaxError(p2.ast, "multiple times 'this' or 'super' found");
                    }
                }
            }
        }
        return pronounPos;
    }


    private ClassDef tryFullname() throws SyntaxError {
        Namespace<?> pkg = unit.getRoot();
        Optional<Id> firstParameterizedClass = ids.stream().filter(id -> id instanceof ParameterizedClass).findFirst();
        var ls = ids.stream().map(Id::text).toList();

        for (int i = ls.size() - 1; i > 0; i--){
            var s = String.join(".", ls.subList(0, i + 1));
            var n = pkg.findByFullname(s);
            if(n == null) {
                // continue
            } else if(n instanceof ClassDef c){
                pos = i + 1;
                if(firstParameterizedClass.isPresent()){
                    throw new SyntaxError("paramterized class not allowed here",firstParameterizedClass.get().sourceLocation);
                }
                return c;
            }
        }
        return null;
    }

    private static PronounType pronounType(AgoParser.PronounContext pronoun) {
        return switch (pronoun) {
            case AgoParser.ThisPrimaryContext thisPrimaryContext ->     PronounType.This;
            case AgoParser.SuperPrimaryContext superPrimaryContext ->   PronounType.Super;
            case AgoParser.FunThisPrimaryContext funThisPrimaryContext -> PronounType.FunThis;
            case AgoParser.ClassThisPrimaryContext classThisPrimaryContext ->   PronounType.ClassThis;
            case AgoParser.ClassSuperPrimaryContext classSuperPrimaryContext -> PronounType.ClassSuper;
            case AgoParser.FunSuperPrimaryContext funSuperPrimaryContext -> PronounType.FunSuper;
            case AgoParser.TraitSuperPrimaryContext traitSuperPrimaryContext -> PronounType.TraitSuper;
            case AgoParser.TraitThisPrimaryContext traitThisPrimaryContext -> PronounType.TraitThis;
            case null, default -> {
                throw new UnsupportedOperationException();
            }
        };
    }


}