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

public class NativeFrame extends CallFrame<AgoNativeFunction> {

    protected NativeFunctionCaller nativeFunctionCaller;
    protected final AgoEngine engine;
    private CallFrame<?> entrance;
    private Object payload;

    public NativeFrame(AgoEngine engine, Slots slots, AgoNativeFunction agoClass) {
        super(slots, agoClass);
        this.engine = engine;
        this.nativeFunctionCaller = this.agoClass.getNativeFunctionCaller();
    }

    public void run(CallFrame<?> self){
        if (this.debugger != null) this.debugger.enterFrame(this);
        this.setRunSpace(runSpace);
        // the native function f(NativeFrame frame, param1, param2), end with `frame.finish(result)`
        nativeFunctionCaller.invoke(this, this.slots);
        this.entrance = self;
    }

    public void beginAsync(){
        this.getRunSpace().waitResult();        // after that currCallFrame is still me
    }

    private RunSpace resumeCallerRunSpace() {
        var caller = this.getCaller();
        RunSpace callerRunSpace = caller.getRunSpace();
        if(callerRunSpace == this.getRunSpace())
            callerRunSpace.setCurrCallFrame(caller);
        return callerRunSpace;
    }

    public void finishVoidAsync() {
        if (stateHandler != null) stateHandler.complete(null);

        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptVoidByAsync();
    }

    public void finishNullAsync() {
        if (stateHandler != null) stateHandler.complete(null);

        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptNullByAsync();;
    }

    public void finishIntAsync(int result) {
        if (stateHandler != null) stateHandler.complete(null);

        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptIntByAsync(result);
    }

    public void finishByteAsync(byte result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptByteByAsync(result);
    }

    public void finishShortAsync(short result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptShortByAsync(result);
    }

    public void finishFloatAsync(float result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptFloatByAsync(result);
    }

    public void finishDoubleAsync(double result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptDoubleByAsync(result);
    }

    public void finishLongAsync(long result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptLongByAsync(result);
    }

    public void finishCharAsync(char result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptCharByAsync(result);
    }

    public void finishStringAsync(String result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptStringByAsync(result);
    }

    public void finishBooleanAsync(boolean result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptBooleanByAsync(result);
    }

    public void finishObjectAsync(Instance<?> result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptObjectByAsync(result);
    }

    public void finishClassRefAsync(AgoClass result) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptClassRefByAsync(result);
    }

    public void finishExceptionAsync(Instance<?> exception) {
        if (stateHandler != null) stateHandler.complete(null);
        if (this.debugger != null) this.debugger.leaveFrame(this);

        RunSpace callerRunSpace = resumeCallerRunSpace();
        callerRunSpace.acceptExceptionByAsync(exception);
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return this.agoClass.getSourceLocation();
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public void setSuspended(boolean suspended) {
        //TODO native frame not support yet
    }

    @Override
    public void resume() {
        // native frame not support yet
    }

    @Override
    public void interrupt() {
        // native frame not support yet
    }
}
