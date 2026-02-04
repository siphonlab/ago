package org.siphonlab.ago.compiler;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

import static org.siphonlab.ago.AgoClass.VAR_ARGS;

public class Parameter extends Field {
    private boolean getterSetterDisabled;

    public Parameter(String fieldName, AgoParser.FormalParameterContext parameterContext) {
        super(fieldName, parameterContext);
    }

    public Parameter(ClassDef classDef, String name, AgoParser.FieldVariableDeclaratorContext variableDeclarator) {
        super(classDef, name, variableDeclarator);
    }

    @Override
    public String toString() {
        return this.getName() + " as " + this.getType();
    }

    @Override
    public Parameter applyTemplate(InstantiationArguments instantiationArguments, ClassDef ownerClass) throws CompilationError {
        var clone = new Parameter(ownerClass, this.name, this.fieldVariableDeclarator);
        clone.parameterContext = this.parameterContext;
        clone.setName(this.name);
        clone.setOwnerClass(ownerClass);
        applyTemplate(clone, instantiationArguments);
        if(getterSetterDisabled) clone.disableGetterSetter();
        return clone;
    }

    @Override
    public AgoParser.FieldGetterSetterContext getGetterSetter() {
        if(getterSetterDisabled) return null;
        return super.getGetterSetter();
    }

    public void disableGetterSetter() {
        this.getterSetterDisabled = true;
    }

    public boolean isVarArgs(){
        return (this.modifiers & VAR_ARGS) != 0;
    }
}
