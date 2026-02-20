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
package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.ClassManager;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public final class TableOfClass {
    private final AgoClass agoClass;
    private final String tableName;
    private final List<ColumnDesc> columns;

    private final Map<Integer, ColumnDesc> columnBySlotIndex;

    public TableOfClass(AgoClass agoClass, String tableName, List<ColumnDesc> columns) {
        this.agoClass = agoClass;
        this.tableName = tableName;
        this.columns = columns;
        this.columnBySlotIndex = columns.stream().collect(Collectors.toMap(c -> c.getSlotDef().getIndex(), c -> c));
    }

    public ColumnDesc columnDescOfSlot(int slotIndex){
        return columnBySlotIndex.get(slotIndex);
    }

    public static String dump(Map<AgoClass, TableOfClass> tables) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        var data = tables.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getFullname(), e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("table", e.getValue().tableName);
            var items = e.getValue().columns().stream().collect(Collectors.toMap(item -> item.getSlotDef().getName(), item -> item.getAdditional() == null ? item.getName() : Arrays.asList(item.getName(), item.getAdditional().getName())));
            m.put("slots", items);
            return m;
        }));
        return yaml.dump(data);
    }

    private static Map<String, Object> columnDescToMap(ColumnDesc columnDesc) {
        Map<String, Object> r = new LinkedHashMap<>();

        var attrs = new HashMap<String, Object>();
        attrs.put("sqltype", columnDesc.getRdbType().getSqlType());
        attrs.put("typename", columnDesc.getRdbType().getTypeName());
        attrs.put("typecode", columnDesc.getRdbType().getTypeCode().value);
        if (columnDesc.getRdbType().getAgoClass() != null) {
            attrs.put("classname", columnDesc.getRdbType().getAgoClass().getFullname());
        }
        r.put(columnDesc.getName(), attrs);
        return r;
    }

    public static Map<AgoClass, TableOfClass> load(InputStream inputStream, ClassManager classManager, RdbAdapter rdbAdapter) {
        Yaml yaml = new Yaml();
        Map<String, Object> tables = yaml.load(inputStream);

        Map<AgoClass, TableOfClass> tableOfClassMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : tables.entrySet()) {
            var agoClas = classManager.getClass(entry.getKey());
            Map<String, Object> attrs = (Map<String, Object>) entry.getValue();
            String tableName = (String) attrs.get("table");

            List<ColumnDesc> columnsOfSlots = new ArrayList<>();
            AgoSlotDef[] slotDefs = agoClas.getSlotDefs();
            var slotByName = Arrays.stream(slotDefs).collect(Collectors.toMap(s -> s.getName(), s -> s));
            Map<String, Object> slots = (Map<String, Object>) attrs.get("slots");
            int i = 0;
            for (Map.Entry<String, Object> e : slots.entrySet()) {
                String slotName = e.getKey();
                AgoSlotDef slotDef = slotByName.get(slotName);
                assert slotDef != null;
                RdbType rdbType = rdbAdapter.mapType(slotDef.getTypeCode(), slotDef.getAgoClass());
                if (e.getValue() instanceof String s) {
                    var c = new ColumnDesc();
                    c.setSlotDef(slotDef);
                    c.setName(s);
                    c.setRdbType(rdbType);
                    columnsOfSlots.add(c);
                } else {
                    List<String> items = (List<String>) e.getValue();
                    var c = new ColumnDesc();
                    c.setSlotDef(slotDef);
                    c.setName(items.getFirst());
                    c.setRdbType(rdbType);
                    columnsOfSlots.add(c);

                    assert rdbType.getAdditional() != null;
                    ColumnDesc additional = new ColumnDesc();
                    additional.setSlotDef(slotDef);
                    additional.setName(items.get(1));
                    additional.setRdbType(rdbType.getAdditional());
                    c.setAdditional(additional);
                }
            }
            columnsOfSlots.sort(Comparator.comparingInt(o -> o.getSlotDef().getIndex()));
            tableOfClassMap.put(agoClas, new TableOfClass(agoClas, tableName, columnsOfSlots));
        }

        return tableOfClassMap;
    }

    public AgoClass agoClass() {return agoClass;}

    public String tableName() {return tableName;}

    public List<ColumnDesc> columns() {return columns;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (TableOfClass) obj;
        return Objects.equals(this.agoClass, that.agoClass) && Objects.equals(this.tableName, that.tableName) && Objects.equals(this.columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agoClass, tableName, columns);
    }

    @Override
    public String toString() {
        return "TableOfClass[" + "agoClass=" + agoClass + ", " + "tableName=" + tableName + ", " + "columns=" + columns + ']';
    }

}

