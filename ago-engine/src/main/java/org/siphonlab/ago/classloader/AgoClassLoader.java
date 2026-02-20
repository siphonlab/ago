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
package org.siphonlab.ago.classloader;

import org.agrona.collections.IntArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.TypeCode.*;

public class AgoClassLoader implements ClassManager{

    private static final Logger LOGGER = LoggerFactory.getLogger(AgoClassLoader.class);

    private final MetaClass theMeta;

    private Map<String, ClassHeader> headers = new TreeMap<>();

    protected List<byte[]> blobs = new ArrayList<>();

    protected Map<String, AgoClass> classByName = new HashMap<>();
    protected List<AgoClass> classes;

    private List<String> strings = new ArrayList<>();       // merged const strings
    private Map<String, Integer> stringTable = new HashMap<>();

    final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    final SlotsCreatorFactory slotsCreatorFactory;
     private LangClasses langClasses;


    public AgoClassLoader(MetaClass theMeta, SlotsCreatorFactory slotsCreatorFactory) {
        this.theMeta = theMeta;
        this.slotsCreatorFactory = slotsCreatorFactory;
    }


    public AgoClassLoader(SlotsCreatorFactory slotsCreatorFactory) {
        this.theMeta = MetaClass.createTheMeta(this);
        this.slotsCreatorFactory = slotsCreatorFactory;
    }

    public AgoClassLoader() {
        this.theMeta = MetaClass.createTheMeta(this);
        this.slotsCreatorFactory = new DefaultSlotsCreatorFactory();
    }

    public SlotsCreatorFactory getSlotsCreatorFactory() {
        return slotsCreatorFactory;
    }

    public MetaClass getTheMeta() {
        return theMeta;
    }

    public void loadClasses(File[] files) throws IOException {
        IoBuffer[] buffers = new IoBuffer[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try(FileInputStream fileInputStream = new FileInputStream(file)) {
                buffers[i] = IoBuffer.wrap(fileInputStream.readAllBytes());
            }
        }
        loadClasses(buffers);
    }

    public void loadClasses(IoBuffer[] buffers) throws IOException {
        // stage: LoadClassNames
        for (IoBuffer buffer : buffers) {
            loadClassNames(buffer);
        }
        // expand children of concrete types
        processStage(LoadingStage.LoadClassNames);

        // ResolveHierarchicalClasses
        processStage(LoadingStage.ResolveHierarchicalClasses);
        // ParseFields
        processStage(LoadingStage.ParseFields);
        // InstantiateFunctionFamily
        processStage(LoadingStage.InstantiateFunctionFamily);
        // ParseCode
        processStage(LoadingStage.ParseCode);
        // BuildClass
        processStage(LoadingStage.BuildClass);
        // CollectMethods
        processStage(LoadingStage.ResolveFunctionIndex);
        for (ClassHeader header : headers.values()) {
            collectMetaAndSuperAndChildren(header);
        }
        collectMethods();
        // collect lang classes
        this.langClasses = new LangClasses(this);
        // the meta
        theMeta.setSuperClass(langClasses.getClassClass());
        theMeta.setMethods(langClasses.getClassClass().getMethods());

        // EnqueueParameterizingClassTask
        enqueueParameterizingClassTasks();
        //
        for (ClassHeader header : headers.values()) {
            buildVariablesAndFunctionBody(header);
        }

        classes = classByName.values().stream().sorted((c1, c2) -> {
            var id1 = headers.get(c1.getFullname()).getClassId();
            var id2 = headers.get(c2.getFullname()).getClassId();
            return id1 - id2;
        }).toList();
        for(var i=0; i<classes.size(); i++){
            assert i == headers.get(classes.get(i).getFullname()).getClassId();
        }
    }

    void processStage(LoadingStage stage){
        int initialSize = headers.size();
        var toSolve = new LinkedList<ClassHeader>();
        toSolve.addAll(headers.values());
        while(true){
            while(!toSolve.isEmpty()){
                ClassHeader classHeader = toSolve.poll();
                if(classHeader.loadingStage.value <= stage.value){
                    boolean r = false;
                    switch (classHeader.loadingStage){
                        case LoadClassNames:    {
                            MutableObject<ClassHeader> createdClass = new MutableObject<>();
                            r = classHeader.processLoadClassName(headers, createdClass);
                            if(createdClass.get() != null){
                                toSolve.add(createdClass.get());
                            }
                        }
                        break;
                        case ResolveHierarchicalClasses:    r = classHeader.resolveHierarchicalClasses(headers); break;
                        case ParseFields:   r = classHeader.parseFields(headers); break;
                        case InstantiateFunctionFamily:   r = classHeader.instantiateFunctionFamily(headers); break;
                        case ParseCode:     r = classHeader.parseCode(headers); break;
                        case BuildClass:    r = (classHeader.buildClass(headers) != null); break;
                        case ResolveFunctionIndex:   resolveFunctionIndex(classHeader); r = true; break;
                    }
                    if(!r || classHeader.loadingStage.value <= stage.value)
                        toSolve.add(classHeader);
                }
            }
            if(headers.size() == initialSize){
                break;
            } else {
                for (ClassHeader header : headers.values()) {
                    if(header.loadingStage.value <= stage.value){
                        toSolve.add(header);
                    }
                }
                if(toSolve.isEmpty()) break;
            }
        }

    }

