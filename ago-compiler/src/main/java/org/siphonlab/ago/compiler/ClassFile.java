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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.SourceMapEntry;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.generic.GenericConcreteType;
import org.siphonlab.ago.compiler.generic.GenericTypeCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClassFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFile.class);

    Map<ClassDef, Integer> marks = new HashMap<>();

    Set<String> wrote = new HashSet<>();

    private Map<ClassDef, Integer> initialStringSizes = new HashMap<>();

    CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();

    public void writeToStream(ClassDef topClass, OutputStream stream) throws IOException, TypeMismatchError {
        var buff = IoBuffer.allocate(1024).setAutoExpand(true);

        // headers
        int p = buff.position();
        buff.putInt(0);     // end of header
        buff.putPrefixedString(topClass.getSourceLocation().getFilename(),encoder);
        putClassHeader(buff, topClass);
        buff.putInt(p, buff.position());

        // bodies
        putClassBody(buff, topClass);

        Channels.newChannel(stream).write(buff.flip().buf());
    }

    private void putClassHeader(IoBuffer buff, ClassDef classDef) throws CharacterCodingException, TypeMismatchError {
        putMetaHeader(buff, classDef);

        putHeader(buff, classDef);
    }

    private void putHeader(IoBuffer buff, ClassDef classDef) throws CharacterCodingException, TypeMismatchError {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("write class " + classDef.getFullname() + " :" + buff.position());
        buff.put(classDef.getClassType());
        buff.putInt(classDef.getModifiers());
        buff.putPrefixedString(classDef.getFullname(), encoder);
        putSourceLocation(buff, classDef.getSourceLocation());

        if(wrote.contains(classDef.getFullname())){
            throw new RuntimeException();
        }
        wrote.add(classDef.getFullname());

        // const strings
        int stringPos = buff.position();        // write string at the end, here is the bookmark
        buff.putInt(0);

        // array blobs
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    array blobs :" + buff.position());
        List<byte[]> arrayBLOBs = classDef.getArrayBLOBs();
        buff.putInt(arrayBLOBs.size());
        for (byte[] arrayBLOB : arrayBLOBs) {
            buff.put(arrayBLOB);
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("    dependency :" + buff.position());
        if(classDef instanceof MetaClassDef meta) {
            // dependencies
            buff.putInt(meta.getDependencies() == null ? 0 : meta.getDependencies().size());
            for (ClassDef dependency : meta.getDependencies()) {
                buff.putPrefixedString(dependency.getFullname(), encoder);
            }
        }
        if(classDef.isEnum()){
            buff.putInt(classDef.getEnumBasePrimitiveType().getTypeCode().value);
            Map<String, Literal<?>> enumValues = classDef.getEnumValues();
            buff.putInt(enumValues.size());
            for (Map.Entry<String, Literal<?>> entry : enumValues.entrySet()) {
                buff.putPrefixedString(entry.getKey(), encoder);
                putLiteral(entry.getValue(),buff,true,false,classDef);
            }
        }
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    metaclass name :" + buff.position());
        // metaclass name
        if(classDef.getMetaClassDef() != null) {
            buff.putPrefixedString(classDef.getMetaClassDef().getFullname(), encoder);
        } else {
            buff.putPrefixedString("", encoder);
        }
        // placeholder for body start
        marks.put(classDef, buff.position());
        buff.putInt(0);     // placeholder for start
        buff.putInt(0);     // placeholder for end

        if(LOGGER.isDebugEnabled()) LOGGER.debug("    superclass :" + buff.position());
        // super class and interfaces
        if(classDef.getSuperClass() != null) {
            buff.putInt(classDef.idOfClass(classDef.getSuperClass()));
        } else {
            buff.putInt(-1);
        }
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    interfaces :" + buff.position());
        List<ClassDef> interfaces = classDef.getAllInterfaces();
        if(CollectionUtils.isNotEmpty(interfaces)) {
            buff.putInt(interfaces.size());
            for (ClassDef interface_ : interfaces) {
                buff.putInt(classDef.idOfClass(interface_));
            }
        } else {
            buff.putInt(0);
        }
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    permit class :" + buff.position());
        // super class and interfaces
        if(classDef.isInterfaceOrTrait() && classDef.getPermitClass() != null) {
            buff.putInt(classDef.idOfClass(classDef.getPermitClass()));
        } else {
            buff.putInt(-1);
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("    concrete types :" + buff.position());
        Map<String, ConcreteType> concreteTypes;
        if(classDef.getParentClass() == null && !(concreteTypes = classDef.getConcreteTypes()).isEmpty()) {
            var col = new ArrayList<>(concreteTypes.values());
            var stack = new Stack<ConcreteType>();
            stack.addAll(col);
            while(!stack.isEmpty()){
                ClassDef c = (ClassDef) stack.pop();
                if(!c.getConcreteTypes().isEmpty()){
                    for (ConcreteType type : c.getConcreteTypes().values()) {
                        if(!col.contains(type)) {
                            col.add(type);
                            stack.add(type);
                        }
                    }
                }
            }
            buff.putInt(col.size());
            while(!col.isEmpty()) {
                var concreteType = col.removeFirst();
                putConcreteType(classDef, buff, concreteType, col);
            }
        } else {
            buff.putInt(0);
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("    generic type params :" + buff.position());
        var genericTypeParams = classDef.getTypeParamsContext();
        if(classDef.isGenericTemplateOrIntermediate()){
            assert genericTypeParams.size() > 0;
            buff.putInt(genericTypeParams.size());
            for (int i = 0; i < genericTypeParams.size(); i++) {
                GenericTypeCode genericTypeCode = genericTypeParams.get(i);
                buff.putPrefixedString(genericTypeParams.getName(i), encoder);        // param name
                if (classDef == genericTypeCode.getTemplateClass()) {   // owner template class
                    buff.putInt(-1);
                } else {
                    buff.putInt(classDef.idOfKnownClass(genericTypeCode.getTemplateClass()));
                }
                buff.putInt(genericTypeCode.getGenericParamIndex());             // index
                buff.putInt(genericTypeCode.value);        // type code
                buff.putInt(classDef.idOfClass(genericTypeCode.getGenericTypeParameterClassDef()));    // concrete type
            }
        } else {
            buff.putInt(0);
        }

        // methods, include inherited
        var functions = classDef.getUniqueChildren().stream().filter(c -> c instanceof FunctionDef).toList();
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    methods (" + functions.size() +") :" + buff.position());
        buff.putInt(functions.size());
        for (ClassDef function : functions) {
//            int distanceToSuperClass = classDef.distanceToSuperClass((ClassDef) function.getParent());
//            assert distanceToSuperClass != -1;
//            buff.putInt(distanceToSuperClass);
//            buff.putPrefixedString(function.getName(), encoder);
            buff.putInt(classDef.simpleNameOfFunction((FunctionDef) function));
            buff.putInt(classDef.idOfKnownClass(function));
        }

        // children names
        var directChildren = classDef.getDirectChildren();
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    children (" + directChildren.size() +") :" + buff.position());
        if(CollectionUtils.isEmpty(directChildren)){
            buff.putInt(0);
        } else {
            buff.putInt(directChildren.size());
            for (ClassDef child : directChildren) {
                putClassHeader(buff, child);
            }
        }

        // put strings
        buff.putInt(stringPos, buff.position());
        List<String> strings = classDef.getStrings();
        int stringsSize = strings.size();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("    strings (" + stringsSize + "):" + buff.position());
        buff.putInt(stringsSize);
        for (String string : strings) {
            buff.putPrefixedString(string, encoder);
        }
        initialStringSizes.put(classDef, stringsSize);
    }

    private void putSourceLocation(IoBuffer buff, org.siphonlab.ago.SourceLocation sourceLocation) {
        if(sourceLocation == null){
            putSourceLocation(buff, SourceLocation.UNKNOWN);
            return;
        }
        buff.putInt(sourceLocation.getLine());
        buff.putInt(sourceLocation.getColumn());
        buff.putInt(sourceLocation.getLength());
        buff.putInt(sourceLocation.getStart());
        buff.putInt(sourceLocation.getEnd());
    }

    void putMetaHeader(IoBuffer buff, ClassDef classDef) throws CharacterCodingException, TypeMismatchError {
        if(classDef.getMetaClassDef() != null){
            var meta = classDef.getMetaClassDef();
            if(meta.getMetaClassDef() != null){
                putHeader(buff, meta.getMetaClassDef());
            }
            putHeader(buff, meta);
        }
    }

    void putMetaBody(IoBuffer buff, ClassDef classDef) throws IOException, TypeMismatchError {
        if(classDef.getMetaClassDef() != null){
            var meta = classDef.getMetaClassDef();
            if(meta.getMetaClassDef() != null){
                putBody(buff, meta.getMetaClassDef());
            }
            putBody(buff, meta);
        }
    }

    private void putClassBody(IoBuffer buff, ClassDef classDef) throws IOException, TypeMismatchError {
        putMetaBody(buff, classDef);
        putBody(buff, classDef);
    }

    protected void putBody(IoBuffer buff, ClassDef classDef) throws IOException, TypeMismatchError {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("Body of " + classDef.getFullname() + " :" + buff.position());

        buff.putInt(marks.get(classDef), buff.position());
        // slots
        var slots = classDef.getSlotsAllocator().getSlots();
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    slots (" + slots.size() + "):" + buff.position());
        writeSlots(classDef, buff, slots);

        // fields
        var fields = classDef.getFields().values().stream().filter(f -> !(f instanceof Parameter)).toList();
        if(LOGGER.isDebugEnabled()) LOGGER.debug("    fields (" + fields.size() + "):" + buff.position());
        buff.putInt(fields.size());
        for (Field field : fields) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    field (" + field.getName() + "):" + buff.position());
            putVariable(buff, field, classDef);
        }
        if(classDef instanceof FunctionDef functionDef){
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    function type (" + functionDef.getResultType() + "):" + buff.position());
            putType(buff, classDef, functionDef.getResultType());
            // params
            var parameters  = functionDef.getParameters();
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    params (" + parameters.size() + "):" + buff.position());
            buff.putInt(parameters.size());
            for (Parameter parameter : parameters) {
                if(LOGGER.isDebugEnabled()) LOGGER.debug("    param (" + parameter.getName() + "):" + buff.position());
                putVariable(buff, parameter, functionDef);
            }
            if(functionDef.isNative()){
                buff.putInt(classDef.idOfKnownConstString(functionDef.getNativeEntrance()));
                buff.putInt(functionDef.getNativeResultSlot());
            } else {
                // local variables
                var variables = functionDef.getLocalVariables().values();
                buff.putInt(variables.size());
                if(LOGGER.isDebugEnabled()) LOGGER.debug("    local variables (" + variables.size() + "):" + buff.position());
                for (Variable variable : variables) {
                    if(LOGGER.isDebugEnabled()) LOGGER.debug("    variable (" + variable.getName() + "):" + buff.position());
                    putVariable(buff, variable, functionDef);
                }
            }
            // switch tables
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    switch tables :" + buff.position());
            var switchTables = functionDef.getSwitchTables();
            if(switchTables == null){
                buff.putInt(0);
            } else {
                buff.putInt(switchTables.size());
                for (var switchTable : switchTables) {
                    buff.put(switchTable.getComposedBlob());
                }
            }
            // try-catch-table
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    try-catch table:" + buff.position());
            var tryCatchTable = functionDef.getTryCatchTable();
            if(tryCatchTable == null || tryCatchTable.isEmpty()){
                buff.putInt(0);
            } else {
                buff.putInt(tryCatchTable.size());
                buff.put(tryCatchTable.createBlob());
            }
            // code
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    code start:" + buff.position());
            int oldPos = buff.position();
            buff.putInt(0);     // length
            if(functionDef.getBody() != null) {
                for (var code : functionDef.getBody()) {
                    buff.putInt(code);
                }
                buff.putInt(oldPos, buff.position() - oldPos - 4);
                putSourceMap(buff, functionDef.getSourceMap());
            }
            if(LOGGER.isDebugEnabled()) LOGGER.debug("    code end:" + buff.position());
        }

        if(classDef.getStrings().size() > initialStringSizes.get(classDef)){
            throw new RuntimeException("size changed");
        }
        buff.putInt(marks.get(classDef) + 4, buff.position());

        for (ClassDef child : classDef.getDirectChildren()) {
            putClassBody(buff, child);
        }
    }

    private void writeSlots(ClassDef classDef, IoBuffer buff, List<SlotDef> slots) throws CharacterCodingException {
        buff.putInt(slots.size());
        for (SlotDef slot : slots) {
            buff.putInt(slot.getIndex());
            buff.putInt(classDef.idOfKnownConstString(slot.getName()));
            putType(buff, classDef, slot.getClassDef());
        }
    }

    /**
     *
     * @param buff
     * @param variable
     * @param ownerClass ownerClass is not field.getOwnerClass()
     */
    private void putVariable(IoBuffer buff, Variable variable, ClassDef ownerClass) throws CharacterCodingException, TypeMismatchError {
        if(variable instanceof Parameter){
            buff.put((byte) 3);
        } else if(variable instanceof Field){
            buff.put((byte) 2);
        } else {
            buff.put((byte) 1);
        }
        buff.putInt(variable.getModifiers());
        buff.putInt(ownerClass.idOfKnownConstString(variable.getName()));
        putType(buff, ownerClass, variable.getType());
        buff.putInt(variable.getSlot().getIndex());
        if(variable.getConstLiteralValue() != null){
            buff.put((byte)1);
            putLiteral(variable.getConstLiteralValue(),buff,true,false,ownerClass);
        } else {
            buff.put((byte)0);
        }
        putSourceLocation(buff,variable.getSourceLocation());
    }

    private void putType(IoBuffer buff, ClassDef classDef, ClassDef compilingType) throws CharacterCodingException {
        buff.putInt(compilingType.getTypeCode().getValue());
        if(compilingType.getTypeCode() instanceof GenericTypeCode genericTypeCode){
            buff.putInt(classDef.idOfKnownConstString(genericTypeCode.getTemplateClass().getFullname()));
            buff.putInt(genericTypeCode.getGenericParamIndex());
        } else if (compilingType.getTypeCode() == TypeCode.OBJECT) {
            buff.putInt(classDef.idOfKnownConstString(compilingType.getFullname()));
        }
    }


    private void putConcreteType(ClassDef ownerClass, IoBuffer buffer, ConcreteType concreteType, List<ConcreteType> solving) {
        solving.remove(concreteType);
        try {
            if (concreteType instanceof ArrayClassDef arrayClassDef) {
                if (arrayClassDef.getElementType() instanceof ConcreteType ele && solving.contains(ele)) {
                    putConcreteType(ownerClass, buffer, ele, solving);
                }
                buffer.put((byte) 1);
                buffer.putInt(ownerClass.idOfKnownConstString(arrayClassDef.getFullname()));
                putType(buffer, ownerClass,arrayClassDef.getElementType());
            } else if (concreteType instanceof ParameterizedClassDef pc) {
                buffer.put((byte) 2);
                buffer.putPrefixedString(pc.getFullname(), encoder);
                buffer.putPrefixedString(pc.getBaseClass().getFullname(), encoder);
                buffer.putPrefixedString(pc.getBaseClass().getMetaClassDef().getFullname(), encoder);
                buffer.putPrefixedString(pc.getParameterizedConstructor().getName(), encoder);
                buffer.putInt(pc.getArguments().length);
                for (Literal<?> argument : pc.getArguments()) {
                    putLiteral(argument, buffer, true, true, ownerClass);
                }
            } else if (concreteType instanceof GenericConcreteType pc) {
                // superclass and interfaces
                ClassDef classDef = (ClassDef) pc;

                buffer.put((byte) 3);
                buffer.putPrefixedString(pc.getFullname(), encoder);
                buffer.putPrefixedString(classDef.getTemplateClass().getFullname(), encoder);
                ClassRefLiteral[] typeArgumentsArray = classDef.getGenericSource().instantiationArguments().getTypeArgumentsArray();
                buffer.putInt(typeArgumentsArray.length);       // it's different with typeMapping.size
                for (ClassRefLiteral argument : typeArgumentsArray) {
                    putType(buffer, ownerClass, argument.getClassDefValue());
                }
                // metaclass
                if(classDef.getMetaClassDef() != null){
                    buffer.putPrefixedString(classDef.getMetaClassDef().getFullname(), encoder);
                } else {
                    buffer.putPrefixedString("", encoder);
                }
                if(classDef.getSuperClass() != null) {
                    buffer.putPrefixedString(classDef.getSuperClass().getFullname(), encoder);
                } else {
                    buffer.putPrefixedString("", encoder);
                }
                ClassDef parentClass = classDef.getParentClass();
                if(parentClass != null) {
                    buffer.putPrefixedString(parentClass.getFullname(), encoder);
                } else {
                    buffer.putPrefixedString("", encoder);
                }
                var interfaces = classDef.getAllInterfaces();       // put all interfaces include interface of interface
                if(CollectionUtils.isNotEmpty(interfaces)) {
                    buffer.putInt(interfaces.size());
                    for (ClassDef interface_ : interfaces) {
                        buffer.putPrefixedString(interface_.getFullname(), encoder);
                    }
                } else {
                    buffer.putInt(0);
                }
            } else {
                throw new RuntimeException("unknown concrete type " + concreteType.getFullname());
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void putLiteral(Literal<?> literal, IoBuffer buff, boolean withType, boolean inConcreteType, ClassDef ownerClass) throws TypeMismatchError, CharacterCodingException {
        if(withType){
            buff.putInt(literal.getTypeCode().getValue());
        }
        if (literal instanceof IntLiteral intLiteral) {
            buff.putInt(intLiteral.value);
        } else if (literal instanceof BooleanLiteral booleanLiteral) {
            buff.put((byte) (booleanLiteral.value ? 1 : 0)); // boolean 需要转换为 byte 存储
        } else if (literal instanceof ByteLiteral byteLiteral) {
            buff.put(byteLiteral.value);
        } else if (literal instanceof ShortLiteral shortLiteral) {
            buff.putShort(shortLiteral.value);
        } else if (literal instanceof LongLiteral longLiteral) {
            buff.putLong(longLiteral.value);
        } else if (literal instanceof FloatLiteral floatLiteral) {
            buff.putFloat(floatLiteral.value);
        } else if (literal instanceof DoubleLiteral doubleLiteral) {
            buff.putDouble(doubleLiteral.value);
        } else if (literal instanceof CharLiteral charLiteral) {
            buff.putChar(charLiteral.value);
        } else if(literal instanceof StringLiteral stringLiteral) {
            if(inConcreteType){
                buff.putInt(ownerClass.idOfKnownConstString(stringLiteral.getString()));
            } else {
                buff.putInt(stringLiteral.value);
            }
        } else if(literal instanceof ClassRefLiteral classRefLiteral){
            if(inConcreteType) {
                buff.putInt(ownerClass.idOfKnownConstString(classRefLiteral.getClassDefValue().getFullname()));
            } else {
                buff.putInt(classRefLiteral.value);
            }
        } else {
            throw new TypeMismatchError("unexpected literal %s, type %s".formatted(literal, literal.classDef), literal.getSourceLocation());
        }
    }

    private void putSourceMap(IoBuffer buff, List<SourceMapEntry> sourceMap) {
        buff.putInt(sourceMap.size());
        for (SourceMapEntry sourceMapEntry : sourceMap) {
            buff.putInt(sourceMapEntry.codeOffset());
            putSourceLocation(buff,sourceMapEntry.sourceLocation());
        }
    }

    public static void saveToDirectory(Unit[] units, String directory) throws IOException, TypeMismatchError {
        for (Unit unit : units) {
            System.out.println(InspectUtil.inspect(unit.getPackage()));
            for (ClassDef classDef : unit.getTopClasses()) {
                if(classDef instanceof MetaClassDef) continue;  // write metaclass with class

                var file = new File(directory,  URLEncoder.encode(classDef.getFullname(), StandardCharsets.UTF_8) + ".agoc");
                if(!file.getParentFile().exists()) {
                	file.getParentFile().mkdirs();
                }
                if(!file.exists())
                    file.createNewFile();
                try(var stream = new FileOutputStream(file)) {
                    new ClassFile().writeToStream(classDef, stream);
                }
            }
        }
    }

    public static byte[] createBinaryPackage(Unit[] units) throws IOException, TypeMismatchError {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        createPackage(units, baos);

        return baos.toByteArray();
    }

    public static void createPackage(Unit[] units, OutputStream outputStream) throws IOException, TypeMismatchError {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (Unit unit : units) {
                for (ClassDef classDef : unit.getTopClasses()) {
                    if (classDef instanceof MetaClassDef) continue;  // write metaclass with class

                    String filename = URLEncoder.encode(classDef.getFullname(), StandardCharsets.UTF_8) + ".agoc";
                    ZipEntry entry = new ZipEntry(filename);
                    zos.putNextEntry(entry);
                    new ClassFile().writeToStream(classDef, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
