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
package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.expression.Expression;

import java.util.Objects;

public final class CollectionElementDef {
    private Expression expression;
    private final boolean isExpando;
    private final ClassDef elementType;

    public CollectionElementDef(Expression expression, boolean isExpando, ClassDef elementType) {
        this.expression = expression;
        this.isExpando = isExpando;
        this.elementType = elementType;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public boolean isExpando() {return isExpando;}

    public ClassDef elementType() {return elementType;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CollectionElementDef) obj;
        return Objects.equals(this.expression, that.expression) &&
                this.isExpando == that.isExpando &&
                Objects.equals(this.elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, isExpando, elementType);
    }

    @Override
    public String toString() {
        if(isExpando){
            return "..." + expression;
        } else {
            return expression.toString();
        }
    }

}
