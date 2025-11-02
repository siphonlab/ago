package org.siphonlab.ago.runtime;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.AgoField;
import org.siphonlab.ago.Instance;

public class UnhandledException extends RuntimeException {

    private final Instance<?> exception;

    public UnhandledException(AgoEngine agoEngine, Instance<?> exception) {
        super(exception.getSlots().getString(1));
        this.exception = exception;

        AgoClass StackTraceElementClass = agoEngine.getClass("lang.StackTraceElement");
        var ThrowableClass = agoEngine.getClass("lang.Throwable");
        var stackTraceElements = ThrowableClass.findField("stackTraceElements");
        ObjectArrayInstance arrayInst = (ObjectArrayInstance) exception.getSlots().getObject(stackTraceElements.getSlotIndex());

        AgoField functionNameField = StackTraceElementClass.findField("functionName");
        AgoField fileNameField = StackTraceElementClass.findField("fileName");
        AgoField lineNumberField = StackTraceElementClass.findField("lineNumber");

        Instance<?>[] value = arrayInst.value;
        StackTraceElement[] j = new StackTraceElement[value.length];
        for (int i = 0; i < value.length; i++) {
            Instance<?> inst = value[i];

            String className = "";
            String funName = inst.getSlots().getString(functionNameField.getSlotIndex());
            String fileName = inst.getSlots().getString(fileNameField.getSlotIndex());
            int line = inst.getSlots().getInt(lineNumberField.getSlotIndex());

            j[i] = new StackTraceElement(className,funName,fileName,line);
        }
        this.setStackTrace(j);
    }

    public Instance<?> getException() {
        return exception;
    }
}
