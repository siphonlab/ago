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
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.generic.*;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.siphonlab.ago.AgoClass.GENERIC_TEMPLATE_NEG;
import static org.siphonlab.ago.AgoClass.TYPE_METACLASS;
import static org.siphonlab.ago.compiler.ClassFile.putLiteral;

public class ClassDef extends ClassContainer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassDef.class);

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


    public ClassDef(String name) {
        super(name);
        this.classType = AgoClass.TYPE_CLASS;
        slotsAllocator = new SlotsAllocator(this);
    }

    public ClassDef(String name, AgoParser.ClassDeclarationContext classDeclaration) {
        this(name);
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

        if(this.getGenericSource() != null){
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
                    this.slotsAllocator.allocateSlot(slot.getName(), slot.getTypeCode(), slot.getClassDef().instantiate(args, null));
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
        if(this.getGenericSource() != null){
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
        if(type.getGenericSource() != null){
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

        if(this.getGenericSource() != null) {
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
        if(this.getGenericSource() == null) createGetterAndSetter();

        var superClass = this.superClass;
        if(superClass != null && this.superClass != this) {
            if(superClass.compilingStage == CompilingStage.InheritsInnerClasses)
                superClass.inheritsChildClasses();

            var classes = superClass.getUniqueChildren();
            inheritsChildClasses(classes);
        }

        for (ClassDef implementedInterface : this.implementedInterfaces) {
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
                    var wrapperFun = new InterfaceFunctionWrapper(this, fun, field, field.getDeclaration());
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
                    var wrapperFun = new InterfaceFunctionWrapper(this, fun, wrapperFld, wrapperFld.getDeclaration());
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
            var mockConstructor = new ConstructorDef(AgoClass.CONSTRUCTOR | AgoClass.PUBLIC, "new#");
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
        this.addChild(new SetterFunction(field, setterContext));
    }

    private void createGetter(Field field, AgoParser.GetterContext getterContext) throws SyntaxError {
        this.addChild(new GetterFunction(field, getterContext));
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
        if(this.getGenericSource() != null) return this.getTemplateClass().getRoot();
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

    public boolean isPrimitiveFamily(){
        return this instanceof PrimitiveClassDef || this.isThatOrDerivedFromThat(getRoot().getPrimitiveTypeInterface());
    }

    public boolean isPrimitive() {
        return this instanceof PrimitiveClassDef;
    }

    public boolean isPrimitiveNumberFamily() {
        return this instanceof PrimitiveClassDef p && p.isNumber() || this.isThatOrDerivedFromThat(getRoot().getPrimitiveNumberTypeInterface());
    }

    /**
     * generic template or intermediate template that can accept TypeArguments
     * @return
     */
    public boolean isGenericTemplateOrIntermediate(){
        return (this.modifiers & AgoClass.GENERIC_TEMPLATE) != 0;
    }

    /**
     * indicate it's generic template, not include intermediate template, that can can accept ClassRef[]
     * @return
     */
    public boolean isGenericTemplate(){
        return this.typeParamsContext != null;
    }

    public boolean isInGenericInstantiation(){
        if ((this.modifiers & AgoClass.GENERIC_INSTANTIATION) != 0)
            return true;

        if (this.parent instanceof ClassDef p) {
            return p.isInGenericInstantiation();
        }
        return false;
    }

    public boolean isInGenericTemplate(){
        if ((this.modifiers & AgoClass.GENERIC_TEMPLATE) != 0) return true;
        if(this.parent instanceof ClassDef p) return p.isInGenericTemplate();
        return false;
    }

    public boolean isPrimitiveBoxed(){
        if(this.isEnum()) return true;
        return  (this.isThatOrDerivedFromThat(PrimitiveClassDef.BYTE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BYTE.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.SHORT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.SHORT.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.CHAR.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.CHAR.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.INT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.INT.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.FLOAT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.FLOAT.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.LONG.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.LONG.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.DOUBLE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.DOUBLE.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.STRING.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.STRING.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.BOOLEAN.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BOOLEAN.getBoxerInterface())) ||
                (this.isThatOrDerivedFromThat(PrimitiveClassDef.CLASS_REF.getBoxedType())
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
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.BYTE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BYTE.getBoxerInterface())) return TypeCode.BYTE;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.SHORT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.SHORT.getBoxerInterface())) return TypeCode.SHORT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.CHAR.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.CHAR.getBoxerInterface())) return TypeCode.CHAR;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.INT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.INT.getBoxerInterface())) return TypeCode.INT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.FLOAT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.FLOAT.getBoxerInterface())) return TypeCode.FLOAT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.LONG.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.LONG.getBoxerInterface())) return TypeCode.LONG;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.DOUBLE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.DOUBLE.getBoxerInterface())) return TypeCode.DOUBLE;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.STRING.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.STRING.getBoxerInterface())) return TypeCode.STRING;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.BOOLEAN.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BOOLEAN.getBoxerInterface())) return TypeCode.BOOLEAN;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.CLASS_REF.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.CLASS_REF.getBoxerInterface())) return TypeCode.CLASS_REF;
        return getTypeCode();
    }
    
    public PrimitiveClassDef getUnboxedType(){
        if(this.isEnum()){
            return enumBasePrimitiveType;
        }
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.BYTE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BYTE.getBoxerInterface())) return PrimitiveClassDef.BYTE;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.SHORT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.SHORT.getBoxerInterface())) return PrimitiveClassDef.SHORT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.CHAR.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.CHAR.getBoxerInterface())) return PrimitiveClassDef.CHAR;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.INT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.INT.getBoxerInterface())) return PrimitiveClassDef.INT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.FLOAT.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.FLOAT.getBoxerInterface())) return PrimitiveClassDef.FLOAT;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.LONG.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.LONG.getBoxerInterface())) return PrimitiveClassDef.LONG;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.DOUBLE.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.DOUBLE.getBoxerInterface())) return PrimitiveClassDef.DOUBLE;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.STRING.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.STRING.getBoxerInterface())) return PrimitiveClassDef.STRING;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.BOOLEAN.getBoxedType()) || this.isDeriveFrom(PrimitiveClassDef.BOOLEAN.getBoxerInterface())) return PrimitiveClassDef.BOOLEAN;
        if(this.isThatOrDerivedFromThat(PrimitiveClassDef.CLASS_REF.getBoxedType())) return PrimitiveClassDef.CLASS_REF;
        throw new UnsupportedOperationException("'%s' is not a boxer type".formatted(this.getFullname()));
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
    // it not need support PermitClass, Parameterized class and Generic Variance
    public int distanceToSuperClass(ClassDef superClass) {
        if(this == superClass) return 0;

        int distance = 0;
        for(var c = this; c != null; c = c.superClass, distance++){
            if(c != this){
                if(c == superClass) return distance;
            }
            if(superClass.isInterfaceOrTrait()) {
                for (ClassDef implementedInterface : c.implementedInterfaces) {
                    var d = implementedInterface.distanceToSuperClass(superClass);
                    if (d != -1) return distance + d;
                }
            }
            if(c == c.superClass) return distance;
        }
        return -1;
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
        return asThatOrSuperOfThat(anotherClass, null);
    }

    public boolean isThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        return asThatOrSuperOfThat(anotherClass, visited) != null;
    }

    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited){
        if(this == anotherClass) {
            return this;
        }

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }

