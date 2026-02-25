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

import org.siphonlab.ago.compiler.FunctionDef;

public abstract class LoopStmt extends Statement{

    protected final String label;
    protected Label exitLabel;            // for break, got value while termVisit
    protected Label continueLabel;        // for continue

    public LoopStmt(FunctionDef ownerFunction, String label){
        super(ownerFunction);
        this.label = label;
    }

    public Label getContinueLabel() {
        return continueLabel;
    }

    public Label getExitLabel() {
        return exitLabel;
    }

    public String getLabel() {
        return label;
    }
}
