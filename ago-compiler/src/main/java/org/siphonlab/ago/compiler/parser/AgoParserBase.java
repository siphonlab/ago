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

import org.antlr.v4.runtime.*;

public abstract class AgoParserBase extends Parser{

    protected boolean inExpressionStmt = false;
    public AgoParserBase(TokenStream input) {
        super(input);
    }

    /**
     * Short form for prev(String str)
     */
    protected boolean p(String str) {
        return prev(str);
    }

    /**
     * Whether the previous token value equals to @param str
     */
    protected boolean prev(String str) {
        return _input.LT(-1).getText().equals(str);
    }

    /**
     * Short form for next(String str)
     */
    protected boolean n(String str) {
        return next(str);
    }

    /**
     * Whether the next token value equals to @param str
     */
    protected boolean next(String str) {
        return _input.LT(1).getText().equals(str);
    }

    protected boolean notLineTerminator() {
        return !lineTerminatorAhead();
    }

//    protected boolean notOpenBraceAndNotFunction() {
//        int nextTokenType = _input.LT(1).getType();
//        return nextTokenType != AgoParser.OpenBrace && nextTokenType != JavaScriptParser.Function_;
//    }
//
    protected boolean closeBrace() {
        return _input.LT(1).getType() == AgoParser.RBRACE;
    }

    protected boolean notOpenBraceAndNotFunction() {
        int nextTokenType = _input.LT(1).getType();
        // return nextTokenType != AgoParser.LBRACE && (nextTokenType != AgoParser.FUN && _input.LT(2).getType() != AgoParser.DOT);
        if(nextTokenType != AgoParser.LBRACE){
            if(nextTokenType != AgoParser.FUN) {
//                System.out.println("notOpenBraceAndNotFunction");
                return true;
            } else {
                if(_input.LT(2).getType() == AgoParser.DOT){
                    if(_input.LT(3).getType() == AgoParser.THIS || _input.LT(3).getType() == AgoParser.SUPER){
//                        System.out.println("notOpenBraceAndNotFunction");
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Returns {@code true} iff on the current index of the parser's
     * token stream a token exists on the {@code HIDDEN} channel which
     * either is a line terminator, or is a multi line comment that
     * contains a line terminator.
     *
     * @return {@code true} iff on the current index of the parser's
     * token stream a token exists on the {@code HIDDEN} channel which
     * either is a line terminator, or is a multi line comment that
     * contains a line terminator.
     */
    protected boolean lineTerminatorAhead() {

        // Get the token ahead of the current index.
        int possibleIndexEosToken = this.getCurrentToken().getTokenIndex() - 1;
        if (possibleIndexEosToken < 0) return false;
        Token ahead = _input.get(possibleIndexEosToken);

//        System.err.println("--------------------------------- curr:%s ahead:%s".formatted(this.getCurrentToken(), ahead));

        if (ahead.getChannel() != Lexer.HIDDEN) {
            // We're only interested in tokens on the HIDDEN channel.
            return false;
        }

        if (ahead.getType() == AgoParser.LineTerminator) {
            // There is definitely a line terminator ahead.
//            System.err.println("--------------------------------- return true");
            return true;
        }

        if (ahead.getType() == AgoParser.WhiteSpaces) {
            // Get the token ahead of the current whitespaces.
            possibleIndexEosToken = this.getCurrentToken().getTokenIndex() - 2;
            if (possibleIndexEosToken < 0) return false;
            ahead = _input.get(possibleIndexEosToken);
        }

        // Get the token's text and type.
        String text = ahead.getText();
        int type = ahead.getType();

        // Check if the token is, or contains a line terminator.
        var r = (type == AgoParser.COMMENT && (text.contains("\r") || text.contains("\n"))) ||
                (type == AgoParser.LineTerminator);
//        if(r){
//            System.err.println("--------------------------------- return true");
//        }
        return r;
    }

    protected boolean noLineBreakBefore(){
        for(var i= this.getCurrentToken().getTokenIndex()-1; i>=0; i--){
            Token back = this._input.get(i);
            if(back.getChannel() == Lexer.HIDDEN){
                var type = back.getType();
                var text = back.getText();
                var r = (type == AgoParser.COMMENT && (text.contains("\r") || text.contains("\n"))) ||
                        (type == AgoParser.LineTerminator);
                if(r) return false;
            } else {
                break;
            }
        }
        return true;
    }

    protected boolean isChildOfExpressionStmt(){
        var r = (this.inExpressionStmt && this.getRuleContext().getParent() instanceof AgoParser.ExpressionStatementContext);
//        System.out.printf("determine child of expression got %s%n", r);
        return r;
    }

    protected void setInExpressionStmt(boolean value){
        this.inExpressionStmt = value;
    }

    protected boolean print(String text){
        System.out.println(text);
        return true;
    }
}
