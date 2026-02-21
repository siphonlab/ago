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
import org.siphonlab.ago.compiler.generic.GenericTypeCode;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;

public class CastStrategy {

    private final SourceLocation sourceLocation;
    private final boolean forceCast;

    enum TypeKind{
        Primitive,                  // primitive and terminated primitive generic, i.e. `T as [int]`
        PrimitiveGeneric,           // T as [Primitive]
        PrimitiveInterface,         // i instanceof Primitive
        Enum,                       // enum, a special Primitive Boxer type, that can extract literal compile time
        PrimitiveBoxer,             // boxer type of primitive type
        Any,                        // any, generic
        langObject,                 // lang.Object
        Object,                     // object type
    }

    public CastStrategy(SourceLocation sourceLocation, boolean forceCast){
        this.sourceLocation = sourceLocation;
        this.forceCast = forceCast;
    }

    private TypeKind typeKind(ClassDef classDef){
        Root root = classDef.getRoot();
        if(classDef instanceof PrimitiveClassDef) {
            return TypeKind.Primitive;
        } else if(classDef instanceof ClassIntervalClassDef || classDef.isDeriveFrom(root.getClassRefClass())) {      // primitive boxer for classref
            return TypeKind.PrimitiveBoxer;
        } else if(classDef instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
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
        } else if(classDef.isThatOrDerivedFromThat(root.getPrimitiveTypeInterface())){      // Primitive or PrimitiveNumber
            return TypeKind.PrimitiveInterface;
        } else if(classDef.isEnum()){
            return TypeKind.Enum;
        } else if(classDef.isPrimitiveBoxed()){
            return TypeKind.PrimitiveBoxer;
        } else if(classDef == root.getAnyClass()){
            return TypeKind.Any;
        } else if(classDef == root.getObjectClass()){
            return TypeKind.langObject;
        } else {
            return TypeKind.Object;
        }
    }

    private ClassDef resolveTerminatedGenericClass(GenericTypeCode.GenericCodeAvatarClassDef genericCodeAvatar, TypeKind typeKind){
        if(typeKind == TypeKind.Object){
            return genericCodeAvatar;
        } else {
            return genericCodeAvatar.getLBoundClass();
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
        if(originLeftType instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
            leftType = resolveTerminatedGenericClass(a,leftTypeKind);
        } else {
            leftType = originLeftType;
        }
        if (originRightType instanceof GenericTypeCode.GenericCodeAvatarClassDef a) {
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
                        unifyTypes(left, new Unbox(right));
                    case langObject -> {
                        left = new Box(left, ((PrimitiveClassDef)leftType).getBoxedType(), Box.BoxMode.Box);
                        left = new ForceCast(left,rightType, ForceCast.CastMode.ObjectCast);
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
                        unifyTypes(new Unbox(left), right);

                    case Enum, PrimitiveBoxer ->
                        unifyTypes(new Unbox(left), new Unbox(right));

                    case langObject -> {
                        left = new ForceCast(left, rightType, ForceCast.CastMode.ObjectCast);
                        yield new UnifyTypeResult(left,right,rightType, true);
                    }

                    default -> throwTypeMismatchError(originLeftType, originRightType);
                };

            case langObject -> this.unifyTypes(right, left).swap();

            case Object ->
                switch (rightTypeKind){
                    case langObject ->
                        new UnifyTypeResult(new ForceCast(left,rightType, ForceCast.CastMode.ObjectCast), right,rightType, true);
                    case Object ->
                        unifyObjectTypes(left, right, leftType, rightType);
                    default -> throwTypeMismatchError(originLeftType, originRightType);
                };

            default -> throwTypeMismatchError(originLeftType,originRightType);
        };
    }

    private UnifyTypeResult unifyObjectTypes(Expression left, Expression right, ClassDef leftType, ClassDef rightType) throws CompilationError {
        if(leftType.isThatOrSuperOfThat(rightType)){
            var r = new ForceCast(right, leftType, ForceCast.CastMode.WearClassMask);
            return new UnifyTypeResult(left, r, leftType,true);
        } else {
            throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(rightType.getFullname(), leftType.getFullname()), right.getSourceLocation());
        }
    }

