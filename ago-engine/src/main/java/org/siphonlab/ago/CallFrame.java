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
package org.siphonlab.ago;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public abstract class CallFrame<F extends AgoFunction> extends Instance<F> {

    private final static Logger logger = LoggerFactory.getLogger(CallFrame.class);

    protected CallFrame<?> caller;

    protected RunSpace runSpace;

    protected boolean suspended = false;

    protected Debugger debugger = null;

    public CallFrame(Slots slots, F agoClass) {
        super(slots, agoClass);
    }

    public CallFrame<?> getCaller() {
        return caller;
    }

    public void setCaller(CallFrame<?> caller) {
        this.caller = caller;
    }

    @Override
    public String toString() {
        return "CallFrame" + "@" + this.agoClass;
    }

    public abstract SourceLocation resolveSourceLocation();

    public RunSpace getRunSpace() {
        return runSpace;
    }

    public AgoEngine getAgoEngine(){
        return this.getRunSpace().getAgoEngine();
    }

    public void finishVoid() {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptVoid(caller);
        } else {
           runSpace.setCurrCallFrame(null);
        }
    }

    public void finishBoolean(boolean result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptBoolean(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishByte(byte result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptByte(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishShort(short result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptShort(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishInt(int result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptInt(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishLong(long result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptLong(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishFloat(float result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptFloat(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishDouble(double result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptDouble(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishDecimal(BigDecimal result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptDecimal(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishChar(char result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptChar(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishObject(Instance<?> result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptObject(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishUnion(Object result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptUnion(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishString(String result) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptString(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishNull() {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptNull(caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishClassRef(AgoClass result){
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptClassRef(result, caller);
        } else {
            runSpace.setCurrCallFrame(null);
        }
    }

    public void finishException(Instance<?> exception) {
        CallFrame<?> caller = getCaller();
        RunSpace runSpace = getRunSpace();
        if(caller != null) {
            RunSpace callerRunSpace = caller.getRunSpace();
            if (callerRunSpace != runSpace) runSpace.setCurrCallFrame(null);
            if (callerRunSpace != null) callerRunSpace.acceptException(exception, caller);
        } else {
            runSpace.acceptException(exception, null);
        }
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean handleException(Instance<?> exception){
        return false;
    }

    public void setRunSpace(RunSpace runSpace) {
        this.runSpace = runSpace;
    }

    public void run(){
        run(this);
    }

    public abstract void run(CallFrame<?> self);

    public void resume() {
        this.setSuspended(false);
        // may enter a new frame and fall in pausing
        RunSpace runSpace = this.getRunSpace();
        CallFrame<?> currentCallFrame = runSpace.getCurrentCallFrame();
        if(currentCallFrame != this){
            currentCallFrame.setSuspended(false);
        }
        runSpace.resumeByAcceptResult();
    }

    public void interrupt(){
    }

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public void yieldVoid() {
        finishVoid();
    }

    public void yieldBoolean(boolean value) {
        finishBoolean(value);
    }

    public void yieldByte(byte value) {
        finishByte(value);
    }

    public void yieldShort(short value) {
        finishShort(value);
    }

    public void yieldInt(int value) {
        finishInt(value);
    }

    public void yieldLong(long value) {
        finishLong(value);
    }

    public void yieldFloat(float value) {
        finishFloat(value);
    }

    public void yieldDouble(double value) {
        finishDouble(value);
    }

    public void yieldDecimal(BigDecimal value) {
        finishDecimal(value);
    }

    public void yieldChar(char value) {
        finishChar(value);
    }

    public void yieldObject(Instance<?> value) {
        finishObject(value);
    }

    public void yieldUnion(Object value) {
        finishUnion(value);
    }

    public void yieldString(String value) {
        finishString(value);
    }

    public void yieldNull() {
        finishNull();
    }

    public void yieldClassRef(AgoClass value) {
        finishClassRef(value);
    }

    protected void reenter(WaitingReentrantFrame<?> waitingReentrantFrame, int state) {

    }
}
