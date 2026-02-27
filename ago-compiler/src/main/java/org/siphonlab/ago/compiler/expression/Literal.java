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
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.parser.AgoParser;

public abstract class Literal<T> implements LiteralResultExpression, TermExpression {
    
    public T value;

    public final ClassDef classDef;
    private SourceLocation sourceLocation;
    private Expression parent;

    public Literal(ClassDef classDef, T value){
        this.classDef = classDef;
        this.value = value;
    }

    public static Literal<?> parse(AgoParser.LiteralContext literal, Root root, SourceLocation sourceLocation) throws TypeMismatchError {
        if(literal instanceof AgoParser.LTemplateStringContext lTemplateString){
            for (var atom : lTemplateString.templateStringLiteral().templateStringAtom()) {
                AgoParser.ExpressionContext atomExpr = atom.expression();
                if (atomExpr != null) {
                    throw new TypeMismatchError("template string not allowed for parameterized class and initializer", sourceLocation);
                }
            }
        }
        var r = parse(literal, root);
        r.setSourceLocation(sourceLocation);
        return r;
    }

    private static Literal<?> parse(AgoParser.LiteralContext literal, Root root){
        if(literal instanceof AgoParser.LIntegerContext lInteger){
            AgoParser.IntegerLiteralContext integerLiteral = lInteger.integerLiteral();
            return parseIntegerLiteral(integerLiteral);
        } else if(literal instanceof AgoParser.LStringContext lString) {
            var s = lString.STRING_LITERAL();
            String s1 = Compiler.parseStringLiteral(s);
            return new StringLiteral(s1);
        } else if(literal instanceof AgoParser.LTemplateStringContext lTemplateString){
            return LiteralParser.parseTemplateStringWithoutExpression(lTemplateString);
        } else if(literal instanceof AgoParser.LCharContext lChar){
            return new CharLiteral(LiteralParser.parseCharLiteral(lChar.CHAR_LITERAL().getText()));
        } else if(literal instanceof AgoParser.LNullContext lNullContext){
            return root.createNullLiteral();
        } else if(literal instanceof AgoParser.LBoolContext lBoolContext){
            return new BooleanLiteral(Boolean.parseBoolean(lBoolContext.getText()));
        } else if(literal instanceof AgoParser.LFloatContext floatContext){
            return parseFloatLiteral(floatContext.floatLiteral());
        }
        throw new UnsupportedOperationException();
    }

    public static Literal<?> parseIntegerLiteral(AgoParser.IntegerLiteralContext integerLiteral) {
        var n = LiteralParser.parseIntegerLiteral(integerLiteral.getText());
        if(n instanceof Integer i){
            return new IntLiteral(i);
        } else if(n instanceof Long l){
            return new LongLiteral(l);
        } else {
            return new ByteLiteral((Byte)n);
        }
    }

    public static Literal<?> parseFloatLiteral(AgoParser.FloatLiteralContext floatLiteral) {
        var n = LiteralParser.parseFloatLiteral(floatLiteral.getText());
        if(n instanceof Double d){
            return new DoubleLiteral(d);
        } else {
            return new FloatLiteral((Float) n);
        }
    }


    @Override
    public ClassDef inferType() {
        return classDef;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    @Override
    public Expression transform() throws CompilationError {
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);      // add to const pool
        blockCompiler.enter(this);

        blockCompiler.getCode().assignLiteral(localVar.getVariableSlot(), this);
        blockCompiler.leave(this);
    }

    @Override
    public Literal<T> visit(BlockCompiler blockCompiler) throws CompilationError {
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        //nothing to do
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Literal<T> setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
        return this;
    }

    @Override
    public SourceLocation getSourceLocation() {
        if(sourceLocation == null && this.parent != null){
            return this.parent.getSourceLocation();
        }
        return sourceLocation;
    }

    @Override
    public Expression setParent(Expression expression) {
        this.parent = expression;
        return this;
    }

    @Override
    public Expression getParent() {
        return parent;
    }

    public abstract String getId();

    public TypeCode getTypeCode() {
        return classDef.getTypeCode();
    }

    public abstract  Literal<?> withSourceLocation(SourceLocation sourceLocation);
}

