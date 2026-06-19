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

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Equals;
import org.siphonlab.ago.compiler.expression.Literal;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.IdentityHashSet;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.array.ArrayLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.DecimalLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.generic.*;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.siphonlab.collection.DuplicatedKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.compiler.ClassFile.putLiteral;

public class ClassDef extends ClassContainer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassDef.class);

    protected final Root root;

    private MetaClassDef metaClassDef;

    protected AgoParser.ClassDeclarationContext classDeclaration;

    protected SourceLocation sourceLocation;
    protected String version;
    protected byte classType;
    protected int modifiers;
    protected ClassDef superClass;
    protected List<ClassDef> implementedInterfaces = new ArrayList<>();
    // all wrapper interfaces will implemented as `fun xx(){return field.xx()}`
    protected Map<ClassDef, AgoParser.IdentifierContext> wrapperInterfaces = new LinkedHashMap<>();
    // auto create a field for a trait, in ParseField stage
    protected Map<ClassDef, Field> traitFields = new LinkedHashMap<>();


    // put fields and static fields together, but there SlotsAllocator are different
    // and fields of super class copied at here too
    protected Map<String, Field> fields = new LinkedHashMap<>();

    protected final SlotsAllocator slotsAllocator;

    protected ConstructorDef constructor;

    protected Map<String, Integer> stringTable = new HashMap<>();
    protected List<String> strings = new ArrayList<>();

    // class fullname -> ConcreteType
    protected Map<String, ConcreteType> concreteTypes = new LinkedHashMap<>();

    protected Set<ClassDef> dependencies = new HashSet<>();
    protected Unit unit;

    protected CompilingStage compilingStage = CompilingStage.ParseClassName;

    // generic type params of template class and its inner classes, may inherit from the outer template class
    protected TypeParamsContext typeParamsContext;
    // instantiations of this template/template-inner class via different InstantiationArguments
    private Map<InstantiationArguments, ClassDef> instantiationsCache;
    // for instantiation, record the original templates and type arguments
    private GenericSource genericSource;
    private Set<InstantiationArguments> instantiatingChildren = new HashSet<>();

    // for Interface
    protected ClassDef permitClass;
    // we need put this field here for instantiated trait
    protected Field fieldForPermitClass;
    private Map<String, GetterSetterPair> attributes = new HashMap<>();

    private NamespaceCollection<FunctionDef> extensionMethods = new NamespaceCollection<>(false);

    public ClassDef(Root root, String name) {
        super(name);
        this.root = root;
        this.classType = AgoClass.TYPE_CLASS;
        slotsAllocator = new SlotsAllocator(this);
    }

    public ClassDef(Root root, String name, AgoParser.ClassDeclarationContext classDeclaration) {
        this(root, name);
        this.classDeclaration  = classDeclaration;
        this.classType = AgoClass.TYPE_CLASS;
    }

    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST(){
        return classDeclaration.genericTypeParameters();
    }

    public ParserRuleContext getDeclarationAst(){
        return classDeclaration;
    }

    public ParserRuleContext getDeclarationName(){
        return classDeclaration.className;
    }

    public AgoParser.DeclarationTypeContext getBaseTypeDecl(){
        if(classDeclaration == null) return null;
        return classDeclaration.extendsPhrase() != null ? classDeclaration.extendsPhrase().baseType : null;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public CompilingStage getCompilingStage() {
        return compilingStage;
    }

    public void setCompilingStage(CompilingStage compilingStage) {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s set compiling stage to %s".formatted(this, compilingStage));
        this.compilingStage = compilingStage;
    }

    public void nextCompilingStage(CompilingStage expected){
        this.setCompilingStage(compilingStage.nextStage());
        assert this.compilingStage == expected;
    }

    /**
     * add field when parsing fields/parameters
     * @param field
     */
    public void addField(Field field) {
        this.fields.put(field.name, field);
        field.setOwnerClass(this);
        this.idOfConstString(field.name);       // update const pool
        if (field.getType().getTypeCode() == TypeCode.OBJECT) {
            if(field.getType() instanceof ConcreteType c){
                this.registerConcreteType(c);
            } else {
                this.idOfClass(field.getType());
            }
        }
    }

//    public void addField(Field field) {
//        this.fields.put(field.name, field);
//        field.setOwnerClass(this);
//        this.idOfConstString(field.name);       // update const pool
//        if (field.getType().getTypeCode() == TypeCode.OBJECT) {
//            this.idOfConstString(field.getType().getFullname());
//        }
//    }

    public void allocateSlotsForFields() throws CompilationError {
        if(this.compilingStage != CompilingStage.AllocateSlots) return;
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: allocate slots".formatted(this));

        if(this.isInGenericInstantiation()){
            instantiateSlots();
            return;
        }

        if(this.superClass != null && this.superClass != this){
            if(this.superClass.getCompilingStage() == CompilingStage.AllocateSlots){
                this.superClass.allocateSlotsForFields();
            }
            this.slotsAllocator.inheritsSlots(this.superClass.slotsAllocator);
        }
        for (Field field : this.fields.values()) {
            if(field.getOwnerClass() == this) {
                field.setSlot(this.slotsAllocator.allocateSlot(field));
            }
        }
        this.nextCompilingStage(CompilingStage.CompileMethodBody);
    }

    protected void instantiateSlots() throws CompilationError {
        if(this.compilingStage != CompilingStage.AllocateSlots) return;
        var templ = this.getTemplateClass();
        if(templ.compilingStage == CompilingStage.AllocateSlots){
            templ.allocateSlotsForFields();
        }
        var args = this.getGenericSource().instantiationArguments();
        for (SlotDef slot : templ.slotsAllocator.slots) {
            var variable = slot.getVariable();
            if(variable instanceof Field field){
                var myFld = this.fields.get(variable.name);
                if(myFld == null) {         // maybe private
                    this.slotsAllocator.allocateSlot(slot.getName(), slot.getTypeCode(), slot.getClassDef().instantiateAsReferenceClass(args, null));
                } else {
                    myFld.setSlot(this.slotsAllocator.allocateSlot(myFld));
                }
            } else {
                // for descendant class of template class, the slots exists, however for private field it won't inherits
//                throw new RuntimeException("variable '%s' not found in '%s'".formatted(variable, this));
            }
        }

        this.nextCompilingStage(CompilingStage.CompileMethodBody);
    }

    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;

        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        if(this.isInGenericInstantiation()){
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        if (!executeParseFieldsOfHierarchyClasses()) {
            return false;
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse fields".formatted(this));
        if (this.getClassBody() instanceof AgoParser.DefaultClassBodyContext defaultClassBodyContext) {
            for (AgoParser.ClassBodyDeclarationContext classBodyDeclaration : defaultClassBodyContext.classBodyDeclaration()) {
                if (classBodyDeclaration.memberDeclaration() instanceof AgoParser.FieldDeclContext fieldDeclContext) {
                    AgoParser.FieldDeclarationContext fieldDeclaration = fieldDeclContext.fieldDeclaration();
                    unit.parseField(this, fieldDeclaration);
                }
            }
        }

        // wrapper of interfaces
        for (var entry : this.wrapperInterfaces.entrySet()) {
            AgoParser.IdentifierContext fldAst = entry.getValue();
            String fldName = fldAst.getText();
            var existed = this.fields.get(fldName);
            ClassDef interfaceDef = entry.getKey();
            if(existed != null){
                if(!interfaceDef.isThatOrSuperOfThat(interfaceDef)){
                    throw unit.typeError(entry.getValue(), "'%s' not compitable for '%s'".formatted(existed.getType().getFullname(), interfaceDef.getFullname()));
                }
                // existed user declared field, use it
            } else {
                var field = new Field(this, fldName, null);
                field.setType(interfaceDef);
                field.setModifiers(AgoClass.PRIVATE);
                field.setDeclaration(fldAst);
                field.setSourceLocation(unit.sourceLocation(fldAst));
                this.addField(field);
            }
        }

        createFieldsOfTrait();

        this.nextCompilingStage(CompilingStage.ValidateHierarchy);
        return true;
    }

    protected void createFieldsOfTrait() {
        // fields for traits
        for (ClassDef anInterface : this.implementedInterfaces) {
            if(anInterface.isTrait()){
                ClassDef traitDef = anInterface;
                String fldName = composeInnerObjectName(traitDef, "@trait");
                var field = new Field(this, fldName, null);
                field.setType(new TraitDefInScope(traitDef));
                //TODO field.setSourceLocation(); trait position
                field.setModifiers(AgoClass.PRIVATE);
                field.setDeclaration(traitDef.getDeclarationAst()); //TODO implementdInterface should be {interfaceDef, ref}
                this.addField(field);
                traitFields.put(traitDef, field);
            }
        }
    }

    protected boolean executeParseFieldsOfHierarchyClasses() throws CompilationError {
        this.setCompilingStage(CompilingStage.ValidateHierarchy);       // temporary shift state to next, avoid recursive call
        if(this.metaClassDef != null){
            if(!metaClassDef.parseFields()) {
                this.setCompilingStage(CompilingStage.ParseFields); // restore stage
                return false;
            }
        }
        if(this.superClass != null && this.superClass != this){
            if(!this.superClass.parseFields()){
                this.setCompilingStage(CompilingStage.ParseFields);
                return false;
            }
        }
        List<ClassDef> interfaces = this.getInterfaces();
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (int i = 0; i < interfaces.size(); i++) {
                ClassDef anInterface = interfaces.get(i);
                if(!anInterface.parseFields()) {
                    this.setCompilingStage(CompilingStage.ParseFields);
                    return false;
                }
            }
        }
        if (this.permitClass != null) {
            if(!this.permitClass.parseFields()) {
                this.setCompilingStage(CompilingStage.ParseFields); // restore stage
                return false;
            }
        }
        this.setCompilingStage(CompilingStage.ParseFields);
        return true;
    }

    private String composeInnerObjectName(ClassDef type, String prefix) {
        String typename;
        if(type.isGenericInstantiation()){
            typename = type.getGenericSource().originalTemplate().getFullname().replace('.', '$');
        } else {
            typename = type.getFullname().replace('.', '$');
        }
        String fldName = prefix + typename;
        for(var i = 0; ;i++) {
            String s = fldName + "_" + i;
            if (this.fields.containsKey(s)) {
                fldName = s;
            } else {
                break;
            }
        }
        return fldName;
    }

    public void inheritsFields() throws CompilationError {
        if (this.compilingStage != CompilingStage.InheritsFields) return;

        if(this.isGenericInstantiation()) {
            this.instantiateFields();
            return;
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherits fields".formatted(this));
        if (superClass != null && superClass != this) {
            if(superClass.getCompilingStage() == CompilingStage.InheritsFields){
                superClass.inheritsFields();
            } else if (superClass.compilingStage.lt(CompilingStage.InheritsFields)) {
                Compiler.processClassTillStage(superClass,CompilingStage.InheritsFields);
            }
            this.inheritsFields(superClass.getFields(), superClass);
        }
        this.nextCompilingStage(CompilingStage.ValidateNewFunctions);
    }

    protected void inheritsFields(Map<String, Field> fields, ClassDef superClass){
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            var field = entry.getValue();
            if(!field.isPrivate()){
                this.fields.put(entry.getKey(), field);
                this.idOfConstString(field.name);
                if (field.getType().getTypeCode() == TypeCode.OBJECT) {
                    this.idOfConstString(field.getType().getFullname());
                }
            }
        }
    }

    public void inheritsChildClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.InheritsInnerClasses) return;

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherit child classes".formatted(this));

        createConstructorForFieldsInitializers();
        if(!this.isInGenericInstantiation()) createGetterAndSetter();

        var superClass = this.superClass;
        if(superClass != null && this.superClass != this) {
            if (superClass.getCompilingStage() == CompilingStage.ValidateNewFunctions) {
                Compiler.validateFunction(superClass);
            }
            if(superClass.compilingStage == CompilingStage.InheritsInnerClasses) {
                superClass.inheritsChildClasses();
            }

            var classes = superClass.getUniqueChildren();
            inheritsChildClasses(classes);
        }

        for (ClassDef implementedInterface : this.implementedInterfaces) {
            if (implementedInterface.getCompilingStage() == CompilingStage.ValidateNewFunctions) {
                Compiler.validateFunction(implementedInterface);
            }
            if(implementedInterface.compilingStage == CompilingStage.InheritsInnerClasses){
                implementedInterface.inheritsChildClasses();
            }
            var wrapperFldName = this.wrapperInterfaces.get(implementedInterface);
            if(wrapperFldName != null) {
                List<ClassDef> others = new ArrayList<>();
                for (ClassDef child : implementedInterface.getUniqueChildren()) {
                    if (!(child instanceof FunctionDef)) {
                        others.add(child);
                        continue;
                    }
                    if (this.getChild(child.getName()) != null)
                        continue;        // already provided an overloaded function

                    var fun = (FunctionDef) child;
                    var field = this.fields.get(wrapperFldName.getText());
                    var wrapperFun = new InterfaceFunctionWrapper(root, this, fun, field, field.getDeclaration());
                    addChild(wrapperFun);
                    wrapperFun.resolveHierarchicalClasses();
                    wrapperFun.setCompilingStage(CompilingStage.ParseFields);
                }
                if (!others.isEmpty()) {
                    inheritsChildClasses(others);
                }
                continue;
            }

            if(implementedInterface.isTrait()){
                var wrapperFld = this.traitFields.get(implementedInterface);
                List<ClassDef> others = new ArrayList<>();
                for (ClassDef child : implementedInterface.getUniqueChildren()) {
                    if (!(child instanceof FunctionDef)) {
                        others.add(child);
                        continue;
                    }
                    if (this.getChild(child.getName()) != null)
                        continue;        // already provided an overloaded function

                    var fun = (FunctionDef) child;
                    var wrapperFun = new InterfaceFunctionWrapper(root, this, fun, wrapperFld, wrapperFld.getDeclaration());
                    addChild(wrapperFun);
                    wrapperFun.resolveHierarchicalClasses();
                    wrapperFun.setCompilingStage(CompilingStage.ParseFields);
                }
                if (!others.isEmpty()) {
                    inheritsChildClasses(others);
                }
                continue;
            }

            inheritsChildClasses(implementedInterface.getUniqueChildren());
        }

        this.nextCompilingStage(CompilingStage.ValidateMembers);      // to ValidateMembers
    }

    private void createConstructorForFieldsInitializers(){
        if(this.getConstructor() != null) return;

        boolean needCreate = hasFieldInitializerOrTrait();
        if(needCreate){
            var mockConstructor = new ConstructorDef(root, AgoClass.CONSTRUCTOR | AgoClass.PUBLIC, "new#");
            mockConstructor.setCompilingStage(this.compilingStage);
            mockConstructor.setUnit(this.unit);
            this.addChild(mockConstructor);
        }
    }

    private void createGetterAndSetter() throws SyntaxError {
        for (Field field : this.fields.values()) {
            AgoParser.FieldGetterSetterContext fieldGetterSetter = field.getGetterSetter();
            if(fieldGetterSetter != null){
                createGetter(field, fieldGetterSetter.getter());
                AgoParser.SetterContext setter = fieldGetterSetter.setter();
                if(setter != null) createSetter(field, setter);
            }
        }

    }

    private void createSetter(Field field, AgoParser.SetterContext setterContext) throws SyntaxError {
        this.addChild(new SetterFunction(root, field, setterContext));
    }

    private void createGetter(Field field, AgoParser.GetterContext getterContext) throws SyntaxError {
        this.addChild(new GetterFunction(getRoot(), field, getterContext));
    }

    public boolean hasFieldInitializerOrTrait() {
        return !traitFields.isEmpty() || this.fields.values().stream().anyMatch(field -> field.getInitializer() != null);
    }

    @Override
    public FunctionDef getSameSignatureFunction(FunctionDef newFun) {
        var r = super.getSameSignatureFunction(newFun);
        if(r != null) return r;
        if(this.superClass != null && this.superClass != this){
            var c = this.superClass.getSameSignatureFunction(newFun);
            if(c instanceof FunctionDef f && !c.isPrivate() && !(c instanceof ConstructorDef)) return f;
        }
        for(var interfaceDef : this.implementedInterfaces){
            var c = interfaceDef.getSameSignatureFunction(newFun);
            if(c instanceof FunctionDef f) return f;
        }
        return null;
    }

    // for ParameterizedClass copy from its base class
    public void inheritsAllChildClasses(Collection<ClassDef> classes) {
        NamespaceCollection<ClassDef> children = this.getChildren();
        for (ClassDef c : classes) {
            if (c instanceof FunctionDef f) {
                if (children.containsKey(f.getName())) {  // overridden, skip this function
                    //TODO check whether visibility lower than super
                } else {
                    children.add(f);
                }
            } else {
                if (!children.containsKey(c.getName()))      // child class cannot override super.sameName
                    children.add(c);
            }
        }
    }

    protected void inheritsChildClasses(Collection<ClassDef> ancientChildren) throws CompilationError {
        NamespaceCollection<ClassDef> children = this.getChildren();
        boolean hasConstructor = this.getConstructor() != null;
        for (ClassDef c : ancientChildren) {
            // auto inherits Constructor if not defined
            if (hasConstructor &&  c instanceof ConstructorDef) continue;       //TODO may override constructor

            if (c instanceof FunctionDef f) {
                if(f instanceof ConstructorDef && f.getParent().equals(getRoot().getObjectClass())){
                    continue;
                }
                if (!f.isPrivate()) {
                    if (children.containsKey(f.getName())) {  // overridden, skip this function
                        //TODO check whether visibility lower than super
                    } else {
                        this.addChild(f);
                    }
                }
            } else {
                if (!c.isPrivate()) {
                    if (!children.containsKey(c.getName()))      // child class cannot override super.sameName
                        this.addChild(c);
                }
            }
        }
    }


    public SlotsAllocator getSlotsAllocator() {
        return slotsAllocator;
    }

    public byte getClassType() {
        return classType;
    }

    public void setClassType(byte classType) {
        this.classType = classType;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public Variable getVariable(String name){
        Field field = this.getFields().get(name);
        return field;
    }

    public MetaClassDef getMetaClassDef() {
        return metaClassDef;
    }

    public Root getRoot() {
        if(this.root != null) return this.root;
        if(this.isGenericInstantiation()) return this.getTemplateClass().getRoot();
        for(var p = this.parent; p != null; p = p.parent){
            if(p instanceof Root) return (Root) p;
        }
        throw new UnsupportedOperationException("Root not found");
    }

    public int idOfConstString(String s){
        if(this.parent != null && this.parent instanceof ClassDef c){
            return c.idOfConstString(s);
        }
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
        return idOfConstString(s);
//        if(this.parent != null && this.parent instanceof ClassDef c){
//            return c.idOfKnownConstString(s);
//        }
//        Integer i = this.stringTable.get(s);
//        if(i != null){
//            return i;
//        }
//        throw new IndexOutOfBoundsException(s + " not existed");
    }

    public int idOfKnownClass(ClassDef classDef) {
        return idOfKnownConstString(classDef.getFullname());
    }

    public List<String> getTopStrings(){
        if(this.parent != null && this.parent instanceof ClassDef c){
            return c.getTopStrings();
        }
        return this.strings;
    }

    /**
     * find out the distance from outerClass or outer to this class
     * @return
     */
    public int distanceToOuterClass(ClassDef outerClass){
        if(this == outerClass) return 0;
        var dist = 1;
        for(var p = this.parent; p != null; p = p.parent, dist++){
            if(p instanceof ClassDef c) {
                if (c == outerClass) {
                    return dist;
                }
            }
        }
        return -1;
    }

    public List<String> getStrings() {
        return strings;
    }

    public boolean isNative() {
        return (this.modifiers & AgoClass.NATIVE) !=0;
    }
    public boolean isStatic() {
        return (this.modifiers & AgoClass.STATIC) !=0;
    }

    public boolean isAbstract() {
        return (this.modifiers & AgoClass.ABSTRACT) !=0;
    }
    public boolean isFinal() {
        return (this.modifiers & AgoClass.FINAL) !=0;
    }
    public int getVisibility() {
        return this.modifiers & 0b111;
    }
    public boolean isPublic(){
        return (this.modifiers & AgoClass.PUBLIC) != 0;
    }
    public boolean isPrivate(){
        return (this.modifiers & AgoClass.PRIVATE) != 0;
    }
    public boolean isProtected(){
        return (this.modifiers & AgoClass.PROTECTED) != 0;
    }


    public boolean isVoid(){return this.getTypeCode() == TypeCode.VOID;}
    public boolean isNull(){return this.getTypeCode() == TypeCode.NULL;}
    public boolean isBoolean(){return this.getTypeCode() == TypeCode.BOOLEAN;}
    public boolean isClassRef(){return this.getTypeCode() == TypeCode.CLASS_REF;}

    public ClassRefLiteral toClassRefLiteral(){
        return root.createClassRefLiteral(this);
    }

    public boolean isPrimitiveFamily(){
        return this instanceof PrimitiveClassDef || this.isThatOrDerivedFromThat(getRoot().getPrimitiveType());
    }

    public boolean isBooleanOrBoxed(){
        if(this.getTypeCode() == TypeCode.BOOLEAN) return true;
        if(this.getTypeCode() == TypeCode.OBJECT && this.isThatOrDerivedFromThat(getRoot().BOOLEAN().getBoxedType()) || this.isDeriveFrom(getRoot().BOOLEAN().getBoxerInterface())){
            return true;
        }
        return false;
    }

    public boolean isPrimitive() {
        return this instanceof PrimitiveClassDef;
    }

    public boolean isPrimitiveNumberFamily() {
        return this instanceof PrimitiveClassDef p && p.isNumber() || this.isThatOrDerivedFromThat(getRoot().getPrimitiveNumberType());
    }

    /**
     * indicate it's generic template, not include intermediate template, that can can accept ClassRef[]
     * @return
     */
    public boolean isGenericTemplate(){
        return (this.modifiers & AgoClass.GENERIC_TEMPLATE) != 0;
    }

    public boolean isInGenericTemplate(){
        if ((this.modifiers & AgoClass.GENERIC_TEMPLATE) != 0) return true;
        if(this.parent instanceof ClassDef p) return p.isInGenericTemplate();
        return false;
    }

    /// isGenericInstantiation for `A<T>.B<Dog>` may got true,
    /// isInGenericInstantiation can solve it
    /// @return
    public boolean isInGenericInstantiation(){
        return this.genericSource != null && !this.genericSource.isPlaceHolderOfTemplate();
    }

    public boolean isPrimitiveBoxed(){
        if(this.isEnum()) return true;
        return  (this.isThatOrDerivedFromThat(getRoot().BYTE().getBoxedType()) || this.isDeriveFrom(getRoot().BYTE().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().SHORT().getBoxedType()) || this.isDeriveFrom(getRoot().SHORT().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().CHAR().getBoxedType()) || this.isDeriveFrom(getRoot().CHAR().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().INT().getBoxedType()) || this.isDeriveFrom(getRoot().INT().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().FLOAT().getBoxedType()) || this.isDeriveFrom(getRoot().FLOAT().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().LONG().getBoxedType()) || this.isDeriveFrom(getRoot().LONG().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().DOUBLE().getBoxedType()) || this.isDeriveFrom(getRoot().DOUBLE().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().DECIMAL().getBoxedType()) || this.isDeriveFrom(getRoot().DECIMAL().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().STRING().getBoxedType()) || this.isDeriveFrom(getRoot().STRING().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().BOOLEAN().getBoxedType()) || this.isDeriveFrom(getRoot().BOOLEAN().getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(getRoot().CLASSREF().getBoxedType())
            );
    }


    public boolean isPrimitiveOrBoxed(){
        return this.isPrimitive() || this.isPrimitiveBoxed();
    }

    public boolean isPrimitiveFamilyOrBoxed() {
        return this.isPrimitiveFamily() || this.isPrimitiveBoxed();
    }

    public TypeCode getUnboxedTypeCode(){
        if(this.isEnum()){
            return enumBasePrimitiveType.getTypeCode();
        }
        if(this.isThatOrDerivedFromThat(getRoot().BYTE().getBoxedType()) || this.isDeriveFrom(getRoot().BYTE().getBoxerInterface())) return TypeCode.BYTE;
        if(this.isThatOrDerivedFromThat(getRoot().SHORT().getBoxedType()) || this.isDeriveFrom(getRoot().SHORT().getBoxerInterface())) return TypeCode.SHORT;
        if(this.isThatOrDerivedFromThat(getRoot().CHAR().getBoxedType()) || this.isDeriveFrom(getRoot().CHAR().getBoxerInterface())) return TypeCode.CHAR;
        if(this.isThatOrDerivedFromThat(getRoot().INT().getBoxedType()) || this.isDeriveFrom(getRoot().INT().getBoxerInterface())) return TypeCode.INT;
        if(this.isThatOrDerivedFromThat(getRoot().FLOAT().getBoxedType()) || this.isDeriveFrom(getRoot().FLOAT().getBoxerInterface())) return TypeCode.FLOAT;
        if(this.isThatOrDerivedFromThat(getRoot().LONG().getBoxedType()) || this.isDeriveFrom(getRoot().LONG().getBoxerInterface())) return TypeCode.LONG;
        if(this.isThatOrDerivedFromThat(getRoot().DOUBLE().getBoxedType()) || this.isDeriveFrom(getRoot().DOUBLE().getBoxerInterface())) return TypeCode.DOUBLE;
        if(this.isThatOrDerivedFromThat(getRoot().STRING().getBoxedType()) || this.isDeriveFrom(getRoot().STRING().getBoxerInterface())) return TypeCode.STRING;
        if(this.isThatOrDerivedFromThat(getRoot().BOOLEAN().getBoxedType()) || this.isDeriveFrom(getRoot().BOOLEAN().getBoxerInterface())) return TypeCode.BOOLEAN;
        if(this.isThatOrDerivedFromThat(getRoot().CLASSREF().getBoxedType()) || this.isDeriveFrom(getRoot().CLASSREF().getBoxerInterface())) return TypeCode.CLASS_REF;
        return getTypeCode();
    }
    
    public PrimitiveClassDef getUnboxedType(){
        if(this.isEnum()){
            return enumBasePrimitiveType;
        }
        if(this.isThatOrDerivedFromThat(getRoot().BYTE().getBoxedType()) || this.isDeriveFrom(getRoot().BYTE().getBoxerInterface())) return getRoot().BYTE();
        if(this.isThatOrDerivedFromThat(getRoot().SHORT().getBoxedType()) || this.isDeriveFrom(getRoot().SHORT().getBoxerInterface())) return getRoot().SHORT();
        if(this.isThatOrDerivedFromThat(getRoot().CHAR().getBoxedType()) || this.isDeriveFrom(getRoot().CHAR().getBoxerInterface())) return getRoot().CHAR();
        if(this.isThatOrDerivedFromThat(getRoot().INT().getBoxedType()) || this.isDeriveFrom(getRoot().INT().getBoxerInterface())) return getRoot().INT();
        if(this.isThatOrDerivedFromThat(getRoot().FLOAT().getBoxedType()) || this.isDeriveFrom(getRoot().FLOAT().getBoxerInterface())) return getRoot().FLOAT();
        if(this.isThatOrDerivedFromThat(getRoot().LONG().getBoxedType()) || this.isDeriveFrom(getRoot().LONG().getBoxerInterface())) return getRoot().LONG();
        if(this.isThatOrDerivedFromThat(getRoot().DOUBLE().getBoxedType()) || this.isDeriveFrom(getRoot().DOUBLE().getBoxerInterface())) return getRoot().DOUBLE();
        if(this.isThatOrDerivedFromThat(getRoot().STRING().getBoxedType()) || this.isDeriveFrom(getRoot().STRING().getBoxerInterface())) return getRoot().STRING();
        if(this.isThatOrDerivedFromThat(getRoot().BOOLEAN().getBoxedType()) || this.isDeriveFrom(getRoot().BOOLEAN().getBoxerInterface())) return getRoot().BOOLEAN();
        if(this.isThatOrDerivedFromThat(getRoot().CLASSREF().getBoxedType())) return getRoot().CLASSREF();
        throw new UnsupportedOperationException("'%s' is not a boxer type".formatted(this.getFullname()));
    }

    public boolean isThatOrBoxOfThat(PrimitiveClassDef primitiveClassDef){
        if(this instanceof PrimitiveClassDef && this == primitiveClassDef){
            return true;
        }
        if(this.getTypeCode() == TypeCode.OBJECT){
            if(primitiveClassDef.isThatOrSuperOfThat(this)) return true;
            return this.getUnboxedTypeCode() == primitiveClassDef.getTypeCode();
        }
        return false;
    }

    public void setPermitClass(ClassDef permitClass) {
        assert this.isInterfaceOrTrait();
        this.permitClass = permitClass;
        this.idOfClass(permitClass);
        if (permitClass instanceof ParameterizedClassDef.PlaceHolder placeHolder) {
            placeHolder.registerReference(new ParameterizedClassDef.RefViaPermitClass(this));
        }
    }

    public TypeCode getTypeCode() {
        return TypeCode.OBJECT;
    }

    public ClassDef getPermitClass() {
        assert this.isInterfaceOrTrait();
        if(permitClass == null) return getRoot().getObjectClass();
        return permitClass;
    }

    @Override
    public String toString() {
        if(this.compilingStage != CompilingStage.Compiled){
            return "(Class %s %s)".formatted(this.getFullname(), this.compilingStage);
        }
        return "(Class %s)".formatted(this.getFullname());
    }

    public String getFullnameWithoutPackage(){
        List<String> arr = new ArrayList<>();
        for(Namespace c = this; c instanceof ClassDef; c = c.parent){
            arr.addFirst(c.name);
        }
        return String.join(".", arr);
    }

    public ClassDef getSuperClass() {
        return superClass;
    }

    public void setSuperClass(ClassDef superClass) {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s set superclass to %s".formatted(this, superClass));
        this.superClass = superClass;
        if(superClass instanceof ParameterizedClassDef.PlaceHolder placeHolder){
            placeHolder.registerReference(new ParameterizedClassDef.RefViaSuperClass(this));
        } else if(superClass != null) {
            this.addDependency(superClass);
            this.idOfClass(superClass);
        }
    }

    @Override
    public void addChild(ClassDef child) {
        super.addChild(child);
        if(child instanceof FunctionDef functionDef) {
            if(functionDef.isGetter() || functionDef.isSetter()){
                var p = this.attributes.computeIfAbsent(functionDef.getCommonName(), k -> new GetterSetterPair());
                if(functionDef.isGetter()) p.setGetter(functionDef);
                if(functionDef.isSetter()) p.setSetter(functionDef);
            }
            if (child instanceof ConstructorDef constructorDef) {
                this.setConstructor(constructorDef);
            }
        }
    }

    public void setConstructor(ConstructorDef constructor) {
        this.constructor = constructor;
    }

    public ConstructorDef getConstructor() {
        return constructor;
    }

    public List<FunctionDef> getConstructors(){
        List<FunctionDef> constructorDefs = new ArrayList<>();
        for (ClassDef child : this.getUniqueChildren()) {
            if(child instanceof ConstructorDef c && ! c.isStatic()){
                if(!constructorDefs.contains(c))
                    constructorDefs.add(c);
            }
        }
        return constructorDefs;
    }

    public ClassDef getParentClass() {
        return this.parent instanceof ClassDef p ? p : null ;
    }

    public boolean isTop() {
        return !(this.parent instanceof ClassDef);
    }


    public boolean isThatOrDerivedFromThat(ClassDef anotherClass){
        if(anotherClass == null) return false;
        return anotherClass.isThatOrSuperOfThat(this);
    }

    public boolean isDeriveFrom(ClassDef maybeSuperClass) {
        if(maybeSuperClass == null)
            return false;

        return maybeSuperClass != this && maybeSuperClass.isThatOrSuperOfThat(this);

//        if(this.genericSource != null && this.genericSource.originalTemplate().isDeriveFrom(maybeSuperClass)){
//            return true;
//        }
//
//        for(var c = this; c != null; c = c.superClass){
//            if(c != this){
//                if(c == maybeSuperClass) return true;
//            }
//            if(maybeSuperClass.isInterfaceOrTrait()) {
//                for (ClassDef implementedInterface : c.implementedInterfaces) {
//                    if (implementedInterface == maybeSuperClass || implementedInterface.isDeriveFrom(maybeSuperClass)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
    }

    // to tell the distance from function's owner to function's real owner
    public int distanceToSuperClass(ClassDef superClass) {
        if(this == superClass) return 0;

        MutableInt depth = new MutableInt();
        var r = superClass.asThatOrSuperOfThat(this, null, depth);
        if(r == null) return -1;
        return depth.get().intValue();
    }

    /**
     * that means, <code>var a as This = (b as anotherClass)</code> can work fine
     * in another word, `This <= That` in the hierarchy tree, `ThisConcept contains That`
     * already support generic variance
     * @param anotherClass
     * @return
     */
    public boolean isThatOrSuperOfThat(ClassDef anotherClass){
        return asThatOrSuperOfThat(anotherClass) != null;
    }

    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass){
        return asThatOrSuperOfThat(anotherClass, null, null);
    }

    public boolean isThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        return asThatOrSuperOfThat(anotherClass, visited, null) != null;
    }

    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited, MutableInt depth){
        if(this == anotherClass) {
            return this;
        }

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }
        if(depth != null) depth.increment();

        if(this == getRoot().getAnyClass() && depth == null) return this;

        if((anotherClass instanceof GenericConcreteType || anotherClass.isGenericTemplate())
                && (this instanceof GenericConcreteType || this.isGenericTemplate())){        // Template is some kind Generic concrete type too
            // yes, should use the type arguments for this template class, don't include parent type arguments
            if(anotherClass.getTemplateClass() == this.getTemplateClass() && isTypeArgumentsMatch(this.genericSource.typeArguments(), anotherClass.genericSource.typeArguments())){
                return anotherClass;
            }
        }

        // the value of ClassBound is classref, not an object
