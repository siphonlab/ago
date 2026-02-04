package org.siphonlab.ago;

public interface ClassManager {

    AgoClass getClass(int classId);

    AgoClass getClass(String className);

    MetaClass getTheMeta();
}
