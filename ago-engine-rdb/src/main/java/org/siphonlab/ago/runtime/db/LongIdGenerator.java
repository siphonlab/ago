package org.siphonlab.ago.runtime.db;

public interface LongIdGenerator extends IdGenerator<Long>{
    long nextLongId();
}
