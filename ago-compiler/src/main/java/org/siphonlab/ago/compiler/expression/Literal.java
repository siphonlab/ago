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
        if(literal instanceof AgoParser.LTemplateStringContext){
            throw new TypeMismatchError("template string not allowed for parameterized class and initializer", sourceLocation);
        }
        var r = parse(literal, root);
        r.setSourceLocation(sourceLocation);
        return r;
    }

    private static Literal<?> parse(AgoParser.LiteralContext literal, Root root){
        if(literal instanceof AgoParser.LIntegerContext lInteger){
            // TODO
            AgoParser.IntegerLiteralContext integerLiteral = lInteger.integerLiteral();
            IntLiteral s = parseIntegerLiteral(integerLiteral);
            if (s != null)
                return s;
        } else if(literal instanceof AgoParser.LStringContext lString) {
            var s = lString.STRING_LITERAL();
            String s1 = Compiler.parseStringLiteral(s);
            return new StringLiteral(s1);
        } else if(literal instanceof AgoParser.LCharContext lChar){
            return new CharLiteral(Compiler.parseStringLiteral(lChar.CHAR_LITERAL()).charAt(0));
        } else if(literal instanceof AgoParser.LNullContext lNullContext){
            return root.createNullLiteral();
        } else if(literal instanceof AgoParser.LBoolContext lBoolContext){
            return new BooleanLiteral(Boolean.parseBoolean(lBoolContext.getText()));
        } else if(literal instanceof AgoParser.LFloatContext floatContext){
            return new DoubleLiteral(Double.parseDouble(floatContext.getText()));
        }
        throw new UnsupportedOperationException();
    }

    public static IntLiteral parseIntegerLiteral(AgoParser.IntegerLiteralContext integerLiteral) {
        if(integerLiteral instanceof AgoParser.IDecContext dec){
            var s = dec.DECIMAL_LITERAL();
            return new IntLiteral(Integer.parseInt(s.getText()));
        }
        return null;
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

