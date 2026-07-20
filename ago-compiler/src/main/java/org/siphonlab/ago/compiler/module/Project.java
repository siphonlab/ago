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
package org.siphonlab.ago.compiler.module;

import org.antlr.v4.runtime.CharStreams;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.module.Module;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.array.ArrayLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.DecimalLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.generic.GenericConcreteType;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.siphonlab.ago.compiler.ClassFile.putLiteral;

public class Project extends Module {
    private List<Unit> units = new ArrayList<>();

    private final Root root = new Root(this);

    protected Map<String, Integer> stringTable = new HashMap<>();
    protected List<String> strings = new ArrayList<>();
    private Map<Object, Integer> blobsIndex = new HashMap<>();
    private List<byte[]> blobs = new ArrayList<>();

    // class fullname -> ConcreteType
    protected Map<String, ConcreteType> concreteTypes = new LinkedHashMap<>();

    public List<Unit> getUnits() {
        return units;
    }

    public void setUnits(List<Unit> units) {
        this.units = units;
    }

    public void registerConcreteType(ConcreteType concreteType) {
        if(concreteTypes.containsKey(concreteType.getFullname())) return;
        this.idOfClass((ClassDef) concreteType);
        // this.addDependency((ClassDef) concreteType);
//        for (ClassDef concreteDependencyClass : concreteType.getConcreteDependencyClasses()) {
//            this.addDependency(concreteDependencyClass);
//        }
//        concreteType.acceptRegisterConcreteType(this);

        concreteTypes.put(concreteType.getFullname(), concreteType);

//        if(concreteType instanceof GenericConcreteType genericConcreteType){
//            var temp = ((ClassDef) concreteType).getTemplateClass();
//            for(ClassDef p = temp; p != null; p = p.getParent() instanceof ClassDef p2 ? p2 : null){
//                if(p instanceof ConcreteType c) {
//                    registerConcreteType(c);
//                }
//            }
//        }
//
//        if(concreteType instanceof ParameterizedClassDef p){
//            for (Literal<?> arg : p.getArguments()) {
//                if(arg instanceof ClassRefLiteral r && r.getClassDefValue() instanceof ConcreteType cr){
//                    registerConcreteType(cr);
//                }
//            }
//        }
//        else
//            if(concreteType instanceof GenericConcreteType) {
//            for (ClassRefLiteral r : ((ClassDef) concreteType).getGenericSource().typeArguments()) {
//                if (r.getClassDefValue() instanceof ConcreteType cr) {
//                    registerConcreteType(cr);
//                }
//            }
//        }
//        } else if(concreteType instanceof ArrayClassDef arrayClassDef){
//            if(arrayClassDef.getElementType() instanceof ConcreteType ce){
//                registerConcreteType(ce);
//            }
//        } else if(concreteType instanceof NullableClassDef nullableClassDef){
//            if(nullableClassDef.getBaseClass() instanceof ConcreteType nv){
//                registerConcreteType(nv);
//            }
//        }
    }

    public int getOrCreateBLOB(List<? extends Literal<?>> literals, ArrayLiteral arrayLiteral) throws TypeMismatchError {
        var existed = blobsIndex.get(arrayLiteral);
        if(existed != null) return existed;

        var buff = IoBuffer.allocate(512).setAutoExpand(true);
        buff.putInt(0);     // length placeholder
        TypeCode prev = literals.get(0).getTypeCode();
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        for (Literal<?> literal : literals) {
            if(literal.getTypeCode() != prev) {
                throw new TypeMismatchError("literal type mismatch with %s".formatted(prev), literal.getSourceLocation());
            }
            try {
                if(literal instanceof StringLiteral stringLiteral){
                    this.idOfConstString(stringLiteral.getString());
                } else if(literal instanceof ClassRefLiteral classRefLiteral){
                    this.idOfClass(classRefLiteral.getClassDefValue());
                }
                putLiteral(literal, buff, false, false, this);
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }
        }
        buff.flip();
        buff.putInt(0, buff.limit() - 4);
        byte[] data = new byte[buff.limit()];
        buff.get(data);
        int index = blobs.size();
        blobsIndex.put(arrayLiteral, index);
        blobs.add(data);
        return index;
    }

    public int getOrCreateBLOB(DecimalLiteral literal) throws TypeMismatchError {
        var existed = blobsIndex.get(literal);
        if(existed != null) return existed;

        var arr = literal.toArray();
        int index = blobs.size();
        blobsIndex.put(literal, index);
        blobs.add(arr);
        return index;
    }

    public List<byte[]> getBlobs() {
        return blobs;
    }

    public int idOfClass(ClassDef classDef) {
        var id = this.stringTable.get(classDef.getFullname());
        if(id != null) return id;

//        if (classDef.isPrimitive()) throw new UnsupportedOperationException(classDef + " is primary type");

        id = idOfConstString(classDef.getFullname());
//        if (classDef instanceof ConcreteType c) {
//            for (ClassDef concreteDependencyClass : c.getConcreteDependencyClasses()) {
//                this.idOfClass(concreteDependencyClass);
//            }
//            if (c instanceof GenericConcreteType genericConcreteType) {
//                for (ClassRefLiteral typeArgument : genericConcreteType.getGenericInstantiate().getTypeArguments()) {
//                    if (typeArgument.getClassDefValue().getTypeCode() instanceof GenericTypeCode genericTypeCode) {
//                        this.idOfClass(genericTypeCode.getTemplateClass());
//                    }
//                }
//            }
//        }
        return id;
    }

    public int idOfConstString(String s){
        Integer i = this.stringTable.get(s);
        if(i != null){
            return i;
        }
        int pos = this.strings.size();
        this.stringTable.put(s, pos);
        this.strings.add(s);
        return pos;
    }

    public int idOfKnownConstString(String s){
//        return idOfConstString(s);
//        if(this.parent != null && this.parent instanceof ClassDef c){
//            return c.idOfKnownConstString(s);
//        }
        Integer i = this.stringTable.get(s);
        if(i != null){
            return i;
        }
        throw new IndexOutOfBoundsException(s + " not existed");
    }

    public int idOfKnownClass(ClassDef classDef) {
        return idOfKnownConstString(classDef.getFullname());
    }

    public List<String> getStrings() {
        return strings;
    }

    public Map<String, ConcreteType> getConcreteTypes() {
        return concreteTypes;
    }

    public void loadUnits(UnitSource[] unitSources) throws IOException {
        for (int i = 0; i < unitSources.length; i++) {
            UnitSource unitSource = unitSources[i];
            var unit = new Unit(unitSource.getFileName(), CharStreams.fromReader(unitSource.getReader()), root);
            unit.setModule(this);
            this.units.add(unit);
        }
    }
    public void importClasses(ClassDef[] classDefs) throws IOException {
        for (Unit unit : this.units) {
            for(var classDef : classDefs){
                unit.importClass(classDef);
            }
        }
    }

    public Root getRoot() {
        return root;
    }
}