//        if(anotherClass instanceof ClassBound classBound){  // for class interval, we can only determine it by lBound
//            ClassDef lBound = classBound.getLBoundClass();
//            if(lBound == anyClass){
//                return anotherClass;
//            } else {
//                return this.asAssignableFrom(lBound);
//            }
//        }
        if(anotherClass instanceof ParameterizedClassDef p){
            // if this is ParameterizedClassDef too, see ParameterizedClassDef.asAssignableFrom
            if(this == p.baseClass) return p;
            var r = this.asThatOrSuperOfThat(p.baseClass, visited, depth);
            if(r != null) return r;
        } else if(anotherClass instanceof ParameterizedClassDef.PlaceHolder p){
            var r = this.asThatOrSuperOfThat(p.getBaseClass(), visited, depth);
            if(r != null) return r;
        }

        if(anotherClass.superClass != null && anotherClass.superClass != anotherClass){    // solve derived class in recursive
            var sp = this.asThatOrSuperOfThat(anotherClass.superClass, visited, depth);
            if(sp != null) return sp;
        }
        if(anotherClass.isInterfaceOrTrait()){
            var permitClass = anotherClass.getPermitClass();
            var t = this.asThatOrSuperOfThat(permitClass, visited == null ? new LinkedHashSet<>() : visited, depth);
            if(t != null) return t;
        }
        if(this.isInterfaceOrTrait()) {
            for (ClassDef implementedInterface : anotherClass.implementedInterfaces) {
                ClassDef i = this.asThatOrSuperOfThat(implementedInterface, visited == null ? new LinkedHashSet<>() : visited, depth);
                if(i != null){
                    return i;
                }
            }
        }

        if(depth != null) depth.decrement();
        return null;
    }


    public List<ClassDef> getInterfaces() {
        return implementedInterfaces;
    }

    public List<ClassDef> getAllInterfaces() {
        List<ClassDef> interfaces = new ArrayList<>();
        for(var cls = this; cls != null; cls = cls.superClass){
            if(cls.implementedInterfaces == null) continue;
            var stack = new ArrayDeque<>(cls.implementedInterfaces);
            while(!stack.isEmpty()){
                var el = stack.pop();
                if(!interfaces.contains(el)){
                    interfaces.add(el);
                    if(el.implementedInterfaces != null)
                        stack.addAll(el.implementedInterfaces);
                }
            }
            if(cls == cls.superClass) break;
        }
        return interfaces;
    }

    public void setInterfaces(List<ClassDef> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
        for (ClassDef implementedInterface : implementedInterfaces) {
            if(implementedInterface instanceof ParameterizedClassDef.PlaceHolder p){
                p.registerReference(new ParameterizedClassDef.RefViaInterface(this));
            } else {
                this.idOfClass(implementedInterface);
            }
        }
    }

    public void addImplementedInterface(ClassDef implementedInterface){
        this.implementedInterfaces.add(implementedInterface);
        if (implementedInterface instanceof ParameterizedClassDef.PlaceHolder p) {
            p.registerReference(new ParameterizedClassDef.RefViaInterface(this));
        } else {
            this.idOfClass(implementedInterface);
        }
    }

    public void setMetaClassDef(MetaClassDef metaClassDef) {
        this.metaClassDef = metaClassDef;
        if(metaClassDef != null) {
            this.addDependency(metaClassDef);
        }
    }

    public AgoParser.ClassBodyContext getClassBody(){
        return this.classDeclaration.classBody();
    }

    public List<AgoParser.InterfaceItemContext> getInterfaceDecls(){
        if(classDeclaration == null) return null;
        AgoParser.ImplementsPhraseContext implementsPhraseContext = classDeclaration.implementsPhrase();
        return implementsPhraseContext != null? implementsPhraseContext.interfaceList().interfaceItem() : null;
    }

    public boolean belongsTo(ClassDef maybeParent) {
        Namespace<?> prev = this;
        for(var p = this.parent; ; prev = p, p = p.parent){
            if(!(p instanceof ClassDef) && prev instanceof MetaClassDef m){
                p = m.getInstanceClassDef();
            }
            if(p == null) break;
            if(p == maybeParent) return true;
            if(maybeParent instanceof MetaClassDef m){
                if(p == m.getInstanceClassDef()) return true;
            }
        }
        return false;
    }

    /**
     * add a dependency class
     * @param dependency
     * @return false if collision found
     */
    public boolean addDependency(ClassDef dependency){
        if(dependency instanceof PrimitiveClassDef) return true;
        if(dependency == this || this.dependencies.contains(dependency)) return true;

        if(dependency.isDependingOn(this, 0)) return false;
        this.dependencies.add(dependency);
        getRoot().getDependencyResultCache().put(Pair.of(this, dependency), true);
        if(dependency instanceof ConcreteType cd) {     // GenericTypeAvatarClassDef included
            registerConcreteType(cd);
        } else {
            if(!dependency.isPrimitive())
                this.idOfClass(dependency);
        }
        return true;
    }

    public boolean isDependingOn(ClassDef classDef, int depth) {
//        if(LOGGER.isDebugEnabled()) LOGGER.debug("%stest %s depend on %s".formatted("\t".repeat(depth), this, classDef));
        if(depth > 100){
            System.out.println(1);
        }
        if(this == classDef) return false;
        var dependencyResultCache = root.getDependencyResultCache();
        Pair<ClassDef, ClassDef> p = Pair.of(this, classDef);
        Boolean r = dependencyResultCache.get(p);
        if(r != null) return r;

        for (ClassDef dependency : this.dependencies) {
            if(dependency == classDef || dependency.isDependingOn(classDef, depth + 1)) {
                r = true;
                break;
            }
        }
        if(r == null) r = false;
        dependencyResultCache.put(p, r);
        return r;
    }

    /**
     * all dependencies classed, meta class and dependencies in constructors
     * @return
     */
    public Set<ClassDef> getDependencies(){
        return dependencies;
    }


    public Package getPackage() {
        for(var p = this.parent; p != null; p = p.parent){
            if(p instanceof Package pkg) return pkg;
        }
        return null;
    }

    protected ConstructorDef getEmptyArgsConstrutor() {
        var r = this.getConstructors().stream().filter(FunctionDef::isEmptyArgs).findFirst();
        return (ConstructorDef) r.orElse(null);
    }

    public boolean isNameMatch(String text) {
        return this.name.equals(text);
    }

    public int idOfClass(ClassDef classDef) {
        if(this.parent != null && this.parent instanceof ClassDef c){
            return c.idOfClass(classDef);
        }

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

    public int simpleNameOfFunction(FunctionDef functionDef) {
        return this.idOfConstString(functionDef.getName());
    }

    public ArrayClassDef getOrCreateArrayType(ClassDef elementType, MutableBoolean returnExisted) throws CompilationError {
        if(this.getParentClass() != null){
            return this.getParentClass().getOrCreateArrayType(elementType, returnExisted);
        }
        ArrayClassDef arrayType = this.unit.getRoot().getOrCreateArrayType(elementType, returnExisted);
        if(arrayType instanceof ConcreteType c){
            this.registerConcreteType(c);
        }
        if(!elementType.isPrimitive()) this.idOfConstString(elementType.getFullname());
        return arrayType;
    }

    public NullableClassDef getOrCreateNullableType(ClassDef baseType, MutableBoolean returnExisted) throws CompilationError {
        if(this.getParentClass() != null){
            return this.getParentClass().getOrCreateNullableType(baseType, returnExisted);
        }
        var nullableType = this.unit.getRoot().getOrCreateNullableType(baseType, returnExisted);
        if(nullableType instanceof ConcreteType c){
            this.registerConcreteType(c);
        }
        if(!baseType.isPrimitive()) this.idOfConstString(baseType.getFullname());
        return nullableType;
    }


    public Map<String, ConcreteType> getConcreteTypes() {
        if(this.getParentClass() != null){
            return this.getParentClass().getConcreteTypes();
        }
        return concreteTypes;
    }

    public void shiftToTemplate() throws CompilationError {
        this.setModifiers(this.modifiers | AgoClass.GENERIC_TEMPLATE);
        var existed = this.getTypeParamsContext();
        if(existed != null) throw new  IllegalStateException(this + " is already template class");

        TypeParamsContext parentParams = null;
        for(var p = this.getParentClass(); p != null; p = p.getParentClass()){
            if(p.isGenericTemplate()){
                parentParams = p.getTypeParamsContext();
                break;
            }
        }
        if (parentParams != null) {
            this.typeParamsContext = new TypeParamsContext(this, parentParams);
        } else {
            this.typeParamsContext = new TypeParamsContext(this);
        }
    }

    public ClassDef getCachedInstantiatedClass(InstantiationArguments instantiationArguments){
        if(this.instantiationsCache == null) return null;
        return this.instantiationsCache.get(instantiationArguments);
    }

    public void putInstantiatedClassToCache(InstantiationArguments instantiationArguments, ClassDef instantiationClass){
        if(this.instantiationsCache == null) this.instantiationsCache = new HashMap<>();
        this.instantiationsCache.put(instantiationArguments, instantiationClass);
    }

    public TypeParamsContext getTypeParamsContext() {
        if(typeParamsContext == null){
            if(this.getGenericSource() != null){
                return this.genericSource.originalTemplate().getTypeParamsContext();
            }
        }
        return typeParamsContext;
    }

    public void registerConcreteType(ClassDef classDef) {
        for(var c = classDef; c!= null; c = c.getParentClass()){
            if(c instanceof ConcreteType concreteType){
                registerConcreteType(concreteType);
            } else {
                if(c.getConcreteTypes() != null){
                    for (ConcreteType concreteType : c.getConcreteTypes().values()) {
                        registerConcreteType(concreteType);
                    }
                }
            }
        }
    }

    public void registerConcreteType(ConcreteType concreteType) {
        if(this.getParentClass() != null){
            this.getParentClass().registerConcreteType(concreteType);
            return;
        }
        if(concreteTypes.containsKey(concreteType.getFullname())) return;
        this.idOfClass((ClassDef) concreteType);
        // this.addDependency((ClassDef) concreteType);
        for (ClassDef concreteDependencyClass : concreteType.getConcreteDependencyClasses()) {
            this.addDependency(concreteDependencyClass);
        }
        concreteType.acceptRegisterConcreteType(this);

        concreteTypes.put(concreteType.getFullname(), concreteType);

        if(concreteType instanceof GenericConcreteType genericConcreteType){
            var temp = ((ClassDef) concreteType).getTemplateClass();
            for(ClassDef p = temp; p != null; p = p.parent instanceof ClassDef p2 ? p2 : null){
                if(p instanceof ConcreteType c) {
                    registerConcreteType(c);
                }
            }
        }
    }


    private Map<Object, Integer> blobsIndex = new HashMap<>();
    private List<byte[]> blobs = new ArrayList<>();
    public int getOrCreateBLOB(List<? extends Literal<?>> literals, ArrayLiteral arrayLiteral) throws TypeMismatchError {
        if(!this.isTop()) return this.getParentClass().getOrCreateBLOB(literals, arrayLiteral);
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
        if(!this.isTop()) return this.getParentClass().getOrCreateBLOB(literal);
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

    @Override
    public ClassDef getOrCreateGenericInstantiationClassDef(ClassDef templateClass, ClassRefLiteral[] typeArguments, MutableBoolean returnExisted) throws CompilationError {
        InstantiationArguments args = new InstantiationArguments(templateClass.getTemplateClass().getTypeParamsContext(), typeArguments);
        if(this.getGenericSource() != null){
            args = args.applyParent(this.getGenericSource().instantiationArguments());
        }
        return templateClass.instantiate(args, returnExisted);
    }

    public ClassDef instantiateAsReferenceClass(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        if(!this.isAffectedByTypeArguments(arguments)) return this;

        ClassDef templ;
        InstantiationArguments args;
        GenericSource genericSource = this.getGenericSource();

        if(genericSource != null) {     // instantiation and generic template
            templ = genericSource.originalTemplate();
            var myArgs = genericSource.instantiationArguments();
            args = myArgs.apply(arguments);     // args become args for me+my parents, `arguments` still preserve child args
            if(myArgs.equals(args)){
                if (returnExisted != null) returnExisted.setTrue();
                return this;
            }
        } else {
            templ = this;
            args = arguments;
        }
        var existed = templ.getCachedInstantiatedClass(args);
        if(existed != null) {
            if (!args.equals(existed.getGenericSource().instantiationArguments()) && !this.instantiatingChildren.contains(args)) {    // arguments changed, try children
                this.instantiateChildren(existed, arguments);
            }
            if (returnExisted != null) returnExisted.setTrue();
            return existed;
        }

        LinkedList<ClassDef> path = new LinkedList<>();
        for(var p = templ.getParentClass(); p != null; p = p.getParentClass()){
            path.addFirst(p);
        }
        ClassDef parentInstantiation = null;
        for(var p : path){
            var e = new MutableBoolean();
            parentInstantiation = p.instantiate(args, parentInstantiation, e);
        }
        return this.instantiate(arguments, parentInstantiation, returnExisted);
    }

    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return instantiate(arguments, null, returnExisted);
    }

    public ClassDef instantiate(InstantiationArguments arguments, ClassDef parentInstantiation, MutableBoolean returnExisted) throws CompilationError {
        if(parentInstantiation == null && !this.isAffectedByTypeArguments(arguments)) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }
        ClassDef templ;
        InstantiationArguments args;
        GenericSource genericSource = this.getGenericSource();

        if(genericSource != null) {     // instantiation and generic template
            templ = genericSource.originalTemplate();
            var myArgs = genericSource.instantiationArguments();
            args = myArgs.apply(arguments);     // args become args for me+my parents, `arguments` still preserve child args
            if(myArgs.equals(args)){
                if(this.parent == parentInstantiation) {
                    if (returnExisted != null) returnExisted.setTrue();
                    return this;
                } else {
                    if(parentInstantiation != null && parentInstantiation.getGenericSource() != null)
                        args = args.applyParent(parentInstantiation.getGenericSource().instantiationArguments());
                }
            }
        } else {
            templ = this;
            args = arguments;
        }

        var existed = templ.getCachedInstantiatedClass(args);
        if(existed != null) {
            if (!args.equals(existed.getGenericSource().instantiationArguments()) && !this.instantiatingChildren.contains(args)) {    // arguments changed, try children
                this.instantiateChildren(existed, args);
            }
            if(returnExisted != null) returnExisted.setTrue();
            return existed;
        } else {
            ClassContainer parent = parentInstantiation == null? (ClassContainer) this.parent : parentInstantiation;
            ClassDef result;
            MutableBoolean childExisted = new MutableBoolean();
            if (templ.isGenericTemplate()) {
                ClassRefLiteral[] argsForTempl = args.takeFor(templ);
                if (argsForTempl != null && !Arrays.equals(templ.genericSource.typeArguments(),  argsForTempl)) {       // if still the template, but whole type arguments changed, make a clone
                    if (templ instanceof FunctionDef templFun) {
                        if (templFun instanceof InterfaceFunctionWrapper interfaceFunctionWrapper) {
                            result = new GenericInstantiationInterfaceFunctionWrapper(interfaceFunctionWrapper, parent, args);
                        } else {
                            result = new GenericInstantiationFunctionDef(templFun, parent, args);
                        }
                    } else {
                        result = new GenericInstantiationClassDef(templ, parent, args);
                    }
                } else {
                    result = cloneForInstantiate(args, parent , childExisted);
                }
            } else {
                result = cloneForInstantiate(args, parent, childExisted);
            }
            return result;
        }
    }

    protected void instantiateChildren(ClassDef instantiatedClass, InstantiationArguments arguments) throws CompilationError {
        this.instantiatingChildren.add(arguments);
        for (ClassDef child : this.getUniqueChildren()) {
            if(!this.gotFromInherited(child)){
                InstantiationArguments childArgs;
                if(child.getGenericSource() != null){
                    childArgs = child.getGenericSource().instantiationArguments().applyParent(arguments);
                } else {
                    childArgs = arguments;
                }

                child.instantiate(childArgs, instantiatedClass, null);
            }
        }
        this.instantiatingChildren.remove(arguments);
    }

    /**
     * clone the modifiers, unit and children structure, equals ParseClassName stage did
     * only child classes of a generic-template-class can enter this invocation, and
     * all child classes of a generic-template-class MUST enter this invocation
     *
     * @param instantiationArguments
     * @param parent
     * @param returnExisted
     * @return
     */
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) throws CompilationError {
        var clone = new ClassDef(root, name, classDeclaration);
        cloneTo(instantiationArguments, clone, parent);
        return clone;
    }

    public void cloneTo(InstantiationArguments instantiationArguments, ClassDef instantiateClass, ClassContainer parent) throws CompilationError {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("apply template instantiation class %s via %s".formatted(instantiateClass, getGenericSource()));

        if(instantiateClass.getGenericSource() == null) {       // GenericInstantiationClassDef set generic source by itself
            instantiateClass.setGenericSource(new GenericSource(this.getTemplateClass(), instantiationArguments, null));
        }
        this.putInstantiatedClassToCache(instantiationArguments, instantiateClass);

        if(instantiateClass instanceof GenericConcreteType) {
            instantiateClass.setModifiers((this.getModifiers() & GENERIC_TEMPLATE_NEG) | AgoClass.GENERIC_INSTANTIATION);
        } else {
            // inner template classes or inner normal classes
            instantiateClass.setModifiers(this.getModifiers());
        }
        instantiateClass.setUnit(this.getUnit());
        instantiateClass.setSourceLocation(this.getSourceLocation());        //TODO the source location assign to template's source location?

        if(instantiateClass instanceof FunctionDef tempFun){
            FunctionDef targetFun = (FunctionDef) instantiateClass;
            targetFun.setNativeEntrance(tempFun.getNativeEntrance());
            targetFun.setCommonName(tempFun.getCommonName());
            targetFun.setThrowsExceptions(tempFun.getThrowsExceptions());
        }
        for (ClassDef argument : instantiationArguments.getAllArguments()) {
            instantiateClass.registerConcreteType(argument);
        }

        instantiateClass.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);      // ParseClassName and ParseGenericParams skipped, it will enter ResolveHierarchicalClasses and ParseField soon

        if(parent != null) parent.addChild(instantiateClass);

        instantiateChildren(instantiateClass, instantiationArguments);
    }

    public GenericTypeCodeAvatarClassDef findGenericType(String genericTypeName) {
        var t = this.typeParamsContext;
        if(t == null){
            if(this.parent instanceof ClassDef p) {
                return p.findGenericType(genericTypeName);
            }
            return null;
        }
        var r = t.get(genericTypeName);
        if(r == null && this.parent instanceof ClassDef p){
            return p.findGenericType(genericTypeName);
        }
        return r;
    }

    public boolean isClass() {
        return this.classType == AgoClass.TYPE_CLASS || this.classType == TYPE_METACLASS || this.classType == TYPE_PRIMITIVE_CLASS || this.classType == TYPE_ANY_CLASS;
    }

    public boolean isMetaClass(){
        return this.classType == TYPE_METACLASS;
    }

    public boolean isInterface() {
        return this.classType == AgoClass.TYPE_INTERFACE;
    }

    public boolean isTrait() {
        return this.classType == AgoClass.TYPE_TRAIT;
    }

    public boolean isFunction() {
        return this.classType == AgoClass.TYPE_FUNCTION;
    }

    public boolean isInterfaceOrTrait() {
        return this.classType == AgoClass.TYPE_INTERFACE || this.classType == AgoClass.TYPE_TRAIT;
    }

    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        if(this.isInGenericInstantiation()){
            instantiateHierarchy();
        } else {
            if(unit != null) unit.resolveHierarchicalClasses(this);
            resolveMetaclass();
            this.nextCompilingStage(CompilingStage.ParseFields);
        }
    }


    void instantiateHierarchy() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        var templ = this.getTemplateClass();
        if(templ.getCompilingStage().lte(CompilingStage.ResolveHierarchicalClasses)){
            Compiler.processClassTillStage(templ,CompilingStage.ResolveHierarchicalClasses);
        }
        var instantiationArguments = this.getGenericSource().instantiationArguments();
        List<ClassDef> list = new ArrayList<>();
        for (ClassDef i : templ.getInterfaces()) {
            var existed = new MutableBoolean();
            ClassDef instantiate = i.instantiateAsReferenceClass(instantiationArguments, existed);
            if(existed.isFalse() && instantiate instanceof GenericConcreteType genericConcreteType){
                this.registerConcreteType(genericConcreteType);
            }
            list.add(instantiate);
        }
        this.setInterfaces(list);
        if(templ.getSuperClass() != null) {
            ClassDef instantiated = templ.getSuperClass().instantiateAsReferenceClass(instantiationArguments, null);
            if(instantiated instanceof ConcreteType concreteType){
                this.registerConcreteType(concreteType);
            }
            this.setSuperClass(instantiated);       // TODO and parameterized superclass
        }
        if(templ.isInterfaceOrTrait() && templ.getPermitClass() != null){
            ClassDef instantiated = templ.getPermitClass().instantiateAsReferenceClass(instantiationArguments, null);
            if(instantiated instanceof ConcreteType concreteType){
                this.registerConcreteType(concreteType);
            }
            this.setPermitClass(instantiated);
        }

        this.instantiateMetaClass();

        this.setCompilingStage(CompilingStage.ParseFields);
    }

    public MetaClassDef resolveMetaclass() throws CompilationError {       // in ResolveHierarchicalClasses
        if(this.isInGenericInstantiation()) {
            return instantiateMetaClass();
        }
        MetaClassDef metaClass = getMetaClassDef();
        if (metaClass == null) {
            if (this.getSuperClass() != null && this.getSuperClass() != this) {
                if(this.getSuperClass().getCompilingStage() == CompilingStage.ResolveHierarchicalClasses){
                    this.getSuperClass().resolveHierarchicalClasses();
                }
                var superMeta = this.superClass.resolveMetaclass();
                if( superMeta != null) {
                    MetaClassDef mockMeta = new MetaClassDef(root, this, superMeta.getMetaLevel(), null);
                    mockMeta.setSuperClass(superMeta);
                    mockMeta.setMetaClassDef(superMeta.resolveMetaclass());
                    mockMeta.setSourceLocation(superMeta.getSourceLocation());
                    mockMeta.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
                    this.setMetaClassDef(mockMeta);
                    this.getPackage().addChild(mockMeta);
                    return mockMeta;
                }
            } else {
                return null;        // no metaclass defined
//                MetaClassDef metaClassDef = new MetaClassDef(classDef, classDef instanceof MetaClassDef ? 2 : 1, null);
//                classDef.setMetaClassDef(metaClassDef);
//                classes.add(metaClassDef);
//                return metaClassDef;
            }
        } else {
            //TODO super meta.meta
            if(this.getSuperClass() != null){
                var superMeta = this.getSuperClass().resolveMetaclass();
                if(superMeta != null) {
                    metaClass.setSuperClass(superMeta);
                }
            }
            return metaClass;
        }
        return null;

    }

    public MetaClassDef instantiateMetaClass() throws CompilationError {
        var templ = this.getTemplateClass();
        MetaClassDef templateMetaClass = templ.getMetaClassDef();
        if(templateMetaClass == null) return null;

        var instantiationArguments = this.getGenericSource().instantiationArguments();

        MutableBoolean returnExisted = new MutableBoolean();
        MetaClassDef metaClassDef = templateMetaClass.instantiate(instantiationArguments, returnExisted);
        this.setMetaClassDef(metaClassDef);
        if(returnExisted.isTrue()) return metaClassDef;

        metaClassDef.setCompilingStage(CompilingStage.ParseFields);
        // resolve hierarchy for children
        for (var d : metaClassDef.getAllDescendants().getUniqueElements()) {
            ClassDef classDef = (ClassDef) d;
            GenericSource genericSource = classDef.getGenericSource();
            if(genericSource != null) {
                classDef.instantiateHierarchy();
                classDef.setCompilingStage(CompilingStage.ParseFields);
            }
        }
        return metaClassDef;
    }

    public boolean instantiateFields() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.InheritsFields) return false;

        var templ = this.getTemplateClass();
        if(templ.getCompilingStage() == CompilingStage.InheritsFields) {
            templ.inheritsFields();
        }
        var instantiationArguments = this.getGenericSource().instantiationArguments();

        for (Map.Entry<String, Field> fieldEntry : templ.getFields().entrySet()) {
            Field field = fieldEntry.getValue();
            Field newField = field.applyTemplate(instantiationArguments, this);
            this.addField(newField);
        }
        this.instantiateFieldsForInterfacesAndTraits();
        this.nextCompilingStage(CompilingStage.ValidateNewFunctions);
        return true;
    }

    protected void instantiateFieldsForInterfacesAndTraits() throws CompilationError {
        var templ = this.getTemplateClass();
        var instantiationArguments = this.getGenericSource().instantiationArguments();
        if(templ.isInterfaceOrTrait() && templ.getPermitClass() != null && templ.fieldForPermitClass != null){
            this.setFieldForPermitClass(this.getFields().get(templ.fieldForPermitClass.name));
        }
        for (Map.Entry<ClassDef, Field> entry : templ.traitFields.entrySet()) {
            this.traitFields.put(entry.getKey().instantiateAsReferenceClass(instantiationArguments, null), this.getFields().get(entry.getValue().name));
        }
        //TODO wrapper interfaces
    }

    public ClassDef getTemplateClass(){
        if(this.getGenericSource() != null) return getGenericSource().originalTemplate();
        return this;
    }

    private boolean isTypeArgumentsMatch(ClassRefLiteral[] myTypeArguments, ClassRefLiteral[] anotherArguments) {
        if(anotherArguments.length != myTypeArguments.length) return false;

        TypeParamsContext paramsContext = this.getTemplateClass().getTypeParamsContext();
        for (int i = 0; i < myTypeArguments.length; i++) {
            var p = paramsContext.get(i);
            var variance = p.getSharedGenericTypeParameterClassDef().getVariance();
            var a1 = myTypeArguments[i].getClassDefValue();
            if (a1 == this.getRoot().getAnyClass()) return true;
            var a2 = anotherArguments[i].getClassDefValue();
            switch (variance){
                case Invariance:
                    if(a1 instanceof ClassBound av1){
                        if(!a1.isThatOrSuperOfThat(a2)) return false;
                    } else {
                        if (a1 != a2) return false;
                    }
                    break;
                case Covariance:
                    if(!a1.isThatOrSuperOfThat(a2)) return false;
                    break;
                case Contravariance:
                    if(!a2.isThatOrSuperOfThat(a1)) return false;
                    break;
            }
        }
        return true;
    }

    /**
     * validate members, abstract function with body, and others
     * all abstract functions implemented?
     */
    public void verifyMembers() throws SyntaxError {
        if(this.isInGenericInstantiation()){
            return;
        }

        for (ClassDef child : this.getUniqueChildren()) {
            if(child instanceof FunctionDef functionDef){
                if(functionDef.getCompilingStage() == CompilingStage.Compiled) continue;

                var methodBodyContext = functionDef.getMethodBodyContext();
                boolean hasBody = (methodBodyContext != null && !(methodBodyContext instanceof AgoParser.MBEmptyContext)) || functionDef.isNative();
                if(functionDef instanceof InterfaceFunctionWrapper){
                    hasBody = true;
                }

                if(functionDef.isAbstract()){
                    if(hasBody){
                        throw unit.syntaxError(methodBodyContext, "body not allowed for abstract method");
                    }
                    if(!this.isAbstract()){
                        if(functionDef.getParent() != this){
                            throw unit.syntaxError(this.getDeclarationName(), "abstract method '%s' not implemented".formatted(functionDef));
                        }
                        throw unit.syntaxError(functionDef.getMethodDecl().methodStarter(), "abstract methods are only allowed in abstract classes, traits and interfaces");
                    }
                } else {
                    if(!hasBody){
                        if(functionDef.isConstructor() || functionDef.isGetter() || functionDef.isSetter() || functionDef instanceof DefaultValueFunDef){
                            continue;
                        }
                        throw unit.syntaxError(functionDef.getDeclarationAst(), "method body not found for non-abstract method");
                    }
                }
            }
        }
    }

    public void verifyFunctionsImplemented(ClassDef abstractClassDef, ParserRuleContext referenceAst) throws ResolveError {
        for (ClassDef child : abstractClassDef.getUniqueChildren()) {
            if(child instanceof FunctionDef functionDef){
                if(functionDef.isAbstract()) {
                    var f = this.getSameSignatureFunction(functionDef);
                    if (f == null || f == functionDef || f.isAbstract()) {
                        throw unit.resolveError(referenceAst, "'%s' of '%s' not implemented".formatted(functionDef.getName(), abstractClassDef.getFullname()));
                    }
                }
            }
        }

    }

    public boolean gotFromInherited(ClassDef child) {
        return child.getParent() != this && child.getParent() instanceof ClassDef classDef && this.isDeriveFrom(classDef);
    }

    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments){
        return isAffectedByTypeArguments(instantiationArguments, new HashSet<>());
    }

    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments, Set<ClassDef> visited) {
        if(visited.contains(this)) return false;
        visited.add(this);
        for(ClassDef p = this; p != null; p = p.getParentClass()){
            if(p.isGenericTemplate()){
                var r = instantiationArguments.canApplyOnTemplate(p);
                if(r) return true;
            }
        }
        if(this.getSuperClass() != null && this.getSuperClass() != this){
            if(this.getSuperClass().isAffectedByTypeArguments(instantiationArguments, visited)) return true;
        }
        if(this.getInterfaces() != null){
            for (ClassDef anInterface : this.getInterfaces()) {
                if(anInterface.isAffectedByTypeArguments(instantiationArguments, visited)) return true;
            }
        }
        if(this.genericSource != null){
            if(this.genericSource.instantiationArguments().canApply(instantiationArguments, visited)){
                return true;
            }
        }
        if(this.permitClass != null){
            if(this.permitClass.isAffectedByTypeArguments(instantiationArguments, visited)) return true;
        }
//        for (ClassDef child : this.getDirectChildren()) {
//            if(child.isAffectedByTypeArguments(instantiationArguments)) return true;
//        }
        return false;
    }

