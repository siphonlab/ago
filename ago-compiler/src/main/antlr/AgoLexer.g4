/*
SPDX-License-Identifier: BSD-3-Clause

Copyright (c) 2022-2026 Inshua(inshua@gmail.com)
----------------------------------------------------------------------
The original Java grammar, https://github.com/antlr/grammars-v4/tree/master/antlr/antlr4/examples/grammars-v4/java/java
and some features come from js, https://github.com/antlr/grammars-v4/blob/master/antlr/antlr4/examples/grammars-v4/javascript/javascript/
----------------------------------------------------------------------

 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 Copyright (c) 2021 Michał Lorek (upgrade to Java 11)
 Copyright (c) 2022 Michał Lorek (upgrade to Java 17)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// outputdir gen/org/siphonlab/ago/compiler/parser
// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar AgoLexer;

@header {
    package org.siphonlab.ago.compiler.parser;
}
options {
    superClass = AgoLexerBase;
}

// Keywords

ABSTRACT     : 'abstract';
ASSERT       : 'assert';
BOOLEAN      : 'boolean';
BREAK        : 'break';
BYTE         : 'byte';
CASE         : 'case';
CATCH        : 'catch';
CHAR         : 'char';
CLASS        : 'class';
METACLASS    : 'metaclass';
CONST        : 'const';
CONTINUE     : 'continue';
DEFAULT      : 'default';
DO           : 'do';
DOUBLE       : 'double';
ELSE         : 'else';
ENUM         : 'enum';
EXTENDS      : 'extends';
FROM         : 'from';

FINAL        : 'final';
FINALLY      : 'finally';
FLOAT        : 'float';
FOR          : 'for';
IF           : 'if';
GOTO         : 'goto';
IMPLEMENTS   : 'implements';
IMPORT       : 'import';
INSTANCEOF   : 'instanceof';
INT          : 'int';
INTERFACE    : 'interface';
LONG         : 'long';
NATIVE       : 'native';
NEW          : 'new';
PACKAGE      : 'package';
PRIVATE      : 'private';
PROTECTED    : 'protected';
PUBLIC       : 'public';
RETURN       : 'return';
SHORT        : 'short';
STATIC       : 'static';
STRING       : 'string';
CLASSREF     : 'classref';
//STRICTFP     : 'strictfp';
SUPER        : 'super';
SWITCH       : 'switch';
//SYNCHRONIZED : 'synchronized';
THIS         : 'this';
THROW        : 'throw';
THROWS       : 'throws';
//TRANSIENT    : 'transient';
TRY          : 'try';
VOID         : 'void';
//VOLATILE     : 'volatile';
WHILE        : 'while';
AS           : 'as';
LIKE         : 'like';
TO           : 'to';
FIELD        : 'field';
CHAN         : 'chan';      // channel
IN           : 'in';
GETTER       : 'get';
SETTER       : 'set';

WITH       : 'with';
VIA        : 'via';

// Local Variable Type Inference
VAR: 'var'; // reserved type name
FUN: 'fun';

// Switch Expressions
YIELD: 'yield'; // reserved type name from Java 14
OVERRIDE: 'override';

// Records
RECORD: 'record';
TRAIT: 'trait';

SPAWN:  'spawn';
FORK:   'fork';
AWAIT:  'await';

NOT      : 'not';
AND      : 'and';
OR       : 'or';
BITXOR   : 'bxor';
BITAND   : 'band';
BITOR    : 'bor';
BITNOT    : 'bnot';
AND_ASSIGN     : AND '=';
OR_ASSIGN      : OR '=';
BITAND_ASSIGN     : BITAND '=';
BITOR_ASSIGN      : BITOR '=';
BITXOR_ASSIGN     : BITXOR '=';
COPY_ASSIGN     : 'o=';

NULL_LITERAL: 'null';

// Sealed Classes
//SEALED     : 'sealed';
//PERMITS    : 'permits';
//NON_SEALED : 'non-sealed';

// Literals

DECIMAL_LITERAL : ('0' | [1-9] (Digits? | '_'+ Digits)) [lLbB]?;
HEX_LITERAL     : '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lLbB]?;
OCT_LITERAL     : '0' '_'* [0-7] ([0-7_]* [0-7])? [lLbB]?;
BINARY_LITERAL  : '0' [bB] [01] ([01_]* [01])? [lLbB]?;

FLOAT_LITERAL:
    (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
    | Digits (ExponentPart [fFdD]? | [fFdD])
;

HEX_FLOAT_LITERAL: '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL: 'true' | 'false';

CHAR_LITERAL: 'c' '\'' (~['\\\r\n] | '\\' EscapeSequence) '\'';      // in ago, char must starts with 'c', c'h'

//STRING_LITERAL: '"' (~["\\\r\n] | EscapeSequence)* '"';
STRING_LITERAL:
    ('"' DoubleStringCharacter* '"' | '\'' SingleStringCharacter* '\'')
;

// Identifiers

IDENTIFIER: IdentifierStart IdentifierPart*;

POST_IDENTIFIER : '#' IdentifierPart*;

TEXT_BLOCK: '"""' [ \t]* [\r\n] (. | EscapeSequence)*? '"""';

// Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
TEMPLATE_CLOSE_BRACE :     {this.IsInTemplateString()}? '}' -> popMode;
RBRACE : '}';

LBRACK : '[';
RBRACK : ']';
SEMI   : ';';
COMMA  : ',';
DOT    : '.';

// Operators

ASSIGN   : '=';
GT       : '>';
LT       : '<';
BANG     : '!';
//TILDE    : '~';
QUESTION : '?';
NULLABLE_DOT : '?.';
// COLON    : ':';
DOUBLE_COLON: '::';
IDENTITY_EQUAL    : '===';
EQUAL    : '==';
LE       : '<=';
GE       : '>=';
NOT_IDENTITY_EQUAL : '!==';
NOTEQUAL : '!=';
INC      : '++';
DEC      : '--';
ADD      : '+';
SUB      : '-';
MUL      : '*';
DIV      : '/';

CARET    : '^';
MOD      : '%';

ADD_ASSIGN     : '+=';
SUB_ASSIGN     : '-=';
MUL_ASSIGN     : '*=';
DIV_ASSIGN     : '/=';

MOD_ASSIGN     : '%=';
LSHIFT_ASSIGN  : '<<=';
RSHIFT_ASSIGN  : '>>=';
URSHIFT_ASSIGN : '>>>=';

// Java 8 tokens

ARROW      : '=>';
SWITCH_ARROW : '->';

SEND_MSG        : '!~';     // call send()
RECV_MSG        : '~!';     // call recv()
SET_VALUE       : ':=';     // call setValue()
GET_VALUE       : ':';      // call getValue()   foo.bar:
VERTICAL_BAR    : '|';
UNDERSCORE      : '_';


// Additional symbols not defined in the lexical specification

AT       : '@';
ELLIPSIS : '...';

// Whitespace and comments
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]*    -> channel(HIDDEN);

BackTick: '$"' {this.IncreaseTemplateDepth();} -> pushMode(TEMPLATE);

WhiteSpaces: [\t\u000B\u000C\u0020\u00A0]+ -> channel(HIDDEN);
LineTerminator: [\r\n\u2028\u2029]+ -> channel(HIDDEN);

mode TEMPLATE;

BackTickInside                : '"$'  {this.DecreaseTemplateDepth();} -> type(BackTick), popMode;
TemplateStringStartExpression : '${' -> pushMode(DEFAULT_MODE);
TemplateStringAtom
    :   ( ~["$] | '"' { _input.LA(1) != '$' }? ) +;

fragment DoubleStringCharacter: ~["\\\r\n] | '\\' EscapeSequence | LineContinuation;

fragment SingleStringCharacter: ~['\\\r\n] | '\\' EscapeSequence | LineContinuation;

fragment EscapeSequence:
    CharacterEscapeSequence
    | '0' // no digit ahead! TODO
    | HexEscapeSequence
    | UnicodeEscapeSequence
    | ExtendedUnicodeEscapeSequence
;

fragment CharacterEscapeSequence: SingleEscapeCharacter | NonEscapeCharacter;

fragment HexEscapeSequence: 'x' HexDigit HexDigit;

fragment UnicodeEscapeSequence:
    'u' HexDigit HexDigit HexDigit HexDigit
    | 'u' '{' HexDigit HexDigit+ '}'
;

fragment ExtendedUnicodeEscapeSequence: 'u' '{' HexDigit+ '}';

fragment SingleEscapeCharacter: ['"\\bfnrtv];

fragment NonEscapeCharacter: ~['"\\bfnrtv0-9xu\r\n];

fragment EscapeCharacter: SingleEscapeCharacter | [0-9] | [xu];

fragment LineContinuation: '\\' [\r\n\u2028\u2029]+;

fragment IdentifierPart: IdentifierStart | [\p{Mn}] | [\p{Nd}] | [\p{Pc}] | '\u200C' | '\u200D';

fragment IdentifierStart: [\p{L}] | [$_] | '\\' UnicodeEscapeSequence;

// Fragment rules

fragment ExponentPart: [eE] [+-]? Digits;

fragment HexDigits: HexDigit ((HexDigit | '_')* HexDigit)?;

fragment HexDigit: [0-9a-fA-F];

fragment Digits: [0-9] ([0-9_]* [0-9])?;

fragment LetterOrDigit: Letter | [0-9];

fragment Letter:
    [a-zA-Z$_]                        // these are the "java letters" below 0x7F
    | ~[\u0000-\u007F\uD800-\uDBFF]   // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
;