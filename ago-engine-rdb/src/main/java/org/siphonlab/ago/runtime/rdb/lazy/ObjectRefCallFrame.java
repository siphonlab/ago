package org.siphonlab.ago.runtime.rdb.lazy;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceAgoFrame;
import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceFrameState;
import org.siphonlab.ago.runtime.rdb.json.lazy.DeferenceNativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason.DropCreatorForCallFrameQuit;

public class ObjectRefCallFrame<F extends AgoFunction> extends CallFrame<F> implements ObjectRefObject, ObjectRefOwner, ReferenceCounter {

    private static final Logger logger = LoggerFactory.getLogger(ObjectRefInstance.class);

    final ObjectRef objectRef;
    final DereferenceAdapter dereferenceAdapter;
    final Set<CallFrame> expanders = new HashSet<CallFrame>();

    Instance<?> deferencedInstance;

    private AtomicInteger referenceCounter = new AtomicInteger(0);
    private Instance deferencedCallFrame;

    public ObjectRefCallFrame(F agoClass, final ObjectRef objectRef, DereferenceAdapter dereferenceAdapter, final RowState rowState) {
        super(DefaultGroovyMethods.with(agoClass.createSlots(), new Closure<Slots>(null, null) {
            public Slots doCall(Slots it) {
                if (it instanceof RdbSlots) {
                    ((RdbSlots) it).setRowState(rowState);
                    ((RdbSlots) it).setId(objectRef.id());
                }
                return it;
            }

            public Slots doCall() {
                return doCall(null);
            }

        }), agoClass);
        this.objectRef = objectRef;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public Instance<?> deference() {
        if(deferencedCallFrame != null) return deferencedCallFrame;

        deference(deferencedInstance, this.dereferenceAdapter, this.objectRef);
        return deferencedCallFrame;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public Instance getDeferencedInstance() {
        return deferencedInstance;
    }

    public Instance getDeferencedCallFrame() {
        return deferencedCallFrame;
    }

    public void foldBy(CallFrame<?> expander) {
        foldBy(expanders, expander);
    }

    public void tryFold() {
        tryFold(expanders, this);
    }

    public Instance dereferenceForExpander(CallFrame expander) {
        return dereferenceForExpander(expanders, expander);
    }

    public void setDeferencedInstance(Instance inst) {
        if(inst == null){
            this.deferencedCallFrame = null;
            this.deferencedInstance = null;
            return;
        }

        this.deferencedInstance = inst;
        if (inst instanceof DeferenceCallFrame r) {
            DeferenceFrameState state = r.getDeferenceFrameState();
            if (state.isEntrance()) {
                inst = new EntranceCallFrame<>(this);
            } else if (state.isAsyncEntrance()) {
                inst = new AsyncEntranceCallFrame<>(this);
            }
        }
        this.deferencedCallFrame = inst;
    }

    @Override
    public Slots getSlots() {
        return deferencedInstance.getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deferencedInstance.getParentScope();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        deferencedInstance.setParentScope(parentScope);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        recomposeAsCallFrame().setCaller(caller);
    }

    public CallFrame recomposeAsCallFrame() {
        if(deferencedInstance == null){
            this.deference();
        }
        return (CallFrame) deferencedInstance;
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return recomposeAsCallFrame().resolveSourceLocation();
    }

    @Override
    public boolean handleException(Instance<?> exception) {
        return recomposeAsCallFrame().handleException(exception);
    }

    @Override
    public void setRunSpace(AgoRunSpace runSpace) {
        recomposeAsCallFrame().setRunSpace(runSpace);
    }

    @Override
    public AgoRunSpace getRunSpace() {
        AgoRunSpace r = super.getRunSpace();
        if (r != null) return ((AgoRunSpace) (r));
        return recomposeAsCallFrame().getRunSpace();
    }

    @Override
    public String toString() {
        return "(ObjectRefCallFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void run() {
        recomposeAsCallFrame().run();
    }

    @Override
    public void run(CallFrame<?> self) {
        CallFrame r = recomposeAsCallFrame();
        if (self.equals(this)) {
            r.run(r);
        } else {
            r.run(self);
        }
    }

    @Override
    public CallFrame<?> getCaller() {
        return recomposeAsCallFrame().getCaller();
    }

    @Override
    public int hashCode() {
        return getObjectRef().hashCode();
    }

    public ExpandableCallFrame createExpander(CallFrame expander, boolean alreadyDeferenced) {
        return new ExpandableCallFrame(this, expander, alreadyDeferenced);
    }

    @Override
    public int getRefCount() {
        return referenceCounter.get();
    }

    static int times = 0;
    @Override
    public void increaseRef(Reason reason) {
        if(getObjectRef().className().equals("main#")){
            times++;
        }
        int r = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s inc ref got %d for %s".formatted(this, r, reason));
    }

    @Override
    public int releaseRef(Reason reason) {
        int r = referenceCounter.decrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s release ref got %d for %s".formatted(this, r, reason));
        if (r == 0) {
            dereferenceAdapter.release(this.getObjectRef());
        }

        return r;
    }

    @Override
    public void fixCache() {
        this.dereferenceAdapter.repair(this.objectRef, this);
    }

}
