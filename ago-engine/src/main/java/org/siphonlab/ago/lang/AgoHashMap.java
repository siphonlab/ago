package org.siphonlab.ago.lang;

import org.agrona.collections.Int2NullableObjectHashMap;
import org.agrona.collections.Long2NullableObjectHashMap;
import org.eclipse.collections.api.iterator.*;
import org.eclipse.collections.impl.list.mutable.primitive.*;
import org.siphonlab.ago.GenericArgumentsInfo;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.TypeInfo;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.siphonlab.ago.TypeCode.*;

public class AgoHashMap {

    public static void create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];     // Key

        var map = switch (typeInfo.getTypeCode().value){
            case INT_VALUE, SHORT_VALUE, BYTE_VALUE, BOOLEAN_VALUE, CLASS_REF_VALUE, FLOAT_VALUE, CHAR_VALUE -> new Int2NullableObjectHashMap<>();  // float stored with Float.floatToIntBits
            case LONG_VALUE, DOUBLE_VALUE -> new Long2NullableObjectHashMap<>();    // double stored with Double.doubleToLongBits

            default -> new java.util.HashMap<>();
        };
        instance.setNativePayload(map);
        callFrame.finishVoid();
    }

    public static void getCount(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        callFrame.finishInt(map.size());
    }

    public static void isReadOnly(NativeFrame callFrame) {
        callFrame.finishBoolean(false);
    }

    public static void clear(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        map.clear();
        callFrame.finishVoid();
    }

    /**
     * The map stored in the instance may be one of:
     *   - Int2NullableObjectHashMap  (for int/short/byte/boolean/char/floats)
     *   - Long2NullableObjectHashMap (for long/doubles)
     *   - java.util.HashMap          (for String / Object keys)
     *
     * For the numeric key types we use a primitive‑hash‑map so that
     * the lookup is O(1) without boxing.  Float/double keys are converted to
     * their bit‑representation before querying the map.
     */
    public static final class Get {

        /* ---------- int key ---------------------------------------------------- */
        public static void get(NativeFrame callFrame, int key) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();
            Object valObj;

            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();

            if (map instanceof Int2NullableObjectHashMap<?> hm) {
                valObj = hm.get(key);
            } else {
                valObj = map.get(key);          
            }

            finish(callFrame, valObj);
        }

        /* ---------- long key --------------------------------------------------- */
        public static void get(NativeFrame callFrame, long key) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();
            Object valObj;

            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();

            if (map instanceof Long2NullableObjectHashMap<?> hm) {         
                valObj = hm.get(key);
            } else {
                valObj = map.get(key);                              
            }

            finish(callFrame, valObj);
        }

        public static void get(NativeFrame callFrame, float key) {
            int bits = Float.floatToIntBits(key);
            get(callFrame, bits);
        }

        public static void get(NativeFrame callFrame, double key) {
            long bits = Double.doubleToLongBits(key);
            get(callFrame, (long)bits);
        }

        public static void get(NativeFrame callFrame, boolean key) {
            get(callFrame, key ? 1 : 0);
        }

        public static void get(NativeFrame callFrame, String key) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();

            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
            Object valObj = map.get(key);

            finish(callFrame, valObj);
        }

        public static void get(NativeFrame callFrame, short key) {
            get(callFrame, (int)key);
        }

        public static void get(NativeFrame callFrame, byte key) {
            get(callFrame, (int)key);
        }

        public static void get(NativeFrame callFrame, char key) {
            get(callFrame, (int)key);
        }

        public static void get(NativeFrame callFrame, Instance<?> key) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();

            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
            Object valObj = map.get(key);

            finish(callFrame, valObj);
        }

        private static void finish(NativeFrame callFrame, Object valObj) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();

            GenericArgumentsInfo genericArgs =
                    (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
            TypeInfo valueType = genericArgs.getArguments()[1];

            Object v = valObj == null ? TypeCode.defaultValue(valueType.getTypeCode()) : valObj;

            switch (valueType.getTypeCode().value) {
                case INT_VALUE:     callFrame.finishInt((Integer) v);   break;
                case LONG_VALUE:    callFrame.finishLong((Long) v);      break;
                case FLOAT_VALUE:   callFrame.finishFloat((Float) v);    break;
                case DOUBLE_VALUE:  callFrame.finishDouble((Double) v);  break;
                case BOOLEAN_VALUE: callFrame.finishBoolean((Boolean) v);break;
                case STRING_VALUE:  callFrame.finishString((String) v);  break;
                case SHORT_VALUE:   callFrame.finishShort((Short) v);    break;
                case BYTE_VALUE:    callFrame.finishByte((Byte) v);     break;
                case CHAR_VALUE:    callFrame.finishChar((Character) v);break;

                case CLASS_REF_VALUE:
                    if (v != null) {
                        callFrame.finishClassRef(callFrame.getAgoEngine()
                                .getClass((Integer) v));
                    } else {
                        callFrame.finishClassRef(null);
                    }
                    break;

                case OBJECT_VALUE:
                    callFrame.finishObject((Instance<?>) v);
                    break;
            }
        }
    }


    /**
     * Put helper – all overloads simply forward to the single
     * put(Object,Object) implementation.
     */
    /**
     * The map stored in the instance may be one of:
     *   - Int2NullableObjectHashMap  (for int/short/byte/boolean/char/floats)
     *   - Long2NullableObjectHashMap (for long/doubles)
     *   - java.util.HashMap          (for String / Object keys)
     *
     * For the numeric key types we use a primitive‑hash‑map so that
     * the lookup is O(1) without boxing.  Float/double keys are converted to
     * their bit‑representation before querying/putting in the map.
     */
    public final class Put {

        /* ---------- int / short / byte / boolean / float / char key ---------- */
        private static void putIntKey(NativeFrame callFrame, int key, Object value) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();

            if (map instanceof Int2NullableObjectHashMap<?> hm) {
                // cast to raw type – the generic key is Integer
                ((Int2NullableObjectHashMap<Object>) hm).put(key, value);
            } else {
                @SuppressWarnings("unchecked")
                Map<Integer, Object> m = (Map<Integer, Object>) map;
                m.put(Integer.valueOf(key), value);
            }
            callFrame.finishVoid();
        }

        /* ---------- long key ------------------------------------------------ */
        private static void putLongKey(NativeFrame callFrame, long key, Object value) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();

            if (map instanceof Long2NullableObjectHashMap<?> hm) {
                ((Long2NullableObjectHashMap<Object>) hm).put(key, value);
            } else {
                @SuppressWarnings("unchecked")
                Map<Long, Object> m = (Map<Long, Object>) map;
                m.put(Long.valueOf(key), value);
            }
            callFrame.finishVoid();
        }

        /* ---------- string / Instance<?> key -------------------------------- */
        private static void putObjectKey(NativeFrame callFrame, Object key, Object value) {
            NativeInstance instance = (NativeInstance) callFrame.getParentScope();
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) instance.getNativePayload();

            map.put(key, value);
            callFrame.finishVoid();
        }

        /* ---------- public overloads ---------------------------------------- */

        /* int key */
        public static void put(NativeFrame callFrame, int key, int   value) { putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, long  value) { putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, float value) { putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, double value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, boolean value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, String value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, short value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, byte  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, char  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, int key, Instance<?> value){ putIntKey(callFrame, key, (Object) value); }

        /* long key */
        public static void put(NativeFrame callFrame, long key, int   value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, long  value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, float value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, double value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, boolean value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, String value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, short value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, byte  value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, char  value){ putLongKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, long key, Instance<?> value){ putLongKey(callFrame, key, (Object) value); }

        /* float key */
        public static void put(NativeFrame callFrame, float key, int   value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, long  value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, float value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, double value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, boolean value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, String value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, short value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, byte  value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, char  value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, float key, Instance<?> value){ putIntKey(callFrame, Float.floatToIntBits(key), (Object) value); }

        /* double key */
        public static void put(NativeFrame callFrame, double key, int   value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, long  value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, float value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, double value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, boolean value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, String value){ putLongKey(callFrame, Double.doubleToLongBits(key), value); }
        public static void put(NativeFrame callFrame, double key, short value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, byte  value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, char  value){ putLongKey(callFrame, Double.doubleToLongBits(key), (Object) value); }
        public static void put(NativeFrame callFrame, double key, Instance<?> value){ putLongKey(callFrame, Double.doubleToLongBits(key), value); }

        /* boolean key */
        public static void put(NativeFrame callFrame, boolean key, int   value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, long  value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, float value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, double value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, boolean value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, String value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, short value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, byte  value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, char  value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }
        public static void put(NativeFrame callFrame, boolean key, Instance<?> value){ putIntKey(callFrame, key ? 1 : 0, (Object) value); }

        /* String key */
        public static void put(NativeFrame callFrame, String key, int   value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, long  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, float value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, double value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, boolean value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, String value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, short value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, byte  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, char  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, String key, Instance<?> value){ putObjectKey(callFrame, key, (Object) value); }

        /* short key */
        public static void put(NativeFrame callFrame, short key, int   value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, long  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, float value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, double value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, boolean value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, String value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, short value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, byte  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, char  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, short key, Instance<?> value){ putIntKey(callFrame, key, (Object) value); }

        /* byte key */
        public static void put(NativeFrame callFrame, byte key, int   value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, long  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, float value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, double value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, boolean value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, String value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, short value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, byte  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, char  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, byte key, Instance<?> value){ putIntKey(callFrame, key, (Object) value); }

        /* char key */
        public static void put(NativeFrame callFrame, char key, int   value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, long  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, float value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, double value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, boolean value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, String value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, short value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, byte  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, char  value){ putIntKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, char key, Instance<?> value){ putIntKey(callFrame, key, (Object) value); }

        /* Instance<?> key */
        public static void put(NativeFrame callFrame, Instance<?> key, int   value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, long  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, float value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, double value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, boolean value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, String value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, short value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, byte  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, char  value){ putObjectKey(callFrame, key, (Object) value); }
        public static void put(NativeFrame callFrame, Instance<?> key, Instance<?> value){ putObjectKey(callFrame, key, (Object) value); }
    }

    public static void containsKey(NativeFrame callFrame, int key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        if (map instanceof Int2NullableObjectHashMap<?>) {
            callFrame.finishBoolean(((Int2NullableObjectHashMap<?>) map).containsKey(key));
        } else {
            callFrame.finishBoolean(map.containsKey(key));
        }
    }

    public static void containsKey(NativeFrame callFrame, long key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        if (map instanceof Long2NullableObjectHashMap<?>) {
            callFrame.finishBoolean(((Long2NullableObjectHashMap<?>) map).containsKey(key));
        } else {
            callFrame.finishBoolean(map.containsKey(key));
        }
    }

    public static void containsKey(NativeFrame callFrame, float key) {
        containsKey(callFrame, (int)key);
    }

    public static void containsKey(NativeFrame callFrame, double key) {
        containsKey(callFrame, (long)key);
    }

    public static void containsKey(NativeFrame callFrame, boolean key) {
        containsKey(callFrame, key ? 1 : 0);
    }

    public static void containsKey(NativeFrame callFrame, String key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        callFrame.finishBoolean(map.containsKey(key));
    }

    public static void containsKey(NativeFrame callFrame, short key) {
        containsKey(callFrame, (int)key);
    }

    public static void containsKey(NativeFrame callFrame, byte key) {
        containsKey(callFrame, (int)key);
    }

    public static void containsKey(NativeFrame callFrame, char key) {
        containsKey(callFrame, (int)key);
    }

    public static void containsKey(NativeFrame callFrame, Instance<?> key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        callFrame.finishBoolean(map.containsKey(key));
    }


    public static void removeByKey(NativeFrame callFrame, int key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        if (map instanceof Int2NullableObjectHashMap<?>) {
            boolean res = ((Int2NullableObjectHashMap<?>) map).remove(key) != null;
            callFrame.finishBoolean(res);
        } else {
            boolean res = map.remove(key) != null;
            callFrame.finishBoolean(res);
        }
    }

    public static void removeByKey(NativeFrame callFrame, long key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();
        if (map instanceof Long2NullableObjectHashMap<?>) {
            boolean res = ((Long2NullableObjectHashMap<?>) map).remove(key) != null;
            callFrame.finishBoolean(res);
        } else {
            boolean res = map.remove(key) != null;
            callFrame.finishBoolean(res);
        }
    }

    public static void removeByKey(NativeFrame callFrame, float key) {
        removeByKey(callFrame, Float.floatToIntBits(key));
    }

    public static void removeByKey(NativeFrame callFrame, double key) {
        removeByKey(callFrame, Double.doubleToLongBits(key));
    }

    public static void removeByKey(NativeFrame callFrame, boolean key) {
        removeByKey(callFrame, Double.doubleToLongBits(key ? 1 : 0));
    }

    public static void removeByKey(NativeFrame callFrame, String key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<Object, Object> map = (Map<Object, Object>) instance.getNativePayload();
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, short key) {
        removeByKey(callFrame, (int) key);
    }

    public static void removeByKey(NativeFrame callFrame, byte key) {
        removeByKey(callFrame, (int) key);
    }

    public static void removeByKey(NativeFrame callFrame, char key) {
        removeByKey(callFrame, (int) key);
    }

    public static void removeByKey(NativeFrame callFrame, Instance<?> key) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Map<Object, Object> map = (Map<Object, Object>) instance.getNativePayload();
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }


    public static void keys(NativeFrame callFrame) {
        callFrame.finishObject(null);   // 目前不实现
    }

    public static void values(NativeFrame callFrame) {
        callFrame.finishObject(null);   // 目前不实现
    }

    /* ---------- Iterator ---------------- */

    public static class HashMapIterator implements Iterator<Map.Entry<Object, Object>> {

        private final Iterator<Map.Entry<Object, Object>> it;

        public HashMapIterator(Iterator<Map.Entry<Object, Object>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Map.Entry<Object, Object> next() {
            return it.next();
        }
    }

    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        Object payload = instance.getNativePayload();

        HashMap<?, ?> map = (HashMap<?, ?>) payload;
        var it = map.entrySet().iterator();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        // 直接使用 Java 的 Iterator
        iteratorInstance.setNativePayload(it);
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Object itObj = iteratorInstance.getNativePayload();

        boolean has;
        if (itObj instanceof Iterator<?> iter) {
            has = iter.hasNext();
        } else {
            // 保险
            has = ((Iterator<?>) itObj).hasNext();
        }
        callFrame.finishBoolean(has);
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Object itObj = iteratorInstance.getNativePayload();

        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<Object, Object>> iter = (Iterator<Map.Entry<Object, Object>>) itObj;
        Map.Entry<Object, Object> entry = iter.next();

        // 直接返回 Java 的 Entry，满足 Ago 中 key/value 属性的访问
//        callFrame.finishObject(entry);
    }
}
