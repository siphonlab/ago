package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.EntranceCallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait;

public interface ReferenceCounter {
    enum Reason{
        ReplaceWithDeferenceFrame,
        RunCallFrame,
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
        if(instance instanceof ObjectRefInstanceTrait objectRefInstanceTrait){
            var inst = objectRefInstanceTrait.getDeferencedInstance();
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


    public static void foldObjectRefFrame(CallFrame<?> callFrame) {
        if (callFrame instanceof EntranceCallFrame<?> entranceCallFrame) {
            callFrame = entranceCallFrame.getInner();
        }
        if (callFrame instanceof ObjectRefInstanceTrait objectRefInstanceTrait) {
            objectRefInstanceTrait.tryFold();
        } else if(callFrame instanceof ExpandableCallFrame<?> expandableCallFrame){
            expandableCallFrame.fold();
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
