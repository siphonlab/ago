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

import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbRunSpace;
import org.siphonlab.ago.runtime.rdb.RdbEngine;

public class ObjectRefResultsRdbRunSpace extends RdbRunSpace {

    public ObjectRefResultsRdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, rdbAdapter, runSpaceHost);
        this.resultSlots = new LazyResultSlots();
    }

    public ObjectRefResultsRdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost, long id) {
        super(agoEngine, rdbAdapter, runSpaceHost, id);
        this.resultSlots = new LazyResultSlots();
    }

    @Override
    protected boolean tryComplete() {
        var r = super.tryComplete();
        if(r){
            ((LazyResultSlots)resultSlots).cleanObjectResult();
        }
        return r;
    }
}
