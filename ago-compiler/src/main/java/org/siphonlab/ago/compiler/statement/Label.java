package org.siphonlab.ago.compiler.statement;

import org.agrona.collections.IntArrayList;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class Label extends Statement {

    protected final int index;
    protected final CodeBuffer codeBuffer;
    boolean addressDetermined = false;
    private IntArrayList positions = new IntArrayList();
    private int resolvedAddress;

    public Label(int index, CodeBuffer codeBuffer) {
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