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

public class LangClasses {

    private final AgoClass objectClass;
    private final AgoClass classClass;        //lang.Class
    private final AgoClass anyClass;
    private final AgoClass classRefClass;
    private final AgoClass classIntervalClass;
    private final AgoClass scopedClassIntervalClass;
    private final AgoClass genericTypeParameterClass;
    private final AgoClass throwableClass;
    private final AgoClass functionClass;
    private final AgoClass runSpaceClass;

    private final AgoClass integerClass;
    private final AgoClass longClass;
    private final AgoClass byteClass;
    private final AgoClass charClass;
    private final AgoClass shortClass;
    private final AgoClass stringClass;
    private final AgoClass booleanClass;
    private final AgoClass floatClass;
    private final AgoClass doubleClass;

    private final AgoClass primitiveClass;
    private final AgoClass primitiveNumberClass;


    private final AgoClass arrayClass;
    private final AgoClass intArrayClass;
    private final AgoClass longArrayClass;
    private final AgoClass byteArrayClass;
    private final AgoClass charArrayClass;
    private final AgoClass shortArrayClass;
    private final AgoClass stringArrayClass;
    private final AgoClass booleanArrayClass;
    private final AgoClass floatArrayClass;
    private final AgoClass doubleArrayClass;
    private final AgoClass classRefArrayClass;
    private final AgoClass objectArrayClass;

    private final AgoClass intEnumClass;
    private final AgoClass byteEnumClass;
    private final AgoClass shortEnumClass;
    private final AgoClass longEnumClass;

    public LangClasses(ClassManager classManager) {
        this.objectClass = classManager.getClass("lang.Object");
        this.classClass = classManager.getClass("lang.Class");
        this.anyClass = classManager.getClass("lang.Any");
        this.classRefClass = classManager.getClass("lang.ClassRef");
        this.classIntervalClass = classManager.getClass("lang.ClassInterval");
        this.scopedClassIntervalClass = classManager.getClass("lang.ScopedClassInterval");
        this.genericTypeParameterClass = classManager.getClass("lang.GenericTypeParameter");
        this.throwableClass = classManager.getClass("lang.Throwable");
        this.functionClass = classManager.getClass("lang.Function");
        this.runSpaceClass = classManager.getClass("lang.RunSpace");

        this.integerClass = classManager.getClass("lang.Integer");
        this.longClass = classManager.getClass("lang.Long");
        this.byteClass = classManager.getClass("lang.Byte");
        this.charClass = classManager.getClass("lang.Char");
        this.shortClass = classManager.getClass("lang.Short");
        this.stringClass = classManager.getClass("lang.String");
        this.booleanClass = classManager.getClass("lang.Boolean");
        this.floatClass = classManager.getClass("lang.Float");
        this.doubleClass = classManager.getClass("lang.Double");

        this.primitiveClass = classManager.getClass("lang.Primitive");
        this.primitiveNumberClass = classManager.getClass("lang.PrimitiveNumber");

        this.arrayClass = classManager.getClass("lang.Array");
        this.intArrayClass = classManager.getClass("lang.[int");
        this.longArrayClass = classManager.getClass("lang.[long");
        this.byteArrayClass = classManager.getClass("lang.[byte");
        this.charArrayClass = classManager.getClass("lang.[char");
        this.shortArrayClass = classManager.getClass("lang.[short");
        this.stringArrayClass = classManager.getClass("lang.[string");
        this.booleanArrayClass = classManager.getClass("lang.[boolean");
        this.floatArrayClass = classManager.getClass("lang.[float");
        this.doubleArrayClass = classManager.getClass("lang.[double");
        this.classRefArrayClass = classManager.getClass("lang.[classref");
        this.objectArrayClass = classManager.getClass("lang.[Object");

        this.intEnumClass = classManager.getClass("lang.IntEnum");
        this.byteEnumClass = classManager.getClass("lang.ByteEnum");
        this.shortEnumClass = classManager.getClass("lang.ShortEnum");
        this.longEnumClass = classManager.getClass("lang.LongEnum");

    }

    public AgoClass getObjectClass() {
        return objectClass;
    }

    public AgoClass getClassClass() {
        return classClass;
    }

    public AgoClass getAnyClass() {
        return anyClass;
    }

    public AgoClass getClassRefClass() {
        return classRefClass;
    }

    public AgoClass getClassIntervalClass() {
        return classIntervalClass;
    }

    public AgoClass getScopedClassIntervalClass() {
        return scopedClassIntervalClass;
    }

    public AgoClass getGenericTypeParameterClass() {
        return genericTypeParameterClass;
    }

    public AgoClass getThrowableClass() {
        return throwableClass;
    }

    public AgoClass getFunctionClass() {
        return functionClass;
    }

    public AgoClass getRunSpaceClass() {
        return runSpaceClass;
    }

    public AgoClass getIntegerClass() {
        return integerClass;
    }

    public AgoClass getLongClass() {
        return longClass;
    }

    public AgoClass getByteClass() {
        return byteClass;
    }

    public AgoClass getCharClass() {
        return charClass;
    }

    public AgoClass getShortClass() {
        return shortClass;
    }

    public AgoClass getStringClass() {
        return stringClass;
    }

    public AgoClass getPrimitiveClass() {
        return primitiveClass;
    }

    public AgoClass getPrimitiveNumberClass() {
        return primitiveNumberClass;
    }

    public AgoClass getBooleanClass() {
        return booleanClass;
    }

    public AgoClass getFloatClass() {
        return floatClass;
    }

    public AgoClass getDoubleClass() {
        return doubleClass;
    }

    public AgoClass getArrayClass() {
        return arrayClass;
    }

    public AgoClass getIntArrayClass() {
        return intArrayClass;
    }

    public AgoClass getLongArrayClass() {
        return longArrayClass;
    }

    public AgoClass getByteArrayClass() {
        return byteArrayClass;
    }

    public AgoClass getCharArrayClass() {
        return charArrayClass;
    }

    public AgoClass getShortArrayClass() {
        return shortArrayClass;
    }

    public AgoClass getStringArrayClass() {
        return stringArrayClass;
    }

    public AgoClass getBooleanArrayClass() {
        return booleanArrayClass;
    }

    public AgoClass getFloatArrayClass() {
        return floatArrayClass;
    }

    public AgoClass getDoubleArrayClass() {
        return doubleArrayClass;
    }

    public AgoClass getClassRefArrayClass() {
        return classRefArrayClass;
    }

    public AgoClass getObjectArrayClass() {
        return objectArrayClass;
    }

    public AgoClass getIntEnumClass() {
        return intEnumClass;
    }

    public AgoClass getByteEnumClass() {
        return byteEnumClass;
    }

    public AgoClass getShortEnumClass() {
        return shortEnumClass;
    }

    public AgoClass getLongEnumClass() {
        return longEnumClass;
    }
}
