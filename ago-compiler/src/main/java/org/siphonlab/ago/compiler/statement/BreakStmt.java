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
package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;

public class BreakStmt extends Statement{

    private final String loopLabel;     // the label of loop statement

    public BreakStmt(String loopLabel){
        this.loopLabel = loopLabel;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        var outbox = findClosetLoopOrSwitch();
        if(outbox == null) throw new ResolveError("'break' outside of switch or loop", this.sourceLocation);

        if(this.loopLabel != null) {
            outbox = findMatchedClosetLoopOrSwitch();
            if (outbox == null) throw new ResolveError("loop statement with label '%s' not found".formatted(loopLabel), this.sourceLocation);
        }

        blockCompiler.enter(this);

        if (outbox instanceof LoopStmt loopStmt) {
            blockCompiler.getCode().jump(loopStmt.getExitLabel());
        } else {
            SwitchCaseStmt switchCaseStmt = (SwitchCaseStmt) outbox;
            blockCompiler.getCode().jump(switchCaseStmt.getExitLabel());
        }
        blockCompiler.leave(this);
    }

    Statement findMatchedClosetLoopOrSwitch(){
        for(var p = this.getParent(); p != null ; p = p.getParent()){
            if(p instanceof LoopStmt loopStmt) {
                if (this.loopLabel != null){
                    if(loopLabel.equals(loopStmt.getLabel())) return loopStmt;
                } else {
                    return loopStmt;
                }
            } else if(p instanceof SwitchCaseStmt switchCaseStmt){
                if(this.loopLabel == null) return switchCaseStmt;
            }
        }
        return null;
    }

    Statement findClosetLoopOrSwitch(){
        for(var p = this.getParent(); p != null ; p = p.getParent()){
            if(p instanceof LoopStmt loopStmt) {
                return loopStmt;
            } else if(p instanceof SwitchCaseStmt switchCaseStmt){
                return switchCaseStmt;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        if(loopLabel == null) return "break";
        return "break " + loopLabel;
    }
}
