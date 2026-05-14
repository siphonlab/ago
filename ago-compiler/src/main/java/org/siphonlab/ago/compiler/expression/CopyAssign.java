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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.statement.BlockStmt;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.ForEachStmt;
import org.siphonlab.ago.compiler.statement.IfThenElseStmt;

import java.util.Collections;
import java.util.List;

public class CopyAssign extends ExpressionInFunctionBody{

    enum Mode{
        ObjectToObject,
        MapToMap,
        ObjectToMap,
        MapToObject
    }
    private final Expression assignee;
    private final Expression value;
    private Mode mode;

    public CopyAssign(FunctionDef ownerFunction, Expression assignee, Expression value) throws CompilationError {
        super(ownerFunction);
        this.assignee = assignee.setParent(this).transform();
        this.value = value.setParent(this).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef assigneeType = assignee.inferType();
        ClassDef valueType = value.inferType();
        boolean aIsMap = getRoot().getAnyReadonlyMap().isThatOrSuperOfThat(assigneeType);
        boolean vIsMap = getRoot().getAnyReadonlyMap().isThatOrSuperOfThat(valueType);
        if(aIsMap && vIsMap){
            mode = Mode.MapToMap;
            mustWritableMap(assigneeType, assignee.getSourceLocation());
            if(!assigneeType.isThatOrSuperOfThat(valueType)){
                throw new TypeMismatchError("'%s' not match to '%s'".formatted(assigneeType, valueType), this.getSourceLocation());
            }
        } else if(aIsMap){
            if(valueType.getTypeCode() != TypeCode.OBJECT){
                throw new TypeMismatchError("an object value expected", value.getSourceLocation());
            }
            mustWritableMap(assigneeType, assignee.getSourceLocation());
            mustBeStringKey(assigneeType, assignee.getSourceLocation());
            mode = Mode.ObjectToMap;
        } else if(vIsMap){
            mode = Mode.MapToObject;
            if(assigneeType.getTypeCode() != TypeCode.OBJECT){
                throw new TypeMismatchError("an object value expected", value.getSourceLocation());
            }
            mustBeStringKey(valueType, assignee.getSourceLocation());
        } else {
            mode = Mode.ObjectToObject;
            if(valueType.getTypeCode() != TypeCode.OBJECT){
                throw new TypeMismatchError("an object value expected", value.getSourceLocation());
            }
            if(assigneeType.getTypeCode() != TypeCode.OBJECT){
                throw new TypeMismatchError("an object value expected", value.getSourceLocation());
            }
        }
        return super.transformInner();
    }

    private void mustWritableMap(ClassDef assigneeType, SourceLocation sourceLocation) throws TypeMismatchError {
        if(!getRoot().getAnyReadwriteMap().isThatOrSuperOfThat(assigneeType)){
            throw new TypeMismatchError("'ReadWriteMap<string, ?>' expected", sourceLocation);
        }
    }

    private void mustBeStringKey(ClassDef assigneeType, SourceLocation sourceLocation) throws TypeMismatchError {
        var t = getRoot().getAnyReadonlyMap().asThatOrSuperOfThat(assigneeType);
        ClassDef classDefValue = t.getGenericSource().typeArguments()[0].getClassDefValue();
        if(!classDefValue.isThatOrBoxOfThat(getRoot().STRING())){
            throw new TypeMismatchError("'ReadOnlyMap<string, ?>' expected", sourceLocation);
        }
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return assignee.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);

            copyAssign(blockCompiler, a, v);

