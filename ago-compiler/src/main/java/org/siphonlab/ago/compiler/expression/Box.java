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


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Box only created by cast, don't create it by yourself, use Cast(int, obj) instead
//
public class Box extends ExpressionInFunctionBody{

    private final Expression expression;
//    private final ClassDef expectedType;
    private final BoxMode boxMode;
    private ClassDef boxType;

    public enum BoxMode{
        Box,
        BoxEnum,
        ForceBox
    }

    public Box(FunctionDef ownerFunction, Expression expression, ClassDef expectedType, BoxMode boxMode) throws CompilationError {
        super(ownerFunction);
        this.expression = expression.transform();
        this.boxType = expectedType;
        this.boxMode = boxMode;

        this.setParent(expression.getParent());
        expression.setParent(this);
        this.setSourceLocation(expression.getSourceLocation());
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(boxMode == BoxMode.Box){
            if(boxType.isTop()){
                return this;
            } else if(boxType.getParent() instanceof MetaClassDef metaClassDef){
                for (FunctionDef constructor : metaClassDef.getConstructors()) {
                    if(!constructor.getParameters().isEmpty()){
                        throw new TypeMismatchError("box type must be top class or child of no-arguments metaclass", this.getSourceLocation());
                    }
                }
            } else {
                throw new TypeMismatchError("box type must be top class or child of no-arguments metaclass", this.getSourceLocation());
            }
        }
        return this;
    }

