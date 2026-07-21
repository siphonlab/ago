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
package org.siphonlab.ago.classloader;

public class ScopedClassIntervalClassHeader extends ParameterizedClassHeader{

    private final String lBoundClassName;
    private final String uBoundClassName;

    public ScopedClassIntervalClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.lBoundClassName = ((ClassRefValue) arguments[0]).className();
        this.uBoundClassName = ((ClassRefValue) arguments[1]).className();
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        throw new UnsupportedOperationException("TOOD");
    }

    public ClassHeader getLBound(){
        return classLoader.getClassHeader(lBoundClassName);
    }

    public ClassHeader getUBound(){
        return classLoader.getClassHeader(uBoundClassName);
    }

    public static String composeName(String lBound, String uBound){
        return "lang.[" + composeNameOfClassInClassInterval(lBound) + '~' + composeNameOfClassInClassInterval(uBound) + ']';
    }

    @Override
    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader parentInstantiation, String suggestionName, String suggestionFullName) {
        var baseClass = classLoader.instantiateReferenceClass(this.baseClass, typeArguments);
        var lBoundClass = classLoader.instantiateReferenceClass(this.lBoundClassName, typeArguments);
        var uBoundClass = classLoader.instantiateReferenceClass(this.uBoundClassName, typeArguments);
        String fullname = composeName(lBoundClassName, uBoundClassName);
        var existed = this.classLoader.getClassHeader(fullname);
        if(existed != null){
            return existed;
        }
        var r = new ScopedClassIntervalClassHeader(fullname, baseClass.fullname, getMetaClass(), constructor, new ClassRefValue[]{
                        new ClassRefValue(lBoundClass.fullname), new ClassRefValue(uBoundClass.fullname)
                }, classLoader);
        this.getSourceTemplate().putInstantiatedClassToCache(typeArguments, r);
        this.classLoader.registerNewClass(r);
        return r;
    }
}
