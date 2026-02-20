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

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ReferenceCounter {
    final static Logger logger = LoggerFactory.getLogger(ReferenceCounter.class);

    enum Reason{
        RunCallFrame,
        SetSlotDrop,
        SetSlotInstall,
        SetParentInstall,
        SetCallerInstall, SetCreatorInstall,
        SetCallerDrop,
        CallFrameInterrupt, RestoreCallFrame, DropCurrentCallFrame,
        InstallCurrentCallFrame,
        CleanSlotsForCallFrameQuit,
        DropParentForCallFrameQuit, DropCallerForCallFrameQuit,
        DropCreatorForCallFrameQuit, CallFrameQuitCleanSlots,
        SetResultSlotsInstall, SetResultSlotsDrop, TakeObjectValue, SetSlotsForRestoreInstance,
        LoadScope, UnloadScope
    }
    void increaseRef(Reason reason);
    int releaseRef(Reason reason);

    public static void releaseRef(Instance<?> instance, Reason reason, Instance<?> eventSource) {
        if(logger.isDebugEnabled()) logger.debug("%s release ref of %s for %s".formatted(eventSource, instance, reason));
        if (instance == null) return;
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ReferenceCounter rc) {
            rc.releaseRef(reason);
        }
    }

    public static void increaseRef(Instance<?> instance, Reason reason, Instance<?> eventSource) {
        if (logger.isDebugEnabled()) logger.debug("%s increase ref of %s for %s".formatted(eventSource, instance, reason));

        if(instance == null) return;
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ReferenceCounter rc) {
            rc.increaseRef(reason);
        }
    }

    public static void releaseRef(Instance<?> instance, Reason reason) {
        if (instance == null) return;
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ReferenceCounter rc) {
            rc.releaseRef(reason);
        }
    }

    public static void increaseRef(Instance<?> instance, Reason reason) {
        if (instance == null) return;
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ReferenceCounter rc) {
            rc.increaseRef(reason);
        }
    }


    public static void releaseDeferenceSlotsAndContext(Instance<?> instance) {
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if(instance instanceof ObjectRefObject objectRefObject){
            var inst = objectRefObject.getDeferencedInstance();
            if(inst != null){
                releaseDeferenceSlotsAndContext(inst);
            }
            return;
        }
        if(instance instanceof ExpandableObject<?> expandableObject){
            expandableObject.fold();
            return;
        }

        if (instance instanceof DeferenceObject deferenceObject) {
            deferenceObject.releaseSlotsDeference(Reason.CleanSlotsForCallFrameQuit);
            releaseRef(instance.getParentScope(), Reason.DropParentForCallFrameQuit);
        }
    }

    // caller was assigned on `Invoke`, after run complete,
    // must release caller event the CallFrame still alive as an object
    public static void releaseCaller(Instance<?> instance){
        if (instance instanceof CallFrame<?> callFrame) {
            releaseRef(callFrame.getCaller(), Reason.DropCallerForCallFrameQuit);
        }
    }

    public static void releaseDeferenceAndContext(Instance<?> instance, Reason reason) {
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ExpandableObject<?> expandableObject) {
            expandableObject.fold();
            ReferenceCounter.releaseRef(instance,reason);
            return;
        }

        if (instance instanceof ObjectRefObject objectRefObject) {
            var inst = objectRefObject.getDeferencedInstance();
            if (inst != null) {
                releaseDeferenceAndContext(inst, reason);
            }
            releaseRef(instance,reason);
            return;
        }
    }


    public static void foldObjectRefFrame(CallFrame<?> callFrame) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof ExpandableCallFrame<?> expandableCallFrame) {
            expandableCallFrame.fold();
        } else if (callFrame instanceof ObjectRefObject objectRefObject) {     // for EntranceFrame like main#, there is no expander when bootstrap
            objectRefObject.tryFold();
        } else if (callFrame instanceof DeferenceObject deferenceObject) {
            throw new IllegalStateException("shouldn't be a DeferenceObject");
        }
    }


    int getRefCount();

    public static void increaseDeferenceSlotsForRestoreInstance(Instance<?> instance) {
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof DeferenceObject deferenceObject) {
            deferenceObject.increaseSlotsDeference(Reason.SetSlotsForRestoreInstance);
        }
    }

}
