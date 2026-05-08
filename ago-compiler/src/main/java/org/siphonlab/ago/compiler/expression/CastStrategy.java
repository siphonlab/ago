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
package org.siphonlab.ago.compiler.expression;



import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.*;

import org.siphonlab.ago.compiler.generic.ClassIntervalClassDef;
import org.siphonlab.ago.compiler.generic.GenericTypeCodeAvatarClassDef;

import java.math.BigDecimal;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;

public class CastStrategy {

    private final FunctionDef ownerFunction;
    private final SourceLocation sourceLocation;
    private final boolean forceCast;

    enum TypeKind{
        Primitive,                  // primitive and terminated primitive generic, i.e. `T as [int]`
        PrimitiveGeneric,           // T as [Primitive]
        PrimitiveInterface,         // i instanceof Primitive
        Enum,                       // enum, a special Primitive Boxer type, that can extract literal compile time
        PrimitiveBoxer,             // boxer type of primitive type
        Any,                        // any, generic, support primitive, union, and other possible class
        langObject,                 // lang.Object
        Object,                     // object type

        Null,                       // null, belongs to Primitive
        Union,                      // union, now only nullable
    }

    public CastStrategy(FunctionDef ownerFunction, SourceLocation sourceLocation, boolean forceCast){
        this.ownerFunction = ownerFunction;
        this.sourceLocation = sourceLocation;
        this.forceCast = forceCast;
    }

    private TypeKind typeKind(ClassDef classDef){
        Root root = classDef.getRoot();
        if(classDef instanceof PrimitiveClassDef) {
            if(classDef instanceof NullClassDef){
                return TypeKind.Null;
            } else {
                return TypeKind.Primitive;
            }
        } else if(classDef instanceof GenericTypeCodeAvatarClassDef a){
            if(a.isPrimitiveFamily()){      // <T as [Primitive]>
                return TypeKind.PrimitiveGeneric;
            }
            if(a.getLBoundClass() == root.getAnyClass()){
                return TypeKind.Any;
            }
            if(a.getLBoundClass() == a.getUBoundClass()){
                return typeKind(a.getLBoundClass());
            }
            return TypeKind.Object;     // rest jobs belong to check type match
        } else if(classDef instanceof ClassIntervalClassDef || classDef.isDeriveFrom(root.getClassRefClass())) {      // primitive boxer for classref
            return TypeKind.PrimitiveBoxer;
        } else if(classDef.isThatOrDerivedFromThat(root.getPrimitiveType())){      // Primitive or PrimitiveNumber
            return TypeKind.PrimitiveInterface;
        } else if(classDef.isEnum()){
            return TypeKind.Enum;
        } else if(classDef.isPrimitiveBoxed()){
            return TypeKind.PrimitiveBoxer;
        } else if(classDef == root.getAnyClass()){
            return TypeKind.Any;
        } else if(classDef == root.getObjectClass()) {
            return TypeKind.langObject;
        } else if(classDef.getTypeCode() == UNION){
            return TypeKind.Union;
        } else {
            return TypeKind.Object;
        }
    }

    private ClassDef resolveTerminatedGenericClass(GenericTypeCodeAvatarClassDef genericTypeCodeAvatarClassDef, TypeKind typeKind){
        if(typeKind == TypeKind.Object){
            return genericTypeCodeAvatarClassDef;
        } else {
            return genericTypeCodeAvatarClassDef.getLBoundClass();
        }
    }

    public record UnifyTypeResult(Expression left, Expression right, ClassDef resultType, boolean changed) {
        public UnifyTypeResult swap() {
            return new UnifyTypeResult(right, left, resultType, changed);
        }
    }

