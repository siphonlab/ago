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

import org.agrona.collections.Int2NullableObjectHashMap;
import org.agrona.collections.Int2NullableObjectHashMap;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2NullableObjectHashMap;
import org.eclipse.collections.api.iterator.*;
import org.eclipse.collections.impl.list.mutable.primitive.*;
import org.siphonlab.ago.*;
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
            case INT_VALUE, SHORT_VALUE, BYTE_VALUE, BOOLEAN_VALUE, CLASS_REF_VALUE, FLOAT_VALUE, CHAR_VALUE -> new Int2NullableObjectHashMap<>();  // float stored with Float.floatToIntBits; boolean, true->1, false -> 0;
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


    public static void keys(NativeFrame callFrame, Instance<?> arrayList) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope();
        Object payload = mapInst.getNativePayload();

        var ls = (((NativeInstance)arrayList).getNativePayload());

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        TypeInfo keyType = genericArgumentsInfo.getArguments()[0];
        TypeInfo valueType = genericArgumentsInfo.getArguments()[1];

        Map<?, ?> map = (Map<?, ?>) payload;
        for (Object v : map.keySet()) {
            switch (keyType.getTypeCode().value){
                case INT_VALUE, CLASS_REF_VALUE:
                    ((IntArrayList)ls).add((Integer)v);
                    break;
                case LONG_VALUE:
                    ((LongArrayList)ls).add((Long)v);
                    break;
                case FLOAT_VALUE:
                    ((FloatArrayList)ls).add(Float.intBitsToFloat((Integer) v));
                    break;
                case DOUBLE_VALUE:
                    ((DoubleArrayList)ls).add(Double.longBitsToDouble((Long) v));
                    break;
                case BOOLEAN_VALUE:
                    ((BooleanArrayList)ls).add((Integer) v == 1);
                    break;
                case STRING_VALUE, OBJECT_VALUE:
                    ((java.util.ArrayList<Object>)ls).add(v);
                    break;
                case SHORT_VALUE:
                    ((ShortArrayList)ls).add(((Integer)v).shortValue());
                    break;
                case BYTE_VALUE:
                    ((ByteArrayList)ls).add(((Integer)v).byteValue());
                    break;
                case CHAR_VALUE:
                    ((CharArrayList)ls).add((char)((Integer)v).intValue());
                    break;
                default:
                    throw new IllegalStateException("Unsupported key type: " + keyType.getTypeCode());
            }
        }
        callFrame.finishVoid();
    }

    public static void values(NativeFrame callFrame, Instance<?> arrayList) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope();
        Object payload = mapInst.getNativePayload();

        var ls = (((NativeInstance)arrayList).getNativePayload());

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        TypeInfo keyType = genericArgumentsInfo.getArguments()[0];
        TypeInfo valueType = genericArgumentsInfo.getArguments()[1];

        Map<?, ?> map = (Map<?, ?>) payload;
        for (Object v : map.values()) {
            switch (valueType.getTypeCode().value){
                case INT_VALUE, CLASS_REF_VALUE:
                    ((IntArrayList)ls).add((Integer)v);
                    break;
                case LONG_VALUE:
                    ((LongArrayList)ls).add((Long)v);
                    break;
                case FLOAT_VALUE:
                    ((FloatArrayList)ls).add(((Float) v));
                    break;
                case DOUBLE_VALUE:
                    ((DoubleArrayList)ls).add(((Double) v));
                    break;
                case BOOLEAN_VALUE:
                    ((BooleanArrayList)ls).add((Integer) v == 1);
                    break;
                case STRING_VALUE, OBJECT_VALUE:
                    ((java.util.ArrayList<Object>)ls).add(v);
                    break;
                case SHORT_VALUE:
                    ((ShortArrayList)ls).add(((Integer)v).shortValue());
                    break;
                case BYTE_VALUE:
                    ((ByteArrayList)ls).add(((Integer)v).byteValue());
                    break;
                case CHAR_VALUE:
                    ((CharArrayList)ls).add((char)((Integer)v).intValue());
                    break;
                default:
                    throw new IllegalStateException("Unsupported key type: " + valueType.getTypeCode());
            }
        }
        callFrame.finishVoid();
    }
    /* ---------- Iterator ---------------- */
    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        Object payload = instance.getNativePayload();

        Map<?, ?> map = (Map<?, ?>) payload;
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        if(map instanceof Int2NullableObjectHashMap<?> || map instanceof Long2NullableObjectHashMap<?>) {
            it = map.entrySet().iterator();       // Int2ObjectHashMap has some bug, the position place at last
        }

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        iteratorInstance.setNativePayload(it);
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator<?> iter = (Iterator<?>) iteratorInstance.getNativePayload();
        callFrame.finishBoolean(iter.hasNext());
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope().getParentScope();

        NativeInstance iterInst = (NativeInstance) callFrame.getParentScope();
        Object itObj = iterInst.getNativePayload();
        var IteratorKeyValuePairType = iterInst.getAgoClass().getInterfaces()[0];          // Iterator<KeyValuePair<Key, Value>>
        var KeyValuePairType =((GenericArgumentsInfo)IteratorKeyValuePairType.getConcreteTypeInfo()).getArguments()[0].getAgoClass();

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        TypeInfo keyType = genericArgumentsInfo.getArguments()[0];
        TypeInfo valueType = genericArgumentsInfo.getArguments()[1];

        AgoEngine agoEngine = callFrame.getAgoEngine();
        var r = agoEngine.createInstance(KeyValuePairType, callFrame);
        Slots slots = r.getSlots();

        switch (keyType.getTypeCode().value) {

            case INT_VALUE, CLASS_REF_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                slots.setInt(0, iterator.getIntKey());
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case LONG_VALUE: {
                @SuppressWarnings("unchecked")
                Long2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Long2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                slots.setLong(0, iterator.getLongKey());
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case FLOAT_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                int   bits = iterator.getIntKey();
                float key  = Float.intBitsToFloat(bits);
                slots.setFloat(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case DOUBLE_VALUE: {
                @SuppressWarnings("unchecked")
                Long2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Long2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                long  bits = iterator.getLongKey();
                double key = Double.longBitsToDouble(bits);
                slots.setDouble(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case BOOLEAN_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                boolean key = iterator.getIntKey() != 0;
                slots.setBoolean(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case CHAR_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                char key = (char) iterator.getIntKey();
                slots.setChar(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case SHORT_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                short key = (short) iterator.getIntKey();
                slots.setShort(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }
            case BYTE_VALUE: {
                @SuppressWarnings("unchecked")
                Int2NullableObjectHashMap<Object>.EntryIterator iterator =
                        (Int2NullableObjectHashMap<Object>.EntryIterator) itObj;
                iterator.next();
                byte key = (byte) iterator.getIntKey();
                slots.setByte(0, key);
                writeValue(slots, 1, valueType, iterator.getValue());
                break;
            }

            case STRING_VALUE:
            case OBJECT_VALUE: {
                @SuppressWarnings("unchecked")
                var entry = ((Iterator<Map.Entry<Object, Object>>)itObj).next();
                Object keyObj = entry.getKey();
                switch (keyType.getTypeCode().value) {
                    case STRING_VALUE:
                        slots.setString(0, (String) keyObj);
                        break;
                    case OBJECT_VALUE:
                        slots.setObject(0, (Instance<?>)keyObj);
                        break;
                }
                writeValue(slots, 1, valueType, entry.getValue());
                break;
            }

            default:
                throw new IllegalStateException("Unsupported key type: " + keyType.getTypeCode());
        }
        callFrame.finishObject(r);
    }

    private static void writeValue(Slots slots, int index, TypeInfo valueType, Object val) {
        switch (valueType.getTypeCode().value) {
            case INT_VALUE:
                slots.setInt(index, (Integer) val);
                break;
            case LONG_VALUE:
                slots.setLong(index, (Long) val);
                break;
            case FLOAT_VALUE:
                slots.setFloat(index, (Float) val);
                break;
            case DOUBLE_VALUE:
                slots.setDouble(index, (Double) val);
                break;
            case BOOLEAN_VALUE:
                slots.setBoolean(index, (Boolean) val);
                break;
            case STRING_VALUE:
                slots.setString(index, (String) val);
                break;
            case SHORT_VALUE:
                slots.setShort(index, (Short) val);
                break;
            case BYTE_VALUE:
                slots.setByte(index, (Byte) val);
                break;
            case CHAR_VALUE:
                slots.setChar(index, (Character) val);
                break;
            case OBJECT_VALUE:
                slots.setObject(index, (Instance<?>) val);
                break;
            case CLASS_REF_VALUE:
                slots.setClassRef(index, (Integer) val);
                break;
        }
    }

}
