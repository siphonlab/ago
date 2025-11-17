package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;

public interface ReferenceCounter {
    enum Reason{
        RunCallFrame,
        SetSlotDrop,
        SetSlotInstall,
        SetParentInstall,
        SetCallerInstall, SetCreatorInstall,
        SetCallerDrop,
        CallFrameInterrupt, RestoreCallFrame, DropCurrentCallFrame, InstallCurrentCallFrame,
        CallFrameQuit,
        DropParentForCallFrameQuit, DropCallerForCallFrameQuit,
        DropCreatorForCallFrameQuit, CallFrameQuitCleanSlots,
        SetResultSlotsInstall, SetResultSlotsDrop, TakeObjectValue, SetSlotsForRestoreInstance
    }
    void increaseRef(Reason reason);
    int releaseRef(Reason reason);

    public static void releaseRef(Instance<?> instance, ReferenceCounter.Reason reason) {
        if (instance == null) return;
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof ReferenceCounter rc) {
            rc.releaseRef(reason);
        }
    }

    public static void increaseRef(Instance<?> instance, ReferenceCounter.Reason reason) {
        if(instance == null) return;
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
            releaseRef(instance.getParentScope(), Reason.DropParentForCallFrameQuit);
            if(instance instanceof CallFrame<?> callFrame) {
                releaseRef(callFrame.getCaller(), Reason.DropCallerForCallFrameQuit);
            }
            releaseRef(instance.getCreator(), ReferenceCounter.Reason.DropCreatorForCallFrameQuit);
            deferenceObject.releaseSlotsDeference(Reason.CallFrameQuitCleanSlots);
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

        if (instance instanceof DeferenceObject deferenceObject) {
            releaseRef(instance.getParentScope(), Reason.DropParentForCallFrameQuit);
            if (instance instanceof CallFrame<?> callFrame) {
                releaseRef(callFrame.getCaller(), Reason.DropCallerForCallFrameQuit);
            }
            releaseRef(instance.getCreator(), ReferenceCounter.Reason.DropCreatorForCallFrameQuit);
        }
    }


    public static void foldObjectRefFrame(CallFrame<?> callFrame) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof ObjectRefObject objectRefObject) {     // for EntranceFrame like main#, there is no expander when bootstrap
            objectRefObject.tryFold();
        } else if(callFrame instanceof ExpandableCallFrame<?> expandableCallFrame){
            expandableCallFrame.fold();
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
