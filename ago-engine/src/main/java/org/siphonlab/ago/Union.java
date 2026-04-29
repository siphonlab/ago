package org.siphonlab.ago;

import org.siphonlab.ago.classloader.ClassRefValue;

import java.math.BigDecimal;
import java.math.MathContext;

public interface Union {
    static TypeCode extractUnionType(Object union){
        return switch (union) {
            case null -> TypeCode.NULL;
            case String s -> TypeCode.STRING;
            case ClassRefValue v-> TypeCode.CLASS_REF;
            case Instance<?> u -> TypeCode.OBJECT;
            case Integer number -> TypeCode.INT;
            case Boolean b -> TypeCode.BOOLEAN;
            case Double number -> TypeCode.DOUBLE;
            case Long number -> TypeCode.LONG;
            case Byte number -> TypeCode.BYTE;
            case BigDecimal number -> TypeCode.DECIMAL;
            case Float number -> TypeCode.FLOAT;
            case Character c -> TypeCode.CHAR;
            case Short number -> TypeCode.SHORT;
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static String unionToString(Object union, AgoEngine engine) {
        return switch (union) {
            case String s -> s;
            case null -> "null";
            case ClassRefValue v-> engine.getClass(v.className()).getFullname();
            case Instance<?> u -> engine.toString(u);
            case Integer number -> number.toString();
            case Boolean b -> b.toString();
            case Double number -> number.toString();
            case Long number -> number.toString();
            case Byte number -> number.toString();
            case BigDecimal number -> number.toString();
            case Float number -> number.toString();
            case Character c -> c.toString();
            case Short number -> number.toString();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static boolean unionToBoolean(Object union, AgoEngine engine) {
        return switch (union) {
            case null -> false;
            case Boolean b -> b;
            case String s -> !s.isEmpty();
            case ClassRefValue v-> true;
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) yield true;
                yield unionToBoolean(union, engine);
            }
            case Integer number -> number != 0;
            case Double number -> number != 0;
            case Long number -> number != 0;
            case Byte number -> number != 0;
            case BigDecimal number -> !number.equals(BigDecimal.ZERO);
            case Float number -> number != 0;
            case Character c -> c != 0;
            case Short number -> number != 0;
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static char unionToChar(Object union, AgoEngine engine) {
        return switch (union) {
            case Character c -> c;
            case null -> '\0';
            case String s -> AgoFrame.stringToChar(s);
            case ClassRefValue v-> throw new ClassCastException("%s is a class, cannot cast to char".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to char".formatted(v));
                yield unionToChar(union, engine);
            }
            case Integer number -> (char)number.intValue();
            case Boolean b -> b ? 't' : 'f';
            case Double number -> (char)number.intValue();
            case Long number -> (char)number.intValue();
            case Byte number -> (char)number.intValue();
            case BigDecimal number -> (char)number.intValue();
            case Float number -> (char)number.intValue();
            case Short number -> (char)number.intValue();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static float unionToFloat(Object union, AgoEngine engine) {
        return switch (union) {
            case Float number -> number;
            case null -> throw new ClassCastException("cannot cast null to float");
            case String s -> Float.parseFloat(s);
            case ClassRefValue v-> throw new ClassCastException("%s is a class, cannot cast to float".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to char".formatted(v));
                yield unionToChar(union, engine);
            }
            case Integer number -> number;
            case Boolean b -> b ? 1f : 0f;
            case Double number -> number.floatValue();
            case Long number -> number.floatValue();
            case Byte number -> number;
            case BigDecimal number -> number.floatValue();
            case Character c -> c;
            case Short number -> number;
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static double unionToDouble(Object union, AgoEngine engine) {
        return switch (union) {
            case Double number -> number;
            case null -> throw new ClassCastException("cannot cast null to double");
            case String s -> Double.parseDouble(s);
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to double".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to double".formatted(v));
                yield unionToDouble(union, engine);
            }
            case Integer number -> number.doubleValue();
            case Boolean b -> b ? 1.0 : 0.0;
            case Long number -> number.doubleValue();
            case Byte number -> number.doubleValue();
            case BigDecimal number -> number.doubleValue();
            case Float number -> number.doubleValue();
            case Character c -> (double) c;
            case Short number -> number.doubleValue();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static byte unionToByte(Object union, AgoEngine engine) {
        return switch (union) {
            case Byte number -> number;
            case null -> throw new ClassCastException("cannot cast null to byte");
            case String s -> {
                try {
                    yield Byte.parseByte(s);
                } catch (NumberFormatException e) {
                    yield (byte) Double.parseDouble(s);
                }
            }
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to byte".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to byte".formatted(v));
                yield unionToByte(union, engine);
            }
            case Integer number -> number.byteValue();
            case Boolean b -> b ? (byte) 1 : (byte) 0;
            case Double number -> number.byteValue();
            case Long number -> number.byteValue();
            case BigDecimal number -> number.byteValue();
            case Float number -> number.byteValue();
            case Character c -> (byte) c.charValue();
            case Short number -> number.byteValue();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static short unionToShort(Object union, AgoEngine engine) {
        return switch (union) {
            case null -> throw new ClassCastException("cannot cast null to short");
            case Short number -> number;
            case String s -> {
                try {
                    yield Short.parseShort(s);
                } catch (NumberFormatException e) {
                    yield (short) Double.parseDouble(s);
                }
            }
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to short".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to short".formatted(v));
                yield unionToShort(union, engine);
            }
            case Integer number -> number.shortValue();
            case Boolean b -> b ? (short) 1 : (short) 0;
            case Double number -> number.shortValue();
            case Long number -> number.shortValue();
            case Byte number -> number;
            case BigDecimal number -> number.shortValue();
            case Float number -> number.shortValue();
            case Character c -> (short) c.charValue();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static int unionToInt(Object union, AgoEngine engine) {
        return switch (union) {
            case Integer number -> number;
            case null -> throw new ClassCastException("cannot cast null to int");
            case String s -> {
                try {
                    yield Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    yield (int) Double.parseDouble(s);
                }
            }
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to int".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to int".formatted(v));
                yield unionToInt(union, engine);
            }
            case Boolean b -> b ? 1 : 0;
            case Double number -> number.intValue();
            case Long number -> number.intValue();
            case Byte number -> number;
            case BigDecimal number -> number.intValue();
            case Float number -> number.intValue();
            case Character c -> (int) c;
            case Short number -> number;
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static long unionToLong(Object union, AgoEngine engine) {
        return switch (union) {
            case Long number -> number;
            case null -> throw new ClassCastException("cannot cast null to long");
            case String s -> {
                try {
                    yield Long.parseLong(s);
                } catch (NumberFormatException e) {
                    yield (long) Double.parseDouble(s);
                }
            }
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to long".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to long".formatted(v));
                yield unionToLong(union, engine);
            }
            case Integer number -> number.longValue();
            case Boolean b -> b ? 1L : 0L;
            case Double number -> number.longValue();
            case Byte number -> number.longValue();
            case BigDecimal number -> number.longValue();
            case Float number -> number.longValue();
            case Character c -> (long) c;
            case Short number -> number.longValue();
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static BigDecimal unionToDecimal(Object union, AgoEngine engine) {
        return switch (union) {
            case BigDecimal number -> number;
            case null -> throw new ClassCastException("cannot cast null to decimal");
            case String s -> new BigDecimal(s, MathContext.DECIMAL128);
            case ClassRefValue v -> throw new ClassCastException("%s is a class, cannot cast to decimal".formatted(v.className()));
            case Instance<?> u -> {
                var v = engine.getBoxer().unbox(u);
                if(v == u) throw new ClassCastException("%s is an object, cannot cast to decimal".formatted(v));
                yield unionToDecimal(union, engine);
            }
            case Integer number -> BigDecimal.valueOf(number);
            case Boolean b -> b ? BigDecimal.ONE : BigDecimal.ZERO;
            case Double number -> BigDecimal.valueOf(number);
            case Long number -> BigDecimal.valueOf(number);
            case Byte number -> BigDecimal.valueOf(number);
            case Float number -> BigDecimal.valueOf(number);
            case Character c -> BigDecimal.valueOf(c);
            case Short number -> BigDecimal.valueOf(number);
            default -> {
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
            }
        };
    }

    static int unionToClassRef(Object union, AgoEngine engine) {
        switch (union) {
            case ClassRefValue v:
                return engine.getClass(v.className()).getClassId();
            case Instance<?> u:
                var v = engine.getBoxer().unbox(u);
                if (!(v instanceof AgoClass))
                    throw new ClassCastException("%s is an object, cannot cast to decimal".formatted(v));
                return ((AgoClass) v).getClassId();
            case null:
                throw new ClassCastException("cannot cast null to classref");
            case String s:
                throw new ClassCastException("cannot string to classref");
            case Integer number:
                throw new ClassCastException("cannot to classref");
            case Boolean b:
                throw new ClassCastException("cannot to classref");
            case Double number:
                throw new ClassCastException("cannot to classref");
            case Long number:
                throw new ClassCastException("cannot to classref");
            case Byte number:
                throw new ClassCastException("cannot to classref");
            case BigDecimal number:
                throw new ClassCastException("cannot to classref");
            case Float number:
                throw new ClassCastException("cannot to classref");
            case Character c:
                throw new ClassCastException("cannot to classref");
            case Short number:
                throw new ClassCastException("cannot to classref");
            default:
                throw new IllegalArgumentException("unknown union type: " + union.getClass());
        }
    }
}
