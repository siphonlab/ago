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

import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;

import java.util.List;

public final class RunSpaceDesc<Id> {
    private Id id;
    private String runSpaceHostClass;
    private ObjectRef<Id> currFrame;
    private ResultSlots resultSlots;
    private byte runningState;
    private ObjectRef<Id> exception;

    private List<RunSpaceDesc<Id>> pausingParents;

    private List<RunSpaceDesc<Id>> forkedRunSpaces;
    private RunSpaceDesc<Id> parentRunSpace;

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getRunSpaceHostClass() {
        return runSpaceHostClass;
    }

    public void setRunSpaceHostClass(String runSpaceHostClass) {
        this.runSpaceHostClass = runSpaceHostClass;
    }

    public ObjectRef<Id> getCurrFrame() {
        return currFrame;
    }

    public void setCurrFrame(ObjectRef<Id> currFrame) {
        this.currFrame = currFrame;
    }

    public ResultSlots getResultSlots() {
        return resultSlots;
    }

    public void setResultSlots(ResultSlots resultSlots) {
        this.resultSlots = resultSlots;
    }

    public byte getRunningState() {
        return runningState;
    }

    public void setRunningState(byte runningState) {
        this.runningState = runningState;
    }

    public ObjectRef<Id> getException() {
        return exception;
    }

    public void setException(ObjectRef<Id> exception) {
        this.exception = exception;
    }

    public List<RunSpaceDesc<Id>> getPausingParents() {
        return pausingParents;
    }

    public void setPausingParents(List<RunSpaceDesc<Id>> pausingParents) {
        this.pausingParents = pausingParents;
    }

    public List<RunSpaceDesc<Id>> getForkedRunSpaces() {
        return forkedRunSpaces;
    }

    public void setForkedRunSpaces(List<RunSpaceDesc<Id>> forkedRunSpaces) {
        this.forkedRunSpaces = forkedRunSpaces;
    }

    public RunSpaceDesc<Id> getParentRunSpace() {
        return parentRunSpace;
    }

    public void setParentRunSpace(RunSpaceDesc<Id> parentRunSpace) {
        this.parentRunSpace = parentRunSpace;
    }

}
