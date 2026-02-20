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
package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

public interface ExpandableObject<T extends AgoClass> {
    Instance<?> expand();

    void fold();

    ObjectRefCallFrame<?> getExpander();

    Instance<T> getObjectRefInstance();

    boolean isExpanded();

    Instance<?> getExpandedInstance();

    ExpandableObject<?> expandFor(ExpandableObject<?> expander);
}
