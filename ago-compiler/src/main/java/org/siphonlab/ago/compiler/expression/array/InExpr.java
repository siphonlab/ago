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
package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.statement.*;

import java.util.List;
import java.util.Objects;

public class InExpr extends ExpressionInFunctionBody {

    private Expression value;
    private Expression site;
    private Mode mode;

    enum Mode{
        InArray,
        InCollection,
        InMapKey,
        IsMember
    }

    public InExpr(FunctionDef ownerFunction, Expression value, Expression site) {
        super(ownerFunction);
        this.value = value.setParent(this);
        this.site = site.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getRoot().BOOLEAN();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.value = this.value.transform();
        this.site = this.site.transform();
        if(this.site.inferType() instanceof NullableClassDef){
            if(!(this.site instanceof NullableValue)){
                this.site = new NullableValue(ownerFunction, site);
            }
            this.site = ((NullableValue)site).nonNullPlaceHolder();
        }

        ClassDef siteType = site.inferType();
        ClassDef t;
        ClassDef elementType;
        if((t = getRoot().getAnyArrayClass().asThatOrSuperOfThat(siteType)) != null) {
            mode = Mode.InArray;
            elementType = t.getGenericSource().typeArguments()[0].getClassDefValue();
        }
        if(t == null && (t = getRoot().getReadonlyMapClass().asThatOrSuperOfThat(siteType)) != null){
            mode = Mode.InMapKey;
            elementType = t.getGenericSource().typeArguments()[0].getClassDefValue();
        }
        if(t == null && (t = getRoot().getAnyCollectionClass().asThatOrSuperOfThat(siteType)) != null){
            mode = Mode.InCollection;
            elementType = t.getGenericSource().typeArguments()[0].getClassDefValue();
        } else {
            mode = Mode.IsMember;
            elementType = getRoot().STRING();
        }

        if(value.inferType() instanceof NullableClassDef){
            if(!(elementType instanceof NullableClassDef)){
                Expression v = value;
                if(!(v instanceof NullableValue)){
                    v = new NullableValue(ownerFunction, v);
                }
                this.value = ((NullableValue) v).nonNullPlaceHolder();
            } else {
                this.value = ownerFunction.cast(this.value, elementType).transform();
            }
        } else {
            this.value = ownerFunction.cast(this.value, elementType).transform();

            if(mode == Mode.IsMember && this.value instanceof StringLiteral stringLiteral){
                siteType = this.site.inferType();
                if(!(site instanceof NullableValue.NonNullPlaceHolder) && !(siteType == getRoot().getObjectClass()) && !(site == getRoot().getAnyClass())){
                    return getRoot().createBooleanLiteral(typeContainsMember(this.site.inferType(), stringLiteral.getString())).setSourceLocation(this.getSourceLocation());
                }
            }
        }

        return this;
    }

    private boolean typeContainsMember(ClassDef classDef, String memberName) {
        var v = classDef.getFields().get(memberName);
        if(v != null && v.isPublic()) return true;

        var attr = classDef.getAttribute(memberName);
        if(attr != null && (attr.getSetter().isPublic() || attr.getGetter().isPublic())) return true;

        var method = classDef.findMethod(memberName);
        if(method!= null && method.isPublic()) return true;

        var child = classDef.getChild(memberName);
        if(child != null && child.isPublic()) return true;

        return false;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        try {
            blockCompiler.enter(this);

            Var.LocalVar site;
            Label setFalse = blockCompiler.createLabel();
            Label exit = blockCompiler.createLabel();
            boolean hasNullable = false;
            if(this.site instanceof NullableValue.NonNullPlaceHolder nonNullPlaceHolder){
                NullableValue nullableValue = nonNullPlaceHolder.getNullableValue();
                var n = nullableValue.visit(blockCompiler);
                blockCompiler.lockRegister(n);
                code.jumpIf(nullableValue.isNull().visit(blockCompiler).getVariableSlot(), setFalse);
                site = nullableValue.nonNullValue().visit(blockCompiler);
                blockCompiler.releaseRegister(n);
                hasNullable = true;
            } else {
                site = (Var.LocalVar) this.site.visit(blockCompiler);
            }
            blockCompiler.lockRegister(site);

            TermExpression value;
            if(this.value instanceof NullableValue.NonNullPlaceHolder nonNullPlaceHolder){
                NullableValue nullableValue = nonNullPlaceHolder.getNullableValue();
                var n = nullableValue.visit(blockCompiler);
                blockCompiler.lockRegister(n);
                code.jumpIf(nullableValue.isNull().visit(blockCompiler).getVariableSlot(), setFalse);
                value = nullableValue.nonNullValue().visit(blockCompiler);
                blockCompiler.releaseRegister(n);
                hasNullable = true;
            } else {
                value = this.value.visit(blockCompiler);
            }

            switch (mode){
                case InArray: {
                    var iterVar = blockCompiler.acquireTempVar(value.inferType());
                    new ForEachStmt(ownerFunction, null, iterVar, site,
                            new IfThenElseStmt(ownerFunction,
                                    new Equals(ownerFunction, iterVar, value, Equals.Type.Equals),
                                    new BlockStmt(ownerFunction, List.of(
                                        new ExpressionStmt(ownerFunction, ownerFunction.assign(localVar, getRoot().createBooleanLiteral(true))),
                                        new Goto(ownerFunction, exit)
                                    )),null), ForEachStmt.Mode.Array, this.getSourceLocation()
                    ).termVisit(blockCompiler);
                    ownerFunction.assign(localVar, getRoot().createBooleanLiteral(false)).termVisit(blockCompiler);
                } break;
                case InCollection:
                    ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, site, site.inferType().findMethod("contains#")), List.of(value), this.getSourceLocation())
                            .transform().outputToLocalVar(localVar, blockCompiler);
                    break;
                case InMapKey:
                    ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, site, site.inferType().findMethod("containsKey#")), List.of(value), this.getSourceLocation())
                            .transform().outputToLocalVar(localVar, blockCompiler);
                    break;
                case IsMember:
                    if(value instanceof Var.LocalVar v) {
                        code.contains_member(localVar.getVariableSlot(), site.getVariableSlot(), v.getVariableSlot());
                    } else {
                        code.contains_member(localVar.getVariableSlot(), site.getVariableSlot(), ((StringLiteral)value).value);
                    }
                    break;
            }

            if(hasNullable){
                code.jump(exit);
                setFalse.here();
                ownerFunction.assign(localVar, getRoot().createBooleanLiteral(false)).termVisit(blockCompiler);
            }
            exit.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }


    @Override
    public String toString() {
        return "(DynContains %s %s)".formatted(value, site);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InExpr inExpr = (InExpr) o;
        return Objects.equals(value, inExpr.value) && Objects.equals(site, inExpr.site) && mode == inExpr.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, site, mode);
    }
}
