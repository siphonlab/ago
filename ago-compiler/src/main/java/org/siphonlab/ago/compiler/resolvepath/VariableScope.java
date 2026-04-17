package org.siphonlab.ago.compiler.resolvepath;

import org.siphonlab.ago.compiler.Variable;
import org.siphonlab.ago.compiler.expression.Var;

import java.util.HashMap;
import java.util.Map;

public class VariableScope {
    VariableScope parent;

    Map<String, Var.LocalVar> variables = new HashMap<>();

    public Var.LocalVar get(String name) {
        if(this.parent == null)
            return variables.get(name);
        else
            return variables.getOrDefault(name, this.parent.get(name));
    }

    public void put(Var.LocalVar variable) {
        this.variables.put(variable.variable.getName(), variable);
    }

    public void setParent(VariableScope parent) {
        this.parent = parent;
    }

    public VariableScope getParent() {
        return parent;
    }

    public int getVariableCount() {
        return variables.size();
    }
}
