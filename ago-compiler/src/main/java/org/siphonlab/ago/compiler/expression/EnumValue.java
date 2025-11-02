package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.Field;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class EnumValue extends ExpressionBase{

    private Var varOfEnumClass;
    private final Field enumField;
    private final ClassDef enumClass;
    private final Literal<?> enumValue;

    public EnumValue(Var.Field varOfEnumClass, Field field) {
        this.varOfEnumClass = varOfEnumClass;
        this.setSourceLocation(varOfEnumClass.getSourceLocation());
        this.enumField = field;
        this.enumClass = enumField.getType();
        this.enumValue = enumClass.getEnumValues().get(enumField.getName());
    }

    @Override
    public EnumValue transformInner() throws CompilationError {
        this.varOfEnumClass = this.varOfEnumClass.transform();
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return enumClass;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            varOfEnumClass.outputToLocalVar(localVar, blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public EnumValue setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(Enum %s %s)".formatted(enumField.getName(), enumValue);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof EnumValue enumValue){
            return this.enumValue.equals(enumValue.enumValue);
        } else if(obj instanceof Literal<?> literal){
            return this.enumValue.equals(literal);
        } else {
            return false;
        }
    }

    public Literal<?> toLiteral() {
        return this.enumValue;
    }
}
