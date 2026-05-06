package org.siphonlab.ago.opcode;

public class Dynamic {
    public static final int KIND_DYNAMIC = 0x36_000000;
    public static final int OP                  = 0x36;

    public static final int dyn_get_member_vuv = 0x36_00_01_03;
    public static final int dyn_set_member_uSv = 0x36_00_02_03;

    // validate instance is callframe, put before invoke_v in dynamic invocation
    // to invoke dynamic,
    // first get_member_vov to create scope bound class, here it's method
    // then new_dynamic to create instance with arguments tuple, then
    // validate_invocable_v to ensure it's a call frame
    // invoke_v to execute it
    // however the result must invoke accept_any_v to save to a union slot
    public static final int dyn_ensure_invocable_vu = 0x36_00_03_02;
    public static final int dyn_ensure_invocable_v = 0x36_00_03_01;

    // put these 2 new_ op in Dynamic, for it maybe fail
    // new_dynamic(target, scoped class instance, args_tuple)
    // auto find constructor to match the args to create instance
    //      for Function, there is no constructor need to invoke(function invoke its constructor in method body)
    // if the class instance is an inner class (need scope) and no scope given, throw error
    // the scoped class instance is Object?
    public static final int dyn_new_vua = 0x36_00_04_03;
    public static final int dyn_new_vu = 0x36_00_04_02;

    public static final int dyn_contains_member_vov = 0x36_00_05_03;
    public static final int dyn_contains_member_voc = 0x36_00_06_03;


    public static String getName(int code){
        return switch (code) {
            case dyn_get_member_vuv -> "dyn_get_member_vuv";
            case dyn_set_member_uSv -> "dyn_set_member_uSv";
            case dyn_ensure_invocable_vu -> "dyn_ensure_invocable_vu";
            case dyn_ensure_invocable_v -> "dyn_ensure_invocable_v";
            case dyn_new_vua -> "dyn_new_vua";
            case dyn_new_vu -> "dyn_new_vu";
            case dyn_contains_member_vov -> "dyn_contains_member_vov";
            case dyn_contains_member_voc -> "dyn_contains_member_voc";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };

    }
}
