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

public class MetaClass extends AgoClass{

    private AgoClass instanceClass;

    private MetaClass(AgoClassLoader classLoader) {
        super(classLoader, null, "<Meta>", "<Meta>");
        this.type = AgoClass.TYPE_METACLASS;
    }

    public static MetaClass createTheMeta(AgoClassLoader classLoader){
        return new MetaClass(classLoader){
            @Override
            public MetaClass getAgoClass() {
                return this;
            }

            @Override
            public AgoClass getInstanceClass() {
                return this;
            }
        };
    }

    public MetaClass(AgoClassLoader classLoader, MetaClass metaClass, String name) {
        super(classLoader, metaClass, name, name);
        this.type = AgoClass.TYPE_METACLASS;
    }

    public void setInstanceClass(AgoClass instanceClass) {
        this.instanceClass = instanceClass;
    }

    public AgoClass getInstanceClass() {
        return instanceClass;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetaClass m && m.getInstanceClass().equals(this);
    }

    @Override
    public boolean isInGenericTemplate() {
        return this.instanceClass != null && (this.instanceClass.isGenericTemplate() || this.instanceClass.isInGenericTemplate());
    }

    @Override
    public MetaClass cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        throw new UnsupportedOperationException("MetaClass cannot bind scope");
    }

}
