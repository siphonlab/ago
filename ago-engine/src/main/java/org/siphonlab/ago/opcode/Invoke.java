package org.siphonlab.ago.opcode;

public class Invoke {
    public static final int KIND_INVOKE = 0x05_000000;
    public static final int OP                  = 0x05;
    // invoke_v(functionInstanceSlot)
    public static final int invoke_v = 0x05_00_01_01;

    public static final int fork_v = 0x05_00_03_01;
    // fork_vv(functionInstanceSlot, result)
    public static final int fork_vv = 0x05_00_03_02;    // fork vv, the result is the forked CallFrame

    public static final int spawn_v = 0x05_00_05_01;
    public static final int spawn_vv = 0x05_00_05_02;    // like fork, the result is the forked CallFrame

    public static final int await_v = 0x05_00_07_01;

    // fork/await with fork via context
    public static final int forkc_vo = 0x05_00_04_02;
    public static final int forkc_vvo = 0x05_00_04_03;

    public static final int spawnc_vo = 0x05_00_06_02;
    public static final int spawnc_vvo = 0x05_00_06_03;

    public static final int awaitc_vo = 0x05_00_08_02;

    public static String getName(int code) {
        return switch (code) {
            case invoke_v -> "invoke_v";
            case fork_v -> "fork_v";
            case spawn_v -> "spawn_v";
            case await_v -> "await_v";

            case spawn_vv -> "spawn_vv";
            case fork_vv -> "fork_vv";

             case forkc_vo -> "forkc_vo";
             case forkc_vvo -> "forkc_vvo";
             case spawnc_vo -> "spawnc_vo";
             case spawnc_vvo -> "spawnc_vvo";
             case awaitc_vo -> "awaitc_vo";

            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }
}
