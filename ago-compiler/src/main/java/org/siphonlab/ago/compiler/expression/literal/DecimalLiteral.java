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

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Literal;

import java.math.BigDecimal;
import java.util.Objects;

public class DecimalLiteral extends Literal<BigDecimal> {
    private int blobIndex = -1;

    public DecimalLiteral(PrimitiveClassDef DECIMAL, BigDecimal value) {
        super(DECIMAL, value);
    }

    @Override
    public String getId() {
        return "D%s".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DecimalLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public DecimalLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new DecimalLiteral((PrimitiveClassDef) this.classDef, this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }

    public byte[] toArray(){
        var bi = this.value.unscaledValue();
        byte[] arr = bi.toByteArray();
        int size = 4 + arr.length;
        return IoBuffer.allocate(4 + size).putInt(size).putInt(value.scale()).put(arr).flip().array();
    }

    @Override
    public Literal<BigDecimal> visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.blobIndex == -1) {
            this.blobIndex = blockCompiler.getFunctionDef().getOrCreateBLOB(this);
        }
        return this;
    }

    public int getBlobIndex() {
        if(blobIndex == -1){
            throw new IllegalStateException("blob index is -1, decimal not visited");
        }
        return blobIndex;
    }

    public DecimalLiteral ensureBlobCreated(ClassDef ownerClass) throws TypeMismatchError {
        if(this.blobIndex == -1) {
            this.blobIndex = ownerClass.getOrCreateBLOB(this);
        }
        return this;
    }
}
