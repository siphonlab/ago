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
package org.siphonlab.ago.lang;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class RunSpaceAware {

    public static void getRunSpace(NativeFrame nativeFrame) {
        CallFrame<?> fun = (CallFrame<?>) nativeFrame.getParentScope();       // Function<R>
        RunSpace runSpace = fun.getRunSpace();

        AgoEngine agoEngine = runSpace.getAgoEngine();
        NativeInstance runspaceObj = (NativeInstance) agoEngine.createNativeInstance(null, agoEngine.getRunSpaceClass(), nativeFrame);
        runspaceObj.setNativePayload(runSpace);

        nativeFrame.finishObject(runspaceObj);
    }

    public static void pause(NativeFrame frame){
        var runspace = getAgoRunSpace(frame);
        runspace.pause();
        frame.finishVoid();
    }

    public static void resume(NativeFrame frame) {
        var runspace = getAgoRunSpace(frame);
        runspace.resume();
        frame.finishVoid();
    }

    public static void interrupt(NativeFrame frame) {
        var runSpace = getAgoRunSpace(frame);
        runSpace.interrupt();
        frame.finishVoid();
    }

    // fun create() as RunSpace
    public static void createRunSpace(NativeFrame nativeFrame){
        var runSpace = getAgoRunSpace(nativeFrame);
        var newRunSpace = runSpace.createChildRunSpace(null);

        AgoEngine agoEngine = runSpace.getAgoEngine();
        NativeInstance runspaceObj = (NativeInstance) agoEngine.createNativeInstance(null, agoEngine.getRunSpaceClass(), nativeFrame);
        runspaceObj.setNativePayload(newRunSpace);

        nativeFrame.finishObject(runspaceObj);
    }

    // fun run<R>(function as Function<R>) as R
    // TODO must implement resume
    public static void run(NativeFrame frame, Instance<?> runnerFrame) throws InterruptedException {
        var runSpace = getAgoRunSpace(frame);   // the RunSpace within scope of run<R>
        CallFrame<?> runner = (CallFrame<?>) runnerFrame;
        runner.setCaller(frame);
        frame.beginAsync();
        runSpace.addCompleteListener(()-> {
            ResultSlots resultSlots = runSpace.getResultSlots();
            if(frame.getAgoClass().getResultClass() == runSpace.getAgoEngine().getClass("lang.Any")){
                frame.finishObject(resultSlots.castAnyToObject(runSpace.getAgoEngine().getBoxer()));
            }
            switch (resultSlots.getDataType()){
                case VOID_VALUE:    frame.finishVoidAsync(); break;
                case NULL_VALUE:    frame.finishNullAsync(); break;
                case OBJECT_VALUE:  frame.finishObjectAsync(resultSlots.takeObjectValue()); break;
                case INT_VALUE:     frame.finishIntAsync(resultSlots.getIntValue()); break;
                case BYTE_VALUE:    frame.finishByteAsync(resultSlots.getByteValue()); break;
                case SHORT_VALUE:   frame.finishShortAsync(resultSlots.getShortValue()); break;
                case LONG_VALUE:    frame.finishLongAsync(resultSlots.getLongValue()); break;
                case FLOAT_VALUE:   frame.finishFloatAsync(resultSlots.getFloatValue()); break;
                case DOUBLE_VALUE:  frame.finishDoubleAsync(resultSlots.getDoubleValue()); break;
                case BOOLEAN_VALUE: frame.finishBooleanAsync(resultSlots.getBooleanValue()); break;
                case CHAR_VALUE:    frame.finishCharAsync(resultSlots.getCharValue()); break;
                case STRING_VALUE:  frame.finishStringAsync(resultSlots.getStringValue()); break;
                case CLASS_REF_VALUE: frame.finishClassRefAsync(resultSlots.getClassRefValue()); break;
                default: throw new UnsupportedOperationException("unsupported data type " + resultSlots.getDataType());
            }
        });
        runner.setRunSpace(runSpace);
        runSpace.start(runner);
    }

    public static void runAsync(NativeFrame frame, Instance<?> runnerFrame) {
        var runSpace = getAgoRunSpace(frame);   // the RunSpace within scope of run<R>
        CallFrame<?> runner = (CallFrame<?>) runnerFrame;
        runner.setRunSpace(runSpace);
        runner.setCaller(frame);
        runSpace.start(new EntranceCallFrame<>(runner));
        frame.finishVoid();
    }

    private static RunSpace getAgoRunSpace(NativeFrame nativeFrame) {
        NativeInstance runSpaceInstance = (NativeInstance) nativeFrame.getParentScope();
        RunSpace runSpace = (RunSpace) runSpaceInstance.getNativePayload();
        return runSpace;
    }

    public static void receive(NativeFrame nativeFrame){
        NativeInstance restService = (NativeInstance) nativeFrame.getParentScope();
        AgoEngine agoEngine = nativeFrame.getAgoEngine();

//        nativeFrame.finishObject();
    }

    public static void sleep(NativeFrame nativeFrame, int millisecond) {
        var runSpaceHost = nativeFrame.getRunSpace().getRunSpaceHost();
        nativeFrame.beginAsync();
        Object obj = runSpaceHost.setTimer(millisecond, nativeFrame::finishVoidAsync);
        nativeFrame.setPayload(obj);
    }

    public static void forkContextSample(NativeFrame nativeFrame) {
        ((NativeInstance)nativeFrame.getParentScope()).setNativePayload(new ForkContext() {
            @Override
            public String toString() {
                return "a sample fork context";
            }
        });
        nativeFrame.finishVoid();
    }


}