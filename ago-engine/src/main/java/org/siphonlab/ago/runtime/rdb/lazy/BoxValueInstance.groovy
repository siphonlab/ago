package org.siphonlab.ago.runtime.rdb.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.*;

import static org.siphonlab.ago.TypeCode.*;

@CompileStatic
public class BoxValueInstance extends Instance<AgoClass> implements ReferenceInstanceTrait {

    private final Object value;

    public BoxValueInstance(Object value, AgoClass agoClass) {
        super(agoClass);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Instance<?> doDeference() {
        AgoEngine agoEngine = this.boundCallFrame.getRunSpace().getAgoEngine();
        assert (agoEngine.getBoxTypes().isBoxType(this.agoClass));

        var callFrame = this.boundCallFrame;

        TypeCode type = agoEngine.getBoxTypes().getUnboxType(agoClass);
        var boxedValue = switch (type.getValue()) {
            case INT_VALUE -> agoEngine.getBoxer().boxInt(callFrame, agoClass, (Integer)value);
            case LONG_VALUE -> agoEngine.getBoxer().boxLong(callFrame, agoClass, (Long) value);
            case FLOAT_VALUE -> agoEngine.getBoxer().boxFloat(callFrame, agoClass, (Float) value);
            case DOUBLE_VALUE -> agoEngine.getBoxer().boxDouble(callFrame, agoClass, (Double) value);
            case SHORT_VALUE -> agoEngine.getBoxer().boxShort(callFrame, agoClass, (Short) value);
            case BYTE_VALUE -> agoEngine.getBoxer().boxByte(callFrame, agoClass, (Byte) value);
            case STRING_VALUE -> agoEngine.getBoxer().boxString(callFrame, agoClass, (String) value);
            default -> throw new UnsupportedOperationException("unknown type");
        };
        return boxedValue;
    }

    @Override
    public Slots getSlots() {
        return deference().getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deference().getParentScope();
    }

    @Override
    public CallFrame<?> getCreator() {
        return deference().getCreator();
    }

    @Override
    public Object invokeMethod(CallFrame<?> caller, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, method, arguments);
    }

    @Override
    public Object invokeMethod(CallFrame<?> caller, AgoRunSpace runSpace, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, runSpace, method, arguments);
    }


}
