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
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

public class Field extends Variable {

    protected AgoParser.FieldVariableDeclaratorContext fieldVariableDeclarator;

    public Field(String fieldName, AgoParser.FormalParameterContext parameterContext) {
        this.name = fieldName;
        this.parameterContext = parameterContext;
        this.setDeclaration(parameterContext);
    }

    public Field(ClassDef ownerClass, String name, AgoParser.FieldVariableDeclaratorContext variableDeclarator){
        this.name = name;
        this.fieldVariableDeclarator = variableDeclarator;
        this.ownerClass = ownerClass;
    }

    public AgoParser.FieldVariableDeclaratorContext getFieldVariableDeclarator() {
        return fieldVariableDeclarator;
    }

    public AgoParser.FieldGetterSetterContext getGetterSetter(){
        if(parameterContext instanceof AgoParser.DefaultParameterContext d) {
            return d.fieldGetterSetter();
        }
        var fieldVariableDeclarator = this.getFieldVariableDeclarator();
        if(fieldVariableDeclarator == null) return null;
        if(fieldVariableDeclarator instanceof AgoParser.VarDeclExplicitTypeContext varDecl){
            return varDecl.fieldGetterSetter();
        } else if(fieldVariableDeclarator instanceof AgoParser.VarDeclImplicitTypeContext varDecl){
            return varDecl.fieldGetterSetter();
        } else {
            throw new RuntimeException("unexpected");
        }
    }

    @Override
    public void setDeclaration(ParserRuleContext declaration) {
        super.setDeclaration(declaration);
        if(declaration instanceof AgoParser.FieldVariableDeclaratorContext f){
            this.fieldVariableDeclarator = f;
        }
    }

    @Override
    public Field applyTemplate(InstantiationArguments instantiationArguments, ClassDef ownerClass) throws CompilationError {
        var clone = new Field(ownerClass, this.name, this.fieldVariableDeclarator);
        clone.setName(this.name);
        clone.setOwnerClass(ownerClass);
        applyTemplate(clone, instantiationArguments);
        return clone;
    }

}
