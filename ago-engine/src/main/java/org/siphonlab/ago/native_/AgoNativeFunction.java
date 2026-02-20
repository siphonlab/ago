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

import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoNativeFunction extends AgoFunction {

    private NativeFunctionCaller nativeFunctionCaller;
    private String nativeEntrance;
    private int resultSlot;

    public AgoNativeFunction(AgoClassLoader agoClassLoader, MetaClass metaClass, String fullname, String name) {
        super(agoClassLoader, metaClass, fullname, name);
    }

    public NativeFunctionCaller getNativeFunctionCaller() {
        return nativeFunctionCaller;
    }

    public void setNativeFunctionCaller(NativeFunctionCaller nativeFunctionCaller) {
        this.nativeFunctionCaller = nativeFunctionCaller;
    }

    public void setResultSlot(int resultSlot) {
        this.resultSlot = resultSlot;
    }

    public int getResultSlot() {
        return resultSlot;
    }

    public String getNativeEntrance() {
        return nativeEntrance;
    }

    public void setNativeEntrance(String nativeEntrance) {
        this.nativeEntrance = nativeEntrance;
    }

    @Override
    public AgoFunction cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoNativeFunction(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

    @Override
    protected void copyTo(AgoClass cls) {
        AgoNativeFunction copy = (AgoNativeFunction) cls;
        super.copyTo(cls);
        copy.setNativeEntrance(this.nativeEntrance);
        copy.setResultSlot(this.resultSlot);
        copy.setNativeFunctionCaller(this.nativeFunctionCaller);
    }

}
