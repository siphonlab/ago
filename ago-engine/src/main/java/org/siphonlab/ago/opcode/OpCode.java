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
package org.siphonlab.ago.opcode;

import org.siphonlab.ago.opcode.arithmetic.*;
import org.siphonlab.ago.opcode.compare.*;
import org.siphonlab.ago.opcode.logic.*;

public class OpCode {
    // instance rule:
    //      -1 - Frame, -2 - thisObj, -3 -... parent and parent,
    // for positive number, get the instance from frame.slots[i]

    public static final int KIND_MASK       = 0xff000000;
    public static final int DTYPE_MASK      = 0x00ff0000;
    public static final int DTYPE_MASK_NEG  = 0xff00ffff;
    public static final int SIZE_MASK       = 0x000000ff;
    public static final int LAST_KIND       = Accept.KIND_ACCEPT;

    public static final int VOID_DTYPE      = 0x00000000;
    public static final int BOOLEAN_DTYPE   = 0x00040000;
    public static final int CHAR_DTYPE      = 0x00050000;
    public static final int FLOAT_DTYPE     = 0x00060000;
    public static final int DOUBLE_DTYPE    = 0x00070000;
    public static final int BYTE_DTYPE      = 0x00080000;
    public static final int SHORT_DTYPE     = 0x00090000;
    public static final int INT_DTYPE       = 0x000a0000;
    public static final int LONG_DTYPE      = 0x000b0000;
    public static final int OBJECT_DTYPE    = 0x00010000;
    public static final int NULL_DTYPE      = 0x00020000;
    public static final int STRING_DTYPE    = 0x00030000;

    public static String getName(int code) {
        int opKind = code & KIND_MASK;

        return switch (opKind) {
            case Const.KIND_CONST -> Const.getName(code);
            case Move.KIND_MOVE -> Move.getName(code);
            case Add.KIND_ADD -> Add.getName(code);

            case New.KIND_NEW -> New.getName(code);     // 0x04_000000

            case Invoke.KIND_INVOKE -> Invoke.getName(code);
            case Debug.KIND_DEBUG -> Debug.getName(code);
            case Return.KIND_RETURN -> Return.getName(code);
            case Cast.KIND_CAST -> Cast.getName(code);
            case Load.KIND_LOAD -> Load.getName(code);
            case Equals.KIND_EQUALS -> Equals.getName(code);
            // 0a
            case NotEquals.KIND_NOT_EQUALS -> NotEquals.getName(code);
            case Concat.KIND_CONCAT -> Concat.getName(code);
            case Array.KIND_ARRAY -> Array.getName(code);
            case Box.KIND_BOX -> Box.getName(code);
            case Jump.KIND_JUMP -> Jump.getName(code);
            case GreaterThan.KIND_GREATER_THAN -> GreaterThan.getName(code);
            case LittleThan.KIND_LITTLE_THAN -> LittleThan.getName(code);
            case GreaterEquals.KIND_GREATER_EQUALS -> GreaterEquals.getName(code);
            case LittleEquals.KIND_LITTLE_EQUALS -> LittleEquals.getName(code);
            case Subtract.KIND_SUBTRACT -> Subtract.getName(code);

            case NewGeneric.KIND_NEW_GENERIC -> NewGeneric.getName(code);       // 0x14_000000

            case Multiply.KIND_MULTIPLY -> Multiply.getName(code);
            case Div.KIND_DIV -> Div.getName(code);
            case Mod.KIND_MOD -> Mod.getName(code);
            case IncDec.KIND_INC_DEC -> IncDec.getName(code);
            case Neg.KIND_NEG -> Neg.getName(code);
            case And.KIND_AND -> And.getName(code);
            case Or.KIND_OR -> Or.getName(code);
            case Not.KIND_NOT -> Not.getName(code);
            case TryCatch.KIND_TRY_CATCH -> TryCatch.getName(code);

            case BitAnd.KIND_BITAND -> BitAnd.getName(code);
            case BitOr.KIND_BITOR -> BitOr.getName(code);
            case BitXor.KIND_BITXOR -> BitXor.getName(code);
            case BitNot.KIND_BITNOT -> BitNot.getName(code);
            case BitShiftLeft.KIND_BIT_LSHIFT -> BitShiftLeft.getName(code);
            case BitShiftRight.KIND_BIT_RSHIFT -> BitShiftRight.getName(code);
            case BitUnsignedRight.KIND_BIT_URSHIFT -> BitUnsignedRight.getName(code);

            case InstanceOf.KIND_INSTANCE_OF ->  InstanceOf.getName(code);

            case Pause.KIND_PAUSE -> Pause.getName(code);
            case Accept.KIND_ACCEPT -> Accept.getName(code);

            default -> "unknown code " + code;
        };
    }

    public static boolean isSymmetrical(int opCode) {
        switch (opCode) {
            case Add.KIND_ADD:
            case Equals.KIND_EQUALS:
            case NotEquals.KIND_NOT_EQUALS:
            case Multiply.KIND_MULTIPLY:
            case And.KIND_AND:
            case Or.KIND_OR:
            case BitAnd.KIND_BITAND:
            case BitOr.KIND_BITOR:
            case BitXor.KIND_BITXOR:
                return true;
            default:
                return false;
        }
    }

    public static int extractType(int code) {
        return (code & OpCode.DTYPE_MASK) >> 16;
    }
}
