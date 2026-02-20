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
package org.siphonlab.ago.compiler.expression.literal;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;

public class ClassRefLiteral extends Literal<Integer> {

    private final ClassDef classDefValue;

    private Expression scope;

    public ClassRefLiteral(ClassDef classDefValue) {
        super(PrimitiveClassDef.CLASS_REF, -1);
        assert classDefValue != null;
        this.classDefValue = classDefValue;
    }

    public ClassDef getClassDefValue() {
        return classDefValue;
    }

    @Override
    public Expression transform() throws CompilationError {
        if(classDefValue instanceof MetaClassDef metaClassDef){
            throw new TypeMismatchError("metaclass not allowed", this.getSourceLocation());
        }
        return super.transform();
    }

    @Override
    public Literal<Integer> visit(BlockCompiler blockCompiler) throws CompilationError {
        this.value = blockCompiler.getFunctionDef().idOfClass(this.classDefValue);
        return super.visit(blockCompiler);
    }

    @Override
    public String toString() {
        return "(ClassRef %s)".formatted(classDefValue);
    }

    @Override
    public String getId() {
        return "C%s".formatted(classDefValue.getFullname().replace('.', '$'));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassRefLiteral b2 && b2.classDefValue == this.classDefValue;
    }

    @Override
    public ClassRefLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new ClassRefLiteral(this.classDefValue);
        r.setSourceLocation(sourceLocation);
        return r;
    }

    public Expression getScope() {
        return scope;
    }

    public void setScope(Expression scope) {
        this.scope = scope;
    }
}