//        ClassDef anyClass = getRoot().getAnyClass();      // any class only works in ClassBound
        if(this == getRoot().getObjectClass()) return this;

        if(anotherClass instanceof GenericConcreteType && this instanceof GenericConcreteType){        // Template -> GenericSource
            if(anotherClass.getTemplateClass() == this.getTemplateClass() && isTypeArgumentsMatch(anotherClass)){
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
            var r = this.asThatOrSuperOfThat(p.baseClass, visited);
            if(r != null){
                return r;
            }
        }

        if(anotherClass.superClass != null && anotherClass.superClass != anotherClass){    // solve derived class in recursive
            var sp = this.asThatOrSuperOfThat(anotherClass.superClass, visited);
            if(sp != null) return sp;
        }
        if(anotherClass.isInterfaceOrTrait()){
            var permitClass = anotherClass.getPermitClass();
            var t = this.asThatOrSuperOfThat(permitClass, visited == null ? new LinkedHashSet<>() : visited);
            if(t != null) return t;
        }
        if(this.isInterfaceOrTrait()) {
            for (ClassDef implementedInterface : anotherClass.implementedInterfaces) {
                ClassDef i = this.asThatOrSuperOfThat(implementedInterface, visited == null ? new LinkedHashSet<>() : visited);
                if(i != null){
                    return i;
                }
            }
        }
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
            this.dependencies.add(metaClassDef);
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

        if(dependency.isDependingOn(this)) return false;
        this.dependencies.add(dependency);
        if(dependency instanceof ConcreteType cd) {
            registerConcreteType(cd);
        } else if(dependency instanceof GenericTypeCode.GenericCodeAvatarClassDef r){
            registerConcreteType(r.getTypeCode().getGenericTypeParameterClassDef());
        } else {
            if(!dependency.isPrimitive())
                this.idOfClass(dependency);
        }
        return true;
    }

    public boolean isDependingOn(ClassDef classDef) {
        if(this == classDef) return false;
        for (ClassDef dependency : this.dependencies) {
            if(dependency == classDef || dependency.isDependingOn(classDef)) return true;
        }
        return false;
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

        if (classDef.isPrimitive()) throw new UnsupportedOperationException(classDef + " is primary type");

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
        this.registerConcreteType(arrayType);
        if(!elementType.isPrimitive()) this.idOfConstString(elementType.getFullname());
        return arrayType;
    }

    public Map<String, ConcreteType> getConcreteTypes() {
        if(this.getParentClass() != null){
            return this.getParentClass().getConcreteTypes();
        }
        return concreteTypes;
    }

    public void shiftToTemplate() {
        this.setModifiers(this.modifiers | AgoClass.GENERIC_TEMPLATE);
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
        return typeParamsContext;
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


    private Map<ArrayLiteral, Integer> arrayBLOBsIndex = new HashMap<>();
    private List<byte[]> arrayBLOBs = new ArrayList<>();
    public int getOrCreateBLOB(List<? extends Literal<?>> literals, ArrayLiteral arrayLiteral) throws TypeMismatchError {
        if(!this.isTop()) return this.getParentClass().getOrCreateBLOB(literals, arrayLiteral);
        var existed = arrayBLOBsIndex.get(arrayLiteral);
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
        int index = arrayBLOBs.size();
        arrayBLOBsIndex.put(arrayLiteral, index);
        arrayBLOBs.add(data);
        return index;
    }

    public List<byte[]> getArrayBLOBs() {
        return arrayBLOBs;
    }

    @Override
    public GenericConcreteType getOrCreateGenericInstantiationClassDef(ClassDef templateClass, ClassRefLiteral[] typeArguments, MutableBoolean returnExisted) throws CompilationError {
        InstantiationArguments args = new InstantiationArguments(templateClass.getGenericSource() == null ? templateClass.getTypeParamsContext() : templateClass.getGenericSource().originalTemplate().getTypeParamsContext(), typeArguments);
        if(this.getGenericSource() != null){
            args = this.getGenericSource().instantiationArguments().applyChild(args);
        }
        return (GenericConcreteType)templateClass.instantiate(args, returnExisted);
    }

    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        var templ = this;
        InstantiationArguments args;
        GenericSource genericSource = this.getGenericSource();
        if(genericSource != null){
            templ = genericSource.originalTemplate();
            var myArgs = genericSource.instantiationArguments();
            if(myArgs.isIntermediate()){
                args = this.mixInstantiationArgs(arguments);
                if(args.equals(myArgs)){
                    if (returnExisted != null) returnExisted.setTrue();
                    return this;
                }
            } else if(!templ.isAffectedByTemplate(arguments)) {
                if(returnExisted != null) returnExisted.setTrue();
                return this;
            } else {
                args = this.mixInstantiationArgs(arguments);
            }
        } else {
            if(!templ.isAffectedByTemplate(arguments)) {
                if(returnExisted != null) returnExisted.setTrue();
                return this;
            }
            args = arguments.takeFor(templ);
            assert args != null;
        }

        var existed = templ.getCachedInstantiatedClass(args);
        if(existed != null) {
            if (!args.equals(arguments) && !this.instantiatingChildren.contains(args)) {    // arguments changed, try children
                this.instantiateChildren(existed, args);
            }
            if(returnExisted != null) returnExisted.setTrue();
            return existed;
        } else {
            if(templ.isGenericTemplateOrIntermediate() && args.getSourceTemplate() == templ){
                var parent = this.parent;
                if(this.parent instanceof ClassDef c){
                    InstantiationArguments argsForParent = args.takeFor(c);
                    if(argsForParent != null) {
                        parent = c.instantiate(argsForParent, null);
                    }
                }
                if(templ instanceof FunctionDef templFun){
                    if(templFun instanceof InterfaceFunctionWrapper interfaceFunctionWrapper){
                        return new GenericInstantiationInterfaceFunctionWrapper(interfaceFunctionWrapper, (ClassContainer) parent, args);
                    } else {
                        return new GenericInstantiationFunctionDef(templFun, (ClassContainer) parent, args);
                    }
                } else {
                    return new GenericInstantiationClassDef(templ, (ClassContainer) parent, args);
                }
            } else {
                return cloneForInstantiate(args, returnExisted);
            }
        }
    }

    protected void instantiateChildren(ClassDef instantiatedClass, InstantiationArguments arguments) throws CompilationError {
        this.instantiatingChildren.add(arguments);
        for (ClassDef child : this.getUniqueChildren()) {
            if(!this.gotFromInherited(child)){
                var returnExisted = new MutableBoolean();
                ClassDef instantiated = child.instantiate(arguments, returnExisted);
                if(returnExisted.isFalse() && !(child instanceof GenericConcreteType)) {      // GenericInstantiationClassDef will append to parent automatically
                    instantiatedClass.addChild(instantiated);
                }
                if(child.getGenericSource() != null){
                    child.getTemplateClass().putInstantiatedClassToCache(instantiatedClass.mixInstantiationArgs(arguments), instantiated);
                }
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
     * @param returnExisted
     * @return
     */
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var clone = new ClassDef(name, classDeclaration);
        cloneTo(instantiationArguments, clone);
        return clone;
    }

    public void cloneTo(InstantiationArguments instantiationArguments, ClassDef instantiateClass) throws CompilationError {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("apply template instantiation class %s via %s".formatted(instantiateClass, getGenericSource()));

        instantiateClass.setGenericSource(new GenericSource(this, instantiationArguments));
        this.putInstantiatedClassToCache(instantiationArguments, instantiateClass);

        if(instantiationArguments.getSourceTemplate() != this){
            var args = instantiationArguments.takeFor(this);
            if(args != null){
                this.putInstantiatedClassToCache(args, instantiateClass);
            }
        }

        if(instantiateClass instanceof GenericConcreteType) {
            instantiateClass.setModifiers((this.getModifiers() & GENERIC_TEMPLATE_NEG) | AgoClass.GENERIC_INSTANTIATION);
            if (instantiationArguments.isIntermediate()) {  // for intermediate class, there is no TypeParamsContext, instead of forwardable Arguments
                instantiateClass.setModifiers(instantiateClass.getModifiers() | AgoClass.GENERIC_TEMPLATE);
            }
        } else {
            // inner template classes or inner normal classes
            instantiateClass.setModifiers(this.getModifiers());
            if(this.isGenericTemplate()){
                instantiateClass.setTypeParamsContext(this.getTypeParamsContext());
            }
        }
        instantiateClass.setUnit(this.getUnit());
        instantiateClass.setSourceLocation(this.getSourceLocation());        //TODO the source location assign to template's source location?

        if(instantiateClass instanceof FunctionDef tempFun){
            FunctionDef targetFun = (FunctionDef) instantiateClass;
            targetFun.setNativeEntrance(tempFun.getNativeEntrance());
            targetFun.setCommonName(tempFun.getCommonName());
            targetFun.setThrowsExceptions(tempFun.getThrowsExceptions());
        }
        instantiateClass.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);      // ParseClassName and ParseGenericParams skipped, it will enter ResolveHierarchicalClasses and ParseField soon

        instantiateChildren(instantiateClass, instantiationArguments);
    }

    public GenericTypeCode findGenericType(String genericTypeName) {
        //TODO TypeParamsContext already recusive its parent template
        var t = this.typeParamsContext;
        if(t == null){
            if(this.parent instanceof ClassDef p) {
                return p.findGenericType(genericTypeName);
            }
            return null;
        }
        var r = t.get(genericTypeName);      // TypeParamsContext.get(name) will recursive its parent
        if(r == null && this.parent instanceof ClassDef p){
            return p.findGenericType(genericTypeName);
        }
        return r;
    }

    public boolean isClass() {
        return this.classType == AgoClass.TYPE_CLASS || this.classType == TYPE_METACLASS;
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

        if(this.getGenericSource() != null){
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
            ClassDef instantiate = i.instantiate(instantiationArguments, existed);
            if(existed.isFalse() && instantiate instanceof GenericConcreteType genericConcreteType){
                this.registerConcreteType(genericConcreteType);
            }
            list.add(instantiate);
        }
        this.setInterfaces(list);        // TODO parameterized interfaces
        if(templ.getSuperClass() != null) {
            this.setSuperClass(templ.getSuperClass().instantiate(instantiationArguments, null));       // TODO and parameterized superclass
        }
        if(templ.isInterfaceOrTrait() && templ.getPermitClass() != null){
            this.setPermitClass(templ.getPermitClass().instantiate(instantiationArguments, null));
        }

        for (ClassDef child : this.getUniqueChildren()) {
            if(!this.gotFromInherited(child)){
                child.instantiateHierarchy();
            }
        }

        this.instantiateMetaClass();

        this.setCompilingStage(CompilingStage.ParseFields);     // bypass ValidateHierarchy
    }

    public MetaClassDef resolveMetaclass() throws CompilationError {       // in ResolveHierarchicalClasses
        if(this.getGenericSource() != null) {
            return instantiateMetaClass();
        }
        MetaClassDef metaClass = getMetaClassDef();
        if (metaClass == null) {
            if (this.getSuperClass() != null && this.getSuperClass() != this) {
                var superMeta = this.superClass.resolveMetaclass();
                if( superMeta != null) {
                    MetaClassDef mockMeta = new MetaClassDef(this, superMeta.getMetaLevel(), null);
                    mockMeta.setSuperClass(superMeta);
                    mockMeta.setMetaClassDef(superMeta.resolveMetaclass());
                    mockMeta.setSourceLocation(superMeta.getSourceLocation());
                    mockMeta.setCompilingStage(superMeta.getCompilingStage());
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
            this.traitFields.put(entry.getKey().instantiate(instantiationArguments, null), this.getFields().get(entry.getValue().name));
        }
        //TODO wrapper interfaces
    }

    public ClassDef getTemplateClass(){
        if(this.getGenericSource() == null) return null;
        return this.getGenericSource().originalTemplate();
    }

    // this is an instantiation class
    // i.e. this is Producer<Animal>, another is Producer<Cat>
    // and this is lang.Function<lang.Any>, another is lang.Function2<int|int|int>
    @Deprecated
    public GenericConcreteType findAssignableGenericInstantiationClass(ClassDef anotherClass, Set<ClassDef> visited) {
        if(this == anotherClass) return (GenericConcreteType) this;

        ClassDef r = this.getTemplateClass().asThatOrSuperOfThat(anotherClass, visited);
        if(r != null && r instanceof GenericConcreteType == false){
            this.getTemplateClass().asThatOrSuperOfThat(anotherClass, visited);
        }
        return (GenericConcreteType) r;
//        ClassDef myTempl = this.getTemplateClass();
//        for(var b = anotherClass; b != null; b = b.getSuperClass()) {
//            if (b instanceof GenericConcreteType gb) {
//                ClassDef bTempl = b.getTemplateClass();
//                if(myTempl.isThatOrSuperOfThat(bTempl, visited) && isTypeArgumentsMatch(b)) return gb;
//                if(bTempl.isInterfaceOrTrait()){
//                    ClassDef bTemplPermitClass = bTempl.getPermitClass();
//                    if(bTemplPermitClass instanceof ConcreteType gbt){
//                        // PermitClass is generic typed, handle it separate
//                        if(myTempl.isThatOrSuperOfThat(bTemplPermitClass.getTemplateClass(), visited == null ? new LinkedHashSet<>() : visited)
//                                    && isTypeArgumentsMatch(bTemplPermitClass))
//                            return gb;
//                    }
//                }
//            }
//            if(myTempl.isInterfaceOrTrait()) {
//                for (ClassDef i : b.getInterfaces()) {
//                    if (i instanceof GenericConcreteType gi
//                            && myTempl.isThatOrSuperOfThat(i.getTemplateClass(), visited == null ? new LinkedHashSet<>() : visited) && isTypeArgumentsMatch(i)) {
//                        return gi;
//                    }
//                }
//            }
//            if(b == b.getSuperClass()) break;
//        }
//        return null;
    }

    private boolean isTypeArgumentsMatch(ClassDef anotherClass) {
        ClassRefLiteral[] typeArgumentsArray = this.getGenericSource().instantiationArguments().getTypeArgumentsArray();
        ClassRefLiteral[] anotherArguments = anotherClass.getGenericSource().instantiationArguments().getTypeArgumentsArray();
        if(anotherArguments.length != typeArgumentsArray.length) return false;

        TypeParamsContext paramsContext = this.getTemplateClass().getTypeParamsContext();
        for (int i = 0; i < typeArgumentsArray.length; i++) {
            var p = paramsContext.get(i).getGenericTypeParameterClassDef();
            var variance = p.getVariance();
            var a1 = typeArgumentsArray[i].getClassDefValue();
            if (a1 == anotherClass.getRoot().getAnyClass()) return true;
            var a2 = anotherArguments[i].getClassDefValue();
            switch (variance){
                case Invariance:
                    if(a1 != a2) return false;
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
        if(this.getGenericSource() != null){
            return;
        }

        for (ClassDef child : this.getUniqueChildren()) {
            if(child instanceof FunctionDef functionDef){
                if(functionDef.getCompilingStage() == CompilingStage.Compiled) continue;

                var methodBodyContext = functionDef.getMethodBodyContext();
                boolean hasBody = (methodBodyContext != null && !(methodBodyContext instanceof AgoParser.MBEmptyContext));
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
                        if(functionDef.isConstructor() || functionDef.isGetter() || functionDef.isSetter()){
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

    public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
        return instantiationArguments.canApplyToTemplate(this);
    }

    /**
     * only for org.siphonlab.ago.compile.expression.Creator.NewProps#resolve(org.siphonlab.ago.compile.ClassDef, org.siphonlab.ago.compile.ClassDef)
     * @return
     */
    public boolean isGenericInstantiateRequiredForNew() {
        if(this.parent instanceof ClassDef cp){
            return cp.isGenericInstantiateRequiredForNew();
        }
        return this.isGenericTemplateOrIntermediate();
    }

    public void setTypeParamsContext(TypeParamsContext typeParamsContext) {
        this.typeParamsContext = typeParamsContext;
    }

    public void setGenericSource(GenericSource genericSource) {
        this.genericSource = genericSource;
    }

    public GenericSource getGenericSource() {
        return genericSource;
    }

    private InstantiationArguments findParentInstantiationArguments() {
        for(var p = this.parent; p != null; p = p.parent){
            if(p instanceof ClassDef pc && pc.getGenericSource() != null){
                return pc.getGenericSource().instantiationArguments();
            }
        }
        return null;
    }

    public InstantiationArguments mixInstantiationArgs(InstantiationArguments args) throws CompilationError {
        assert (this.getGenericSource() != null);
        var existedArgs = this.getGenericSource().instantiationArguments();
        ClassDef mySourceTmpl = this.getGenericSource().originalTemplate();
        var remappedValue = false;
        if(existedArgs.valuesMatch(args)){
            existedArgs = mapTypeValues(existedArgs, args);
            remappedValue = true;
        }
        if(existedArgs.getSourceTemplate() == mySourceTmpl){
            if(existedArgs.isIntermediate()) {
                existedArgs = existedArgs.applyIntermediate(args);
                if (mySourceTmpl.belongsTo(args.getSourceTemplate())) {
                    return args.applyChild(existedArgs);
                } else if(args.getSourceTemplate().belongsTo(mySourceTmpl)){
                    return existedArgs.applyChild(args);
                } else {
                    return existedArgs;
                }
            }
        }
        if(args.getSourceTemplate().belongsTo(existedArgs.getSourceTemplate())) {
            return existedArgs.applyChild(args);
        } else if(existedArgs.getSourceTemplate().belongsTo(args.getSourceTemplate())){
            return args.applyChild(existedArgs);
        } else {
            if(remappedValue) return existedArgs;
            return existedArgs.size() >= args.size() ? existedArgs : args;
        }
    }

    private InstantiationArguments mapTypeValues(InstantiationArguments existedTypeArguments, InstantiationArguments instantiationArguments) throws CompilationError {
        ClassRefLiteral[] typeArgumentsArray = existedTypeArguments.getTypeArgumentsArray();
        ClassRefLiteral[] newArray = new ClassRefLiteral[typeArgumentsArray.length];
        for (int i = 0; i < typeArgumentsArray.length; i++) {
            ClassRefLiteral classRefLiteral = typeArgumentsArray[i];
            var c = classRefLiteral.getClassDefValue();
            var b = false;
            if (c.getGenericSource() != null) {
                InstantiationArguments innerArgs = c.getGenericSource().instantiationArguments();
                if (instantiationArguments.canApplyToTemplate(c) || innerArgs.valuesMatch(instantiationArguments)) {
                    b = true;
                    newArray[i] = new ClassRefLiteral(c.instantiate(instantiationArguments, null));
                }
            }
            if(!b) newArray[i] = classRefLiteral;
        }
        return new InstantiationArguments(existedTypeArguments.getSourceTemplate().getTypeParamsContext(), newArray);
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

    public boolean isGenericType() {
        return this.getTypeCode().isGeneric() || this.getGenericSource() != null && this.getGenericSource().instantiationArguments().isIntermediate();
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
}
