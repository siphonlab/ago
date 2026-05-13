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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

import static org.siphonlab.ago.TypeCode.BOOLEAN;

// the result of Cast.transform, and
public class ForceCast extends ExpressionInFunctionBody{

    public enum CastMode{
        PrimitiveCast,   // assume the expression is primitive
        ObjectCast,      // assume the expression is an object
        CastToAny,       // try cast to any type, with cast_vvtCtC
        WearClassMask,   // don't generate code, just mask to another class, i.e. T as [int to _], the target is already int, use this mask to play as T
        CastToBoolean,   // o2B
        ToUnion,        // from base class to its nullable class
        FromUnion,      // from nullable to its base class
    }

    private final Expression expression;
    private final ClassDef toType;
    private final CastMode castMode;

    public ForceCast(FunctionDef ownerFunction, Expression expression, ClassDef toType, CastMode castMode) throws CompilationError {
        super(ownerFunction);
        this.expression = expression.transform();
        this.setSourceLocation(expression.getSourceLocation());
        this.toType = toType;
        this.castMode = castMode;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return toType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(toType == expression.inferType()) return expression;
        if(castMode == CastMode.CastToAny && toType instanceof AnyClassDef){
            return new ForceCast(ownerFunction,expression, toType, CastMode.ToUnion);
        }
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var code = blockCompiler.getCode();
            if (expression instanceof LiteralResultExpression literalResultExpression) {
                var literal = literalResultExpression.visit(blockCompiler);
                castLiteral(literal, localVar, blockCompiler);
            } else {
                castToLocalVar(localVar, (Var.LocalVar) expression.visit(blockCompiler), blockCompiler);
            }

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void castLiteral(Literal<?> literal, Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        switch (this.castMode){
            case PrimitiveCast:
                assert localVar.inferType().getTypeCode() == literal.getTypeCode();
                ownerFunction.assign(localVar, literal).termVisit(blockCompiler);
                break;
            case CastToBoolean:
                ownerFunction.assign(localVar, getRoot().createBooleanLiteral( BooleanLiteral.isTrue(literal)).setSourceLocation(expression.getSourceLocation()).transform()).termVisit(blockCompiler);
                break;
            case CastToAny: {
                var tempVar = blockCompiler.acquireTempVar(literal.classDef);
                blockCompiler.lockRegister(tempVar);
                ownerFunction.assign(tempVar, literal).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                blockCompiler.getCode().cast_to_any(tempVar.getVariableSlot(), literal.getTypeCode(), -1, localVar.getVariableSlot(),
                        toType.getTypeCode(), toType.isPrimitiveFamily() ? -1 : blockCompiler.getFunctionDef().idOfClass(toType));
                blockCompiler.releaseRegister(tempVar);
                break;
            }
            case ToUnion: {
                if(literal instanceof NullLiteral){
                    blockCompiler.getCode().assignLiteral(localVar.getVariableSlot(), literal);
                } else if(toType instanceof NullableClassDef nullableType){
                    var tempVar = blockCompiler.acquireTempVar(nullableType.getNullableBaseClass());
                    blockCompiler.lockRegister(tempVar);
                    ownerFunction.assign(tempVar, literal).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                    blockCompiler.getCode().cast(nullableType.getNullableBaseClass().getTypeCode(), tempVar.getVariableSlot(), TypeCode.UNION, localVar.getVariableSlot());
                    blockCompiler.releaseRegister(tempVar);
                } else if(toType instanceof AnyClassDef){
                    var tempVar = blockCompiler.acquireTempVar(literal.inferType());
                    blockCompiler.lockRegister(tempVar);
                    ownerFunction.assign(tempVar, literal).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                    blockCompiler.getCode().cast(literal.getTypeCode(), tempVar.getVariableSlot(), TypeCode.UNION, localVar.getVariableSlot());
                    blockCompiler.releaseRegister(tempVar);
                    break;
                }
                break;
            }
            default:
                throw new TypeMismatchError("'%s' can't cast to '%s'".formatted(literal, toType.getFullname()), this.getSourceLocation());
        }
    }

    private void castToLocalVar(Var.LocalVar target, Var.LocalVar src, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        ClassDef fromType = src.inferType();
        var fromTypeCode = fromType.getTypeCode();
        switch (castMode){
            case PrimitiveCast:
                code.cast(fromTypeCode, src.getVariableSlot(), toType.getTypeCode(), target.getVariableSlot());
                break;
            case ObjectCast:    // cast object with validation
                code.cast_object(src.getVariableSlot(), target.getVariableSlot(), toType.getTypeCode(), blockCompiler.getFunctionDef().idOfClass(toType));
                break;
            case CastToAny:
                code.cast_to_any(src.getVariableSlot(), fromTypeCode,
                                        fromType.isPrimitiveFamily() ? -1 : blockCompiler.getFunctionDef().idOfClass(fromType),
                                 target.getVariableSlot(), toType.getTypeCode(),
                                        toType.isPrimitiveFamily() ? -1 : blockCompiler.getFunctionDef().idOfClass(toType));
                break;
            case WearClassMask:
                code.assign(target.getVariableSlot(), toType.getTypeCode(),src.getVariableSlot());   // now `obj.f as Object = (Object)v` will generate `@t as Object = v; obj.f = @t`
                break;
            case CastToBoolean:
                code.cast(fromTypeCode, src.getVariableSlot(), BOOLEAN, target.getVariableSlot());
                break;
            case ToUnion, FromUnion:
                code.cast(fromTypeCode, src.getVariableSlot(), toType.getTypeCode(), target.getVariableSlot());
                break;
        }
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (expression instanceof Literal<?> literal && literal.inferType() == this.toType) {
                return literal;
            }

            if (expression instanceof LiteralResultExpression literalResultExpression) {
                var literal = literalResultExpression.visit(blockCompiler);
                if(this.castMode == CastMode.ToUnion || this.castMode == CastMode.CastToAny){
                    if(literal instanceof NullLiteral) return literal;

                    var temp = blockCompiler.acquireTempVar(toType);
                    castLiteral(literal, temp, blockCompiler);
                    return temp;
                }
                return new CastStrategy(ownerFunction, this.getSourceLocation(), false).castTo(literal,toType).visit(blockCompiler);
            }
            if (castMode == CastMode.WearClassMask) {
                return expression.visit(blockCompiler);
            }

            Var.LocalVar localVar = blockCompiler.acquireTempVar(this);
            castToLocalVar(localVar, (Var.LocalVar) expression.visit(blockCompiler), blockCompiler);
            return localVar;
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.expression.termVisit(blockCompiler);
    }

    @Override
    public String toString() {
        return "(ForceCast %s %s %s)".formatted(this.castMode, this.toType, this.expression);
    }
}
