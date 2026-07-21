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
package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.TypeCode;

import java.util.List;
import java.util.Objects;

public abstract class ObjectRef<T> {
    protected final String className;

    public ObjectRef(String className) {
        this.className = className;
    }

    public String className() {return className;}

    public abstract T id();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ObjectRef<?>) obj;
        return Objects.equals(this.className, that.className) &&
                Objects.equals(this.id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, id());
    }

    @Override
    public String toString() {
        return "ObjectRef[" +
                "className=" + className + ", " +
                "id=" + id() + ']';
    }

    @SuppressWarnings("unchecked")          // 由于泛型擦除导致的 unchecked 转换
    public static <T> ObjectRef<T> create(String className, T id) {
        return (ObjectRef<T>) switch (id) {
            case Long l      -> new LongObjectRef(className, l);
            case String s    -> new StringObjectRef(className, s);
            default          -> throw new IllegalArgumentException("unsupported type " + id);
        };
    }
}
