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
package org.siphonlab.ago.runtime.rdb.reactive;

import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.agrona.concurrent.IdGenerator;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.rdb.ColumnDesc;
import org.siphonlab.ago.runtime.rdb.H2Adapter;

import java.util.Set;

public class H2PersistentAdapter extends H2Adapter {

    private final SlotsAdapter slotsAdapter;

    public H2PersistentAdapter(BoxTypes boxTypes, ClassManager classManager, SlotsAdapter slotsAdapter, IdGenerator idGenerator) {
        super(boxTypes, classManager, idGenerator);
        this.slotsAdapter = slotsAdapter;
    }

    @Override
    public void composeInstanceColumns(CreateTable createTable, AgoClass agoClass, Set<String> usedNames) {
        var columns = createTable.getColumn();

        // ---------------------- instance ----------------------------
        // Instance.agoClass
        ColumnDesc columnAgoClass = composeObjectField("agoClass", usedNames);
        columns.add(toColumn(columnAgoClass));

        // parentScope
        ColumnDesc columnParentScope = composeObjectField("parentScope", usedNames);
        columns.add(toColumn(columnParentScope));

        // creator
        ColumnDesc columnCreator = composeObjectField("creator", usedNames);
        columns.add(toColumn(columnCreator));

        // ---- for AgoClass we don't need store meta information in the db, the meta-data still stored in memory


        // ---------------- AgoFrame/AgoFunction --------------------------
        if(agoClass instanceof AgoFunction agoFunction){
            columns.add(toColumn(composeObjectField("caller", usedNames)));
            columns.add(toColumn(composeField("state", TypeCode.INT, usedNames)));      // PENDING/RUNNING/PAUSE/DONE

            if (agoClass instanceof AgoNativeFunction agoNativeFunction) {
                columns.add(toColumn(composeField("resultSlot", TypeCode.INT, usedNames)));
            } else {
                columns.add(toColumn(composeField("receiverSlot", TypeCode.INT, usedNames)));
                columns.add(toColumn(composeField("pc", TypeCode.INT, usedNames)));
                columns.add(toColumn(composeObjectField("exception", usedNames)));
            }
        }
    }

    @Override
    public ColumnDesc composeIdColumn(Set<String> usedNames) {
        return super.composeIdColumn(usedNames);
    }
}