    // unify types
    public UnifyTypeResult unifyTypes(Expression left, Expression right) throws CompilationError {
        var originLeftType = left.inferType();
        var originRightType = right.inferType();

        TypeKind leftTypeKind = typeKind(originLeftType);
        TypeKind rightTypeKind = typeKind(originRightType);

        ClassDef leftType, rightType;
        if(originLeftType instanceof GenericTypeCodeAvatarClassDef a){
            leftType = resolveTerminatedGenericClass(a,leftTypeKind);
        } else {
            leftType = originLeftType;
        }
        if (originRightType instanceof GenericTypeCodeAvatarClassDef a) {
            rightType = resolveTerminatedGenericClass(a, rightTypeKind);
        } else {
            rightType = originRightType;
        }

        if(leftType == rightType){
            return new UnifyTypeResult(left, right, leftType,false);
        }

        if(rightTypeKind == TypeKind.PrimitiveInterface){
            throw new TypeMismatchError("cann't cast to Primitive interface", this.sourceLocation);
        }

        return switch (leftTypeKind){
            case Primitive ->
                switch (rightTypeKind){
                    case Primitive ->
                        unifyPrimitiveType(left, right, (PrimitiveClassDef) leftType, (PrimitiveClassDef) rightType);
                    case Enum, PrimitiveBoxer ->
                        unifyTypes(left, ownerFunction.unbox(right));
                    case langObject -> {
                        left = new Box(ownerFunction, left, ((PrimitiveClassDef)leftType).getBoxedType(), Box.BoxMode.Box);
                        left = new ForceCast(ownerFunction, left,rightType, ForceCast.CastMode.WearClassMask);
                        yield new UnifyTypeResult(left,right,rightType,true);
                    }
                    default -> throwTypeMismatchError(originLeftType, originRightType);
                };
            case PrimitiveGeneric ->
                switch (rightTypeKind){
                    case PrimitiveGeneric -> {
                        if (originLeftType == originRightType) {
                            yield new UnifyTypeResult(left, right, originLeftType, false);
                        } else {
                            yield throwTypeMismatchError(originLeftType,originRightType);
                        }
                    }
                    case langObject ->{
                        left = forceBox(left);
                        yield new UnifyTypeResult(left,right,rightType,true);
                    }
                    default ->
                        throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(originLeftType.getFullname(), originRightType.getFullname()), this.sourceLocation);
                };

            case PrimitiveBoxer, Enum ->
                switch (rightTypeKind){
                    case Primitive ->
                        unifyTypes(ownerFunction.unbox(left), right);

                    case Enum, PrimitiveBoxer ->
                        unifyTypes(ownerFunction.unbox(left), ownerFunction.unbox(right));

                    case langObject -> {
                        left = new ForceCast(ownerFunction, left, rightType, ForceCast.CastMode.ObjectCast);
                        yield new UnifyTypeResult(left,right,rightType, true);
                    }

                    case Object -> {
                        if(rightType.getTypeCode() == NULL) {
                            yield throwTypeMismatchError(originLeftType, originRightType);
                        } else {
                            left = new ForceCast(ownerFunction, left, rightType, ForceCast.CastMode.ObjectCast);
                            yield new UnifyTypeResult(left,right,rightType, true);
                        }
                    }

                    default -> throwTypeMismatchError(originLeftType, originRightType);
                };

            case langObject -> this.unifyTypes(right, left).swap();

            case Object ->
                switch (rightTypeKind){
                    case langObject ->
                        new UnifyTypeResult(new ForceCast(ownerFunction, left,rightType, ForceCast.CastMode.ObjectCast), right,rightType, true);
                    case Object, PrimitiveBoxer, Enum ->
                        unifyObjectTypes(left, right, leftType, rightType);
                    default -> throwTypeMismatchError(originLeftType, originRightType);
                };

            default -> throwTypeMismatchError(originLeftType,originRightType);
        };
    }

    private UnifyTypeResult unifyObjectTypes(Expression left, Expression right, ClassDef leftType, ClassDef rightType) throws CompilationError {
        if(leftType.isThatOrSuperOfThat(rightType)){
            var r = new ForceCast(ownerFunction, right, leftType, ForceCast.CastMode.WearClassMask);
            return new UnifyTypeResult(left, r, leftType,true);
        } else {
            throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(rightType.getFullname(), leftType.getFullname()), right.getSourceLocation());
        }
    }

    private UnifyTypeResult throwTypeMismatchError(ClassDef originLeftType, ClassDef originRightType) throws TypeMismatchError {
        throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(originLeftType.getFullname(), originRightType.getFullname()), this.sourceLocation);
    }

    private Expression throwTypeMismatchErrorExpr(ClassDef originLeftType, ClassDef originRightType) throws TypeMismatchError {
        throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(originLeftType.getFullname(), originRightType.getFullname()), this.sourceLocation);
    }

    private UnifyTypeResult unifyPrimitiveType(Expression l, Expression r, PrimitiveClassDef t1, PrimitiveClassDef t2) throws CompilationError {
        var resultType = t2.getTypeCode().isHigherThan(t1.getTypeCode()) ? unifyPrimitiveType(t1, t2) : unifyPrimitiveType(t2, t1);
        if(resultType == null){
            throw new TypeMismatchError("cannot find unify type between '%s' and '%s'".formatted(l.inferType().getFullname(), r.inferType().getFullname()), this.sourceLocation);
        }
        boolean changed = false;
        if (resultType != t1) {
            l = new Cast(ownerFunction, l, resultType);
            changed = true;
        }
        if (resultType != t2) {
            r = new Cast(ownerFunction, r, resultType);
            changed = true;
        }
        return new UnifyTypeResult(l, r, resultType, changed);
    }

    private Root getRoot(){
        return ownerFunction.getRoot();
    }

    protected PrimitiveClassDef unifyPrimitiveType(PrimitiveClassDef loType, PrimitiveClassDef hiType) {
        var t1 = loType.getTypeCode();
        var t2 = hiType.getTypeCode();

        if (t1 == STRING)
            return hiType;        // string -> int... number types
        if (t2 == STRING)
            return loType;

        if(t2 == BOOLEAN){
            return hiType;
        }

        switch (t1.value) {
            case BYTE_VALUE, SHORT_VALUE, CHAR_VALUE:
                if (t2 == CHAR || t2 == SHORT || t2 == INT || t2 == FLOAT || t2 == LONG || t2 == DOUBLE) {
                    return hiType;
                }
                break;
            case INT_VALUE:
                if (t2 == LONG || t2 == DOUBLE) {
                    return hiType;
                } else if (t2 == FLOAT) {
                    return getRoot().DOUBLE();
                }
                break;
            case FLOAT_VALUE:
                if (t2 == DOUBLE) {
                    return hiType;
                } else if (t2 == INT || t2 == LONG) {
                    return getRoot().DOUBLE();
                }
                break;
            case LONG_VALUE:
                if (t2 == FLOAT) {
                    return getRoot().DOUBLE();
                } else if (t2 == DOUBLE) {
                    return getRoot().DOUBLE();
                }
        }
        return null;
    }

    // solve a path(an expression) connect fromType -> toType
    public Expression castTo(Expression expression, ClassDef toType) throws CompilationError {
        ClassDef fromType = expression.inferType();
        TypeKind fromTypeKind = typeKind(fromType);
        TypeKind toTypeKind = typeKind(toType);

        if (fromTypeKind == TypeKind.PrimitiveInterface || toTypeKind == TypeKind.PrimitiveInterface) {
            typeKind(fromType);
            throw new TypeMismatchError("can't cast to Primitive interface or cast from it", this.sourceLocation);
        }

        ClassDef originToType = toType;
        if (toType instanceof GenericTypeCodeAvatarClassDef a) {
            toType = resolveTerminatedGenericClass(a, toTypeKind);
        }

        if(fromType == toType){
            if(toType == originToType){
                return expression;
            } else {
                return new ForceCast(ownerFunction, expression, originToType, ForceCast.CastMode.WearClassMask);
            }
        }

        if (expression instanceof Literal<?> literal) {
            if (literal instanceof NullLiteral n) {
                if (toTypeKind == TypeKind.langObject || toTypeKind == TypeKind.Object || toTypeKind == TypeKind.PrimitiveBoxer) {
                    throw new TypeMismatchError("cannot cast null to object", this.sourceLocation);
                }
            }
            if (toType instanceof PrimitiveClassDef primitiveClassDef) {
                return new ForceCast(ownerFunction, castLiteral(literal, primitiveClassDef, this.sourceLocation), originToType, ForceCast.CastMode.WearClassMask).transform();
            }
        }

        var r = switch (fromTypeKind){
            case Primitive -> {
                final PrimitiveClassDef primitiveFromType = (PrimitiveClassDef) fromType;
                yield switch (toTypeKind) {
                    case Primitive ->
                        castPrimitive(expression, primitiveFromType, (PrimitiveClassDef) toType);
                    case PrimitiveGeneric ->
                        forceCastPrimitive(expression, primitiveFromType, (GenericTypeCodeAvatarClassDef) originToType);
                    case PrimitiveBoxer ->
                        boxPrimitive(expression, primitiveFromType, toType);
                    case Enum ->
                        boxEnum(expression, primitiveFromType, toType);
                    case Any ->
                        castToAny(expression, fromType, originToType);
                    case langObject ->
                        boxPrimitive(expression, primitiveFromType, primitiveFromType.getBoxedType());
                    case Object, Null ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);
                    case Union -> {
                        if(fromType.getTypeCode() == NULL || fromType.getTypeCode() == VOID){
                            yield new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.ToUnion);
                        } else {
                            yield toNullable(castTo(expression, ((NullableClassDef) toType).getNullableBaseClass()), (NullableClassDef) toType);
                        }
                    }

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };
            }
            case PrimitiveGeneric ->
                switch (toTypeKind) {
                    case Primitive, PrimitiveGeneric ->
                        forceCastPrimitive(expression, fromType, originToType);

                    case PrimitiveBoxer -> {
                        var r1 = forceCastPrimitive(expression, fromType, toType.getUnboxedType());
                        yield new Box(ownerFunction, r1, toType, Box.BoxMode.Box).transform();
                    }

                    case Enum ->{
                        var r1 = forceCastPrimitive(expression, fromType, toType.getUnboxedType());
                        yield new Box(ownerFunction, r1, toType, Box.BoxMode.BoxEnum).transform();
                    }
                    case Any ->
                        castToAny(expression, fromType, originToType);
                    case langObject ->
                        forceBox(expression);

                    case Object, Null ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);

                    case Union -> toNullable(castTo(expression, ((NullableClassDef) toType).getNullableBaseClass()), (NullableClassDef) toType);

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };
            case PrimitiveBoxer, Enum ->
                switch (toTypeKind) {
                    case Primitive ->
                        unbox(expression, fromType, (PrimitiveClassDef) toType);
                    case PrimitiveGeneric ->
                        unboxAndForceCastPrimitive(expression, fromType, toType);       // classref?

                    case PrimitiveBoxer -> {
                        var unboxed = unbox(expression,fromType,fromType.getUnboxedType());
                        yield boxPrimitive(unboxed,fromType.getUnboxedType(),toType);
                    }
                    case langObject ->  expression;

                    case Enum -> {
                        var unboxed = unbox(expression, fromType, fromType.getUnboxedType());
                        yield boxEnum(unboxed, (PrimitiveClassDef)unboxed.inferType(), toType).transform();
                    }
                    case Any ->
                        castToAny(expression, fromType, originToType);
                    case Object ->
                        castObject(expression, fromType, toType);

                    case Null ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);

                    case Union ->
                        toNullable(castTo(expression, ((NullableClassDef)toType).getNullableBaseClass()), (NullableClassDef) toType);

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };

            case Any ->     castToAny(expression, fromType, originToType);

            case langObject ->
                switch (toTypeKind) {
                    case Primitive -> {
                        if(forceCast) {
                            yield unboxObject(expression, toType, true);
                        } else {
                            yield throwTypeMismatchErrorExpr(fromType, originToType);
                        }
                    }
                    case PrimitiveGeneric -> {
                        if(forceCast) {
                            yield forceUnbox(expression, toType);
                        } else {
                            yield throwTypeMismatchErrorExpr(fromType, originToType);
                        }
                    }

                    case PrimitiveBoxer, Enum, Object ->
                        castObject(expression, fromType, toType);

                    case langObject ->  expression;

                    case Any ->
                        castToAny(expression, fromType, originToType);

                    case Null ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);

                    case Union ->
                        toNullable(castTo(expression, ((NullableClassDef)toType).getNullableBaseClass()), (NullableClassDef) toType);

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };
            case Object ->
                switch (toTypeKind) {
                    case Primitive ->{
                        if(toType.getTypeCode() == CLASS_REF && fromType instanceof MetaClassDef metaClassDef){
                            Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(expression, this.sourceLocation);
                            if(pair == null){
                                throw new TypeMismatchError("'%s' is not a class expression", this.sourceLocation);
                            }
                            ClassDef value = pair.getValue();
                            yield value.toClassRefLiteral();
                        }
                        yield unboxObject(expression,toType, false);
                    }

                    case PrimitiveBoxer -> {
                        if (toType.getUnboxedType().getTypeCode() == CLASS_REF && fromType instanceof MetaClassDef) {
//                            Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(expression, this.sourceLocation);
//                            if (pair == null) {
//                                throw new TypeMismatchError("'%s' is not a class expression", this.sourceLocation);
//                            }
//                            ClassDef value = pair.getValue();
//                            ClassRefLiteral classRefLiteral = new ClassRefLiteral(value);
//                            classRefLiteral.setScope(pair.getLeft());
//                            yield new Box(classRefLiteral, toType, Box.BoxMode.Box);
                            yield Assign.processBoundClass(ownerFunction, toType,expression,this.sourceLocation);
                        }
                        throw new TypeMismatchError("can't convert '%s' to '%s'".formatted(fromType.getFullname(), toType.getFullname()), this.sourceLocation);
                    }

                    case PrimitiveGeneric, Enum ->
                        throw new TypeMismatchError("can't convert '%s' to '%s'".formatted(fromType.getFullname(), toType.getFullname()), this.sourceLocation);

                    case langObject ->  expression;

                    case Object->
                        castObject(expression, fromType, toType);

                    case Any ->
                        castToAny(expression, fromType, originToType);

                    case Null -> throw new TypeMismatchError("can't convert '%s' to '%s'".formatted(fromType.getFullname(), toType.getFullname()), this.sourceLocation);

                    case Union ->
                        toNullable(castTo(expression, ((NullableClassDef)toType).getNullableBaseClass()), (NullableClassDef) toType);

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };

            case Null -> {
                yield switch (toTypeKind){
                    case Primitive -> {     // there is no box type for null
                        if(toType.getTypeCode() == CLASS_REF){
                            yield castPrimitive(expression, (PrimitiveClassDef) fromType, (PrimitiveClassDef) originToType);
                        } else {
                            yield throwTypeMismatchErrorExpr(fromType, originToType);
                        }
                    }
                    case Union ->
                        toNullable(expression, (NullableClassDef) toType);
                    default -> throwTypeMismatchErrorExpr(fromType, originToType);
                };
            }

            case Union -> {
                var expr = fromNullable(expression);
                yield switch (toTypeKind){
                    case Primitive, PrimitiveBoxer,PrimitiveGeneric, PrimitiveInterface, Enum, Any, langObject, Object  -> {
                        if(forceCast) {
                            yield castTo(expr, toType);
                        } else {
                            yield throwTypeMismatchErrorExpr(fromType, originToType);
                        }
                    }
                    case Null -> throwTypeMismatchErrorExpr(fromType, originToType);
                    case Union -> {
                        var toBase = ((NullableClassDef)toType).getNullableBaseClass();
                        if(toBase.getTypeCode() == OBJECT){       // type match, need cast
                            ClassDef exprType = expr.inferType();
                            if(toBase.isThatOrSuperOfThat(exprType)) {
                                yield new ForceCast(ownerFunction, expression, originToType, ForceCast.CastMode.WearClassMask);
                            } else if(forceCast && toBase.isThatOrDerivedFromThat(exprType)){
                                yield new ForceCast(ownerFunction, expression, originToType, ForceCast.CastMode.WearClassMask);
                            } else if(exprType.getTypeCode() == OBJECT){
                                yield throwTypeMismatchErrorExpr(fromType, originToType);
                            }
                        }
                        yield BlockCompiler.nullableIfThenExpr(ownerFunction, expression, nonNullExpression ->
                            castTo(nonNullExpression, toBase)
                        );
                    }
                };
            }

            default -> throw new UnsupportedOperationException("no this type kind");
        };
        if(toType != r.inferType()){
            return new ForceCast(ownerFunction, r.setSourceLocation(this.sourceLocation),originToType, ForceCast.CastMode.WearClassMask);
        }
        return r.setSourceLocation(this.sourceLocation);
    }

    // Box implicit primitive expression, with default box type, the targetType must be `lang.Object`
    // and the result is Integer, Long, ...
    private Expression forceBox(Expression expression) throws CompilationError {
        return new Box(ownerFunction, expression, expression.inferType().getRoot().getObjectClass(), Box.BoxMode.ForceBox);
    }

    private Expression toNullable(Expression expression, NullableClassDef nullableClassDef) throws CompilationError {
        return new ForceCast(ownerFunction, expression, nullableClassDef, ForceCast.CastMode.ToUnion);
    }

    private Expression fromNullable(Expression expression) throws CompilationError {
        ClassDef type = expression.inferType();
        return new ForceCast(ownerFunction, expression, ((NullableClassDef)type).getNullableBaseClass(), ForceCast.CastMode.FromUnion);
    }

    private Expression castObject(Expression expression, ClassDef fromType, ClassDef toType) throws CompilationError {
        if(fromType.isThatOrDerivedFromThat(toType)){
            return new ForceCast(ownerFunction, expression,toType, ForceCast.CastMode.WearClassMask);
        } else if(toType.isDeriveFrom(fromType)){
            if(forceCast) {     //TODO sometimes conversion is allowed, i.e. Cat c=(Cat)animal, however not sure generic type was allowed, i.e. Producer<Cat> p = (Producer<Cat>)producerAnimal
                return new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.ObjectCast);
            }
        }
        throw new TypeMismatchError("can't convert from '%s' to '%s'".formatted(fromType.getFullname(), toType.getFullname()), this.sourceLocation);
    }

    // forceUnbox from lang.Object, which we the target type is undetermined primitive type
    private Expression forceUnbox(Expression expression, ClassDef toType) throws CompilationError {
        return new ForceUnbox(ownerFunction, expression, toType).setSourceLocation(this.sourceLocation);
    }

    private Expression unboxObject(Expression expression, ClassDef toType, boolean allowForceUnbox) throws CompilationError {
        if (toType.getTypeCode() == BOOLEAN) {
            return new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.CastToBoolean);
        } else if (toType.getTypeCode() == STRING) {
            return new ToString(ownerFunction, expression);
        }
        if(allowForceUnbox) {
            return new ForceUnbox(ownerFunction, expression, toType).setSourceLocation(this.sourceLocation);
        }
        throw new TypeMismatchError("can't convert to '%s' from '%s'".formatted(toType.getFullname(), expression.inferType().getFullname()), this.sourceLocation);
    }


    private Expression unbox(Expression expression, ClassDef fromBoxType, PrimitiveClassDef toType) throws CompilationError {
        Expression r = ownerFunction.unbox(expression);
        if(toType == fromBoxType.getUnboxedType()){
            return r;
        } else {
            return castPrimitive(r,fromBoxType.getUnboxedType(),toType);
        }
    }

    private Expression unboxAndForceCastPrimitive(Expression expression, ClassDef fromBoxType, ClassDef implicitPrimaryType) throws CompilationError {
        Expression r = ownerFunction.unbox(expression);
        return forceCastPrimitive(r, fromBoxType.getUnboxedType(), implicitPrimaryType);
    }

    private Expression castToAny(Expression expression, ClassDef fromType, ClassDef toType) throws CompilationError {
        if(fromType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("classref not allowed", expression.getSourceLocation());
        }
        if(toType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("can't cast to classref", this.sourceLocation);
        }
        return new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.CastToAny);
    }

    private Expression boxEnum(Expression expression, PrimitiveClassDef fromType, ClassDef toEnumType) throws CompilationError {
        Expression r;
        if (fromType == toEnumType.getEnumBasePrimitiveType()) {
            r = new Box(ownerFunction, expression, toEnumType, Box.BoxMode.BoxEnum).transform();
        } else {
            if(!fromType.isPrimitiveNumberFamily()){
                throw new TypeMismatchError("number expected", expression.getSourceLocation());
            }
            var cast = castPrimitive(expression,fromType,toEnumType.getEnumBasePrimitiveType());
            r = new Box(ownerFunction, cast, toEnumType, Box.BoxMode.BoxEnum).transform();
        }
        return r;
    }

    private Expression boxPrimitive(Expression expression, PrimitiveClassDef fromType, ClassDef toType) throws CompilationError {
        Expression r;
        ClassDef boxedType = fromType.getBoxedType();
        if(toType == boxedType){
            r = new Box(ownerFunction, expression, toType, Box.BoxMode.Box).transform();
        } else if(toType.isDeriveFrom(boxedType)){
            r = new Box(ownerFunction, expression, toType, Box.BoxMode.Box).transform();
        } else {
            r = new Box(ownerFunction, castPrimitive(expression, fromType, toType.getUnboxedType()), toType, Box.BoxMode.Box);
        }
        return r;
    }

    // all conversion between primitive types are valid
    private Expression castPrimitive(Expression expression, PrimitiveClassDef fromType, PrimitiveClassDef toType) throws CompilationError {
        if(fromType == toType) return expression;
        TypeCode toTypeCode = toType.getTypeCode();
        if(toTypeCode != BOOLEAN){
            if(fromType.getTypeCode() == STRING){
                if(toType.isNumber() || toTypeCode == CHAR){
                    return new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.PrimitiveCast);
                }
                throw new TypeMismatchError("'%s' can't cast to '%s'".formatted(fromType.getFullname(), toType.getFullname()), expression.getSourceLocation());
            }
            if (fromType.getTypeCode() == CLASS_REF) {
                if (toTypeCode != STRING) {
                    throw new TypeMismatchError("'%s' can't cast to '%s'".formatted(fromType.getFullname(), toType.getFullname()), expression.getSourceLocation());
                }
            }
            if (toTypeCode == CLASS_REF || toTypeCode == VOID || toTypeCode == NULL) {      // no primitive type can cast to classref
                throw new TypeMismatchError("'%s' can't cast to '%s'".formatted(fromType.getFullname(), toType.getFullname()), expression.getSourceLocation());
            }
            if(!toTypeCode.isHigherThan(fromType.getTypeCode())){
                if(!forceCast){
                    throw new TypeMismatchError("'%s' can't cast to '%s' implicitly".formatted(fromType.getFullname(), toType.getFullname()), expression.getSourceLocation());
                }
            }
        }
        return new ForceCast(ownerFunction, expression, toType, ForceCast.CastMode.PrimitiveCast);
    }

    private Expression forceCastPrimitive(Expression expression, PrimitiveClassDef fromType, GenericTypeCodeAvatarClassDef originToType) throws CompilationError {
        if (fromType.getTypeCode() == CLASS_REF) {      // no primitive type can cast to classref
            throw new TypeMismatchError("classref cannot cast to unknown type", this.sourceLocation);
        }
        // here the toType is GenericTypeCode, i.e. T as [Primitive]
        return new ForceCast(ownerFunction, expression, originToType, ForceCast.CastMode.PrimitiveCast);
    }

    private Expression forceCastPrimitive(Expression expression, ClassDef fromType, ClassDef originToType) throws CompilationError {
        if(originToType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("can't cast to classref", this.sourceLocation);
        }
        // here the toType is GenericTypeCode, i.e. T as [Primitive]
        return new ForceCast(ownerFunction, expression, originToType, ForceCast.CastMode.PrimitiveCast);
    }

    public static Literal<?> castLiteral(Literal<?> literal, PrimitiveClassDef toType, SourceLocation sourceLocation) throws CompilationError {
        if(toType == literal.getClassDef()) return literal;
        var toTypeCode = toType.getTypeCode();
        var root = literal.getClassDef().getRoot();
        if (toTypeCode == TypeCode.BOOLEAN) {
            return root.createBooleanLiteral(BooleanLiteral.isTrue(literal));
        } else if (toTypeCode == TypeCode.STRING) {
            String string = String.valueOf(literal.value);
            return root.createStringLiteral(string);
        }

        // cast literal directly
        if (literal instanceof CharLiteral c) {
            switch (toTypeCode.value) {
                case FLOAT_VALUE:
                    return root.createFloatLiteral((float) c.value);
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral((double) c.value);
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(new BigDecimal(c.value));
                case BYTE_VALUE:
                    return root.createByteLiteral((byte) c.value.charValue());
                case SHORT_VALUE:
                    return root.createShortLiteral((short) c.value.charValue());
                case INT_VALUE:
                    return root.createIntLiteral((int) c.value);
                case LONG_VALUE:
                    return root.createLongLiteral((long) c.value);
            }
        } else if (literal instanceof FloatLiteral f) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) f.value.intValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral((double) f.value);
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(f.value));
                case BYTE_VALUE:
                    return root.createByteLiteral((byte) f.value.floatValue());
                case SHORT_VALUE:
                    return root.createShortLiteral(f.value.shortValue());
                case INT_VALUE:
                    return root.createIntLiteral(f.value.intValue());
                case LONG_VALUE:
                    return root.createLongLiteral(f.value.longValue());
            }
        } else if (literal instanceof DoubleLiteral d) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) d.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(d.value.floatValue());
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(d.value));
                case BYTE_VALUE:
                    return root.createByteLiteral(d.value.byteValue());
                case SHORT_VALUE:
                    return root.createShortLiteral(d.value.shortValue());
                case INT_VALUE:
                    return root.createIntLiteral(d.value.intValue());
                case LONG_VALUE:
                    return root.createLongLiteral(d.value.longValue());
            }
        } else if (literal instanceof DecimalLiteral d) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) d.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(d.value.floatValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(d.value.doubleValue());
                case BYTE_VALUE:
                    return root.createByteLiteral(d.value.byteValue());
                case SHORT_VALUE:
                    return root.createShortLiteral(d.value.shortValue());
                case INT_VALUE:
                    return root.createIntLiteral(d.value.intValue());
                case LONG_VALUE:
                    return root.createLongLiteral(d.value.longValue());
            }
        } else if (literal instanceof ByteLiteral b) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) b.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(b.value.floatValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(b.value.doubleValue());
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(b.value));
                case SHORT_VALUE:
                    return root.createShortLiteral(b.value.shortValue());
                case INT_VALUE:
                    return root.createIntLiteral(b.value.intValue());
                case LONG_VALUE:
                    return root.createLongLiteral(b.value.longValue());
            }
        } else if (literal instanceof ShortLiteral s) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) s.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(s.value.floatValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(s.value.doubleValue());
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(s.value));
                case BYTE_VALUE:
                    return root.createByteLiteral(s.value.byteValue());
                case INT_VALUE:
                    return root.createIntLiteral(s.value.intValue());
                case LONG_VALUE:
                    return root.createLongLiteral(s.value.longValue());
            }
        } else if (literal instanceof IntLiteral i) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) i.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(i.value.floatValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(i.value.doubleValue());
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(i.value));
                case BYTE_VALUE:
                    return root.createByteLiteral(i.value.byteValue());
                case SHORT_VALUE:
                    return root.createShortLiteral(i.value.shortValue());
                case LONG_VALUE:
                    return root.createLongLiteral(i.value.longValue());
            }
        } else if (literal instanceof LongLiteral l) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return root.createCharLiteral((char) l.value.intValue());
                case FLOAT_VALUE:
                    return root.createFloatLiteral(l.value.floatValue());
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(l.value.doubleValue());
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(BigDecimal.valueOf(l.value));
                case BYTE_VALUE:
                    return root.createByteLiteral(l.value.byteValue());
                case SHORT_VALUE:
                    return root.createShortLiteral(l.value.shortValue());
                case INT_VALUE:
                    return root.createIntLiteral(l.value.intValue());
            }
        } else if (literal instanceof StringLiteral s) {
            String str = s.getString();
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    if(str.isEmpty())
                        return root.createCharLiteral('\0');
                    else if(str.length() == 1){
                        return root.createCharLiteral(str.charAt(0));
                    }
                case FLOAT_VALUE:
                    return root.createFloatLiteral(Float.parseFloat(str));
                case DOUBLE_VALUE:
                    return root.createDoubleLiteral(Double.parseDouble(str));
                case DECIMAL_VALUE:
                    return root.createDecimalLiteral(new BigDecimal(str));
                case BYTE_VALUE:
                    return root.createByteLiteral(Byte.parseByte(str));
                case SHORT_VALUE:
                    return root.createShortLiteral(Short.parseShort(str));
                case INT_VALUE:
                    return root.createIntLiteral(Integer.parseInt(str));
                case LONG_VALUE:
                    return root.createLongLiteral(Long.parseLong(str));
            }
        } else if(literal instanceof NullLiteral n){
            if(toType.getTypeCode() == CLASS_REF){
                return n.getClassDef().toClassRefLiteral();
            }
        }
        throw new TypeMismatchError(literal.inferType().getTypeCode() + " cannot cast to " + toTypeCode, sourceLocation);
    }

}