            ownerFunction.assign(localVar,a).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);

            copyAssign(blockCompiler, a, v);

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void copyAssign(BlockCompiler blockCompiler, Var.LocalVar a, Var.LocalVar v) throws CompilationError{
        blockCompiler.lockRegister(a);
        blockCompiler.lockRegister(v);
        switch(mode){
            case MapToMap : mapToMap(blockCompiler, a, v); break;
            case MapToObject: mapToObject(blockCompiler, a, v); break;
            case ObjectToMap: objectToMap(blockCompiler, a, v); break;
            case ObjectToObject: objectToObject(blockCompiler, a, v); break;
        }
        blockCompiler.releaseRegister(v);
        blockCompiler.releaseRegister(a);
    }

    private Invoke getClass(Var.LocalVar v) throws CompilationError {
        return ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, v, getRoot().getObjectClass().findMethod("getClass#")), Collections.emptyList(), sourceLocation).transform();
    }

    private void mapToObject(BlockCompiler blockCompiler, Var.LocalVar a, Var.LocalVar v) throws CompilationError {
        var mapType = getRoot().getAnyReadonlyMap().asThatOrSuperOfThat(v.inferType());

        FunctionDef getKeys = mapType.findMethod("keys#get");
        var invokeGetKeys = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, v, getKeys), Collections.emptyList(), getSourceLocation()).transform();

        var key = blockCompiler.acquireTempVar(getRoot().STRING());
        blockCompiler.lockRegister(key);

        var assigneeClassRef = getClass(a).visit(blockCompiler);
        blockCompiler.lockRegister(assigneeClassRef);

        FunctionDef getProperty = getRoot().getClassRefClass().findMethod("getProperty#pub");
        Invoke invokeGetProperty = ownerFunction.invoke(Invoke.InvokeMode.Invoke,
                ClassUnder.create(ownerFunction, assigneeClassRef, getProperty), List.of(key), sourceLocation).transform();
        var propertyDescN = blockCompiler.acquireTempVar(getProperty.getResultType());      // PropertyDesc?
        blockCompiler.lockRegister(propertyDescN);

        var PropertyDesc = ((NullableClassDef)getProperty.getResultType()).getNullableBaseClass();
        var propertyDesc = blockCompiler.acquireTempVar(PropertyDesc);
        blockCompiler.lockRegister(propertyDesc);
        var nullablePropDesc = new NullableValue(ownerFunction, propertyDescN, propertyDesc);

        var isWritable = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, nullablePropDesc.nonNullValue(), PropertyDesc.findMethod("writable#get")), Collections.emptyList(), sourceLocation).transform();

        var mapGet = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, v, mapType.findMethod("get#key")), List.of(key), sourceLocation).transform();
        var setProp = ownerFunction.invoke(Invoke.InvokeMode.Invoke, new ConstClass(getRoot().findByFullname("lang.prop#set_prop_desc")),
                            List.of(a, propertyDesc, mapGet), sourceLocation).transform();

        new ForEachStmt(ownerFunction, null, key, invokeGetKeys, new BlockStmt(ownerFunction, List.of(
                new ExpressionStmt(ownerFunction, ownerFunction.assign(propertyDescN, invokeGetProperty)),
                new IfThenElseStmt(ownerFunction, nullablePropDesc.isNotNull(),
                        new IfThenElseStmt(ownerFunction, isWritable, new ExpressionStmt(ownerFunction, setProp), null)
                , null)
        )), ForEachStmt.Mode.Iterable, this.sourceLocation).termVisit(blockCompiler);

        blockCompiler.releaseRegister(propertyDesc);
        blockCompiler.releaseRegister(propertyDescN);
        blockCompiler.releaseRegister(assigneeClassRef);
        blockCompiler.releaseRegister(key);
    }

    private void objectToObject(BlockCompiler blockCompiler, Var.LocalVar a, Var.LocalVar v) throws CompilationError {
        var valueClassRef = getClass(v);
        FunctionDef getProperties = getRoot().getClassRefClass().findMethod("properties#get");

        var PropertyDescArray = (ArrayClassDef)getProperties.getResultType();

        var invokeGetProperties = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valueClassRef, getProperties), Collections.emptyList(), getSourceLocation()).transform();

        ClassDef PropertyDesc = PropertyDescArray.getElementType();
        var valuePropertyDesc  = blockCompiler.acquireTempVar(PropertyDesc);
        blockCompiler.lockRegister(valuePropertyDesc);

        var invokePropertyName = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valuePropertyDesc, PropertyDesc.findMethod("name#get")), Collections.emptyList(), sourceLocation).transform();
        var propertyName = blockCompiler.acquireTempVar(getRoot().STRING());
        blockCompiler.lockRegister(propertyName);

        var invokeIsReadable = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valuePropertyDesc, PropertyDesc.findMethod("readable#get")), Collections.emptyList(), sourceLocation).transform();

        var assigneeClassRef = getClass(a).visit(blockCompiler);
        blockCompiler.lockRegister(assigneeClassRef);

        FunctionDef getPropertyInAssignee = getRoot().getClassRefClass().findMethod("getProperty#pub");
        Invoke invokeGetProperty = ownerFunction.invoke(Invoke.InvokeMode.Invoke,
                ClassUnder.create(ownerFunction, assigneeClassRef, getPropertyInAssignee), List.of(propertyName), sourceLocation).transform();
        var propertyDescN = blockCompiler.acquireTempVar(getPropertyInAssignee.getResultType());      // PropertyDesc?
        blockCompiler.lockRegister(propertyDescN);

        var assigneePropertyDesc = blockCompiler.acquireTempVar(PropertyDesc);
        blockCompiler.lockRegister(assigneePropertyDesc);
        var nullablePropDesc = new NullableValue(ownerFunction, propertyDescN, assigneePropertyDesc);

        var isWritable = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, nullablePropDesc.nonNullValue(), PropertyDesc.findMethod("writable#get")), Collections.emptyList(), sourceLocation).transform();

        var getPropOfValue = ownerFunction.invoke(Invoke.InvokeMode.Invoke, new ConstClass(getRoot().findByFullname("lang.prop#get_prop_desc")),
                                List.of(v, valuePropertyDesc), sourceLocation).transform();
        var setProp = ownerFunction.invoke(Invoke.InvokeMode.Invoke, new ConstClass(getRoot().findByFullname("lang.prop#set_prop_desc")),
                                List.of(a, assigneePropertyDesc, getPropOfValue), sourceLocation).transform();

        new ForEachStmt(ownerFunction, null, valuePropertyDesc, invokeGetProperties, new BlockStmt(ownerFunction, List.of(
                new IfThenElseStmt(ownerFunction, invokeIsReadable,
                        new BlockStmt(ownerFunction, List.of(
                                new ExpressionStmt(ownerFunction, ownerFunction.assign(propertyName, invokePropertyName)),
                                new BlockStmt(ownerFunction, List.of(
                                        new ExpressionStmt(ownerFunction, ownerFunction.assign(propertyDescN, invokeGetProperty)),
                                        new IfThenElseStmt(ownerFunction, nullablePropDesc.isNotNull(),
                                                new BlockStmt(ownerFunction, List.of(
                                                        new IfThenElseStmt(ownerFunction, isWritable, new ExpressionStmt(ownerFunction,
                                                                setProp
                                                        ), null)
                                                ))
                                                , null)
                                    )
                                )
                            )
                        )
                        , null)
        )), ForEachStmt.Mode.Array, this.sourceLocation).termVisit(blockCompiler);

        blockCompiler.releaseRegister(assigneePropertyDesc);
        blockCompiler.releaseRegister(propertyDescN);
        blockCompiler.releaseRegister(assigneeClassRef);
        blockCompiler.releaseRegister(propertyName);
        blockCompiler.releaseRegister(valuePropertyDesc);
    }

    private void objectToMap(BlockCompiler blockCompiler, Var.LocalVar a, Var.LocalVar v) throws CompilationError {
        var valueClassRef = getClass(v);
        FunctionDef getProperties = getRoot().getClassRefClass().findMethod("properties#get");

        var PropertyDescArray = (ArrayClassDef)getProperties.getResultType();

        var invokeGetProperties = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valueClassRef, getProperties), Collections.emptyList(), getSourceLocation()).transform();

        ClassDef PropertyDesc = PropertyDescArray.getElementType();
        var valuePropertyDesc  = blockCompiler.acquireTempVar(PropertyDesc);
        blockCompiler.lockRegister(valuePropertyDesc);

        var invokePropertyName = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valuePropertyDesc, PropertyDesc.findMethod("name#get")), Collections.emptyList(), sourceLocation).transform();
        var propertyName = blockCompiler.acquireTempVar(getRoot().STRING());
        blockCompiler.lockRegister(propertyName);

        var invokeIsReadable = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, valuePropertyDesc, PropertyDesc.findMethod("readable#get")), Collections.emptyList(), sourceLocation).transform();

        var mapType = getRoot().getAnyReadwriteMap().asThatOrSuperOfThat(a.inferType());

        var getPropOfValue = ownerFunction.invoke(Invoke.InvokeMode.Invoke, new ConstClass(getRoot().findByFullname("lang.prop#get_prop_desc")),
                List.of(v, valuePropertyDesc), sourceLocation).transform();
        var putValue = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, a, mapType.findMethod("put#")),
                List.of(ownerFunction.cast(propertyName, mapType.getGenericSource().typeArguments()[0].getClassDefValue(), true).transform(),
                        ownerFunction.cast(getPropOfValue, mapType.getGenericSource().typeArguments()[1].getClassDefValue(), true).transform()
                        ), sourceLocation).transform();

        new ForEachStmt(ownerFunction, null, valuePropertyDesc, invokeGetProperties, new BlockStmt(ownerFunction, List.of(
                new IfThenElseStmt(ownerFunction, invokeIsReadable,
                        new BlockStmt(ownerFunction, List.of(
                                new ExpressionStmt(ownerFunction, ownerFunction.assign(propertyName, invokePropertyName)),
                                new BlockStmt(ownerFunction, List.of(
                                        new ExpressionStmt(ownerFunction, putValue)
                                    )
                                )
                            )
                        )
                        , null)
        )), ForEachStmt.Mode.Array, this.sourceLocation).termVisit(blockCompiler);

        blockCompiler.releaseRegister(propertyName);
        blockCompiler.releaseRegister(valuePropertyDesc);
    }


    private void mapToMap(BlockCompiler blockCompiler, Var.LocalVar a, Var.LocalVar v) throws CompilationError {
        ownerFunction.invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(ownerFunction, a, a.inferType().findMethod("putAll#")), List.of(v), sourceLocation).transform().termVisit(blockCompiler);
    }

    @Override
    public CopyAssign setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public CopyAssign setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(CopyAssign %s %s)".formatted(assignee, value);
    }
}
