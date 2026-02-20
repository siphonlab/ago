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


import org.siphonlab.ago.AgoSlotDef;

public class ColumnDesc {
    private RdbType rdbType;

    private boolean notNull;
    private String checkConstraint;
    private String checkConstraintName;
    private String comments;
    private String name;
    private boolean primaryKey;
    private ColumnDesc additional;
    private AgoSlotDef slotDef;

    public RdbType getRdbType() {
        return rdbType;
    }

    public void setRdbType(RdbType rdbType) {
        this.rdbType = rdbType;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public String getCheckConstraint() {
        return checkConstraint;
    }

    public void setCheckConstraint(String checkConstraint) {
        this.checkConstraint = checkConstraint;
    }

    public String getCheckConstraintName() {
        return checkConstraintName;
    }

    public void setCheckConstraintName(String checkConstraintName) {
        this.checkConstraintName = checkConstraintName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "(%s AS %s)".formatted(name, rdbType);
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setAdditional(ColumnDesc additional) {
        this.additional = additional;
    }

    public ColumnDesc getAdditional() {
        return additional;
    }

    public void setSlotDef(AgoSlotDef slotDef) {
        this.slotDef = slotDef;
    }

    public AgoSlotDef getSlotDef() {
        return slotDef;
    }
}
