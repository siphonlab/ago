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

import org.eclipse.collections.api.iterator.*;
import org.eclipse.collections.impl.list.mutable.primitive.*;
import org.siphonlab.ago.GenericArgumentsInfo;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.TypeInfo;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import static org.siphonlab.ago.TypeCode.*;

public class ArrayList {

    public static void create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];
        var ls = switch (typeInfo.getTypeCode().value) {
            case INT_VALUE -> new IntArrayList();
            case LONG_VALUE -> new LongArrayList();
            case FLOAT_VALUE -> new FloatArrayList();
            case DOUBLE_VALUE -> new DoubleArrayList();
            case BOOLEAN_VALUE -> new BooleanArrayList();
            case STRING_VALUE -> new java.util.ArrayList<String>();
            case SHORT_VALUE -> new ShortArrayList();
            case BYTE_VALUE -> new ByteArrayList();
            case CHAR_VALUE -> new CharArrayList();
            case OBJECT_VALUE -> new java.util.ArrayList<Instance<?>>();
            case CLASS_REF_VALUE -> new IntArrayList();
            default -> throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        };
        instance.setNativePayload(ls);
        callFrame.finishVoid();
    }

    public static void getCount(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof IntArrayList ia) {
            callFrame.finishInt(ia.size());
        } else if (payload instanceof LongArrayList la) {
            callFrame.finishInt(la.size());
        } else if (payload instanceof FloatArrayList fa) {
            callFrame.finishInt(fa.size());
        } else if (payload instanceof DoubleArrayList da) {
            callFrame.finishInt(da.size());
        } else if (payload instanceof BooleanArrayList ba) {
            callFrame.finishInt(ba.size());
        } else if (payload instanceof ShortArrayList sa) {
            callFrame.finishInt(sa.size());
        } else if (payload instanceof ByteArrayList ba2) {
            callFrame.finishInt(ba2.size());
        } else if (payload instanceof CharArrayList ca) {
            callFrame.finishInt(ca.size());
        } else {
            callFrame.finishInt(((java.util.ArrayList<?>) payload).size());
        }
    }

    public static void isReadOnly(NativeFrame callFrame) {
        callFrame.finishBoolean(false);
    }

    public static void clear(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof IntArrayList ia) ia.clear();
        else if (payload instanceof LongArrayList la) la.clear();
        else if (payload instanceof FloatArrayList fa) fa.clear();
        else if (payload instanceof DoubleArrayList da) da.clear();
        else if (payload instanceof BooleanArrayList ba) ba.clear();
        else if (payload instanceof ShortArrayList sa) sa.clear();
        else if (payload instanceof ByteArrayList ba2) ba2.clear();
        else if (payload instanceof CharArrayList ca) ca.clear();
        else ((java.util.ArrayList<?>) payload).clear();
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof IntArrayList ia) ia.add(item);
        else ((java.util.ArrayList<Integer>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        ((java.util.ArrayList<Instance<?>>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof LongArrayList la) la.add(item);
        else ((java.util.ArrayList<Long>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof FloatArrayList fa) fa.add(item);
        else ((java.util.ArrayList<Float>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof DoubleArrayList da) da.add(item);
        else ((java.util.ArrayList<Double>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof BooleanArrayList ba) ba.add(item);
        else ((java.util.ArrayList<Boolean>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        ((java.util.ArrayList<String>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof ShortArrayList sa) sa.add(item);
        else ((java.util.ArrayList<Short>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof ByteArrayList ba2) ba2.add(item);
        else ((java.util.ArrayList<Byte>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof CharArrayList ca) ca.add(item);
        else ((java.util.ArrayList<Character>) payload).add(item);
        callFrame.finishVoid();
    }

    public static void contains(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res = ((java.util.ArrayList<Instance<?>>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof IntArrayList ia) res = ia.contains(item);
        else res = ((java.util.ArrayList<Integer>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof LongArrayList la) res = la.contains(item);
        else res = ((java.util.ArrayList<Long>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof FloatArrayList fa) res = fa.contains(item);
        else res = ((java.util.ArrayList<Float>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof DoubleArrayList da) res = da.contains(item);
        else res = ((java.util.ArrayList<Double>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof BooleanArrayList ba) res = ba.contains(item);
        else res = ((java.util.ArrayList<Boolean>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res = ((java.util.ArrayList<String>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof ShortArrayList sa) res = sa.contains(item);
        else res = ((java.util.ArrayList<Short>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof ByteArrayList ba2) res = ba2.contains(item);
        else res = ((java.util.ArrayList<Byte>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void contains(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof CharArrayList ca) res = ca.contains(item);
        else res = ((java.util.ArrayList<Character>) payload).contains(item);
        callFrame.finishBoolean(res);
    }

    public static void copyTo(NativeFrame callFrame, Instance<?> array, int arrayIndex) {
        // 留空，未实现
        callFrame.finishVoid();
    }

    public static void remove(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof IntArrayList ia) res = ia.remove(item);
        else res = ((java.util.ArrayList<Integer>) payload).remove(Integer.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof LongArrayList la) res = la.remove(item);
        else res = ((java.util.ArrayList<Long>) payload).remove(Long.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof FloatArrayList fa) res = fa.remove(item);
        else res = ((java.util.ArrayList<Float>) payload).remove(Float.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof DoubleArrayList da) res = da.remove(item);
        else res = ((java.util.ArrayList<Double>) payload).remove(Double.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof BooleanArrayList ba) res = ba.remove(item);
        else res = ((java.util.ArrayList<Boolean>) payload).remove(Boolean.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res = ((java.util.ArrayList<String>) payload).remove(item);
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof ShortArrayList sa) res = sa.remove(item);
        else res = ((java.util.ArrayList<Short>) payload).remove(Short.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof ByteArrayList ba2) res = ba2.remove(item);
        else res = ((java.util.ArrayList<Byte>) payload).remove(Byte.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res;
        if (payload instanceof CharArrayList ca) res = ca.remove(item);
        else res = ((java.util.ArrayList<Character>) payload).remove(Character.valueOf(item));
        callFrame.finishBoolean(res);
    }

    public static void remove(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        boolean res = ((java.util.ArrayList<Instance<?>>) payload).remove(item);
        callFrame.finishBoolean(res);
    }

    public static void getAtIndex(NativeFrame callFrame, int index) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object ls = instance.getNativePayload();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];
        switch (typeInfo.getTypeCode().value) {
            case INT_VALUE:
                callFrame.finishInt(((IntArrayList) ls).get(index));
                break;
            case LONG_VALUE:
                callFrame.finishLong(((LongArrayList) ls).get(index));
                break;
            case FLOAT_VALUE:
                callFrame.finishFloat(((FloatArrayList) ls).get(index));
                break;
            case DOUBLE_VALUE:
                callFrame.finishDouble(((DoubleArrayList) ls).get(index));
                break;
            case BOOLEAN_VALUE:
                callFrame.finishBoolean(((BooleanArrayList) ls).get(index));
                break;
            case STRING_VALUE:
                callFrame.finishString(((java.util.ArrayList<String>) ls).get(index));
                break;
            case SHORT_VALUE:
                callFrame.finishShort(((ShortArrayList) ls).get(index));
                break;
            case BYTE_VALUE:
                callFrame.finishByte(((ByteArrayList) ls).get(index));
                break;
            case CHAR_VALUE:
                callFrame.finishChar(((CharArrayList) ls).get(index));
                break;
            case OBJECT_VALUE:
                callFrame.finishObject(((java.util.ArrayList<Instance<?>>) ls).get(index));
                break;
            case CLASS_REF_VALUE:
                callFrame.finishInt(((IntArrayList) ls).get(index));
                break;
            default:
                throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }
    }

    public static void setAtIndex(NativeFrame callFrame, int index, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        ((java.util.ArrayList<Instance<?>>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof IntArrayList ia) ia.set(index, item);
        else ((java.util.ArrayList<Integer>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof LongArrayList la) la.set(index, item);
        else ((java.util.ArrayList<Long>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof FloatArrayList fa) fa.set(index, item);
        else ((java.util.ArrayList<Float>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof DoubleArrayList da) da.set(index, item);
        else ((java.util.ArrayList<Double>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof BooleanArrayList ba) ba.set(index, item);
        else ((java.util.ArrayList<Boolean>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        ((java.util.ArrayList<String>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof ShortArrayList sa) sa.set(index, item);
        else ((java.util.ArrayList<Short>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof ByteArrayList ba2) ba2.set(index, item);
        else ((java.util.ArrayList<Byte>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object payload = instance.getNativePayload();
        if (payload instanceof CharArrayList ca) ca.set(index, item);
        else ((java.util.ArrayList<Character>) payload).set(index, item);
        callFrame.finishVoid();
    }

    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        Object payload = instance.getNativePayload();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Object it;
        if (payload instanceof IntArrayList ia) {
            it = ia.intIterator();
        } else if (payload instanceof LongArrayList la) {
            it = la.longIterator();
        } else if (payload instanceof FloatArrayList fa) {
            it = fa.floatIterator();
        } else if (payload instanceof DoubleArrayList da) {
            it = da.doubleIterator();
        } else if (payload instanceof BooleanArrayList ba) {
            it = ba.booleanIterator();
        } else if (payload instanceof ShortArrayList sa) {
            it = sa.shortIterator();
        } else if (payload instanceof ByteArrayList ba2) {
            it = ba2.byteIterator();
        } else if (payload instanceof CharArrayList ca) {
            it = ca.charIterator();
        } else {
            it = ((java.util.ArrayList<?>) payload).iterator();
        }

        iteratorInstance.setNativePayload(it);
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        var it = iteratorInstance.getNativePayload();

        boolean has;
        if (it instanceof java.util.Iterator<?> iter) {
            has = iter.hasNext();
        } else if (it instanceof IntIterator ii) {
            has = ii.hasNext();
        } else if (it instanceof LongIterator li) {
            has = li.hasNext();
        } else if (it instanceof FloatIterator fi) {
            has = fi.hasNext();
        } else if (it instanceof DoubleIterator di) {
            has = di.hasNext();
        } else if (it instanceof BooleanIterator bi) {
            has = bi.hasNext();
        } else if (it instanceof ShortIterator si) {
            has = si.hasNext();
        } else if (it instanceof ByteIterator boi) {
            has = boi.hasNext();
        } else if (it instanceof CharIterator ci) {
            has = ci.hasNext();
        } else {
            // fall back to generic iterator
            has = ((java.util.Iterator<?>) it).hasNext();
        }

        callFrame.finishBoolean(has);
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        var it = iteratorInstance.getNativePayload();

        GenericArgumentsInfo genericArgumentsInfo =
                (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];

        switch (typeInfo.getTypeCode().value) {
            case INT_VALUE -> {
                if (it instanceof IntIterator ii) {
                    callFrame.finishInt(ii.next());
                } else {
                    callFrame.finishInt(((java.util.Iterator<Integer>) it).next());
                }
            }
            case LONG_VALUE -> {
                if (it instanceof LongIterator li) {
                    callFrame.finishLong(li.next());
                } else {
                    callFrame.finishLong(((java.util.Iterator<Long>) it).next());
                }
            }
            case FLOAT_VALUE -> {
                if (it instanceof FloatIterator fi) {
                    callFrame.finishFloat(fi.next());
                } else {
                    callFrame.finishFloat(((java.util.Iterator<Float>) it).next());
                }
            }
            case DOUBLE_VALUE -> {
                if (it instanceof DoubleIterator di) {
                    callFrame.finishDouble(di.next());
                } else {
                    callFrame.finishDouble(((java.util.Iterator<Double>) it).next());
                }
            }
            case BOOLEAN_VALUE -> {
                if (it instanceof BooleanIterator bi) {
                    callFrame.finishBoolean(bi.next());
                } else {
                    callFrame.finishBoolean(((java.util.Iterator<Boolean>) it).next());
                }
            }
            case STRING_VALUE -> callFrame.finishString(((java.util.Iterator<String>) it).next());
            case SHORT_VALUE -> {
                if (it instanceof ShortIterator si) {
                    callFrame.finishShort(si.next());
                } else {
                    callFrame.finishShort(((java.util.Iterator<Short>) it).next());
                }
            }
            case BYTE_VALUE -> {
                if (it instanceof ByteIterator boi) {
                    callFrame.finishByte(boi.next());
                } else {
                    callFrame.finishByte(((java.util.Iterator<Byte>) it).next());
                }
            }
            case CHAR_VALUE -> {
                if (it instanceof CharIterator ci) {
                    callFrame.finishChar(ci.next());
                } else {
                    callFrame.finishChar(((java.util.Iterator<Character>) it).next());
                }
            }
            case OBJECT_VALUE -> callFrame.finishObject(((java.util.Iterator<Instance<?>>) it).next());
            case CLASS_REF_VALUE -> {
                if (it instanceof IntIterator ii) {
                    callFrame.finishClassRef(callFrame.getAgoEngine().getClass(ii.next()));
                } else {
                    callFrame.finishClassRef(
                            callFrame.getAgoEngine()
                                    .getClass(((java.util.Iterator<Integer>) it).next()));
                }
            }
            default -> throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }
    }

}
