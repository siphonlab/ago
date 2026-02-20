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
package org.siphonlab.ago.compiler;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.siphonlab.collection.DuplicatedKeyException;

import java.util.*;

public class NamespaceCollection<T extends Namespace> extends TreeMap<String, T> {

    private final boolean forFullName;

    private MultiValuedMap<String, FunctionDef> sameCommonNameFunctions;
    private List<T> uniqueElements;

    public NamespaceCollection(boolean forFullName) {
        this.forFullName = forFullName;
    }

    private void putIfNotExists(String key, T element){
        if(element instanceof Package){
            var existed = this.putIfAbsent(key, element);
            if(existed != null && existed != element){
                throw new DuplicatedKeyException("'%s' already existed".formatted(key));
            }
        } else {
            if (this.containsKey(key))
                throw new DuplicatedKeyException("'%s' already existed".formatted(key));
            this.put(key, element);
        }
    }

    public void add(T element){
        this.putIfNotExists(forFullName ? element.getFullname() : element.getName(), element);
        if(uniqueElements != null) {
            uniqueElements.add(element);
        }

        if(element instanceof FunctionDef f && !forFullName) {
            if(uniqueElements == null) {
                uniqueElements = new ArrayList<>(this.values());    // now there will be multikey to one element, name-> f, common name-> f
            }

            String functionCommonName = f.getCommonName();
            this.putIfAbsent(functionCommonName, element);

            // common name may have many functions
            if(sameCommonNameFunctions == null) sameCommonNameFunctions = new ArrayListValuedHashMap<>();
            sameCommonNameFunctions.put(functionCommonName, f);
        }

        if(uniqueElements != null) {
            assert new HashSet<>(uniqueElements).size() == uniqueElements.size();
        }
    }

    public Collection<T> getUniqueElements() {
        return uniqueElements == null ? this.values() : uniqueElements;
    }

    public Collection<FunctionDef> getFunctionsByCommonName(String commonName){
        if(sameCommonNameFunctions == null) return null;
        return sameCommonNameFunctions.get(commonName);
    }

    public Collection<T> search(String name){
        var existed = this.get(name);
        if(existed instanceof FunctionDef f && !name.endsWith("#")){
            return (Collection<T>) sameCommonNameFunctions.get(f.getCommonName());
        }
        return Collections.singleton(existed);
    }

}