    private final List<AgoEngine.MetaClassCreatingTask> metaClassCreationQueue = new LinkedList<>();

    private void enqueueParameterizingClassTasks(){
        for (ClassHeader classHeader : headers.values()) {
            AgoClass agoClass = classHeader.agoClass;
            MetaClass metaClass = agoClass.getAgoClass();

            if(agoClass.isGenericTemplate() || agoClass.isInGenericTemplate()){
                classHeader.setLoadingStage(LoadingStage.BuildVariablesAndFunctionBody);
                continue;
            }
            if(classHeader instanceof ParameterizedClassHeader parameterizedClassHeader) {
                var task = new AgoEngine.MetaClassCreatingTask();
                task.setTarget(agoClass);
                task.setMetaClass(metaClass);
                task.setConstructor(metaClass.findMethod(parameterizedClassHeader.constructor));
                task.setArguments(parameterizedClassHeader.arguments);
                metaClassCreationQueue.add(task);
            } else {
                if(metaClass != null && metaClass != theMeta) {
                    var task = new AgoEngine.MetaClassCreatingTask();
                    task.setTarget(agoClass);
                    task.setMetaClass(metaClass);
                    AgoFunction emptyArgsConstructor = metaClass.getEmptyArgsConstructor();
                    if (emptyArgsConstructor != null) {
                        task.setConstructor(emptyArgsConstructor);
                        task.setArguments(new Object[0]);
                        metaClassCreationQueue.add(task);
                    }
                }
            }
            classHeader.setLoadingStage(LoadingStage.BuildVariablesAndFunctionBody);
        }

    }

    private void loadClassNames(IoBuffer buffer) throws IOException{
        int headerEnd = buffer.getInt();
        String sourceFileName = buffer.getPrefixedString(decoder);
        // read all headers
        while(buffer.position() < headerEnd){
            readClassName(buffer, null, sourceFileName);
        }
    }

    private ClassHeader readClassName(IoBuffer buffer, String[] stringsOfParent, String sourceFileName) throws CharacterCodingException {
        byte type = buffer.get();
        int modifiers = buffer.getInt();
        String classFullName = buffer.getPrefixedString(decoder);
        String className;

        if(type == TYPE_METACLASS){
            className = classFullName;
        } else {
            className = extractName(classFullName);
        }
        SourceLocation sourceLocation = readSourceLocation(buffer, sourceFileName);

        // const strings
        int stringPos = buffer.getInt();
        buffer.mark().position(stringPos);

        int stringsSize = buffer.getInt();
        String[] strings;
        if (stringsOfParent != null && type != AgoClass.TYPE_METACLASS)     // metaclass are top classes
            strings = stringsOfParent;
        else
            strings = new String[stringsSize];
        if(stringsOfParent != null && type != AgoClass.TYPE_METACLASS) assert stringsSize == 0;
        if(stringsSize > 0){
            for (int i = 0; i < stringsSize; i++) {
                strings[i] = buffer.getPrefixedString(decoder);
            }
        }
        int stringEnd = buffer.position();
        buffer.position(buffer.markValue());

        // array blob
        int blobCount = buffer.getInt();
        List<byte[]> blobs = new ArrayList<>();
        for (int i = 0; i < blobCount; i++) {
            int length = buffer.getInt();
            byte[] blob = new byte[length];
            buffer.get(blob);
            blobs.add(blob);
        }

        // dependencies
        String[] dependencies = null;
        if(type == TYPE_METACLASS){
            int cnt = buffer.getInt();
            dependencies = new String[cnt];
            for (int i = 0; i < cnt; i++) {
                dependencies[i] = buffer.getPrefixedString(decoder);
            }
        }
        TypeCode enumBasePrimitiveType = null;
        Map<String, Object> enumValues = null;
        if(type == TYPE_ENUM){
            int typeCode = buffer.getInt();
            enumBasePrimitiveType = TypeCode.of(typeCode);
            int size = buffer.getInt();
            enumValues = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = buffer.getPrefixedString(decoder);
                var literal = readLiteral(buffer,strings);
                enumValues.put(key, literal);
            }
        }
        // metaclass
        String metaclass = buffer.getPrefixedString(decoder);

        // body start and end
        int start = buffer.getInt();
        int end = buffer.getInt();