    //    @Override
//    protected Expression transformInner() throws CompilationError {
//        var type = expression.inferType();
//        if(type instanceof PrimitiveClassDef primitiveClassDef) {
//            this.boxType = primitiveClassDef.getBoxedType();
//            if (this.boxType != this.expectedType) {
//                boolean castToSuper = (boxType.isDerivedFrom(expectedType));
//                boolean boxToDown = (this.expression instanceof ClassRefLiteral && expectedType.isDerivedFrom(boxType));
//                if (boxToDown) {
//                    this.boxType = this.expectedType;
//                } else if (castToSuper) {
//                    return ownerFunction.cast(new Box(this.expression, this.boxType), expectedType).setSourceLocation(this.getSourceLocation()).transform();
//                } else if (this.expectedType.isDerivedFrom(boxType)) {
//                    this.boxType = this.expectedType;
//                } else if (this.expectedType.isEnum()) {
//                    var enumDef = this.expectedType;
//                    if (type == enumDef.getEnumBasePrimitiveType()) {
//                        this.boxType = expectedType;
//                        return this;
//                    } else if (enumDef.getEnumBasePrimitiveType().getTypeCode().isHigherThan(type.getTypeCode())) {
//                        return new Box(ownerFunction.cast(this.expression, enumDef.getEnumBasePrimitiveType()).transform(), enumDef);
//                    }
//                } else {
//                    throw new TypeMismatchError("'%s' cannot cast to '%s'".formatted(this.boxType, expectedType), this.getSourceLocation());
//                }
//            }
//        } else if(type instanceof GenericTypeCode.GenericCodeAvatarClassDef a) {
//            if (expectedType != type.getRoot().getObjectClass()) {
//                throw new TypeMismatchError("'%s' can only box to Object".formatted(expression, type.getFullname()), getSourceLocation());
//            } else {
//                this.boxType = type.getRoot().getObjectClass();
//            }
//        } else if (type.isThatOrDerivedFromThat(type.getRoot().getPrimitiveTypeInterface())) {
//            if (expectedType != type.getRoot().getObjectClass()) {
//                throw new TypeMismatchError("'%s' can only box to Object".formatted(expression, type.getFullname()), getSourceLocation());
//            } else {
//                this.boxType = type.getRoot().getObjectClass();
//            }
//        } else {
//            throw new TypeMismatchError("the type of %s is %s, it's not primitive".formatted(expression, type.getFullname()), getSourceLocation());
//        }
//        return this;
//    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return boxType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var c = this.expression.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            boolean boxToDescendant;
            if(boxMode == BoxMode.Box) {
                PrimitiveClassDef sourceType = (PrimitiveClassDef) this.expression.inferType();
                boxToDescendant = sourceType.getBoxedType() != this.boxType;
            } else {
                boxToDescendant = false;
            }
            if (c instanceof Literal<?> literal) {
                switch (this.boxMode){
                    case Box:
                        if (literal instanceof ClassRefLiteral classRefLiteral && this.boxType != PrimitiveClassDef.CLASS_REF.getBoxedType()) {
                            Var.LocalVar scopeVar = null;
                            if(boxType.isThatOrDerivedFromThat(boxType.getRoot().getScopedClassInterval())){
                                if(classRefLiteral.getScope() != null){
                                    Expression scope = classRefLiteral.getScope();
                                    scopeVar = (Var.LocalVar) scope.visit(blockCompiler);
                                }
                            }
                            literal.visit(blockCompiler);
                            code.box_classref(localVar.getVariableSlot(), literal, blockCompiler.getFunctionDef().idOfClass(this.boxType));
                            if(scopeVar != null){
                                var field = boxType.getRoot().getScopedClassInterval().getVariable("scope");
                                ownerFunction.assign(new Var.Field(ownerFunction, localVar,field),scopeVar).termVisit(blockCompiler);
                            }
                        } else {
                            blockCompiler.getFunctionDef().idOfClass(this.boxType);
                            if(!boxToDescendant) {
                                code.box(localVar.getVariableSlot(), literal);
                            } else {
                                runCreator(localVar, literal, blockCompiler);
                            }
                        }
                        break;
                    case BoxEnum:
                        var enumDef = boxType;
                        var fld = enumDef.resolveEnumField(literal);
                        if (fld == null) {
                            throw new ResolveError("value '%d' not found in enum '%s'".formatted(literal, enumDef.getFullname()), this.getSourceLocation());
                        }
                        ownerFunction.assign(localVar, Var.of(ownerFunction, new ConstClass(boxType), fld)).termVisit(blockCompiler);
                        break;
                    case ForceBox:
                        var temp = blockCompiler.acquireTempVar(this);
                        ownerFunction.assign(temp, literal).termVisit(blockCompiler);
                        code.box_any(localVar.getVariableSlot(), temp.getVariableSlot());
                        break;
                }
            } else {
                var v = (Var.LocalVar) c;
                ClassDef srcType = v.inferType();
                switch (this.boxMode) {
                    case Box:
                        if (srcType == PrimitiveClassDef.CLASS_REF && boxType != PrimitiveClassDef.CLASS_REF.getBoxedType()) {
                            code.box_classref(localVar.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(boxType));
                        } else {
                            blockCompiler.getFunctionDef().idOfClass(this.boxType);
                            if(!boxToDescendant) {
                                code.box(localVar.getVariableSlot(), v.getVariableSlot());
                            } else {
                                blockCompiler.lockRegister(v);
                                runCreator(localVar, v, blockCompiler);
                                blockCompiler.releaseRegister(v);
                            }
                        }
                        break;
                    case BoxEnum:
                        ClassUnder valueOf = ClassUnder.create(ownerFunction, new ConstClass(boxType), boxType.getMetaClassDef().getChild("valueOf#"));
                        ownerFunction.invoke(Invoke.InvokeMode.Invoke, valueOf, List.of(v), null).transform().outputToLocalVar(localVar, blockCompiler);
                        break;
                    case ForceBox:
                        code.box_any(localVar.getVariableSlot(), v.getVariableSlot());
                        break;
                }


//                if (srcType == PrimitiveClassDef.CLASS_REF && boxType != PrimitiveClassDef.CLASS_REF.getBoxedType()) {
//                    code.box_classref(localVar.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(boxType));
//                } else if (boxType.isEnum()) {
//                    // call Enum.valueOf
//                    ClassUnder valueOf = ClassUnder.create(new ConstClass(boxType), boxType.getMetaClassDef().getChild("valueOf#"));
//                    new Invoke(Invoke.InvokeMode.Invoke, valueOf, List.of(v), null).transform().outputToLocalVar(localVar, blockCompiler);
//                } else if(srcType instanceof PrimitiveClassDef){
//                    blockCompiler.getFunctionDef().idOfClass(this.boxType);
//                    code.box(localVar.getVariableSlot(), v.getVariableSlot());
//                } else if(srcType.isThatOrDerivedFromThat(boxType.getRoot().getPrimitiveTypeInterface())) {
//                    blockCompiler.getFunctionDef().idOfClass(this.boxType);
//                    code.box_any(localVar.getVariableSlot(), v.getVariableSlot());
//                } else if(srcType instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
//                    if(a.getLBoundClass() instanceof PrimitiveClassDef){
//                        blockCompiler.getFunctionDef().idOfClass(this.boxType);
//                        code.box(localVar.getVariableSlot(), v.getVariableSlot());
//                    } else if(a.getLBoundClass().isThatOrDerivedFromThat(boxType.getRoot().getPrimitiveTypeInterface())){
//                        blockCompiler.getFunctionDef().idOfClass(this.boxType);
//                        code.box_any(localVar.getVariableSlot(), v.getVariableSlot());
//                    } else {
//                        throw new RuntimeException("unsupported type '%s' to box".formatted(srcType.getFullname()));
//                    }
//                } else {
//                    throw new RuntimeException("unsupported type '%s' to box".formatted(srcType.getFullname()));
//                }
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private void runCreator(Var.LocalVar target, Expression value, BlockCompiler blockCompiler) throws CompilationError {
        List<Expression> arguments = Collections.singletonList(value);
        if(boxType.isTop()) {
            new Creator(ownerFunction, new ConstClass(this.boxType), arguments, getSourceLocation())
                    .outputToLocalVar(target, blockCompiler);
        } else {
            MetaClassDef metaClassDef = (MetaClassDef) boxType.getParent();
            ClassUnder classUnder = ClassUnder.create(ownerFunction, new ConstClass(metaClassDef.getInstanceClassDef()), boxType);
            new Creator(ownerFunction, classUnder, arguments, getSourceLocation())
                    .outputToLocalVar(target, blockCompiler);
        }
    }

    @Override
    public String toString() {
//        if(this.expectedType != this.boxType){
            return "(Box %s to %s)".formatted(expression, this.boxType);
//        } else {
//          return "(Box %s)".formatted(expression);
//        }
    }

    @Override
    public Box setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Box box)) return false;
        return Objects.equals(expression, box.expression) && Objects.equals(boxType, box.boxType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, boxType);
    }
}
