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

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

public class BooleanLiteral extends Literal<Boolean> {
    public BooleanLiteral(Boolean value) {
        super(PrimitiveClassDef.BOOLEAN, value);
    }

    @Override
    public String getId() {
        return this.value ? "t" : "f";
    }

    @Override
    public BooleanLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new BooleanLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }

    public static boolean isTrue(Literal<?> literal){
        if(literal instanceof BooleanLiteral b) {
            return b.value;
        } else if(literal.value instanceof Number n){
            return !n.equals(0);
        } else if(literal instanceof CharLiteral c){
            return c.value != 0;
        } else if(literal instanceof StringLiteral s){
            return StringUtils.isNotEmpty(s.getString());
        } else if(literal instanceof VoidLiteral || literal instanceof NullLiteral) {
            return false;
        } else if(literal instanceof ClassRefLiteral c){
            return c.value != 0;
        } else {
            return literal.value != null;
        }
    }

    public static boolean isFalse(Literal<?> literal){
        return !isTrue(literal);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BooleanLiteral b2 && b2.value == this.value;
    }
}
