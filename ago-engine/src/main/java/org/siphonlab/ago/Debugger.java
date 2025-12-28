package org.siphonlab.ago;

public interface Debugger {
    void enterFrame(CallFrame<?> callFrame);
    void leaveFrame(CallFrame<?> callFrame);
    void updatePC(CallFrame<?> callFrame, int pc);
}
