package org.siphonlab.ago.compiler;

public class Instruction {

    private final int code;

    private final int size;

    Instruction(int code, int size){
        this.code = code;
        this.size = size;
    }

    public static final int LITERAL_STRING = 0x0001;
    public static final int DEF_PACKAGE = 0x0002;
    public static final int DEF_CLASS = 0x0003;
    public static final int IMPLEMENT_INTERFACE = 0x0004;
    public static final int DEF_FIELD = 0x0005;
    public static final int NEW = 0x006;

    public static final int PUSH = 0x007;
    public static final int PUSH_THIS = 0x008;

    public static Instruction[] INSTRUCTIONS = {
        new Instruction(LITERAL_STRING, 2),     // literal_string(index, string)
        new Instruction(DEF_PACKAGE, 2),         // def_package(id, packageName)
        new Instruction(DEF_CLASS, 7),          // def_class(id, package, className, type, modifier, superClass, ownerClass)
        new Instruction(IMPLEMENT_INTERFACE, 2),     // implement_interface(classId, interface)
        new Instruction(DEF_FIELD, 5),          // def_field(id, fieldName, ownerClass, type, modifier), include parameters
        new Instruction(NEW, 1),            // new(classId)
        new Instruction(PUSH, 1),           // push(value)
    };
}
