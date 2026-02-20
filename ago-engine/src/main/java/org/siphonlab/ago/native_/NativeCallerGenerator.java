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
package org.siphonlab.ago.native_;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.Variable;
import org.siphonlab.ago.*;

import java.lang.reflect.InvocationTargetException;

import static org.siphonlab.ago.TypeCode.VOID;

public class NativeCallerGenerator {
    private final AgoNativeFunction nativeFunction;

    public NativeCallerGenerator(AgoNativeFunction nativeFunction){
        this.nativeFunction = nativeFunction;
    }

    public NativeFunctionCaller generate(){
        String entrance = nativeFunction.getNativeEntrance();

        Class<?> callerClass = NativeFunctionCaller.class;
        var clsCM = ClassMaker.begin(escapeName(nativeFunction.getFullname()) + "_caller").public_().extend(callerClass);

        clsCM.addConstructor().public_();

        var invokeCM = clsCM.addMethod(void.class, "invoke", NativeFrame.class, Slots.class).override();

        int p = entrance.lastIndexOf('.');
        String methodName = entrance.substring(p + 1);
        String className = entrance.substring(0, p);
        AgoParameter[] parameters = nativeFunction.getParameters();

        Variable[] variableDescs = new Variable[parameters.length + 1];

        int startParam = 0;

        // NativeFrame
        variableDescs[startParam ++] = invokeCM.param(0);

        var slots = invokeCM.param(1);
        for (int i = 0; i < parameters.length; i++) {
            AgoParameter parameter = parameters[i];
            TypeCode typeCode = parameter.getTypeCode();
            variableDescs[i + startParam] = slots.invoke("get" + DefaultSlotsCreatorFactory.slotFunctionName(typeCode), i);
        }
        try {
            invokeCM.var(Class.forName(className)).invoke(methodName, variableDescs);       // async function set result by itself
            Class<?> calllerCls = clsCM.finish();
            return (NativeFunctionCaller) ConstructorUtils.invokeConstructor(calllerCls);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeName(String fullname) {
        return StringEscapeUtils.escapeHtml4(fullname)
                .replace('.', '$').replace('%', '$')
                .replace('&', '$')
                .replace(';', '$')
                .replace('[', '$');
    }
}
