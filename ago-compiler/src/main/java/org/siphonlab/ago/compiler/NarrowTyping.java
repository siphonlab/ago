/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.compiler;

import org.eclipse.collections.impl.stack.mutable.primitive.IntArrayStack;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.resolvepath.VariableScope;

import java.util.Stack;

class NarrowTyping {

    private final BlockCompiler blockCompiler;

    private int narrowTypingDepth = 0;
    private Stack<ScopePair> scopeStack = new Stack<>();
    private IntArrayStack installationStack = new IntArrayStack();

    final static int INSTALLED_POS = 1;
    final static int INSTALLED_NEG = -1;
    final static int INSTALLED_NONE = 0;

    private int installation = INSTALLED_NONE;

    record ScopePair(VariableScope pos, VariableScope neg){}

    ScopePair currScopePair;

    NarrowTyping(BlockCompiler blockCompiler){
        this.blockCompiler = blockCompiler;
    }

    public boolean isCollecting(){
        return narrowTypingDepth > 0;
    }

    public void collectPosVar(Var.LocalVar narrowVar) {
        this.currScopePair.pos.put(narrowVar);
    }

    public void collectNegVar(Var.LocalVar narrowVar) {
        this.currScopePair.neg.put(narrowVar);
    }

    public void beginCollect() {
        narrowTypingDepth++;
        var scopePair = new ScopePair(new VariableScope(), new VariableScope());
        this.scopeStack.push(scopePair);
        this.installationStack.push(this.installation);
        this.currScopePair = scopePair;
        this.installPosScope();
    }

    public void endCollect() {
        narrowTypingDepth--;
    }

    public void exit(){
        uninstallCurrentScope();
        if(!scopeStack.empty()) {
            this.currScopePair = scopeStack.pop();
            var installation = installationStack.pop();
            switch (installation) {
                case INSTALLED_NONE: this.installation = INSTALLED_NONE; break;
                case INSTALLED_POS: this.installPosScope();break;
                case INSTALLED_NEG: this.installNegScope();break;
            }
        } else {
            this.currScopePair = null;
            this.installation = INSTALLED_NONE;
        }
    }

    public void uninstallCurrentScope() {
        var ownerFunction = blockCompiler.getFunctionDef();
        switch(installation) {
            case INSTALLED_NONE:
                break;
            case INSTALLED_POS: {
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currScopePair.pos;
                this.installation = INSTALLED_NONE;
                break;
            }
            case INSTALLED_NEG: {
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currScopePair.neg;
                this.installation = INSTALLED_NONE;
                break;
            }
        }

    }

    public void installPosScope() {
        var ownerFunction = blockCompiler.getFunctionDef();
        uninstallCurrentScope();
        ownerFunction.enterVariableScope(currScopePair.pos);
        this.installation = INSTALLED_POS;
    }

    public void installNegScope(){
        var ownerFunction = blockCompiler.getFunctionDef();
        uninstallCurrentScope();
        ownerFunction.enterVariableScope(currScopePair.neg);
        this.installation = INSTALLED_NEG;
    }


}
