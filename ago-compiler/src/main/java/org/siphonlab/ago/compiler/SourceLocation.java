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


import org.antlr.v4.runtime.ParserRuleContext;

public class SourceLocation extends org.siphonlab.ago.SourceLocation {

    public static final SourceLocation UNKNOWN = new SourceLocation("<UNKNOWN>", 0, 0, 0, 0, 0);

    public SourceLocation(String filename, ParserRuleContext ast){
        super(filename,  ast.start.getLine(), ast.start.getCharPositionInLine(),
                ast.stop.getStopIndex() - ast.start.getStartIndex(),
                ast.start.getStartIndex(), ast.stop.getStopIndex());
    }

    public SourceLocation(String filename, ParserRuleContext from, ParserRuleContext toInclude){
        super(filename, from.start.getLine(), from.start.getCharPositionInLine(),
                toInclude.stop.getStopIndex() - from.start.getStartIndex(),
                from.start.getStartIndex(), toInclude.stop.getStopIndex());
    }

    public SourceLocation(String filename, int line, int column, int length, int start, int end) {
        super(filename, line, column, length, start, end);
    }
}
