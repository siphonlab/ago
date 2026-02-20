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
package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.IllegalExpressionError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Assign;
import org.siphonlab.ago.compiler.expression.Cast;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Unbox;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.opcode.logic.BitShiftLeft;
import org.siphonlab.ago.opcode.logic.BitShiftRight;
import org.siphonlab.ago.opcode.logic.BitUnsignedRight;

import java.util.Objects;

public class SelfBitShiftExpr extends SelfOpExpr {

    private final Expression site;
    private Expression change;
    private final Type type;

    public enum Type{
        LShift(BitShiftLeft.KIND_BIT_LSHIFT),
        RShift(BitShiftRight.KIND_BIT_RSHIFT),
        URShift(BitUnsignedRight.KIND_BIT_URSHIFT);

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }

    public SelfBitShiftExpr(Expression site, Expression change, Type type) throws CompilationError {
        super(site, change);
        this.site = site.transform().setParent(this);
        this.change = new Cast(change.setParent(this), site.inferType()).transform();
        this.type = type;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.site.inferType();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if (!(site instanceof Assign.Assignee)) {
            throw new SyntaxError("bad target for self logic operation", this.site.getSourceLocation());
        }
        ClassDef type = site.inferType();
        if(!type.getUnboxedTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family expression expected",this.getSourceLocation());
        }
        if(!change.inferType().getTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family value expected", change.getSourceLocation());
        }
        change = new Cast(change, PrimitiveClassDef.INT).transform();
        if(change instanceof IntLiteral r){
            if(r.value == 0) return site;
            if(r.value < 0) throw new IllegalExpressionError("illegal bits value", change.getSourceLocation());
        }
        return this;
    }

    Expression expr(Expression left, Expression right) throws CompilationError {
        switch (type){
            case LShift:
                return new BitShiftExpr(BitShiftExpr.Type.LShift, new Unbox(left).transform(), right);
            case RShift:
                return new BitShiftExpr(BitShiftExpr.Type.RShift, new Unbox(left).transform(), right);
            case URShift:
                return new BitShiftExpr(BitShiftExpr.Type.URShift, new Unbox(left).transform(), right);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }


    @Override
    public SelfBitShiftExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(this.type, this.site, this.change);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SelfBitShiftExpr that)) return false;
        return Objects.equals(site, that.site) && Objects.equals(change, that.change) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, change, type);
    }
}