        // superclass
        var superClassId = buffer.getInt();
        String superClass = superClassId == -1 ? null : strings[superClassId];
        // interfaces
        int cnt = buffer.getInt();
        String[] interfaces = new String[cnt];
        for (int i = 0; i < cnt; i++) {
            interfaces[i] = strings[buffer.getInt()];
        }
        // superclass
        var permitClassId = buffer.getInt();
        String permitClass = permitClassId == -1 ? null : strings[permitClassId];
        // concrete types, array type, parameterized class, generic instantiation, generic param type
        cnt = buffer.getInt();
        for (int i = 0; i < cnt; i++) {
            readConcreteType(buffer, strings);
        }
        // generic type params
        cnt = buffer.getInt();
        var genericTypeParamDescs = new GenericTypeDesc[cnt];
        for (int i = 0; i < cnt; i++) {
            String paramName = buffer.getPrefixedString(decoder);
            int ownerClass = buffer.getInt();
            String templateClass = ownerClass == -1 ? classFullName : strings[ownerClass];     // owner template class
            int index = buffer.getInt();                        // index of GenericTypeCode
            int t = buffer.getInt();                            // GenericTypeCode
            String classParamType = strings[buffer.getInt()];   // a SharedGenericTypeParameterClassDef
            var typeCode = new TypeCode(t, paramName);
            genericTypeParamDescs[i] = new GenericTypeDesc(typeCode, classParamType, templateClass, index, paramName);
        }
        // methods
        cnt = buffer.getInt();
        MethodDesc[] methods = new MethodDesc[cnt];
        for (int i = 0; i < cnt; i++) {
            methods[i] = new MethodDesc(strings[buffer.getInt()], strings[buffer.getInt()]);
        }

        ClassHeader header;
        if(type == TYPE_METACLASS){
            var metaClassHeader = new MetaClassHeader(classFullName, type, modifiers, buffer.getSlice(start, end - start), this);
            metaClassHeader.setDependencies(dependencies);
            header = metaClassHeader;
        } else {
            header = new ClassHeader(classFullName, type, modifiers, buffer.getSlice(start, end - start), this);
        }
        header.setRange(start, end);
        header.setName(className);
        header.setSourceLocation(sourceLocation);
        header.setMetaClass(metaclass);
        header.setSuperClass(superClass);
        header.setInterfaces(interfaces);
        header.setPermitClass(permitClass);
        header.setMethods(new ArrayList<>(Arrays.asList(methods)));
        if(type == TYPE_ENUM){
            header.setEnumBasePrimitiveType(enumBasePrimitiveType);
            header.setEnumValues(enumValues);
        }
        header.genericTypeParamDescs = genericTypeParamDescs;
        if(stringsOfParent == null || type == TYPE_METACLASS) {
            header.strings = strings;
        }
        if(!blobs.isEmpty()){
            header.blobOffset = this.blobs.size();
            this.blobs.addAll(blobs);
        }

        if(headers.containsKey(classFullName)){
            throw new RuntimeException(format("class %s already existed", classFullName));
        }
        registerNewClass(header);

        int childrenCount = buffer.getInt();
        ClassHeader[] children = new ClassHeader[childrenCount];
        if(childrenCount > 0){
            for (int i = 0; i < childrenCount; i++) {
                ClassHeader child = readClassName(buffer, strings, sourceFileName);
                if(child instanceof MetaClassHeader) {
                    i--;       // meta of children
                } else {
                    children[i] = child;
                    child.parent = header;
                }
            }
        }
        header.children = new ArrayList<>(Arrays.asList(children));

        assert buffer.position() == stringPos;
        buffer.position(stringEnd);

