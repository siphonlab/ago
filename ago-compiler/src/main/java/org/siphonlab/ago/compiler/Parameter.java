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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

import static org.siphonlab.ago.AgoClass.VAR_ARGS;

public class Parameter extends Field {
    private boolean getterSetterDisabled;

    // default value of Parameter, will compile to an inner function, named as defaultValue_N
    private AgoParser.ExpressionContext defaultValueAst;
    private FunctionDef defaultValueFun;

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

    public static String composeDefaultValueFunctionName(Parameter parameter){
        return "defaultValue_" + parameter.getName() + "#";
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

    public boolean isReceiverParameter() {
        return (this.modifiers & AgoClass.THIS_PARAM) != 0;
    }

    public boolean hasDefaultValue(){
        return (this.modifiers & AgoClass.DEFAULT_VALUE_PARAM) != 0;
    }

    public void setDefaultValueAst(AgoParser.ExpressionContext defaultValueAst) {
        this.defaultValueAst = defaultValueAst;
    }

    public AgoParser.ExpressionContext getDefaultValueAst() {
        return defaultValueAst;
    }

    public void setDefaultValueFun(FunctionDef defaultValueFun) {
        this.defaultValueFun = defaultValueFun;
    }

    public FunctionDef getDefaultValueFun() {
        return defaultValueFun;
    }
}
