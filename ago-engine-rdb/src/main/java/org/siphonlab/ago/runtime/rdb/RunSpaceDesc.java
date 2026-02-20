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

import java.util.List;
import java.util.Objects;

public final class RunSpaceDesc {
    private long id;
    private String runSpaceHostClass;
    private ObjectRef currFrame;
    private ResultSlots resultSlots;
    private byte runningState;
    private ObjectRef exception;

    private List<RunSpaceDesc> pausingParents;

    private List<RunSpaceDesc> forkedRunSpaces;
    private RunSpaceDesc parentRunSpace;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRunSpaceHostClass() {
        return runSpaceHostClass;
    }

    public void setRunSpaceHostClass(String runSpaceHostClass) {
        this.runSpaceHostClass = runSpaceHostClass;
    }

    public ObjectRef getCurrFrame() {
        return currFrame;
    }

    public void setCurrFrame(ObjectRef currFrame) {
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

    public ObjectRef getException() {
        return exception;
    }

    public void setException(ObjectRef exception) {
        this.exception = exception;
    }

    public List<RunSpaceDesc> getPausingParents() {
        return pausingParents;
    }

    public void setPausingParents(List<RunSpaceDesc> pausingParents) {
        this.pausingParents = pausingParents;
    }

    public List<RunSpaceDesc> getForkedRunSpaces() {
        return forkedRunSpaces;
    }

    public void setForkedRunSpaces(List<RunSpaceDesc> forkedRunSpaces) {
        this.forkedRunSpaces = forkedRunSpaces;
    }

    public RunSpaceDesc getParentRunSpace() {
        return parentRunSpace;
    }

    public void setParentRunSpace(RunSpaceDesc parentRunSpace) {
        this.parentRunSpace = parentRunSpace;
    }

}
