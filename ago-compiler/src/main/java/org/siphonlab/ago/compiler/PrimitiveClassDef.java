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
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Collections;

import static org.siphonlab.ago.TypeCode.*;

public class PrimitiveClassDef extends ClassDef {

    public final static PrimitiveClassDef INT = new PrimitiveClassDef(TypeCode.INT);
    public final static PrimitiveClassDef VOID = new PrimitiveClassDef(TypeCode.VOID);
    public final static PrimitiveClassDef BOOLEAN = new PrimitiveClassDef(TypeCode.BOOLEAN);
    public final static PrimitiveClassDef CHAR = new PrimitiveClassDef(TypeCode.CHAR);
    public final static PrimitiveClassDef FLOAT = new PrimitiveClassDef(TypeCode.FLOAT);
    public final static PrimitiveClassDef DOUBLE = new PrimitiveClassDef(TypeCode.DOUBLE);
    public final static PrimitiveClassDef BYTE = new PrimitiveClassDef(TypeCode.BYTE);
    public final static PrimitiveClassDef SHORT = new PrimitiveClassDef(TypeCode.SHORT);
    public final static PrimitiveClassDef LONG = new PrimitiveClassDef(TypeCode.LONG);
    public final static PrimitiveClassDef STRING = new PrimitiveClassDef(TypeCode.STRING);
    public final static PrimitiveClassDef CLASS_REF = new PrimitiveClassDef(TypeCode.CLASS_REF);

    private final TypeCode typeCode;

    private ClassDef boxedType;
    private ClassDef boxerInterface;        // lang.Boxer<int> ...
    private Root root;

    public PrimitiveClassDef(TypeCode typeCode) {
        super(typeCode.toString());
        this.typeCode = typeCode;
        this.compilingStage = CompilingStage.Compiled;
    }


    public TypeCode getTypeCode() {
        return typeCode;
    }

    public static PrimitiveClassDef fromTypeCode(TypeCode typeCode){
        if(typeCode == null)
            throw new RuntimeException("typeCode is null");
        return switch (typeCode.value) {
            case BYTE_VALUE -> BYTE;
            case SHORT_VALUE -> SHORT;
            case INT_VALUE -> INT;
            case LONG_VALUE -> LONG;
            case FLOAT_VALUE -> FLOAT;
            case DOUBLE_VALUE -> DOUBLE;
            case CHAR_VALUE -> CHAR;
            case VOID_VALUE -> VOID;
            case BOOLEAN_VALUE -> BOOLEAN;
            case OBJECT_VALUE -> throw new IllegalArgumentException("this class only handle primary type");
            case STRING_VALUE -> STRING;
            case CLASS_REF_VALUE -> CLASS_REF;

            default -> throw new IllegalStateException("Unexpected value: " + typeCode);
        };
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
//            else if (anotherClass.getTypeCode().isHigherThan(getTypeCode())) {        // don't handle this, it's not compatible in runtime, this can make PECS for primitive type, like List<double> <-> List<int>, but the opcode is different, so that's impossible
//                return anotherClass;
//            }
//        } else if (anotherClass == getBoxedType()) {   // boxing is lower than type implicit casting
//            return anotherClass;
//        } else if(anotherClass == getBoxerInterface()){
//            return anotherClass;
        }
        // for the same reason, we cannot support conversion between List<Integer> and List<int>
        // all the implicit conversions are implemented within CastStrategysudo
//        if(this.boxedType != null){
//            var r = this.boxedType.asAssignableFrom(anotherClass);
//            if(r != null) return r;
//        }
//        if(this.boxerInterface != null){
//            return this.boxerInterface.asAssignableFrom(anotherClass);
//        }
        return null;
    }

    public static PrimitiveClassDef fromPrimitiveTypeAst(AgoParser.PrimitiveTypeContext primitiveType){
        var type = switch (primitiveType.start.getType()) {
            case AgoLexer.BOOLEAN -> BOOLEAN;
            case AgoLexer.CHAR -> CHAR;
            case AgoLexer.SHORT -> SHORT;
            case AgoLexer.INT -> INT;
            case AgoLexer.LONG -> LONG;
            case AgoLexer.DOUBLE -> DOUBLE;
            case AgoLexer.FLOAT -> FLOAT;
            case AgoLexer.STRING -> STRING;
            case AgoLexer.BYTE -> BYTE;
            case AgoLexer.VOID -> VOID;
            case AgoLexer.CLASSREF -> CLASS_REF;
            default -> throw new RuntimeException("not supported type " + primitiveType.getText());
        };
        return type;
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
                case CHAR_VALUE -> root.findByFullname("lang.Boxer<char>");
                case BOOLEAN_VALUE -> root.findByFullname("lang.Boxer<boolean>");
                case STRING_VALUE -> root.findByFullname("lang.Boxer<string>");
                //case CLASS_REF_VALUE -> CLASS_REF;
                default -> null;
            };
        }
        return boxerInterface;
    }

    public void setBoxerInterface(ClassDef boxerInterface) {
        this.boxerInterface = boxerInterface;
    }

    public void setPrimitiveInterface(ClassDef primitiveInterface){
        this.setInterfaces(Collections.singletonList(primitiveInterface));
    }

    public static PrimitiveClassDef fromBoxedType(ClassDef boxedType){
        return PrimitiveClassDef.fromTypeCode(boxedType.getUnboxedTypeCode());
    }

    @Override
    public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
        return false;
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        return this;
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
        if(returnExisted!=null) returnExisted.setTrue();
        return this;
    }

    @Override
    public boolean isGenericInstantiateRequiredForNew() {
        return false;
    }

    public boolean isNumber(){
        return typeCode.isNumber();
    }

    public void setRoot(Root root) {
        this.root = root;
    }

    @Override
    public Root getRoot() {
        return root;
    }
}
