package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.RunSpaceHost;

import java.util.Set;

public class SavableRunSpace extends RunSpace {
    protected final RdbAdapter rdbAdapter;
    public final long id;

    public SavableRunSpace(AgoEngine agoEngine, RdbAdapter adapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, runSpaceHost);
        this.rdbAdapter = adapter;
        this.id = adapter.nextId();
    }

    public SavableRunSpace(AgoEngine agoEngine, RdbAdapter adapter, RunSpaceHost runSpaceHost, long id) {
        super(agoEngine, runSpaceHost);
        this.id = id;
        this.rdbAdapter = adapter;
    }

    public Set<RunSpace> getPausingParents() {
        return pausingParents;
    }

    public Set<RunSpace> getForkedSpaces() {
        return forkedSpaces;
    }
}