        return header;
    }

    SourceLocation readSourceLocation(IoBuffer buffer, String sourceFileName){
        int line = buffer.getInt();
        int col = buffer.getInt();
        int length = buffer.getInt();
        int start = buffer.getInt();
        int end = buffer.getInt();
        return new SourceLocation(sourceFileName,line, col, length, start, end);
    }

    public void registerNewClass(ClassHeader header) {
        assert !headers.containsKey(header.fullname);
        header.setClassId(headers.size());
        headers.put(header.fullname, header);
    }

    private static String extractName(String classFullName) {
        int length = classFullName.length();
        int start = 0;
        int depth = 0;
        i_loop:
        for(var i = 0; i< length; i++){
            char c = classFullName.charAt(i);
            if(c == '.'){
                start = i + 1;
            }
            for(var j = i + 1; j < length; j++){
                char cj = classFullName.charAt(j);
                if(cj == '.') {
                    i = j - 1;
                    continue i_loop;
                } else if(cj == '<'){
                    depth ++;
                    for(j++; j < length; j++){
                        cj = classFullName.charAt(j);
                        if(cj == '<') depth ++;
                        if(cj == '>') {
                            depth --;
                            if(depth == 0)
                                break;        // skip <...>
                        }
                    }
                }
            }
            break;
        }
        return classFullName.substring(start);
    }

    private void readConcreteType(IoBuffer buffer, String[] strings) throws CharacterCodingException {
        int kind = buffer.get();
        if(kind == 1){
            String arrayClassName = strings[buffer.getInt()];
            var elementType = readType(buffer, strings);
            headers.computeIfAbsent(arrayClassName, n -> {
                ArrayTypeHeader header = new ArrayTypeHeader(arrayClassName, elementType, this);
                header.setClassId(headers.size());
                return header;
            });
        } else if(kind == 2) {
            readParameterizedClass(buffer, strings);
        } else if(kind == 3) {
            readGenericParameterizedClass(buffer, strings);
        }
    }

    private void readParameterizedClass(IoBuffer buffer, String[] strings) throws CharacterCodingException {
        String fullname = buffer.getPrefixedString(decoder);
        String baseClass = buffer.getPrefixedString(decoder);
        String metaClass = buffer.getPrefixedString(decoder);
        String constructor = buffer.getPrefixedString(decoder);
        int argumentLength = buffer.getInt();
        Object[] arguments = new Object[argumentLength];
        for (int i = 0; i < argumentLength; i++) {
            arguments[i] = readLiteral(buffer,strings);
        }
        if(headers.containsKey(fullname)) return;
        var header = new ParameterizedClassHeader(fullname, baseClass, metaClass, constructor, arguments, this);
        header.setName(extractName(fullname));
        registerNewClass(header);
    }

    public Object readLiteral(IoBuffer buffer, String[] strings){
        var typeCode = of(buffer.getInt());
        return switch (typeCode.value){
                    case VOID_VALUE -> null;
                    case BOOLEAN_VALUE -> (buffer.get() == 1);
                    case CHAR_VALUE -> buffer.getChar();
                    case FLOAT_VALUE -> buffer.getFloat();
                    case DOUBLE_VALUE -> buffer.getDouble();
                    case BYTE_VALUE -> buffer.get();
                    case SHORT_VALUE -> buffer.getShort();
                    case INT_VALUE -> buffer.getInt();
                    case LONG_VALUE -> buffer.getLong();
                    case OBJECT_VALUE -> null;
                    case NULL_VALUE -> null;
                    case STRING_VALUE -> strings[buffer.getInt()];
                    case CLASS_REF_VALUE -> {
                        String className = strings[buffer.getInt()];
                        idOfString(className);
                        yield new ClassRefValue(className);
                    }
                    default -> throw new UnsupportedOperationException("TODO");     //TODO
                };
    }

    private void readGenericParameterizedClass(IoBuffer buffer, String[] strings) throws CharacterCodingException {
        String fullname = buffer.getPrefixedString(decoder);
        String templateClass = buffer.getPrefixedString(decoder);
        int argumentLength = buffer.getInt();
        var arguments = new TypeDesc[argumentLength];
        for (int i = 0; i < argumentLength; i++) {
            arguments[i] = readType(buffer, strings);
        }
        String metaclass = buffer.getPrefixedString(decoder);
        String superclass = buffer.getPrefixedString(decoder);
        String parentClass = buffer.getPrefixedString(decoder);
        int interfaceCount = buffer.getInt();
        String[] interfaces = new String[interfaceCount];
        for (int i = 0; i < interfaceCount; i++) {
            interfaces[i] = buffer.getPrefixedString(decoder);
        }

        if(headers.containsKey(fullname)) return;

        var header = new GenericInstantiationClassHeader.PlaceHolder(fullname, templateClass, arguments, this);
        header.setName(extractName(fullname));
        header.parentClassName = parentClass;
        registerNewClass(header);
        header.setMetaClass(metaclass.isEmpty() ? null : metaclass);
        header.setSuperClass(superclass.isEmpty() ? null : superclass);
        header.setInterfaces(interfaces);
    }

    void parseBody(ClassHeader header) {
        if(header.isBodyParsed()) return;
        var buffer = header.getSlice();
        // slots
        var strings = header.loadStrings();
        int slotCount = buffer.getInt();
        if(slotCount > 0)
        {
            SlotDesc[] slotDescs = parseSlots(buffer, slotCount, strings);
            header.setSlots(slotDescs);
        } else {
            header.setSlots(new SlotDesc[0]);
        }
        // fields
        var filedCount = buffer.getInt();
        var fields = new VariableDesc[filedCount];
        for (int i = 0; i < filedCount; i++) {
            fields[i] = readVariable(buffer, strings, header.sourceFilename);
        }
        header.fields = fields;

        if(header.isFunction()) {
            header.functionResultType = readType(buffer, strings);
            // params
            var paramCount = buffer.getInt();
            var parameters = new VariableDesc[paramCount];
            for (int i = 0; i < paramCount; i++) {
                parameters[i] = readVariable (buffer, strings, header.sourceFilename);
            }
            header.functionParams = parameters;
            if ((header.modifiers & NATIVE) == NATIVE) {
                String entrance = strings[buffer.getInt()];
                header.nativeFunctionEntrance = entrance;
                header.functionResultSlot = buffer.getInt();
            } else {
                // variables
                var variablesCount = buffer.getInt();
                var variables = new VariableDesc[variablesCount];
                for (int i = 0; i < variablesCount; i++) {
                    variables[i] = readVariable(buffer, strings, header.sourceFilename);
                }
                header.functionVariables = variables;
            }
            // switch table
            int switchTableCount = buffer.getInt();
            SwitchTableDesc[] switchTableDescs = new SwitchTableDesc[switchTableCount];
            for (int i = 0; i < switchTableCount; i++) {
                var sw = new SwitchTableDesc();
                switchTableDescs[i] = sw;
                sw.id = i;
                byte type = buffer.get();
                sw.type = type;
                if(type == 1){      // dense
                    IntArrayList data = new IntArrayList();
                    data.addInt(buffer.getInt());       // first key
                    var size = buffer.getInt();
                    for (int j = 0; j < size; j++) {
                        data.addInt(buffer.getInt());   // address
                    }
                    sw.data = data.toIntArray();
                } else if(type == 2){
                    IntArrayList data = new IntArrayList();
                    data.addInt(buffer.getInt());       // default address
                    var size = buffer.getInt();
                    for (int j = 0; j < size; j++) {
                        data.addInt(buffer.getInt());   // key
                        data.addInt(buffer.getInt());   // address
                    }
                    sw.data = data.toIntArray();
                } else {
                    throw new RuntimeException("unknown switch table type %d at %d".formatted(type, buffer.position() - 1));
                }
            }
            header.switchTables = switchTableDescs;
            // try-catch table
            int tryCatchTableCount = buffer.getInt();
            TryCatchItemDesc[] tryCatchItems = new TryCatchItemDesc[tryCatchTableCount];
            for (int i = 0; i < tryCatchTableCount; i++) {
                int begin = buffer.getInt();
                int end = buffer.getInt();
                int handler = buffer.getInt();
                int exceptionCount = buffer.getInt();
                int[] exceptionClasses = new int[exceptionCount];
                for (int j = 0; j < exceptionCount; j++) {
                    exceptionClasses[j] = buffer.getInt();
                }
                tryCatchItems[i] = new TryCatchItemDesc(begin,end,handler,exceptionClasses);
            }
            header.tryCatchItems = tryCatchItems;
            // code
            int length = buffer.getInt();
            header.compiledCode = buffer.getSlice(length);
            if(length > 0){
                header.sourceMap = readSourceMap(buffer, header.sourceFilename);
            }
        }
        header.setBodyParsed(true);
    }

    private SourceMapEntry[] readSourceMap(IoBuffer buffer, String sourceFileName) {
        var size = buffer.getInt();
        SourceMapEntry[] sourceMapEntries = new SourceMapEntry[size];
        for (int i = 0; i < size; i++) {
            int offset = buffer.getInt();
            SourceLocation sourceLocation = readSourceLocation(buffer,sourceFileName);
            sourceMapEntries[i] = new SourceMapEntry(offset,sourceLocation);
        }
        return sourceMapEntries;
    }

    private void buildVariablesAndFunctionBody(ClassHeader header){
        if(header.loadingStage != LoadingStage.BuildVariablesAndFunctionBody)
            return;

        if(header instanceof ParameterizedClassHeader parameterizedClassHeader) {
            buildVariablesAndFunctionBodyForParameterized(parameterizedClassHeader);
            return;
        } else if(header instanceof ArrayTypeHeader arrayTypeHeader){
            buildVariablesAndFunctionBodyForArray(arrayTypeHeader);
            return;
        } else if(header.getSourceHeader() != null && header.genericSource == null){        // a cloner
            if(header.getSourceHeader().getLoadingStage() == LoadingStage.BuildVariablesAndFunctionBody){
                buildVariablesAndFunctionBody(header.getSourceHeader());
            }
            cloneCompiled(header.agoClass, header.getSourceHeader().agoClass);
            header.setLoadingStage(LoadingStage.Done);
            return;
        }

        var agoClass = header.agoClass;

        SlotDesc[] slotDescs = header.slotDescs;
        AgoSlotDef[] slotDefs = new AgoSlotDef[header.slotDescs.length];
        for (int i = 0; i < slotDescs.length; i++) {
            SlotDesc desc = slotDescs[i];
            var type = desc.type();
            var c = type.typeCode == OBJECT ? classByName.get(type.className) : null;
            AgoSlotDef agoSlotDef = new AgoSlotDef(desc.index(), desc.name(), type.typeCode, c);
            slotDefs[i] = agoSlotDef;
            assert i == agoSlotDef.getIndex();
        }
        agoClass.setSlotDefs(slotDefs);

        agoClass.setFields(buildVariables(agoClass, header.fields).map(v -> (AgoField)v).toArray(AgoField[]::new));

        if (!header.isInGenericTemplate(headers)) {   // template class doesn't create slots, but still create fields and function
            agoClass.setSlotsCreator(slotsCreatorFactory.generateSlotsCreator(agoClass));
        }

        if(agoClass instanceof AgoFunction agoFunction){
            agoFunction.setResultType(header.functionResultType.typeCode, classByName.get(header.functionResultType.className));
            agoFunction.setParameters(buildVariables(agoClass, header.functionParams).map(v -> (AgoParameter)v).toArray(AgoParameter[]::new));
            if (agoFunction instanceof AgoNativeFunction nativeFunction) {
                nativeFunction.setResultSlot(header.functionResultSlot);
                generateNativeCaller(nativeFunction);
            } else {
                agoFunction.setVariables(buildVariables(agoClass, header.functionVariables).toArray(AgoVariable[]::new));
            }
            if(!header.isInGenericTemplate(headers)) {
                agoFunction.setCode(new CodeTransformer(this, header, headers).transformCode());
            } else {
                int[] arr = new int[header.compiledCode.remaining() / 4];
                header.compiledCode.duplicate().asIntBuffer().get(arr);
                agoFunction.setCode(arr);
            }
            agoFunction.setSourceMap(header.sourceMap);
            // switch table
            if(header.switchTables != null && header.switchTables.length > 0) {
                agoFunction.setSwitchTables(Arrays.stream(header.switchTables).map(s -> {
                    if (s.type == 1) {
                        return new DenseSwitchTable(s.data);
                    } else {
                        return new SparseSwitchTable(s.data);
                    }
                }).toArray(SwitchTable[]::new));
            }
            // try-catch table
            TryCatchItemDesc[] tryCatchItems = header.tryCatchItems;
            if(tryCatchItems != null && tryCatchItems.length > 0){
                var strings = header.loadStrings();
                TryCatchItem[] items = new TryCatchItem[tryCatchItems.length];
                for (int j = 0; j < tryCatchItems.length; j++) {
                    TryCatchItemDesc tryCatchItemDesc = tryCatchItems[j];
                    int[] exceptionClasses = tryCatchItemDesc.exceptionClasses;
                    var classes = new AgoClass[exceptionClasses.length];
                    for (int i = 0; i < exceptionClasses.length; i++) {
                        int exceptionClass = exceptionClasses[i];
                        classes[i] = headers.get(strings[exceptionClass]).agoClass;
                    }
                    items[j] = new TryCatchItem(tryCatchItemDesc.begin,tryCatchItemDesc.end,tryCatchItemDesc.handler, classes);
                }
                agoFunction.setTryCatchItems(items);
            }
        }

        header.setLoadingStage(LoadingStage.Done);
    }

    private Stream<AgoVariable> buildVariables(AgoClass owner, VariableDesc[] variables){
        if(variables == null) return Stream.of();
        return Arrays.stream(variables).map(v -> {
            var type = v.variableKind;
            var agoClass = v.type.typeCode == OBJECT ? classByName.get(v.type.className) : null;
            var r = switch (type) {
                case Field -> new AgoField(v.name, v.modifiers, v.type.typeCode, agoClass, v.slotIndex, owner, v.constLiteralValue);
                case Parameter -> new AgoParameter(v.name, v.modifiers, v.type.typeCode, agoClass, v.slotIndex, (AgoFunction) owner, v.constLiteralValue);
                case Variable -> new AgoVariable(v.name, v.modifiers, v.type.typeCode, agoClass, v.slotIndex, v.constLiteralValue);
                case null, default -> throw new UnsupportedOperationException();
            };
            r.setSourceLocation(v.getSourceLocation());
            return r;
        }).sorted(Comparator.comparingInt(AgoVariable::getSlotIndex));
    }


    private void cloneCompiled(AgoClass cloner, AgoClass source) {
        if(cloner == source) return;
        cloner.setSlotDefs(ArrayUtils.clone( source.getSlotDefs()));
        cloner.setFields(ArrayUtils.clone(source.getFields()));
        cloner.setSlotsCreator(source.getSlotsCreator());
        if(cloner instanceof AgoFunction clonerFun){
            AgoFunction sourceFun = (AgoFunction) source;
            clonerFun.setCode(sourceFun.getCode());
            clonerFun.setSourceMap(sourceFun.getSourceMap());
            clonerFun.setSwitchTables(sourceFun.getSwitchTables());
            clonerFun.setTryCatchItems(sourceFun.getTryCatchItems());
            clonerFun.setResultType(sourceFun.getResultTypeCode(),sourceFun.getResultClass());
            if(clonerFun instanceof AgoNativeFunction clonerNativeFun){
                AgoNativeFunction sourceNativeFun = (AgoNativeFunction) sourceFun;
                clonerNativeFun.setNativeEntrance(sourceNativeFun.getNativeEntrance());
                clonerNativeFun.setNativeFunctionCaller(sourceNativeFun.getNativeFunctionCaller());
                clonerNativeFun.setResultSlot(sourceNativeFun.getResultSlot());
            }
        }
    }

    private void buildVariablesAndFunctionBodyForParameterized(ParameterizedClassHeader header) {
        if(header.loadingStage != LoadingStage.BuildVariablesAndFunctionBody) return;

        var baseClass = classByName.get(header.baseClass);
        ClassHeader baseHeader = headers.get(header.baseClass);
        if(baseHeader.loadingStage == LoadingStage.BuildVariablesAndFunctionBody){
            buildVariablesAndFunctionBody(baseHeader);
        }
        cloneCompiled(header.agoClass, baseClass);
        header.setLoadingStage(LoadingStage.Done);
    }

    private void buildVariablesAndFunctionBodyForArray(ArrayTypeHeader header) {
        if(header.loadingStage != LoadingStage.BuildVariablesAndFunctionBody) return;

        var s = headers.get(header.getSuperClass());
        if(header != s && s.loadingStage == LoadingStage.BuildVariablesAndFunctionBody){
            buildVariablesAndFunctionBody(s);
        }

        var baseClass = classByName.get(header.getSuperClass());
        if(baseClass != header.agoClass) {
            cloneCompiled(header.agoClass, baseClass);
        }
        header.setLoadingStage(LoadingStage.Done);
    }


    private SlotDesc[] parseSlots(IoBuffer buffer, int slotCount, String[] strings) {
        SlotDesc[] slotDescs = new SlotDesc[slotCount];
        for (int i = 0; i < slotCount; i++) {
            int index = buffer.getInt();
            String name = strings[buffer.getInt()];
            var type = readType(buffer, strings);
            slotDescs[i] = new SlotDesc(index, name, type);
        }
        return slotDescs;
    }

    public SlotDesc[] getSlots(AgoClass agoClass){
        var header = this.headers.get(agoClass.getFullname());
        return header.getSlots();
    }



    protected int idOfString(String string) {
        Integer i = this.stringTable.get(string);
        if(i != null){
            return i;
        }
        int pos = this.strings.size();
        this.stringTable.put(string, pos);
        this.strings.add(string);
        return pos;
    }

    private TypeDesc readType(IoBuffer buff, String[] strings) {
        int typeCodeValue = buff.getInt();
        if(typeCodeValue >= GENERIC_TYPE_START){
            String templateClass = strings[buff.getInt()];
            int parameterIndex = buff.getInt();
            var h = headers.get(templateClass);
            if(h == null) {
                var r = new GenericTypeDesc(templateClass, parameterIndex);
                return r;
            } else {
                return h.genericTypeParamDescs[parameterIndex];
            }
        }
        TypeCode typeCode = of(typeCodeValue);
        if (typeCode == OBJECT) {
            String className = strings[buff.getInt()];
            return new TypeDesc(typeCode, className);
        }
        return new TypeDesc(typeCode, null);
    }

    private VariableDesc readVariable(IoBuffer buff, String[] strings, String sourceFilename){
        var type = buff.get();
        int modifiers = buff.getInt();
        String name = strings[buff.getInt()];
        TypeDesc typeDesc = readType(buff, strings);
        int slotIndex = buff.getInt();
        var vt = switch(type){
            case 1 -> VariableDesc.VariableKind.Variable;
            case 2 -> VariableDesc.VariableKind.Field;
            case 3 -> VariableDesc.VariableKind.Parameter;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        boolean hasConstLiteralValue = (buff.get() != 0);

        Object constLiteralValue;
        if(hasConstLiteralValue){
            constLiteralValue = readLiteral(buff,strings);
        } else {
            constLiteralValue = null;
        }
        SourceLocation sourceLocation = readSourceLocation(buff,sourceFilename);
        return new VariableDesc(name, modifiers, vt, typeDesc, slotIndex, constLiteralValue, sourceLocation);
    }


    protected void generateNativeCaller(AgoNativeFunction nativeFunction) {
        nativeFunction.setNativeFunctionCaller(new NativeCallerGenerator(nativeFunction).generate());
    }

    @Override
    public AgoClass getClass(int classId) {
        return classes.get(classId);
    }

    public AgoClass getClass(String fullname) {
        return classByName.get(fullname);
    }

    public BoxTypes getBoxTypes() {
        return new BoxTypes(this);
    }


    static class GenericLoadingContext{
        final Queue<FailableRunnable<RuntimeException>> delayTasks = new LinkedList<>();
        final Map<String, ClassHeader> headers;

        GenericLoadingContext(Map<String, ClassHeader> headers) {
            this.headers = headers;
        }
        public void put(ClassHeader header){
            if(this.headers.containsKey(header.fullname)){
                return;
            }
            this.headers.put(header.fullname, header);
        }
        public int size(){
            return this.headers.size();
        }

        public ClassHeader get(String fullname) {
            return headers.get(fullname);
        }
    }


    private void collectMetaAndSuperAndChildren(ClassHeader header) {
        var cls = header.agoClass;
        cls.setSuperClass(classByName.get(header.getSuperClass()));
        if(header.getPermitClass() != null) {
            cls.setPermitClass(classByName.get(header.getPermitClass()));
        }
        if(header.getInterfaces() != null) {
            cls.setInterfaces(Arrays.stream(header.interfaces).map(c -> classByName.get(c)).toArray(AgoClass[]::new));
        }
        cls.setChildren(header.children.stream().map(c -> {
            var child = classByName.get(c.fullname);
            child.setParent(cls);
            return child;
        }).toArray(AgoClass[]::new));
    }

    private void resolveFunctionIndex(ClassHeader header){
        if(header.loadingStage != LoadingStage.ResolveFunctionIndex) return;

        ClassHeader superHeader = null;
        if(header.superClass != null && !header.fullname.equals(header.superClass)){
            superHeader = headers.get(header.superClass);
            if(superHeader.loadingStage == LoadingStage.ResolveFunctionIndex)
                resolveFunctionIndex(superHeader);
        }

        if(header instanceof GenericInstantiationClassHeader genericInstantiationClassHeader){
            var templateHeader = genericInstantiationClassHeader.templateClass;
            if(templateHeader.loadingStage == LoadingStage.ResolveFunctionIndex)
                resolveFunctionIndex(templateHeader);
        }
        //TODO for interfaces, the methods is not required
        if(header.methods != null) {
            if(superHeader != null) {
                header.nonPrivateFunctionIndexes.putAll(superHeader.nonPrivateFunctionIndexes);
            }
            // non-private methods
            for (var methodDesc : header.methods) {
                var f = header.findMethod(methodDesc, headers);
                if((f.getVisibility() & PRIVATE) != PRIVATE){   // PUBLIC or PROTECTED
                    var index = header.nonPrivateFunctionIndexes.get(methodDesc.getName());
                    if(index == null){
                        methodDesc.setMethodIndex(header.nonPrivateFunctionIndexes.size());
                        header.nonPrivateFunctionIndexes.put(methodDesc.getName(), methodDesc.getMethodIndex());
                    } else {
                        // inherits
                        assert superHeader != null && Objects.equals(superHeader.nonPrivateFunctionIndexes.get(methodDesc.getName()), index);
                        methodDesc.setMethodIndex(index);
                    }
                }
            }
            // private methods
            int publicMethodIndexEnd = header.nonPrivateFunctionIndexes.size();
            var privateFunctionIndexes = new HashMap<String, Integer>();
            for (var methodDesc : header.methods) {
                var f = header.findMethod(methodDesc, headers);
                if((f.getVisibility() & PRIVATE) == PRIVATE){
                    var index = privateFunctionIndexes.get(methodDesc.getName());
                    if(index == null){
                        methodDesc.setMethodIndex(publicMethodIndexEnd + privateFunctionIndexes.size());
                        privateFunctionIndexes.put(methodDesc.getName(), methodDesc.getMethodIndex());
                    } else {
                        throw new RuntimeException("'%s' duplicated in '%s'".formatted(methodDesc.getName(), header));
                    }
                }
            }
        }
        header.setLoadingStage(LoadingStage.CollectMethods);
    }

    private void collectMethods(){
        for (ClassHeader header : headers.values()) {
            header.collectMethods(headers);
            header.setConcreteTypeInfo(headers);
        }

        for (ClassHeader header : headers.values()) {
            header.buildInterfaceMethodMap(headers);
        }

    }

    public void loadClasses(String directory) throws IOException {
        var dir = new File(directory);
        loadClasses(Objects.requireNonNull(dir.listFiles((dir1, name) -> name.endsWith(".agoc"))));
    }

    public void loadClasses(ZipInputStream packageStream) throws IOException {
        List<IoBuffer> streams = new ArrayList<>();
        ZipEntry entry;

        while ((entry = packageStream.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                packageStream.closeEntry();
                continue;
            }

            streams.add(IoBuffer.wrap(packageStream.readAllBytes()));

            packageStream.closeEntry();
        }
        loadClasses(streams.toArray(new IoBuffer[0]));
    }

    public void loadClasses(String... directory) throws IOException {
        List<File> files = new ArrayList<>();
        for (String d : directory) {
            Collections.addAll(files, Objects.requireNonNull(new File(d).listFiles((dir, name) -> name.endsWith(".agoc"))));
        }
        loadClasses(files.toArray(new File[0]));
    }

    public Map<String, AgoClass> getClassByName() {
        return classByName;
    }

    public List<AgoClass> getClasses() {
        return classes;
    }

    public List<String> getStrings() {
        return strings;
    }

    public List<byte[]> getBlobs() {
        return blobs;
    }

    public List<AgoEngine.MetaClassCreatingTask> getMetaClassCreationQueue() {
        return metaClassCreationQueue;
    }

    public Boxer createBoxer(AgoEngine engine) {
        var boxer = new Boxer(getBoxTypes(), langClasses);
        boxer.setEngine(engine);
        return boxer;
    }

    public LangClasses getLangClasses() {
        return langClasses;
    }
}
