package org.siphonlab.ago.runtime.stateful;

public enum RunningState {
    PENDING(0),
    RUNNING(1),     // when engine shutdown, it's stay at RUNNING state, and after engine restart all RUNNING frames will resume

    WAITING_RESULT(2),
    SUSPENDED(3),
    WAITING_MESSAGE(4),     // waiting message, engine will resume it when message arrived
    WAITING_IN_QUEUE(5),    // set to waiting by scheduler, for the event loop need handle other jobs at first

    DONE(6),
    ERROR(7),        // done with Exception
    CANCEL(8);

    private final int code;

    RunningState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RunningState fromCode(int code) {
        switch (code) {
            case 0: return PENDING;
            case 1: return RUNNING;
            case 2: return WAITING_RESULT;
            case 3: return SUSPENDED;
            case 4: return WAITING_MESSAGE;
            case 5: return WAITING_IN_QUEUE;
            case 6: return DONE;
            case 7: return ERROR;
            case 8: return CANCEL;
            default: throw new IllegalArgumentException("Invalid state code: " + code);
        }
    }
}
