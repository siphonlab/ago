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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;

public class Cast extends ExpressionBase{

    private final Expression expression;
    private final boolean forceCast;
    public final ClassDef toType;

    public Cast(Expression expression, ClassDef toType) throws CompilationError {
        this(expression, toType, false);
    }

    public Cast(Expression expression, ClassDef toType, boolean forceCast) throws CompilationError {
        this.expression = expression.transform();
        this.forceCast = forceCast;
        this.setParent(this.expression.getParent());
        this.expression.setParent(this);
        this.toType = toType;
        assert toType != null;
        this.setSourceLocation(expression.getSourceLocation());     // for implicit casting. For explicit casting, it will call setSourceLocation again
    }

    @Override
    public Cast setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        return new CastStrategy(this.getSourceLocation(), forceCast).castTo(this.expression, this.toType).transform();
    }

    //    @Override
//    public Expression transformInner() throws CompilationError {
//        ClassDef fromType = expression.inferType();
//        if(toType == fromType){
//            expression.setParent(this.getParent());
//            return expression;
//        }
//        var toTypeCode = this.toType.getTypeCode();
//        if(toTypeCode == TypeCode.CLASS_REF){
//            Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(expression, this.getSourceLocation());
//            if(pair == null){
//                throw new TypeMismatchError("'%s' is not a class expression", this.getSourceLocation());
//            }
//            ClassDef value = pair.getValue();
//            return new ClassRefLiteral(value);
//        } else if(this.toType.getUnboxedTypeCode() == TypeCode.CLASS_REF){
//            Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(expression, this.getSourceLocation());
//            if(pair == null){
//                throw new TypeMismatchError("'%s' is not a class expression", this.getSourceLocation());
//            }
//            ClassDef value = pair.getValue();
//            Box boxed = new Box(new ClassRefLiteral(value), this.toType);
//            if(pair.getLeft() != null){
//                return new BindScopeToScopedClassInterval(boxed, pair.getLeft());
//            }
//            return boxed.transformInner();
//        }
//        if(toTypeCode == TypeCode.VOID || toTypeCode == TypeCode.NULL){
//            throw new TypeMismatchError("cannot cast to " + toTypeCode, sourceLocation);
//        }
//        if(this.expression instanceof NullAsObject nullAsObject){
//            if(toType.isAssignableFrom(fromType) || fromType.isAssignableFrom(toType)){
//                return new NullAsObject(nullAsObject.getNullLiteral(), toType);
//            }
//        }
//
//        if(expression instanceof Literal<?> literal){
//            if(toTypeCode == TypeCode.BOOLEAN){
//                return new BooleanLiteral(BooleanLiteral.isTrue(literal));
//            } else if(toTypeCode == TypeCode.STRING){
//                String string = String.valueOf(literal.value);
//                return new StringLiteral(string);
//            } else if(toTypeCode == TypeCode.OBJECT){
//                if(expression instanceof NullLiteral n)
//                    return new NullAsObject(n,toType);
//
//                var r = new Box(literal, this.toType).transform();
//                return r.inferType() == this.toType ? r : new Cast(r, this.toType);
//            }
//
//            // cast literal directly
//            if(expression instanceof CharLiteral c){
//                switch (toTypeCode.value){
//                    case FLOAT_VALUE:
//                        return new FloatLiteral((float)c.value);
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral((double)c.value);
//                    case BYTE_VALUE:
//                        return new ByteLiteral((byte)c.value.charValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral((short)c.value.charValue());
//                    case INT_VALUE:
//                        return new IntLiteral((int)c.value);
//                    case LONG_VALUE:
//                        return new LongLiteral((long)c.value);
//                }
//            } else if(expression instanceof FloatLiteral f){
//                switch (toTypeCode.value){
//                    case CHAR_VALUE:
//                        return new CharLiteral((char)f.value.intValue());
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral((double)f.value);
//                    case BYTE_VALUE:
//                        return new ByteLiteral((byte)f.value.floatValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral(f.value.shortValue());
//                    case INT_VALUE:
//                        return new IntLiteral(f.value.intValue());
//                    case LONG_VALUE:
//                        return new LongLiteral(f.value.longValue());
//                }
//            } else if(expression instanceof DoubleLiteral d){
//                switch (toTypeCode.value){
//                    case CHAR_VALUE:
//                        return new CharLiteral((char)d.value.intValue());
//                    case FLOAT_VALUE:
//                        return new FloatLiteral(d.value.floatValue());
//                    case BYTE_VALUE:
//                        return new ByteLiteral(d.value.byteValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral(d.value.shortValue());
//                    case INT_VALUE:
//                        return new IntLiteral(d.value.intValue());
//                    case LONG_VALUE:
//                        return new LongLiteral(d.value.longValue());
//                }
//            } else if(expression instanceof ByteLiteral b){
//                switch (toTypeCode.value){
//                    case CHAR_VALUE:
//                        return new CharLiteral((char)b.value.intValue());
//                    case FLOAT_VALUE:
//                        return new FloatLiteral(b.value.floatValue());
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral(b.value.doubleValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral(b.value.shortValue());
//                    case INT_VALUE:
//                        return new IntLiteral(b.value.intValue());
//                    case LONG_VALUE:
//                        return new LongLiteral(b.value.longValue());
//                }
//            } else if(expression instanceof ShortLiteral s){
//                switch (toTypeCode.value){
//                    case CHAR_VALUE:
//                        return new CharLiteral((char)s.value.intValue());
//                    case FLOAT_VALUE:
//                        return new FloatLiteral(s.value.floatValue());
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral(s.value.doubleValue());
//                    case BYTE_VALUE:
//                        return new ByteLiteral(s.value.byteValue());
//                    case INT_VALUE:
//                        return new IntLiteral(s.value.intValue());
//                    case LONG_VALUE:
//                        return new LongLiteral(s.value.longValue());
//                }
//            } else if(expression instanceof IntLiteral i){
//                switch (toTypeCode.value){
//                    case CHAR_VALUE:
//                        return new CharLiteral((char)i.value.intValue());
//                    case FLOAT_VALUE:
//                        return new FloatLiteral(i.value.floatValue());
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral(i.value.doubleValue());
//                    case BYTE_VALUE:
//                        return new ByteLiteral(i.value.byteValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral(i.value.shortValue());
//                    case LONG_VALUE:
//                        return new LongLiteral(i.value.longValue());
//                }
//            } else if(expression instanceof LongLiteral l) {
//                switch (toTypeCode.value) {
//                    case CHAR_VALUE:
//                        return new CharLiteral((char) l.value.intValue());
//                    case FLOAT_VALUE:
//                        return new FloatLiteral(l.value.floatValue());
//                    case DOUBLE_VALUE:
//                        return new DoubleLiteral(l.value.doubleValue());
//                    case BYTE_VALUE:
//                        return new ByteLiteral(l.value.byteValue());
//                    case SHORT_VALUE:
//                        return new ShortLiteral(l.value.shortValue());
//                    case INT_VALUE:
//                        return new IntLiteral(l.value.intValue());
//                }
//            } else {
//                throw new TypeMismatchError(literal.inferType().getTypeCode() + " cannot cast to " + toTypeCode, sourceLocation);
//            }
//        }
//
//        if(fromType.isPrimitiveFamily()){
//            if(toTypeCode == TypeCode.OBJECT) {
//                var r = new Box(expression, this.toType).setSourceLocation(this.getSourceLocation()).transform();
//                return r.inferType() == this.toType ? r : new Cast(r, this.toType);
//            }
//            if(this.toType.isPrimitiveFamily()){
//                return this;
//            }
//        }
//
//        if(this.toType.isPrimitiveOrBoxed()) {
//            if(toTypeCode == BOOLEAN){
//                return this;        // every type can cast to boolean
//            } else if(toType.getUnboxedTypeCode() == BOOLEAN){
//                return new Box(this,PrimitiveClassDef.BOOLEAN.getBoxedType());
//            }
//            if (this.toType.isPrimitiveFamily()) {
//                if (fromType.getUnboxedTypeCode() == this.toType.getTypeCode()) {
//                    return new Unbox(expression).setSourceLocation(this.getSourceLocation()).transform();
//                } else if(toTypeCode == TypeCode.STRING){
//                    return new ToString(expression).setSourceLocation(this.getSourceLocation());
//                }
//            } else {
//                if(this.toType.getUnboxedTypeCode() == fromType.getUnboxedTypeCode()){
//                    var unbox = new Unbox(expression).setSourceLocation(this.getSourceLocation());
//                    return new Box(unbox, this.toType).setSourceLocation(this.getSourceLocation()).transform();
//                }
//            }
//            throw new TypeMismatchError("cannot cast %s' to '%s'".formatted(fromType, this.toType), this.getSourceLocation());
//        }
//
//        if(toTypeCode == TypeCode.STRING){
//            assert fromType.getTypeCode() == TypeCode.OBJECT;
//            return new ToString(expression).setSourceLocation(this.getSourceLocation());
//        }
//
//        if(toTypeCode == TypeCode.OBJECT){
//            if(!this.toType.isAssignableFrom(fromType)){
//                throw new TypeMismatchError("cannot cast %s' to '%s'".formatted(fromType, this.toType), this.getSourceLocation());
//            }
//        }
//        return this;
//    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return toType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("shouldn't enter here");
//        try {
//            blockCompiler.enter(this);
//
//            var code = blockCompiler.getCode();
//            if (expression instanceof LiteralResultExpression literalResultExpression) {
//                var literal = literalResultExpression.visit(blockCompiler);
//                Assign.to(localVar, new Cast(literal, toType).setSourceLocation(expression.getSourceLocation()).transform()).termVisit(blockCompiler);
//            } else {
//                castToLocalVar(localVar, (Var.LocalVar) expression.visit(blockCompiler), code);
//            }
//
//        } catch (CompilationError e) {
//            throw e;
//        } finally {
//            blockCompiler.leave(this);
//        }
    }

    private void castToLocalVar(Var.LocalVar localVar, Var.LocalVar result, CodeBuffer code) throws CompilationError {
        var toTypeCode = this.toType.getTypeCode();
        var fromTypeCode = result.inferType().getTypeCode();
        if(result.inferType().isPrimitiveFamily()) {
            assert toType.isPrimitiveFamily();
            code.cast(fromTypeCode, result.getVariableSlot(), toTypeCode, localVar.getVariableSlot());
        } else if(toTypeCode == BOOLEAN){
            code.cast(fromTypeCode, result.getVariableSlot(), toTypeCode, localVar.getVariableSlot());
        } else {
            assert !toType.isPrimitiveFamily() && (fromTypeCode == toTypeCode || fromTypeCode.isGeneric() || toTypeCode.isGeneric());
            assert localVar.inferType() == this.toType;
            code.assign(localVar.getVariableSlot(), toTypeCode, result.getVariableSlot());
        }
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("shouldn't enter here");
//        try {
//            blockCompiler.enter(this);
//
//            if (expression instanceof Literal<?> literal && literal.inferType() == this.toType) {
//                return literal;
//            }
//
//            if (expression instanceof LiteralResultExpression literalResultExpression) {
//                var literal = literalResultExpression.visit(blockCompiler);
//                return new Cast(literal, this.toType).setSourceLocation(this.getSourceLocation()).transform().visit(blockCompiler);
//            }
//            Var.LocalVar localVar = blockCompiler.acquireTempVar(this);
//            castToLocalVar(localVar, (Var.LocalVar) expression.visit(blockCompiler), blockCompiler.getCode());
//            return localVar;
//        } catch (CompilationError e) {
//            throw e;
//        } finally {
//            blockCompiler.leave(this);
//        }

    }

    @Override
    public String toString() {
        return "(Cast %s %s)".formatted(toType, expression);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cast cast)) return false;
        return Objects.equals(expression, cast.expression) && Objects.equals(toType, cast.toType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, toType);
    }
}
