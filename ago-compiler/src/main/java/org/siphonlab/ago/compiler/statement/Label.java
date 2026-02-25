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
package org.siphonlab.ago.compiler.statement;

import org.agrona.collections.IntArrayList;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class Label extends Statement {

    protected final int index;
    protected final CodeBuffer codeBuffer;
    boolean addressDetermined = false;
    private IntArrayList positions = new IntArrayList();
    private int resolvedAddress;

    public Label(FunctionDef ownerFunction, int index, CodeBuffer codeBuffer) {
        super(ownerFunction);
        this.index = index;
        this.codeBuffer = codeBuffer;
    }

    public boolean isAddressDetermined() {
        return addressDetermined;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("label not support");
    }

    public int getIndex() {
        return index;
    }

    public void registerPos(int pos) {
        if (addressDetermined) {
            codeBuffer.updateLabelAddress(pos, resolvedAddress);
        } else {
            positions.addInt(pos);
        }
    }

    public void acceptAddress(int resolvedAddress) {
        this.resolvedAddress = resolvedAddress;
        assert !addressDetermined;
        addressDetermined = true;
        for (int i = 0; i < positions.size(); i++) {
            codeBuffer.updateLabelAddress(positions.getInt(i), resolvedAddress);
        }
    }

    public Label here() {
        acceptAddress(codeBuffer.pos());
        return this;
    }

    public int getResolvedAddress() {
        return resolvedAddress;
    }

    public String toString() {
        return "(Label %d)".formatted(index);
    }

}