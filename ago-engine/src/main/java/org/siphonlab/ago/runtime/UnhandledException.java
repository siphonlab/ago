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
