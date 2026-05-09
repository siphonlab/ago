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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class Property {

    protected final AgoClass ownerClass;

    protected Property(AgoClass ownerClass) {this.ownerClass = ownerClass;}

    public abstract String getName();

    public abstract boolean isReadable();

    public abstract boolean isWritable();

    public abstract AgoClass getType();

    public abstract Visibility getVisibilityForRead();

    public abstract Visibility getVisibilityForWrite();


    public AgoClass getOwnerClass() {
        return ownerClass;
    }

    public static class FieldProperty extends Property{

        private final AgoField agoField;

        public FieldProperty(AgoClass ownerClass, AgoField agoField){
            super(ownerClass);
            this.agoField = agoField;
        }

        @Override
        public String getName() {
            return agoField.getName();
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isWritable() {
            return !Modifier.isFinal(agoField.getModifiers());
        }

        @Override
        public AgoClass getType() {
            return agoField.getAgoClass();
        }

        @Override
        public Visibility getVisibilityForRead() {
            return Modifier.getVisibility(agoField.getModifiers());
        }

        @Override
        public Visibility getVisibilityForWrite() {
            return Modifier.getVisibility(agoField.getModifiers());
        }

        public AgoField getAgoField() {
            return agoField;
        }
    }

    public static class AttributeProperty extends Property{
        private final AgoFunction getter;
        private final AgoFunction setter;

        public AttributeProperty(AgoClass agoClass, AgoFunction getter, AgoFunction setter){
            super(agoClass);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public String getName() {
            return ObjectUtils.getIfNull(getter.getCommonName(), setter.getCommonName());
        }

        @Override
        public boolean isReadable() {
            return getter != null;
        }

        @Override
        public boolean isWritable() {
            return setter != null;
        }

        @Override
        public AgoClass getType() {
            return getter.getResultClass();
        }

        @Override
        public Visibility getVisibilityForRead() {
            return Modifier.getVisibility(getter.getModifiers());
        }

        @Override
        public Visibility getVisibilityForWrite() {
            return Modifier.getVisibility(setter.getModifiers());
        }

        public AgoFunction getGetter() {
            return getter;
        }

        public AgoFunction getSetter() {
            return setter;
        }
    }
}
