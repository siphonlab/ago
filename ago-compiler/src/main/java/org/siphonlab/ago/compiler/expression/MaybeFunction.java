package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.FunctionDef;

import java.util.Collection;

public interface MaybeFunction {
    boolean isFunction();

    FunctionDef getFunction();

    void setCandidates(Collection<FunctionDef> candidates);

    Collection<FunctionDef> getCandidates();

    Expression getScopeOfFunction();
}
