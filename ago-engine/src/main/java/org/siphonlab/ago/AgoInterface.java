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
package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoInterface extends AgoClass{

    protected AgoInterface(AgoClassLoader classLoader, String fullname, String name) {
        super(classLoader, fullname, name);
        this.type = AgoClass.TYPE_INTERFACE;
    }

    public AgoInterface(AgoClassLoader classLoader, MetaClass metaClass, String fullname, String name) {
        super(classLoader, metaClass, fullname, name);
        this.type = AgoClass.TYPE_INTERFACE;
    }

    @Override
    public AgoInterface cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoInterface(this.getClassLoader(), this.agoClass, this.fullname, this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

}
