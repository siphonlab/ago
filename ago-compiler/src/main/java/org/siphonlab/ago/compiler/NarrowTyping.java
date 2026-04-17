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
    final static int NOT_INSTALLED = 0;
    final static int DISABLED = 2;

    private int state = NOT_INSTALLED;

    record ScopePair(VariableScope pos, VariableScope neg){}

    ScopePair currScopePair;

    NarrowTyping(BlockCompiler blockCompiler){
        this.blockCompiler = blockCompiler;
    }

    public boolean isCollecting(){
        return narrowTypingDepth > 0 && state != DISABLED;
    }

    public void disable(){
        if(this.state == DISABLED) return;
        this.uninstallCurrentScope();
        state = DISABLED;
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
        this.installationStack.push(this.state);
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
                case INSTALLED_POS: this.installPosScope();break;
                case INSTALLED_NEG: this.installNegScope();break;
                default:
                    this.state = DISABLED;
            }
        } else {
            this.currScopePair = null;
            this.state = NOT_INSTALLED;
        }
    }

    public void uninstallCurrentScope() {
        var ownerFunction = blockCompiler.getFunctionDef();
        switch(state) {
            case NOT_INSTALLED, DISABLED:
                break;
            case INSTALLED_POS: {
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currScopePair.pos;
                this.state = NOT_INSTALLED;
                break;
            }
            case INSTALLED_NEG: {
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currScopePair.neg;
                this.state = NOT_INSTALLED;
                break;
            }
        }

    }

    public void installPosScope() {
        if(this.state == DISABLED) return;
        var ownerFunction = blockCompiler.getFunctionDef();
        uninstallCurrentScope();
        ownerFunction.enterVariableScope(currScopePair.pos);
        this.state = INSTALLED_POS;
    }

    public boolean installNegScope(){
        if(this.state == DISABLED) return false;

        var ownerFunction = blockCompiler.getFunctionDef();
        uninstallCurrentScope();
        if(currScopePair.neg.getVariableCount() > 1){       // for neg condition, only 1 variable allowed
            return false;
        }
        ownerFunction.enterVariableScope(currScopePair.neg);
        this.state = INSTALLED_NEG;
        return true;
    }


}
