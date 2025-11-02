package org.siphonlab.ago;

/*
    Instance 是 AgoClass 的 ago 实例, 不是 AgoClass 的 Java 实例. AgoClass 的实例相当于 Class<T>
 */
public class Instance<C extends AgoClass>  {

    protected C agoClass;
    protected Instance parentScope;
    protected CallFrame<?> creator;      // it's always the creator CallFrame which created this instance

    protected Slots slots;

    public Instance(final Slots slots, C agoClass){
        this.slots = slots;
        this.agoClass = agoClass;
    }

    public Instance(C agoClass){
        this.agoClass = agoClass;
    }

    public C getAgoClass() {
        return agoClass;
    }

    protected void setAgoClass(C agoClass) {
        this.agoClass = agoClass;
    }

    public Instance getParentScope() {
        return parentScope;
    }

    public void setParentScope(Instance parentScope) {
        this.parentScope = parentScope;
    }

    public CallFrame<?> getCreator() {
        return creator;
    }

    public void setCreator(CallFrame<?> creator) {
        this.creator = creator;
    }

    public Slots getSlots() {
        return slots;
    }

    @Override
    public String toString() {
        return "Instance" + "@" + this.agoClass;
    }

    public Object invokeMethod(CallFrame<?> caller, AgoFunction method, Object... arguments) {
        return invokeMethod(caller,caller.getRunSpace(),method, arguments);
    }
    public Object invokeMethod(CallFrame<?> caller, AgoRunSpace runSpace, AgoFunction method, Object... arguments){
        CallFrame<?> callFrame = runSpace.getAgoEngine().createFunctionInstance(this, method, caller, caller, runSpace);
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            if(argument instanceof Integer integer) {
                callFrame.getSlots().setInt(i, integer);
            } else if(argument instanceof AgoClass agoClass){
                callFrame.getSlots().setClassRef(i, agoClass.getClassId());
            } else if (argument instanceof Instance<?> instance) {
                callFrame.getSlots().setObject(i, instance);
            } else if (argument instanceof String s) {
                callFrame.getSlots().setString(i, s);
            } else if (argument instanceof Boolean b) {
                callFrame.getSlots().setBoolean(i, b);
            } else if (argument instanceof Double d) {
                callFrame.getSlots().setDouble(i, d);
            } else if (argument instanceof Byte b) {
                callFrame.getSlots().setByte(i, b);
            } else if (argument instanceof Long l) {
                callFrame.getSlots().setLong(i, l);
            } else if (argument instanceof Float f) {
                callFrame.getSlots().setFloat(i, f);
            } else if (argument instanceof Short s) {
                callFrame.getSlots().setShort(i, s);
            } else if(argument == null){
                callFrame.getSlots().setObject(i, null);
            }
        }
        return runSpace.awaitTillComplete(callFrame);
    }

//    public void run(Instance<?> instance, String functionName, Object... arguments) throws ClassNotFoundException {
//        var method = instance.getAgoClass().findMethod(functionName);
//        if (method == null)
//            throw new ClassNotFoundException("'%s' in '%s' not found".formatted(functionName, instance.getAgoClass().getFullname()));
//        var frame = createInstance(instance, method);
//        AgoParameter[] parameters = method.getParameters();
//        for (int i = 0; i < parameters.length; i++) {
//            AgoParameter parameter = parameters[i];
//            Slots slots = frame.getSlots();
//            switch (parameter.getTypeCode().value) {
//                case INT_VALUE:
//                    slots.setInt(i, (Integer) arguments[i]);
//                    break;
//                case BOOLEAN_VALUE:
//                    slots.setBoolean(i, (Boolean) arguments[i]);
//                    break;
//                case CHAR_VALUE:
//                    slots.setChar(i, (Character) arguments[i]);
//                    break;
//                case FLOAT_VALUE:
//                    slots.setFloat(i, (Float) arguments[i]);
//                    break;
//                case DOUBLE_VALUE:
//                    slots.setDouble(i, (Double) arguments[i]);
//                    break;
//                case BYTE_VALUE:
//                    slots.setByte(i, (Byte) arguments[i]);
//                    break;
//                case SHORT_VALUE:
//                    slots.setShort(i, (Short) arguments[i]);
//                    break;
//                case LONG_VALUE:
//                    slots.setLong(i, (Long) arguments[i]);
//                    break;
//                case OBJECT_VALUE:
//                    slots.setObject(i, (Instance<?>) arguments[i]);
//                    break;
//                case NULL_VALUE:
//                    slots.setObject(i, null);
//                case STRING_VALUE:
//                    slots.setString(i, (String) arguments[i]);
//                    break;
//                case CLASS_REF_VALUE:
//                    slots.setInt(i, ((AgoClass) arguments[i]).getClassId());
//            }
//        }
//        runSpace.runTillComplete((CallFrame<?>) frame);
//    }

}
