package org.siphonlab.ago.opcode;

public class Dynamic {
    public static final int KIND_DYNAMIC = 0x36_000000;
    public static final int OP                  = 0x36;

    public static final int get_member_vov = 0x36_00_01_03;
    public static final int set_member_vov = 0x36_00_02_03;

    // validate instance is callframe, put before invoke_v in dynamic invocation
    // to invoke dynamic,
    // first get_member_vov to create scope bound class, here it's method
    // then new_dynamic to create instance with arguments tuple, then
    // validate_invocable_v to ensure it's a call frame
    // invoke_v to execute it
    // however the result must invoke accept_any_v to save to a union slot
    public static final int validate_invocable_v = 0x36_00_03_01;

    // put these 2 new_ op in Dynamic, for it maybe fail
    // new_dynamic(target, scoped class instance, args_tuple)
    // auto find constructor to match the args to create instance
    //      for Function, there is no constructor need to invoke(function invoke its constructor in method body)
    // if the class instance is an inner class (need scope) and no scope given, throw error
    public static final int new_dynamic_voa = 0x36_00_04_03;
    public static final int new_dynamic_vo  = 0x36_00_04_02;


    public static String getName(int code){
        return switch (code) {
            case get_member_vov -> "get_member_vov";
            case set_member_vov -> "set_member_vov";
            case validate_invocable_v -> "validate_invocable_v";
            case new_dynamic_voa -> "new_dynamic_voa";
            case new_dynamic_vo -> "new_dynamic_vo";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };

    }
}
