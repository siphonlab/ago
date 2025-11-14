package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;

public interface ReferenceCounter {
    enum Reason{
        ReplaceWithDeferenceFrame,
        RunYouCallFrame,
        SetSlotDrop,
        SetSlotInstall,
        SetParentInstall,
        SetParentDrop,
        SetCallerInstall, CreateDeferenceFrame, SetCreatorInstall,
        SetCallerDrop,
        SaveInstanceComplete,
        SetParentDropForFrameFree,
        SetCreatorDropForFrameFree,
        SetCallerDropForFrameFree,
        ReleaseRefForDeferenceInstanceFree,
        CallFrameInterrupt, RestoreCallFrame, DropCurrentCallFrame, InstallCurrentCallFrame,
        CallFrameQuit,
        DropParentForCallFrameQuitOrSaveInstanceDone, DropCallerForCallFrameQuit, DropCreatorForCallFrameQuit, CallFrameQuitCleanSlots,
        SetParentForRestoreFrame, SetCallerForRestoreFrame, SetCreatorForRestoreFrame, SetSlotsForRestoreInstance
    }
    void increaseRef(Reason reason);
    int releaseRef(Reason reason);

    public static void releaseRefOfCallFrame(CallFrame<?> callFrame, ReferenceCounter.Reason reason) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof ReferenceCounter rc) {
            rc.releaseRef(reason);
        }
    }

    public static void increaseRefOfCallFrame(CallFrame<?> callFrame, ReferenceCounter.Reason reason) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof ReferenceCounter rc) {
            rc.increaseRef(reason);
        }
    }

    public static void releaseDeferenceSlotsAndContext(Instance<?> instance) {
        if (instance instanceof EntranceCallFrame<?> entranceCallFrame) {
            instance = entranceCallFrame.getInner();
        }
        if (instance instanceof DeferenceObject deferenceObject) {
            if (instance.getParentScope() instanceof ReferenceCounter rc) {
                rc.releaseRef(ReferenceCounter.Reason.DropParentForCallFrameQuitOrSaveInstanceDone);
            }
            if(instance instanceof CallFrame<?> callFrame) {
                releaseRefOfCallFrame(callFrame.getCaller(), ReferenceCounter.Reason.DropCallerForCallFrameQuit);
            }
            releaseRefOfCallFrame(instance.getCreator(), ReferenceCounter.Reason.DropCreatorForCallFrameQuit);
            deferenceObject.releaseSlotsDeference(Reason.CallFrameQuitCleanSlots);
        }
    }


    public static void releaseDeferenceSlotsAndContextBeforeSave(CallFrame<?> callFrame) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof DeferenceObject deferenceObject) {
            if(callFrame instanceof ReferenceCounter rc){
                if(rc.getRefCount() == 0) return;
            }
            releaseDeferenceSlotsAndContext(callFrame);
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
