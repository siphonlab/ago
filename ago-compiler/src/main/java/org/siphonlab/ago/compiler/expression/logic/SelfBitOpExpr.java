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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import org.siphonlab.ago.compiler.expression.*;


import java.util.Objects;

import static org.siphonlab.ago.opcode.logic.BitAnd.KIND_BITAND;
import static org.siphonlab.ago.opcode.logic.BitOr.KIND_BITOR;
import static org.siphonlab.ago.opcode.logic.BitXor.KIND_BITXOR;

public class SelfBitOpExpr extends SelfOpExpr {

    private final Expression site;
    private Expression change;
    private final Type type;

    public enum Type{
        BitAnd(KIND_BITAND),
        BitOr(KIND_BITOR),
        BitXor(KIND_BITXOR)
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }

    public SelfBitOpExpr(Expression site, Expression change, Type type) throws CompilationError {
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
            throw new SyntaxError("bad target for self bit operation", this.site.getSourceLocation());
        }
        if(!this.site.inferType().getTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family expression expected", this.site.getSourceLocation());
        }
        if(!change.inferType().getUnboxedTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family expression expected", this.change.getSourceLocation());
        }
        if(change.inferType().getUnboxedTypeCode().isHigherThan(this.site.inferType().getTypeCode())){
            throw new TypeMismatchError("type is higher than '%s'".formatted(this.site.inferType().getFullname()), this.change.getSourceLocation());
        }
        if(change.inferType().getTypeCode().isObject()){
            this.change = new Unbox(change).transform();
        }

        if(change instanceof Literal<?> literal){
            switch (this.type){
                case BitAnd: {
                    if (literal.value instanceof Number number && number.doubleValue() == 0) {
                        return literal;
                    }
                }
                case BitOr:{
                    if (literal.value instanceof Number number && number.doubleValue() == 0) {
                        return this.site;
                    }
                }
            }
        }
        return this;
    }

    Expression expr(Expression left, Expression right) throws CompilationError {
        switch (type){
            case BitAnd:
                return new BitOpExpr(BitOpExpr.Type.BitAnd, left, right);
            case BitOr:
                return new BitOpExpr(BitOpExpr.Type.BitOr, left, right);
            case BitXor:
                return new BitOpExpr(BitOpExpr.Type.BitXor, left, right);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    @Override
    public SelfBitOpExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(this.type, this.site, this.change);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SelfBitOpExpr that)) return false;
        return Objects.equals(site, that.site) && Objects.equals(change, that.change) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, change, type);
    }
}
