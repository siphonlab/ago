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

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

public class NullLiteral extends Literal<Object> {
    public NullLiteral(ClassDef nullClass) {
        super(nullClass, null);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String getId() {
        return "n";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullLiteral;
    }

    @Override
    public NullLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new NullLiteral(this.classDef);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
