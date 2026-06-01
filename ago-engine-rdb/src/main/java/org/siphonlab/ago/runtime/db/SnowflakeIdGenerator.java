package org.siphonlab.ago.runtime.db;

import org.agrona.concurrent.IdGenerator;

public class SnowflakeIdGenerator implements LongIdGenerator {

    private final IdGenerator inner;

    public SnowflakeIdGenerator(long nodeId) {
        this.inner = new org.agrona.concurrent.SnowflakeIdGenerator(nodeId);
    }

    @Override
    public long nextLongId() {
        return inner.nextId();
    }

    @Override
    public Long nextId() {
        return nextLongId();
    }
}
