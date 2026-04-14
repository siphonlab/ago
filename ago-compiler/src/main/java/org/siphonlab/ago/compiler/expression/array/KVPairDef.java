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

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;

public final class KVPairDef extends ObjectLiteralKVDef {

    private final FunctionDef ownerFunction;
    private Expression key;
    private Expression value;
    private final SourceLocation sourceLocation;

    public KVPairDef(FunctionDef ownerFunction, Expression key, Expression value, SourceLocation sourceLocation) throws CompilationError {
        this.ownerFunction = ownerFunction;
        this.key = key;
        this.value = value;
        this.sourceLocation = sourceLocation;

        this.keyType = key.inferType();
        this.valueType = value.inferType();
    }

    public Expression getKey() {
        return key;
    }

    public void setKey(Expression key) {
        this.key = key;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return key.toString() + ":" + value.toString();
    }

    public SourceLocation getSourceLocation() {
        return this.sourceLocation;
    }
}
