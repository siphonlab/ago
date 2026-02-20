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
package org.siphonlab.ago.compiler.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

public abstract class AgoLexerBase extends Lexer {

    /**
     * Keeps track of the the current depth of nested template string backticks.
     * E.g. after the X in:
     *
     * `${a ? `${X
     *
     * templateDepth will be 2. This variable is needed to determine if a `}` is a
     * plain CloseBrace, or one that closes an expression inside a template string.
     */
    private int templateDepth = 0;
    private Token lastToken;

    public AgoLexerBase(CharStream input) {
        super(input);
    }

    public boolean IsStartOfFile() {
        return lastToken == null;
    }

    public boolean IsInTemplateString() {
        return this.templateDepth > 0;
    }

    /**
     * Return the next token from the character stream and records this last
     * token in case it resides on the default channel. This recorded token
     * is used to determine when the lexer could possibly match a regex
     * literal. Also changes scopeStrictModes stack if tokenize special
     * string 'use strict';
     *
     * @return the next token from the character stream.
     */
    @Override
    public Token nextToken() {
        Token next = super.nextToken();
        if (next.getChannel() == Token.DEFAULT_CHANNEL) {
            // Keep track of the last token on the default channel.
            this.lastToken = next;
        }

        return next;
    }

    public void IncreaseTemplateDepth() {
        this.templateDepth++;
    }

    public void DecreaseTemplateDepth() {
        this.templateDepth--;
    }

    @Override
    public void mode(int m) {
        super.mode(m);
    }

    @Override
    public void reset() {
        this.lastToken = null;
        this.templateDepth = 0;
        super.reset();
    }
}
