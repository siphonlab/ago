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

import com.google.common.hash.Hashing;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class StringLiteral extends Literal<Integer> {
    private final String string;

    public StringLiteral(String s) {
        super(PrimitiveClassDef.STRING, -1);
        this.string = s;
    }

    public String getString() {
        return string;
    }

    @Override
    public Literal<Integer> visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.value == -1) {
            this.value = blockCompiler.getFunctionDef().idOfConstString(this.string);
        }
        return this;
    }

    @Override
    public String toString() {
        return "\"%s\"".formatted(this.string);     //TODO quote string
    }

    @Override
    public String getId() {
        var l = Hashing.murmur3_128().hashString(string, StandardCharsets.UTF_8);
        return "S%s".formatted(l.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringLiteral b2 && Objects.equals(b2.string, this.string);
    }

    @Override
    public StringLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new StringLiteral(this.string);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
