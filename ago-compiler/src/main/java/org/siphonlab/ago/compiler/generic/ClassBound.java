package org.siphonlab.ago.compiler.generic;

import org.siphonlab.ago.compiler.ClassDef;

public interface ClassBound {
    ClassDef getLBoundClass();

    ClassDef getUBoundClass();
}
