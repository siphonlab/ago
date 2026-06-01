package org.siphonlab.ago.runtime.db;

public interface IdGenerator<T> {
    T nextId();
}
