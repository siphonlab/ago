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

import org.antlr.v4.runtime.*;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;

public class AgoErrorStrategy extends DefaultErrorStrategy {

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        ParserRuleContext rule = recognizer.getContext();
        if (rule instanceof AgoParser.BlockStmtContext) {
            while (true) {
                int type = recognizer.getCurrentToken().getType();
                if (type == AgoLexer.LineTerminator || type == AgoLexer.EOF) {    // proceed to next line
                    break;
                }
                recognizer.consume();
            }
        } else if(rule instanceof AgoParser.BlockContext){
            // this rule will be broken, just ignore it, then it will goto the outer tie
        } else {
            //recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
            unhandledException(recognizer, e);
        }
    }

    protected void unhandledException(Parser recognizer, RecognitionException e) {
    }

    public String getMessage(NoViableAltException e) {
        TokenStream tokens = (TokenStream) e.getInputStream();
        String input;
        if (tokens != null) {
            if (e.getStartToken().getType() == Token.EOF) input = "<EOF>";
            else input = tokens.getText(e.getStartToken(), e.getOffendingToken());
        } else {
            input = "<unknown input>";
        }
        String msg = "no viable alternative at input " + escapeWSAndQuote(input);
        return msg;
    }

    public String  getMessage(InputMismatchException e) {
        String msg = "mismatched input " + getTokenErrorDisplay(e.getOffendingToken()) +
                " expecting " + e.getExpectedTokens().toString(e.getRecognizer().getVocabulary());
        return msg;
    }

    public String getMessage(FailedPredicateException e) {
        String ruleName = e.getRecognizer().getRuleNames()[e.getCtx().getRuleIndex()];
        String msg = "rule " + ruleName + " " + e.getMessage();
        return msg;
    }

    public String getMessage(RecognitionException e) {
        if(e instanceof NoViableAltException){
            return getMessage((NoViableAltException)e);
        } else if(e instanceof InputMismatchException){
            return getMessage((InputMismatchException) e);
        } else if (e instanceof FailedPredicateException){
            return getMessage((FailedPredicateException)e);
        }
        return "unknown recognition error type: "+ e.getClass().getName() + ": " + e.getMessage();
    }
}