//    /**
//     * only for org.siphonlab.ago.compile.expression.Creator.NewProps#resolve(org.siphonlab.ago.compile.ClassDef, org.siphonlab.ago.compile.ClassDef)
//     * @return
//     */
//    public boolean isGenericInstantiateRequiredForNew() {
//        if(this.parent instanceof ClassDef cp){
//            return cp.isGenericInstantiateRequiredForNew();
//        }
//        return this.isGenericTemplateOrIntermediate();
//    }

    public void setTypeParamsContext(TypeParamsContext typeParamsContext) {
        this.typeParamsContext = typeParamsContext;
    }

    public void setGenericSource(GenericSource genericSource) {
        this.genericSource = genericSource;
    }

    public GenericSource getGenericSource() {
        return genericSource;
    }

    // compile fields initializer codes
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
        this.nextCompilingStage(CompilingStage.Compiled);   // Compiled
    }

    public Field getFieldForTrait(ClassDef trait) {
        return this.traitFields.get(trait);
    }

    public Field getFieldForPermitClass() {
        return fieldForPermitClass;
    }

    public void setFieldForPermitClass(Field fieldForPermitClass) {
        this.fieldForPermitClass = fieldForPermitClass;
    }

    public GetterSetterPair getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Map<String, GetterSetterPair> getAttributes() {
        return attributes;
    }

    public static ClassDef findCommonType(ClassDef typeA, ClassDef typeB) {
        if(typeA == null) return typeB;
        if(typeB.isThatOrDerivedFromThat(typeA)){
            return typeA;
        }
        if(typeA.isDeriveFrom(typeB)) return typeB;
        for(var sp = typeA.getSuperClass(); sp != null; sp = sp.getSuperClass()){
            if(typeB.isThatOrDerivedFromThat(sp)){
                return sp;
            }
            if(sp == sp.getSuperClass()) break;
        }
        for (var sp = typeB.getSuperClass(); sp != null; sp = sp.getSuperClass()) {
            if (typeA.isThatOrDerivedFromThat(sp)) {
                return sp;
            }
            if (sp == sp.getSuperClass()) break;
        }

        if(typeA.getInterfaces() != null) {
            for (ClassDef anInterface : typeA.getInterfaces()) {
                var c = findCommonType(anInterface, typeB);
                if(c != null) return c;
            }
        }
        if (typeB.getInterfaces() != null) {
            for (ClassDef anInterface : typeB.getInterfaces()) {
                var c = findCommonType(anInterface, typeA);
                if (c != null) return c;
            }
        }
        return null;
    }


    public boolean isEnum(){
        return this.classType == AgoClass.TYPE_ENUM;
    }

    protected Map<String, Literal<?>> enumValues;
    protected PrimitiveClassDef enumBasePrimitiveType;
    public PrimitiveClassDef getEnumBasePrimitiveType() {
        return enumBasePrimitiveType;
    }

    public Map<String, Literal<?>> getEnumValues() {
        return enumValues;
    }

    public void setEnumBasePrimitiveType(PrimitiveClassDef enumBasePrimitiveType) {
        this.enumBasePrimitiveType = enumBasePrimitiveType;
    }

    public void setEnumValues(Map<String, Literal<?>> enumValues) {
        this.enumValues = enumValues;
    }

    public Field resolveEnumField(Literal<?> literal) {
        var metaFields = this.getMetaClassDef().getFields();
        for (Map.Entry<String, Literal<?>> entry : this.enumValues.entrySet()) {
            try {
                if(Equals.isLiteralEquals(literal,entry.getValue())){
                    return metaFields.get(entry.getKey());
                }
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }


    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    /**
     * whether I have type parameters, ignore my superclass, interfaces, and other related classes
     * @return
     */
    public boolean isGenericType() {
        return this.getTypeCode().isGeneric() || this.isGenericInstantiation() && !this.getGenericSource().instantiationArguments().isTerminated();
    }

    public Set<ClassDef> getAllAncestors(boolean includeSelf) {
        var set = new IdentityHashSet<ClassDef>();
        if(includeSelf) set.add(this);
        if(this.getSuperClass() != this && this.getSuperClass() != null) {
            if(set.add(this.getSuperClass())) {
                set.addAll(this.getSuperClass().getAllAncestors(false));
            }
        }
        if(this.getInterfaces() != null){
            for (ClassDef anInterface : this.getInterfaces()) {
                if(set.add(anInterface)) {
                    set.addAll(anInterface.getAllAncestors(false));
                    if(anInterface.getPermitClass() != null){
                        if(set.add(anInterface.getPermitClass())){
                            set.addAll(anInterface.getPermitClass().getAllAncestors(false));
                        }
                    }
                }
            }

        }
        return set;
    }

    public boolean isGenericTerminated(){
        return isGenericTerminated(new HashSet<>());
    }

    public boolean isGenericTerminated(Set<ClassDef> visited) {
        visited.add(this);
        if(this.isInGenericTemplate()) return false;
        if(this.getSuperClass() != null && this.getSuperClass() != this){
            if(!this.getSuperClass().isGenericTerminated(visited)) return false;
        }
        if(this.getInterfaces() != null){
            for (ClassDef anInterface : this.getInterfaces()) {
                if(!anInterface.isGenericTerminated(visited)) return false;
            }
        }
        if(this.genericSource != null){
            if(!this.genericSource.instantiationArguments().isTerminated()){
                return false;
            }
        }
        if(this.permitClass != null && !visited.contains(permitClass)){
            visited.add(permitClass);
            if(!this.permitClass.isGenericTerminated(visited)) return false;
        }
        return true;
    }

    public boolean isGenericInstantiation() {
        return this.genericSource != null && !this.isGenericTemplate();
    }

    public void createTemplateDefaultGenericSource() throws CompilationError {
        InstantiationArguments instantiationArguments = typeParamsContext.getDefaultInstantiationArguments();
        this.setGenericSource(new GenericSource(this, instantiationArguments, typeParamsContext.createDefaultArgumentsArray(), true));
        this.putInstantiatedClassToCache(instantiationArguments, this);
    }

    protected void addExtensionMethod(FunctionDef functionDef) throws DuplicatedKeyException {
        if(this.isInGenericInstantiation()){
            this.getTemplateClass().addExtensionMethod(functionDef);
            return;
        }
        if(extensionMethods.containsKey(functionDef.getName())){
            throw new DuplicatedKeyException("extension method '%s' for '%s' already exists".formatted(functionDef.getName(), this.getFullname()));
        }
        this.extensionMethods.add(functionDef);
    }

    public FunctionDef getExtensionMethod(String name) {
        for(var c = this; c!= null; c = c.getSuperClass()){
            if(c.isInGenericInstantiation()){
                c = c.getTemplateClass();
            }
            var f = c.extensionMethods.get(name);
            if(f != null) return f;

            if(c.getInterfaces() != null) {
                for (var i : c.getInterfaces()) {
                    if(i.isInGenericInstantiation()){
                        i = i.getTemplateClass();
                    }
                    f = i.extensionMethods.get(name);
                    if(f != null) return f;
                }
            }

            if(c.getSuperClass() == c){
                break;
            }
        }
        return null;
    }
}
