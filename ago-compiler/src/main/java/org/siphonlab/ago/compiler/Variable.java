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

import org.antlr.v4.runtime.ParserRuleContext;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Cast;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

public class Variable {
    protected AgoParser.FormalParameterContext parameterContext;
    protected String name;
    protected ClassDef ownerClass;
    protected int modifiers;
    private ClassDef type;
    private SlotDef slot;
    private int slotIndex = -1;          // for ClassParser
    private AgoParser.ExpressionContext initializer;
    private ParserRuleContext declaration;
    protected Literal<?> constLiteralValue;
    private SourceLocation sourceLocation;

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClassDef getOwnerClass() {
        return ownerClass;
    }

    public void setOwnerClass(ClassDef ownerClass) {
        this.ownerClass = ownerClass;
    }

    public ClassDef getType() {
        return type;
    }

    public void setType(ClassDef type) {
        this.type = type;
        if(type instanceof ParameterizedClassDef.PlaceHolder placeHolder){
            placeHolder.registerReference(new ParameterizedClassDef.RefViaVariable(this));
        }
    }

    public void setSlot(SlotDef slot) {
        if(this.slot != null){
            throw new RuntimeException("slot already exists");
        }
        this.slot = slot;
        this.setSlotIndex(slot.getIndex());
    }

    public Variable(){

    }

    public SlotDef getSlot() {
        return slot;
    }

    public boolean isStatic() {
        return (this.modifiers & AgoClass.STATIC) !=0;
    }

    public boolean isField() {
        return (this.modifiers & AgoClass.FIELD_PARAM) !=0;
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

    @Override
    public String toString() {
        String ownerClassName = this.ownerClass != null ? this.ownerClass.getFullname() : "<NO OWNER>";
        if(this.slot != null) {
            return String.format("(Var %s.%s as %s slot:%s)", ownerClassName, this.getName(), this.getType(), this.slot.getIndex());
        } else {
            return String.format("(Var %s.%s as %s slot:n/a)", ownerClassName, this.getName(), this.getType());
        }
    }

    public Variable applyTemplate(InstantiationArguments instantiationArguments, ClassDef ownerClass) throws CompilationError {
        // must always clone, for its own class was affected by generic
        var clone = new Variable();
        clone.setName(this.name);
        clone.setOwnerClass(ownerClass);
        applyTemplate(clone, instantiationArguments);
        return clone;
    }

    protected void applyTemplate(Variable clone, InstantiationArguments instantiationArguments) throws CompilationError {
        var newType = this.type.instantiate(instantiationArguments, null);
        clone.setModifiers(this.modifiers);
        clone.setType(newType);
        clone.setSourceLocation(this.sourceLocation);
        clone.setSlotIndex(this.slotIndex);
        clone.setDeclaration(this.declaration);
        // clone.setConstLiteralValue(); that's impossible, T won't have const value
    }

    public void setInitializer(AgoParser.ExpressionContext initializer) throws CompilationError {
        this.initializer = initializer;
        if(this.isFinal() && initializer instanceof AgoParser.PrimaryExprContext primaryExprContext){
            if(primaryExprContext.primaryExpression() instanceof AgoParser.LiteralExprContext literalExpr){
                Literal<?> literalValue = Literal.parse(literalExpr.literal(), ownerClass.getRoot(), ownerClass.unit.sourceLocation(literalExpr));
                if(this.type.isPrimitive()){
                    var l = new Cast(literalValue, this.type).transform();
                    if(l instanceof Literal<?> l2){
                        this.constLiteralValue = l2;
                    }
                }
            }
        }
    }

    public AgoParser.ExpressionContext getInitializer() {
        return initializer;
    }

    public void setDeclaration(ParserRuleContext declaration) {
        this.declaration = declaration;
        if(declaration instanceof AgoParser.FormalParameterContext p){
            this.parameterContext = p;
        }
    }

    public ParserRuleContext getDeclaration() {
        return declaration;
    }

    public Literal<?> getConstLiteralValue() {
        return constLiteralValue;
    }

    public void setConstLiteralValue(Literal<?> constLiteralValue) {
        this.constLiteralValue = constLiteralValue;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
