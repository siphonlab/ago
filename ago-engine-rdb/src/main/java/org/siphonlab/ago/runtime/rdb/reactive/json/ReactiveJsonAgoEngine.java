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
package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

public class ReactiveJsonAgoEngine extends PersistentRdbEngine {

    public ReactiveJsonAgoEngine(ReactiveJsonPGAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
    }

    public ReactiveJsonAgoEngine(ReactiveJsonPGAdapter rdbAdapter) {
        super(rdbAdapter);
    }

    @Override
    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return new RunSpace(this, runSpaceHost);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        var inst = new ReactiveJsonCallFrame(agoFunction.createSlots(), agoFunction,this);
        if(parentScope != null) inst.setParentScope(parentScope);
        saveInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = super.createInstance(parentScope, agoClass, creator);
        return inst;
    }
}
