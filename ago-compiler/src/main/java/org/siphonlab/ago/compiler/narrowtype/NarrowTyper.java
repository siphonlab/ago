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
package org.siphonlab.ago.compiler.narrowtype;

import org.eclipse.collections.impl.stack.mutable.primitive.IntArrayStack;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

/*
ScopePair ->  variable mapper.
    pos   -> true set
    neg   -> false set

collectNarrowVar
    append determined variable

and expr:
    var left = expression();
    installPos();       // process pos
    leftMapper = peek();
    var right = expression();   left pos scope is the parent of right scope.
    installPos();       // deeper positive
    rightMapper = peak

    if(leftMapper got new mapper){      // when collectNarrowVar
        (right scope).neg = (left.neg UNION right.neg)
    }

or expr:
    var left = expression();
    uninstall()
    var right = expression();
    uninstall()

    if(left scope != old || right scope != old){
        pos = left.pos UNION right.pos
        neg = left.neg INTERSECT right.neg

        enter new Scope(pos, neg);
    }

 */
public class NarrowTyper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NarrowTyper.class);

    private final BlockCompiler blockCompiler;

    private int narrowTypingDepth = 0;
    private final Stack<NarrowNodePair> scopeStack = new Stack<>();
    private final IntArrayStack installationStack = new IntArrayStack();

    final static int INSTALLED_POS = 1;
    final static int INSTALLED_NEG = -1;
    final static int NOT_INSTALLED = 0;
    final static int DISABLED = 2;

    private int installationState = NOT_INSTALLED;

    public record NarrowNodePair(NarrowNode pos, NarrowNode neg){

        public NarrowNodePair intersect(BlockCompiler blockCompiler, NarrowNodePair another) throws CompilationError {
            if(!this.isDirty()){
                return another;
            } else if(!another.isDirty()){
                return this;
            } else {
                var p = intersect(blockCompiler, this.pos, another.pos);
                var n = union(blockCompiler, this.neg, another.neg);
                return new NarrowNodePair(p, n);
            }
        }

        public NarrowNodePair union(BlockCompiler blockCompiler, NarrowNodePair another) throws CompilationError {
            if(!this.isDirty()){
                return another;
            } else if(!another.isDirty()){
                return this;
            } else {
                var p = union(blockCompiler, this.pos, another.pos);
                var n = intersect(blockCompiler, this.neg, another.neg);
                return new NarrowNodePair(p, n);
            }
        }

        public NarrowNodePair not(){
            return new NarrowNodePair(neg, pos);
        }

        private NarrowNode union(BlockCompiler blockCompiler, NarrowNode node1, NarrowNode node2) throws CompilationError {
            var n = new NarrowNode();
            for (var entry : node1.getTypeMapper().entrySet()) {
                var variable = entry.getKey();
                var c = entry.getValue();
                var c2 = node2.getTypeMapper().get(variable);
                ClassDef union = union(blockCompiler.getFunctionDef(), c, c2);
                if(union != variable.getType() && union != null) {
                    n.addMapper(blockCompiler.acquireNarrowTypingVar(variable, union));
                }   // otherwise don't map it
            }
// not necessary to handle node2.keys not in node1, for the must be null
//            for(var entry : node2.getTypeMapper().entrySet()){
//                if(!node1.getTypeMapper().containsKey(entry.getKey())){
//
//                }
//            }
            return n;
        }

        private NarrowNode intersect(BlockCompiler blockCompiler, NarrowNode node1, NarrowNode node2) throws CompilationError {
            NarrowNode p = new NarrowNode();
            for (var entry : node1.getTypeMapper().entrySet()) {
                var variable = entry.getKey();
                var c = entry.getValue();
                var c2 = node2.getTypeMapper().get(variable);
                ClassDef intersected = intersect(c, c2);
                if(intersected == c){
                    p.addMapper(node1.getVariableMapper().get(variable));
                } else if(intersected == c2){
                    p.addMapper(node2.getVariableMapper().get(variable));
                } else {
                    throw new IllegalArgumentException("cannot intersect '%s' with '%s'".formatted(c, c2));
                }
            }
            for(var entry : node2.getTypeMapper().entrySet()){
                if(!node1.getTypeMapper().containsKey(entry.getKey())){
                    var variable = entry.getKey();
                    p.addMapper(node2.getVariableMapper().get(variable));
                }
            }
            return p;
        }

        public boolean isDirty() {
            return !pos.getTypeMapper().isEmpty() || !neg.getTypeMapper().isEmpty();
        }

        private ClassDef intersect(ClassDef c1, ClassDef c2) {
            if(c1 == null || c1 instanceof NullableClassDef){
                return c2;
            } else if(c2 == null || c2 instanceof NullableClassDef){
                return c1;
            } else if(c1 instanceof NullClassDef){
                return c1;
            } else if(c2 instanceof NullClassDef){
                return c2;
            } else {
                assert c1 == c2;
                return c1;
            }
        }

        private ClassDef union(FunctionDef owner, ClassDef c1, ClassDef c2) throws CompilationError {
            if(c1 == null || c1 instanceof NullableClassDef){
                return c1;
            } else if(c2 == null || c2 instanceof NullableClassDef){
                return c2;
            } else if(c1 instanceof NullClassDef){
                if(c2 instanceof NullClassDef) return c1;
                return owner.getOrCreateNullableType(c2, null);
            } else if(c2 instanceof NullClassDef){
                return owner.getOrCreateNullableType(c1, null);
            } else {
                assert c1 == c2;
                return c1;
            }
        }
    }

    NarrowNodePair currNarrowNodePair;

    public NarrowTyper(BlockCompiler blockCompiler){
        this.blockCompiler = blockCompiler;
    }

    public boolean isCollecting(){
        return narrowTypingDepth > 0 && installationState != DISABLED;
    }

    public void collectNarrowVar(Var.NarrowTypingLocalVar nonNullVar, Var.NarrowTypingLocalVar nullVar) {
        this.currNarrowNodePair.pos.addMapper(nonNullVar);
        this.currNarrowNodePair.neg.addMapper(nullVar);
    }

    public void updateCurrent(NarrowNodePair result, boolean pos) {
        this.uninstallCurrentScope();
        this.currNarrowNodePair = result;
        if(pos){
            this.installPosScope();
        } else {
            this.installNegScope();
        }
    }
    /**
     * enter a new ScopePair and activate the pos node
     */
    public void enter() {
        narrowTypingDepth++;
        var scopePair = new NarrowNodePair(new NarrowNode(), new NarrowNode());
        if(this.currNarrowNodePair != null) {
            this.scopeStack.push(this.currNarrowNodePair);
            this.installationStack.push(this.installationState);
        }
        this.currNarrowNodePair = scopePair;
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("narrow typing step in, level %d, count %d, curr %s".formatted(narrowTypingDepth, scopeStack.size(), currNarrowNodePair));
        this.installPosScope();
    }

    public NarrowNodePair exit(){
        var old = currNarrowNodePair;
        narrowTypingDepth--;
        uninstallCurrentScope();
        if (!scopeStack.empty()) {
            this.currNarrowNodePair = scopeStack.pop();
            this.installationState = installationStack.pop();       // needn't install to functionDef.variableScope, leave scope already rollback to the scope of curr
        } else {
            this.currNarrowNodePair = null;
            this.installationState = NOT_INSTALLED;
        }
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("narrow typing step out, level %d, count %d, curr %s".formatted(narrowTypingDepth, scopeStack.size(), currNarrowNodePair));
        return old;
    }

    public NarrowNodePair peek() {
        return currNarrowNodePair;
    }

    public void reenter(NarrowNodePair narrowNodePair, boolean pos) {
        if(this.installationState == DISABLED) return;
        if(this.currNarrowNodePair != null) {
            if(this.installationState == INSTALLED_POS) {
                releaseVariables(this.currNarrowNodePair.pos);
            } else if(this.installationState == INSTALLED_NEG) {
                releaseVariables(this.currNarrowNodePair.neg);
            }
            this.scopeStack.push(this.currNarrowNodePair);
            this.installationStack.push(this.installationState);
        }
        this.currNarrowNodePair = narrowNodePair;
        this.narrowTypingDepth ++;

        if(LOGGER.isDebugEnabled())
            LOGGER.debug("narrow typing re-enter %s, level %d, count %d".formatted(currNarrowNodePair, narrowTypingDepth, scopeStack.size()));

        if(pos) {
            this.installPosScope();
        } else {
            this.installNegScope();
        }
    }

    public void uninstallCurrentScope() {
        var ownerFunction = blockCompiler.getFunctionDef();
        switch(installationState) {
            case NOT_INSTALLED, DISABLED:
                break;
            case INSTALLED_POS: {
                releaseVariables(currNarrowNodePair.pos);
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currNarrowNodePair.pos.getScope();
                scope.setParent(null);
                this.installationState = NOT_INSTALLED;
                break;
            }
            case INSTALLED_NEG: {
                releaseVariables(currNarrowNodePair.neg);
                var scope = ownerFunction.leaveVariableScope();
                assert scope == currNarrowNodePair.neg.getScope();
                scope.setParent(null);
                this.installationState = NOT_INSTALLED;
                break;
            }
        }

    }

    public void installPosScope() {
        if(this.installationState == DISABLED) return;
        var ownerFunction = blockCompiler.getFunctionDef();
        ownerFunction.enterVariableScope(currNarrowNodePair.pos.getScope());
        this.installationState = INSTALLED_POS;
        lockVariables(currNarrowNodePair.pos);
    }

    public void installNegScope(){
        if(this.installationState == DISABLED) return;

        var ownerFunction = blockCompiler.getFunctionDef();
        ownerFunction.enterVariableScope(currNarrowNodePair.neg.getScope());
        this.installationState = INSTALLED_NEG;
        lockVariables(currNarrowNodePair.neg);
    }

    private void lockVariables(NarrowNode narrowNode) {
        for (Var.NarrowTypingLocalVar localVar : narrowNode.getVariableMapper().values()) {
            blockCompiler.lockRegister(localVar);
        }

    }
    private void releaseVariables(NarrowNode narrowNode) {
        for (Var.NarrowTypingLocalVar localVar : narrowNode.getVariableMapper().values()) {
            blockCompiler.releaseRegister(localVar);
        }
    }


}