    private UnifyTypeResult throwTypeMismatchError(ClassDef originLeftType, ClassDef originRightType) throws TypeMismatchError {
        throw new TypeMismatchError("cannot cast '%s' to '%s'".formatted(originLeftType.getFullname(), originRightType.getFullname()), this.sourceLocation);
    }

    private UnifyTypeResult unifyPrimitiveType(Expression l, Expression r, PrimitiveClassDef t1, PrimitiveClassDef t2) throws CompilationError {
        var resultType = t2.getTypeCode().isHigherThan(t1.getTypeCode()) ? unifyPrimitiveType(t1, t2) : unifyPrimitiveType(t2, t1);
        if(resultType == null){
            throw new TypeMismatchError("cannot find unify type between '%s' and '%s'".formatted(l.inferType().getFullname(), r.inferType().getFullname()), this.sourceLocation);
        }
        boolean changed = false;
        if (resultType != t1) {
            l = new Cast(l, resultType);
            changed = true;
        }
        if (resultType != t2) {
            r = new Cast(r, resultType);
            changed = true;
        }
        return new UnifyTypeResult(l, r, resultType, changed);
    }

    protected PrimitiveClassDef unifyPrimitiveType(PrimitiveClassDef loType, PrimitiveClassDef hiType) {
        var t1 = loType.getTypeCode();
        var t2 = hiType.getTypeCode();

        if (t1 == STRING)
            return hiType;        // string -> int... number types
        if (t2 == STRING)
            return loType;

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
                    return PrimitiveClassDef.DOUBLE;
                }
                break;
            case FLOAT_VALUE:
                if (t2 == DOUBLE) {
                    return hiType;
                } else if (t2 == INT || t2 == LONG) {
                    return PrimitiveClassDef.DOUBLE;
                }
                break;
            case LONG_VALUE:
                if (t2 == FLOAT) {
                    return PrimitiveClassDef.DOUBLE;
                } else if (t2 == DOUBLE) {
                    return PrimitiveClassDef.DOUBLE;
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
        if (toType instanceof GenericTypeCode.GenericCodeAvatarClassDef a) {
            toType = resolveTerminatedGenericClass(a, toTypeKind);
        }

        if(fromType == toType){
            if(toType == originToType){
                return expression;
            } else {
                return new ForceCast(expression, originToType, ForceCast.CastMode.WearClassMask);
            }
        }

        if (expression instanceof Literal<?> literal) {
            if (toType instanceof PrimitiveClassDef) {
                return new ForceCast(castLiteral(literal, toType), originToType, ForceCast.CastMode.WearClassMask).transform();
            }
            if (literal instanceof NullLiteral n) {
                if (toTypeKind == TypeKind.langObject || toTypeKind == TypeKind.Object || toTypeKind == TypeKind.Any) {
                    return new NullAsObject(n,originToType);
                }
            }
        }

        var r = switch (fromTypeKind){
            case Primitive -> {
                final PrimitiveClassDef primitiveFromType = (PrimitiveClassDef) fromType;
                yield switch (toTypeKind) {
                    case Primitive ->
                        castPrimitive(expression, primitiveFromType, (PrimitiveClassDef) toType);
                    case PrimitiveGeneric ->
                        forceCastPrimitive(expression, primitiveFromType, (GenericTypeCode.GenericCodeAvatarClassDef) originToType);
                    case PrimitiveBoxer ->
                        boxPrimitive(expression, primitiveFromType, toType);
                    case Enum ->
                        boxEnum(expression, primitiveFromType, toType);
                    case Any ->
                        castToAny(expression, fromType, originToType);
                    case langObject ->
                        boxPrimitive(expression, primitiveFromType, primitiveFromType.getBoxedType());
                    case Object ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);
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
                        yield new Box(r1, toType, Box.BoxMode.Box).transform();
                    }

                    case Enum ->{
                        var r1 = forceCastPrimitive(expression, fromType, toType.getUnboxedType());
                        yield new Box(r1, toType, Box.BoxMode.BoxEnum).transform();
                    }
                    case Any ->
                        castToAny(expression, fromType, originToType);
                    case langObject ->
                        forceBox(expression);

                    case Object ->
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);
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
                        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);
                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };
            case Any ->     castToAny(expression, fromType, originToType);

            case langObject ->
                switch (toTypeKind) {
                    case Primitive ->
                        unboxObject(expression, toType, true);
                    case PrimitiveGeneric ->
                        forceUnbox(expression, toType);

                    case PrimitiveBoxer, Enum, Object ->
                        new ForceCast(expression,toType, ForceCast.CastMode.ObjectCast);

                    case langObject ->  expression;

                    case Any ->
                        castToAny(expression, fromType, originToType);

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
                            yield new ClassRefLiteral(value);
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
                            yield Assign.processBoundClass(toType,expression,this.sourceLocation);
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

                    default ->
                        throw new UnsupportedOperationException("no this type kind");
                };

            default -> throw new UnsupportedOperationException("no this type kind");
        };
        if(toType != r.inferType()){
            return new ForceCast(r.setSourceLocation(this.sourceLocation),originToType, ForceCast.CastMode.WearClassMask);
        }
        return r.setSourceLocation(this.sourceLocation);
    }

    // Box implicit primitive expression, with default box type, the targetType must be `lang.Object`
    // and the result is Integer, Long, ...
    private Expression forceBox(Expression expression) throws CompilationError {
        return new Box(expression, expression.inferType().getRoot().getObjectClass(), Box.BoxMode.ForceBox);
    }

    private Expression castObject(Expression expression, ClassDef fromType, ClassDef toType) throws CompilationError {
        if(fromType.isThatOrDerivedFromThat(toType)){
            return new ForceCast(expression,toType, ForceCast.CastMode.WearClassMask);
        } else if(toType.isDeriveFrom(fromType)){
            if(forceCast) {     //TODO sometimes conversion is allowed, i.e. Cat c=(Cat)animal, however not sure generic type was allowed, i.e. Producer<Cat> p = (Producer<Cat>)producerAnimal
                return new ForceCast(expression, toType, ForceCast.CastMode.ObjectCast);
            }
        }
        throw new TypeMismatchError("can't convert from '%s' to '%s'".formatted(fromType.getFullname(), toType.getFullname()), this.sourceLocation);
    }

    // forceUnbox from lang.Object, which we the target type is undetermined primitive type
    private Expression forceUnbox(Expression expression, ClassDef toType) throws CompilationError {
        return new ForceUnbox(expression, toType).setSourceLocation(this.sourceLocation);
    }

    private Expression unboxObject(Expression expression, ClassDef toType, boolean allowForceUnbox) throws CompilationError {
        if (toType.getTypeCode() == BOOLEAN) {
            return new ForceCast(expression, toType, ForceCast.CastMode.CastToBoolean);
        } else if (toType.getTypeCode() == STRING) {
            return new ToString(expression);
        }
        if(allowForceUnbox) {
            return new ForceUnbox(expression, toType).setSourceLocation(this.sourceLocation);
        }
        throw new TypeMismatchError("can't convert to '%s'".formatted(toType.getFullname()), this.sourceLocation);
    }


    private Expression unbox(Expression expression, ClassDef fromBoxType, PrimitiveClassDef toType) throws CompilationError {
        Expression r = new Unbox(expression);
        if(toType == fromBoxType.getUnboxedType()){
            return r;
        } else {
            return castPrimitive(r,fromBoxType.getUnboxedType(),toType);
        }
    }

    private Expression unboxAndForceCastPrimitive(Expression expression, ClassDef fromBoxType, ClassDef implicitPrimaryType) throws CompilationError {
        Expression r = new Unbox(expression);
        return forceCastPrimitive(r, fromBoxType.getUnboxedType(), implicitPrimaryType);
    }

    private Expression castToAny(Expression expression, ClassDef fromType, ClassDef toType) throws CompilationError {
        if(fromType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("classref not allowed", expression.getSourceLocation());
        }
        if(toType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("can't cast to classref", this.sourceLocation);
        }
        return new ForceCast(expression, toType, ForceCast.CastMode.CastToAny);
    }

    private Expression boxEnum(Expression expression, PrimitiveClassDef fromType, ClassDef toEnumType) throws CompilationError {
        Expression r;
        if (fromType == toEnumType.getEnumBasePrimitiveType()) {
            r = new Box(expression, toEnumType, Box.BoxMode.BoxEnum).transform();
        } else {
            if(!fromType.isPrimitiveNumberFamily()){
                throw new TypeMismatchError("number expected", expression.getSourceLocation());
            }
            var cast = castPrimitive(expression,fromType,toEnumType.getEnumBasePrimitiveType());
            r = new Box(cast, toEnumType, Box.BoxMode.BoxEnum).transform();
        }
        return r;
    }

    private Expression boxPrimitive(Expression expression, PrimitiveClassDef fromType, ClassDef toType) throws CompilationError {
        Expression r;
        ClassDef boxedType = fromType.getBoxedType();
        if(toType == boxedType){
            r = new Box(expression, toType, Box.BoxMode.Box).transform();
        } else if(toType.isDeriveFrom(boxedType)){
            r = new Box(expression, toType, Box.BoxMode.Box).transform();
        } else {
            r = new Box(castPrimitive(expression, fromType, toType.getUnboxedType()), toType, Box.BoxMode.Box);
        }
        return r;
    }

    // all conversion between primitive types are valid
    private Expression castPrimitive(Expression expression, PrimitiveClassDef fromType, PrimitiveClassDef toType) throws CompilationError {
        TypeCode toTypeCode = toType.getTypeCode();
        if(toTypeCode != BOOLEAN){
            if(fromType.getTypeCode() == STRING){
                if(toType.isNumber() || toTypeCode == CHAR){
                    return new ForceCast(expression, toType, ForceCast.CastMode.PrimitiveCast);
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
        }
        return new ForceCast(expression, toType, ForceCast.CastMode.PrimitiveCast);
    }

    private Expression forceCastPrimitive(Expression expression, PrimitiveClassDef fromType, GenericTypeCode.GenericCodeAvatarClassDef originToType) throws CompilationError {
        if (fromType.getTypeCode() == CLASS_REF) {      // no primitive type can cast to classref
            throw new TypeMismatchError("classref cannot cast to unknown type", this.sourceLocation);
        }
        // here the toType is GenericTypeCode, i.e. T as [Primitive]
        return new ForceCast(expression, originToType, ForceCast.CastMode.PrimitiveCast);
    }

    private Expression forceCastPrimitive(Expression expression, ClassDef fromType, ClassDef originToType) throws CompilationError {
        if(originToType.getUnboxedTypeCode() == CLASS_REF){
            throw new TypeMismatchError("can't cast to classref", this.sourceLocation);
        }
        // here the toType is GenericTypeCode, i.e. T as [Primitive]
        return new ForceCast(expression, originToType, ForceCast.CastMode.PrimitiveCast);
    }

    Expression castLiteral(Literal<?> literal, ClassDef toType) throws CompilationError {
        var toTypeCode = toType.getTypeCode();
        if (toTypeCode == TypeCode.BOOLEAN) {
            return new BooleanLiteral(BooleanLiteral.isTrue(literal));
        } else if (toTypeCode == TypeCode.STRING) {
            String string = String.valueOf(literal.value);
            return new StringLiteral(string);
        }

        // cast literal directly
        if (literal instanceof CharLiteral c) {
            switch (toTypeCode.value) {
                case FLOAT_VALUE:
                    return new FloatLiteral((float) c.value);
                case DOUBLE_VALUE:
                    return new DoubleLiteral((double) c.value);
                case BYTE_VALUE:
                    return new ByteLiteral((byte) c.value.charValue());
                case SHORT_VALUE:
                    return new ShortLiteral((short) c.value.charValue());
                case INT_VALUE:
                    return new IntLiteral((int) c.value);
                case LONG_VALUE:
                    return new LongLiteral((long) c.value);
            }
        } else if (literal instanceof FloatLiteral f) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) f.value.intValue());
                case DOUBLE_VALUE:
                    return new DoubleLiteral((double) f.value);
                case BYTE_VALUE:
                    return new ByteLiteral((byte) f.value.floatValue());
                case SHORT_VALUE:
                    return new ShortLiteral(f.value.shortValue());
                case INT_VALUE:
                    return new IntLiteral(f.value.intValue());
                case LONG_VALUE:
                    return new LongLiteral(f.value.longValue());
            }
        } else if (literal instanceof DoubleLiteral d) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) d.value.intValue());
                case FLOAT_VALUE:
                    return new FloatLiteral(d.value.floatValue());
                case BYTE_VALUE:
                    return new ByteLiteral(d.value.byteValue());
                case SHORT_VALUE:
                    return new ShortLiteral(d.value.shortValue());
                case INT_VALUE:
                    return new IntLiteral(d.value.intValue());
                case LONG_VALUE:
                    return new LongLiteral(d.value.longValue());
            }
        } else if (literal instanceof ByteLiteral b) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) b.value.intValue());
                case FLOAT_VALUE:
                    return new FloatLiteral(b.value.floatValue());
                case DOUBLE_VALUE:
                    return new DoubleLiteral(b.value.doubleValue());
                case SHORT_VALUE:
                    return new ShortLiteral(b.value.shortValue());
                case INT_VALUE:
                    return new IntLiteral(b.value.intValue());
                case LONG_VALUE:
                    return new LongLiteral(b.value.longValue());
            }
        } else if (literal instanceof ShortLiteral s) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) s.value.intValue());
                case FLOAT_VALUE:
                    return new FloatLiteral(s.value.floatValue());
                case DOUBLE_VALUE:
                    return new DoubleLiteral(s.value.doubleValue());
                case BYTE_VALUE:
                    return new ByteLiteral(s.value.byteValue());
                case INT_VALUE:
                    return new IntLiteral(s.value.intValue());
                case LONG_VALUE:
                    return new LongLiteral(s.value.longValue());
            }
        } else if (literal instanceof IntLiteral i) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) i.value.intValue());
                case FLOAT_VALUE:
                    return new FloatLiteral(i.value.floatValue());
                case DOUBLE_VALUE:
                    return new DoubleLiteral(i.value.doubleValue());
                case BYTE_VALUE:
                    return new ByteLiteral(i.value.byteValue());
                case SHORT_VALUE:
                    return new ShortLiteral(i.value.shortValue());
                case LONG_VALUE:
                    return new LongLiteral(i.value.longValue());
            }
        } else if (literal instanceof LongLiteral l) {
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    return new CharLiteral((char) l.value.intValue());
                case FLOAT_VALUE:
                    return new FloatLiteral(l.value.floatValue());
                case DOUBLE_VALUE:
                    return new DoubleLiteral(l.value.doubleValue());
                case BYTE_VALUE:
                    return new ByteLiteral(l.value.byteValue());
                case SHORT_VALUE:
                    return new ShortLiteral(l.value.shortValue());
                case INT_VALUE:
                    return new IntLiteral(l.value.intValue());
            }
        } else if (literal instanceof StringLiteral s) {
            String str = s.getString();
            switch (toTypeCode.value) {
                case CHAR_VALUE:
                    if(str.isEmpty())
                        return new CharLiteral('\0');
                    else if(str.length() == 1){
                        return new CharLiteral(str.charAt(0));
                    }
                case FLOAT_VALUE:
                    return new FloatLiteral(Float.parseFloat(str));
                case DOUBLE_VALUE:
                    return new DoubleLiteral(Double.parseDouble(str));
                case BYTE_VALUE:
                    return new ByteLiteral(Byte.parseByte(str));
                case SHORT_VALUE:
                    return new ShortLiteral(Short.parseShort(str));
                case INT_VALUE:
                    return new IntLiteral(Integer.parseInt(str));
                case LONG_VALUE:
                    return new LongLiteral(Long.parseLong(str));
            }
        }
        throw new TypeMismatchError(literal.inferType().getTypeCode() + " cannot cast to " + toTypeCode, sourceLocation);
    }

}
