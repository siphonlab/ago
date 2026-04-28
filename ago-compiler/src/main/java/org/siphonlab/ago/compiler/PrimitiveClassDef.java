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
package org.siphonlab.ago.compiler;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Set;

import static org.siphonlab.ago.TypeCode.*;

public class PrimitiveClassDef extends ClassDef {

    private final TypeCode typeCode;

    private ClassDef boxedType;
    private ClassDef boxerInterface;        // lang.Boxer<int> ...

    public PrimitiveClassDef(Root root, TypeCode typeCode) {
        super(root, typeCode.toString());
        this.typeCode = typeCode;
        this.classType = AgoClass.TYPE_PRIMITIVE_CLASS;
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
    }

    void setClassDeclaration(AgoParser.ClassDeclarationContext classDeclaration){
        this.classDeclaration = classDeclaration;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        super.resolveHierarchicalClasses();
        this.setCompilingStage(CompilingStage.Compiled);
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass) {
        if(this == anotherClass) return this;

        if (anotherClass.isPrimitive()) {
            if (this.getTypeCode() == anotherClass.getTypeCode()) {
                return anotherClass;
            } else {
                return null;
            }
        }
        return null;
    }

    public ClassDef getBoxedType() {
        return boxedType;
    }

    public void setBoxedType(ClassDef boxedType) {
        this.boxedType = boxedType;
    }

    public ClassDef getBoxerInterface() {
        if(this.boxerInterface == null){
            this.boxerInterface = switch (typeCode.value) {
                case BYTE_VALUE -> root.findByFullname("lang.Boxer<byte>");
                case SHORT_VALUE -> root.findByFullname("lang.Boxer<short>");
                case INT_VALUE -> root.findByFullname("lang.Boxer<int>");
                case LONG_VALUE -> root.findByFullname("lang.Boxer<long>");
                case FLOAT_VALUE -> root.findByFullname("lang.Boxer<float>");
                case DOUBLE_VALUE -> root.findByFullname("lang.Boxer<double>");
                case DECIMAL_VALUE -> root.findByFullname("lang.Boxer<decimal>");
                case CHAR_VALUE -> root.findByFullname("lang.Boxer<char>");
                case BOOLEAN_VALUE -> root.findByFullname("lang.Boxer<boolean>");
                case STRING_VALUE -> root.findByFullname("lang.Boxer<string>");
                //case CLASS_REF_VALUE -> CLASS_REF;
                default -> null;
            };
        }
        return boxerInterface;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        return false;
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) {
        return this;
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
        if(returnExisted!=null) returnExisted.setTrue();
        return this;
    }

    @Override
    public ClassDef instantiateAsReferenceClass(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return instantiate(arguments, returnExisted);
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return true;
    }

    public boolean isNumber(){
        return typeCode.isNumber();
    }

    @Override
    public Root getRoot() {
        return root;
    }
}
