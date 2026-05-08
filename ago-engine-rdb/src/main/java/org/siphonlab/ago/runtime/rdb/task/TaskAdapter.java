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
package org.siphonlab.ago.runtime.rdb.task;

import org.agrona.concurrent.IdGenerator;
import org.siphonlab.ago.BoxTypes;
import org.siphonlab.ago.ClassManager;
import org.siphonlab.ago.runtime.rdb.SavableRunSpace;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonPGAdapter;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TaskAdapter extends LazyJsonPGAdapter {
    public TaskAdapter(BoxTypes boxTypes, ClassManager classManager, DataSource ds, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
    }

    public void saveRunspaceWithTx(@Nonnull Connection conn, SavableRunSpace space) {
        var sql = """
                update ago_runspace
                set curr_frame_table = ?, -- 1
                    curr_frame_id = ?, -- 2
                    result_slots = ?, -- 3
                    running_state = ?, -- 4
                    exception_id = ?, -- 5
                    pausing_parents = ?, -- 6
                    forked_runspaces = ? -- 7
                where id = ? -- 8
                """;
        var obj = this.toUpdateMap(space);

        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) obj.get("curr_frame_table"));
            ps.setObject(2, obj.get("curr_frame_id"));
            ps.setObject(3, obj.get("result_slots"));
            ps.setByte(4, (Byte) obj.get("running_state"));
            ps.setObject(5, obj.get("exception_id"));
            ps.setArray(6, conn.createArrayOf("bigint", (Long[]) obj.get("pausing_parents")));
            ps.setArray(7, conn.createArrayOf("bigint", (Long[]) obj.get("forked_runspaces")));
            ps.setObject(8, obj.get("id"));

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
