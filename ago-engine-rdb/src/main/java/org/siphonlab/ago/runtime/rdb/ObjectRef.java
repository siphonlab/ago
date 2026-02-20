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
package org.siphonlab.ago.runtime.rdb;

import java.util.Objects;

public final class ObjectRef {
    private final String className;
    private final long id;

    public ObjectRef(String className, long id) {
        this.className = className;
        this.id = id;
    }

    public String className() {return className;}

    public long id() {return id;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ObjectRef) obj;
        return Objects.equals(this.className, that.className) &&
                this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, id);
    }

    @Override
    public String toString() {
        return "ObjectRef[" +
                "className=" + className + ", " +
                "id=" + id + ']';
    }
}
