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
// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar AgoParser;
@header {
    // for IDEA plugin, output to gen/org/siphonlab/ago/compiler/parserm, don't set package name, due the `antlr4-maven-plugin` need package name
    package org.siphonlab.ago.compiler.parser;
}
options {
    tokenVocab = AgoLexer;
    superClass = AgoParserBase;
}

@parser::members {
    private int withDepth = 0;
}


compilationUnit
    : packageDeclaration? (importDeclaration | ';')* (typeDeclaration | ';')*
    EOF
    ;

packageDeclaration
    : PACKAGE qualifiedName eos
    ;

importDeclaration
    : IMPORT qualifiedNameAllowPostfix ('.' '*')? eos
    ;

typeDeclaration
    :  classDeclaration         #ClassDecl
     | enumDeclaration          #EnumDecl
     | interfaceDeclaration     #InterfaceDecl
     | traitDeclaration         #TraitDecl
     | methodDeclaration        #TopFunctionDecl
;

commonVisiblility:
    PUBLIC | PROTECTED | PRIVATE;

classModifier:
    commonVisiblility | ABSTRACT | FINAL | NATIVE;      // FINAL for class only -- does not apply to interfaces
    // | STRICTFP | SEALED  // Java17 | NON_SEALED // Java17

fieldModifier:
    commonVisiblility | FINAL;     // TRANSIENT | VOLATILE

methodModifier:
    commonVisiblility | ABSTRACT | FINAL  | OVERRIDE //|  STATIC | SYNCHRONIZED
    ;

interfaceModifier:
    commonVisiblility | FINAL;

native: NATIVE STRING_LITERAL;

variableModifier
    : FINAL    | FIELD   | CHAN    | THIS;

variableModifiers:
        (variableModifier+ VAR?)
    |   VAR
;

methodStarter:
      fieldModifier* OVERRIDE (FUN | GETTER | SETTER)?
    | methodModifier*  (FUN | GETTER | SETTER)
;

classDeclaration
    : classModifier* CLASS className=identifier genericTypeParameters? extendsPhrase? implementsPhrase?
        classBody
    ;

traitDeclaration
    : classModifier* TRAIT className=identifier genericTypeParameters? extendsPhrase? implementsPhrase? permitsType?
        classBody
    ;

// <+T as [Foo to Bar], +E as [Animal to Cat]>
genericTypeParameters    : '<' genericTypeParameter (',' genericTypeParameter)* '>';

genericTypeParameter    : ('+'|'-')? identifier typeOfGenericParam?;

typeOfGenericParam:
      AS typeRange             // as [Animal to _], as [Function]
;

enumDeclaration
    : classModifier* ENUM identifier (FROM primitiveType)? '{' enumConstants '}'
    ;

enumConstants
    : enumConstant (',' enumConstant)* ','?
    ;

enumConstant
    : identifier ('=' integerLiteral)?
    ;

interfaceDeclaration
    : interfaceModifier* INTERFACE interfaceName=identifier genericTypeParameters? extendsInterfaces? permitsType?
            classBody
    ;

permitsType: FOR declarationType;

classBody
    : '{' classBodyDeclaration* '}'     #DefaultClassBody
    | eos                               #EmptyClassBody
    ;

classBodyDeclaration
    : ';'
    | memberDeclaration
    ;


memberDeclaration :
      constructorDeclaration            # ConstructorDecl       // Class    Trait
    | methodDeclaration                 # MethodDecl            // Class    Trait   Interface(No MethodBody)
    | fieldDeclaration                  # FieldDecl             // Class    Trait
    // | constDeclaration                  # ConstDecl      // use final instead
    | interfaceDeclaration              # InnerInterfaceDecl    // Class    Trait   Interface
    | classDeclaration                  # InnerClassDecl        // Class    Trait   Interface
    | enumDeclaration                   # InnerEnumDecl         // Class    Trait   Interface
    | traitDeclaration                  # InnerTraitDecl
    | metaclassDeclaration              # MetaclassDecl         // Class    Trait   Interface
;

metaclassDeclaration:   METACLASS classBody;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.

   TODO ATTENTION! execlude AsMetaType from typeOfVariable
 */
methodDeclaration
    : methodStarter methodName genericTypeParameters? formalParameters typeOfFunction? implementsPhrase? throwsPhrase?
        methodBody?
    ;

throwsPhrase:       THROWS declarationTypeList;
extendsPhrase:      FROM baseType=declarationType;
implementsPhrase:   WITH interfaceList;
extendsInterfaces:  FROM interfaceList;

    declarationTypeList
        : declarationType (',' declarationType)*
        ;

    interfaceList
        : interfaceItem (',' interfaceItem)*
        ;

    interfaceItem:
        declarationType                             #SimpleInterface
        | '(' identifier AS  declarationType ')'    #WrapperInterface
    ;


methodBody:
    ';'                     # MBEmpty
    | block                 # MBBLock
    | native eos            # MBNative
;

constructorDeclaration
    : methodStarter? NEW POST_IDENTIFIER? formalParameters throwsPhrase? constructorBody = block
    ;

fieldDeclaration: fieldModifier* fieldVariableDeclarators eos;

constDeclaration: commonVisiblility? CONST identifier typeOfVariable '=' expression;

fieldVariableDeclarators
    : fieldVariableDeclarator (',' fieldVariableDeclarator)*
    ;

fieldVariableDeclarator
    :  CHAN? identifier typeOfVariable fieldGetterSetter? variableInitializer?      #VarDeclExplicitType
    |  CHAN? identifier fieldGetterSetter?  variableInitializer                 #VarDeclImplicitType
    ;

arrayLiteral
    : '[' (variableType '|')? elementList ']'
    ;

// JavaScript supports arrasys like [,,1,2,,].
elementList
    : ','* arrayElement? (','+ arrayElement) * ','* // Yes, everything is optional
    ;

arrayElement
    : expando='...'? expression
    ;

objectLiteral
    : declarationType? '{' (propertyAssignment (',' propertyAssignment)* ','?)? '}'
    ;

propertyAssignment
    : propertyName ':' expression                                # PropertyExpressionAssignment
//    | Async? '*'? propertyName '(' formalParameterList? ')' functionBody # FunctionProperty
//    | getter '(' ')' functionBody                                        # PropertyGetter
//    | setter '(' formalParameterArg ')' functionBody                     # PropertySetter
    | '...'? expression                                         # PropertyShorthand
    ;

propertyName
    : identifier
    | STRING_LITERAL
    | integerLiteral
    | floatLiteral
    | '[' expression ']'
    ;


typeOfFunction:      AS variableType;

typeOfVariable:
      AS variableType implementsPhrase?             # AsType
    | AS typeRange                                  # AsTypeRange         // as [Animal to _], as [Function]
    | LIKE namePath                                 # LikeType            // like Animal, like function
    | AS classDeclaration                           # AsClassDecl
;

fieldGetterSetter:
    '{' getter setter? '}'
    ;

getter:
    commonVisiblility? GETTER eos;

setter:
    commonVisiblility? SETTER eos;

variableInitializer:
      '=' expression
    | AS creator
;

typeArgument
    : declarationType
    //| '?' ((FROM | SUPER) typeType)?      // TODO not sure
    ;

formalParameters: '(' (formalParameter (','formalParameter )*)?  ')';

formalParameter:
        THIS typeOfVariable           #ReceiverParameter
    |   variableModifier* identifier typeOfVariable fieldGetterSetter?  ('=' literal)?      #DefaultParameter
    |   variableModifier* identifier typeOfVariable '...'   #VarArgsParameter
;

// local variable type inference
lambdaLVTIList
    : lambdaLVTIParameter (',' lambdaLVTIParameter)*
    ;

lambdaLVTIParameter
    : variableModifiers identifier
    ;

qualifiedName
    : identifier ('.' identifier)*
    ;

literal
    : integerLiteral        #lInteger
    | floatLiteral          #lFloat
    | CHAR_LITERAL          #lChar
    | STRING_LITERAL        #lString
    | templateStringLiteral #lTemplateString
    | BOOL_LITERAL          #lBool
    | NULL_LITERAL          #lNull
    | TEXT_BLOCK            #lTextBlock  // Java17
    | objectLiteral         #lObject
    | arrayLiteral          #lArray
    ;

templateStringLiteral
    : BackTick templateStringAtom* BackTick
    ;

templateStringAtom:
    TemplateStringAtom
    | TemplateStringStartExpression expression TEMPLATE_CLOSE_BRACE
    ;

integerLiteral
    : DECIMAL_LITERAL       # iDec
    | HEX_LITERAL           # iHex
    | OCT_LITERAL           # iOct
    | BINARY_LITERAL        # iBin
    ;

floatLiteral
    : FLOAT_LITERAL         # fDec
    | HEX_FLOAT_LITERAL     # fHex
    ;

// STATEMENTS / BLOCKS
block
    : '{' blockStatement* '}'
    ;

blockStatement
    : localVariableDeclaration eos      # LocalVarDecl
    | localTypeDeclaration              # LocalTypeDecl
    | statement                         # DefaultBlockStmt
    ;

localVariableDeclaration
    : variableModifiers identifier typeOfVariable? (variableInitializer)?
    ;


identifier
    : IDENTIFIER
     //| MODULE    | OPEN    | REQUIRES    | EXPORTS    | OPENS    | TO    | USES      | PROVIDES       | TRANSITIVE
     | WITH| YIELD    | FROM      | FIELD     | RECORD  | GETTER | SETTER   | TO    | LIKE
//    | SEALED   | PERMITS
    ;

typeIdentifier // Identifiers that are not restricted for type declarations
    : identifierAllowPostfix
    // | WITH    | RECORD
//    | MODULE    | OPEN    | REQUIRES    | EXPORTS    | OPENS    | USES    | PROVIDES  | TRANSITIVE
    //    | SEALED    | PERMITS
;

identifierAllowPostfix: identifier POST_IDENTIFIER?;

qualifiedNameAllowPostfix : identifierAllowPostfix ('.' identifierAllowPostfix)*;


methodName: identifierAllowPostfix;      // TODO allow "class" as function name, for getter


localTypeDeclaration
    : classDeclaration          #ClassDeclInBlock
    | interfaceDeclaration      #InterfaceInBlock
    | methodDeclaration         #MethodInBlock
    | enumDeclaration           #EnumInBlock
    | metaclassDeclaration      #MetaclassInBlock
    ;

statement
    : blockLabel = block        # BlockStmt
//    | ASSERT expression (':' expression)? eos
    | IF parExpression trueBranch=statement (ELSE falseBranch=statement)?      # IfStmt
    | label? FOR '(' forControl ')' statement      # ForStmt
    | label? WHILE parExpression statement         # WhileStmt
    | label? DO statement WHILE parExpression      # DoWhileStmt
    | TRY block (catchClause+ finallyBlock? | finallyBlock)                     # TryStmt
    | SWITCH parExpression '{' switchBlockStatementGroup* switchLabel* '}'      # SwitchStmt
    | RETURN {noLineBreakBefore()}? expression? eos                # ReturnStmt
    | THROW {noLineBreakBefore()}? expression eos                  # ThrowStmt
    | BREAK ({noLineBreakBefore()}? identifier)? eos                 # BreakStmt
    | CONTINUE ({noLineBreakBefore()}? identifier)? eos              # ContinueStmt
    | YIELD {noLineBreakBefore()}? expression eos                  # YieldStmt     // Java17
    | WITH parExpression {withDepth++;} statement {withDepth--;} eos       # WithStmt
    | VIA parExpression statement  eos                                     # ViaStmt
    | SEMI                                  # EmptyStmt
    | switchExpression eos                  # SwitchExprStmt    // Java17
    | expressionStatement                   # ExpressionStmt
    | AWAIT                                 # AwaitStmt
    | (SPAWN | FORK) expression viaForkContext?    # AsyncInvokeFunctorStmt
    ;

label: identifier ':';

expressionStatement
    : {this.notOpenBraceAndNotFunction()}? {this.setInExpressionStmt(true);} expression eos {this.setInExpressionStmt(false);}
    ;

catchClause
    : CATCH '(' variableModifiers? identifier AS catchType ')' block
    ;

catchType
    : declarationType (OR declarationType)*
    ;

finallyBlock
    : FINALLY block
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : switchLabel+ blockStatement+
    ;

switchLabel
    : CASE (
          enumConstantName = namePath
        | constantExpression = expression
        | varName = identifier AS variableType
    ) ':'
    | DEFAULT ':'
    ;

forControl
    : enhancedForControl
    | forInit? ';' expression? ';' forUpdate = expressionList?
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

enhancedForControl
    : variableModifiers? identifier (AS declarationType)? IN expression
    ;

// EXPRESSIONS

parExpression
    : '(' (expression | localVariableDeclaration) ')'
    ;

expressionList: expression (',' expression)*;

methodCall
    : namePath arguments      # NormalInvoke
    | invokeMode namePath arguments viaForkContext?      # AsyncInvoke
;

invokeMode: SPAWN | FORK | AWAIT;

viaForkContext: VIA forkContext=expression;

postWith : WITH {withDepth++;} statement {withDepth--;};


expression:
    // Expression order in accordance with https://introcs.cs.princeton.edu/java/11precedence/
    // Level 16, Primary, array and member access
     '(' expression ')'                 # QuotedExpr
    | expression '!'                    # ValueFromNullable
    | methodCall                        # MethodCallExpr
    | expression '[' expression ']'     # ElementExpr
    | expression '.' 'class'            # ClassExpr
    // Method calls and method references are part of primary, and hence level 16 precedence
    | expression bop = ('.' | '?.') (
           methodCall
         | namePath
    )                           # MemberAccessExpr
    | {withDepth > 0}? '.' (
                namePath
            |   methodCall
            //| THIS
    )               # WithMemberAccessExpr
//    | expression '::' typeArguments? identifier
//    | typeType '::' (typeArguments? identifier | NEW)
//    | classType '::' typeArguments? NEW
    | switchExpression  # SwitchExpr // Java17
    | AWAIT expression viaForkContext?             # AwaitFunctor

    // Level 15 Post-increment/decrement operators
    | expression postfix = ('++' | '--')        # IncDecExpr

    // Level 14, Unary operators
    | expression postfix = (GET_VALUE | '~!')       # PostfixExpr
    | prefix = ('+' | '-' | '++' | '--' | BITNOT | NOT) expression      # PrefixExpr

    // Level 13 Cast and object creation
//    | '(' typeType ('&' typeType)* ')' expression
// java support `Object aTest = (String & CharSequence) "test";` https://stackoverflow.com/questions/51070344/strange-java-cast-syntax-using
    | expression (AS | '|') variableType                    # CastTypeExpr      // `1|double` or `1 as double`
    | creator                                               # CreatorExpr
    | expression '.' chainCreator                           # ChainCreatorExpr

    // Level 12 to 1, Remaining operators
    | expression bop = ('*' | '/' | '%') expression          # MultiDivModExpr // Level 12, Multiplicative operators
    | expression bop = ('+' | '-') expression                # AddSubtractExpr // Level 11, Additive operators
    | expression ('<''<' | '>''>''>' | '>''>') expression    # ShiftExpr     // Level 10, Shift operators
    | expression bop = ('<=' | '>=' | '>' | '<') expression  #  CompareExpr  // Level 9, Relational operators
    | expression bop = INSTANCEOF variableType identifier?   # InstanceOfExpr
    | expression bop = ('==' | '!=' | '===' | '!==') expression             # EqualsExpr      // Level 8, Equality Operators
    | expression bop = BITAND expression                    # BitAndExpr            // Level 7, Bitwise AND
    | expression bop = BITXOR expression                    # BitXorExpr         // Level 6, Bitwise XOR
    | expression bop = BITOR expression                     # BitOrExpr         // Level 5, Bitwise OR
    | expression bop = AND expression                       # AndExpr        // Level 4, Logic AND
    | expression bop = OR expression                        # OrExpr       // Level 3, Logic OR
    | expression postWith                                   # PostWithExpr
//    | <assoc = right> expression bop = '?' expression '|' expression // Level 2, Ternary
    | ifPart=expression {noLineBreakBefore()}? IF condition=expression ELSE elsePart=expression           # IfElseExpr
    // Level 1, Assignment
    | <assoc = right> expression bop = (
        '='
        | COPY_ASSIGN
        | ':='
        | '+='
        | '-='
        | '*='
        | '/='
        | AND_ASSIGN
        | OR_ASSIGN
        | BITAND_ASSIGN
        | BITOR_ASSIGN
        | BITXOR_ASSIGN
        | '>>='
        | '>>>='
        | '<<='
        | '%='
    ) expression                                        # AssignExpr

//    // Level 0, Lambda Expression
    | lambdaExpression                                  # LambdaExpr// Java8
    | primaryExpression                                 # PrimaryExpr
//
//    | expression '!~' expression                        # SendMessageExpr             // last level, send message
    ;

primaryExpression:
        literal          #LiteralExpr
    |   namePath         #NamePathExpr
;


// Java8
lambdaExpression
    : lambdaParameters ARROW lambdaBody
    ;

// Java8
lambdaParameters
    : identifier
    | '(' formalParameters? ')'
    | '(' identifier (',' identifier)* ')'
    | '(' lambdaLVTIList? ')'
    ;

// Java8
lambdaBody
    : expression
    | block
    ;

// Java17
switchExpression
    : SWITCH parExpression '{' switchLabeledRule* '}'
    ;

// Java17
switchLabeledRule
    : CASE (expressionList | NULL_LITERAL | guardedPattern) SWITCH_ARROW  switchRuleOutcome
    | DEFAULT SWITCH_ARROW switchRuleOutcome
    ;

// Java17
guardedPattern
    : '(' guardedPattern ')'
    | variableModifiers? identifier AS variableType (AND expression)*       // TODO maybe should capture with `like` too
    | guardedPattern AND expression
    ;

// Java17
switchRuleOutcome
    : block
    | blockStatement*
    ;

creator
    //: nonWildcardTypeArguments? createdName classCreatorRest        # NormalCreator     // new <Animal>Dog
    : NEW declarationType classCreatorRest                             #NormalCreator    // TODO exclude primitiveType
    | chainCreator                                                     #ChainingCreator
    | NEW declarationType ('[' expression ']') ('[' expression? ']')*  #ArrayCreator
    ;

chainCreator:   declarationType '.' NEW POST_IDENTIFIER? classCreatorRest;

/*
    var staff = new Staff ("Tom") TomStaff with AnInterface{
        fun doJob(){
        }
    }
    not valuable in our case, just declare class?
    btw, now the design is ambiguous with `with{}`
*/
classCreatorRest
    : arguments // (identifier classBody)?
    ;


/*
    It's type for declaration and variable decalaration and type expression and id.id.... expression

    For Field and in extendsPhrase, implementsPhrase, pronoun is illegal, however, For LocalVar, pronoun and identifier(local variable) is legal


                                    Field(as type)     Var(as type)         methodCall and formalId expression
    namePath ->
        Primitive                   √                   √                   √
        PrimitiveArray              √                   √                   √
        FormalNamePath              √                   √                   √
            PrimitiveType
            FormalNamePath
                parameterizedType
                    typeIdentifier typeArguments? classCreatorArguments?
                identifier
                prononun
                primitiveType
        FormalArray                 √                   √                   √
        TypePrimaryId
            PronounPrimary          X                   √                   √
            IdenfierPrimary         X                   √                   √
*/
namePath
     : primitiveType                            #Primitive
     | possibleName ('.' possibleName)*         #FormalNamePath           // only parameterizedType, identifier can appear many times, primitive type can only show once
    ;

declarationType:    namePath;       // type literal

variableType:       // variable and fileds
           declarationType (brace = '[' ']')+     #VarTypeArray
        |  declarationType                        #VarTypeNormal
;

possibleName
    : parameterizedType         #NameParameterizedClassType
    | identifier                #NameIdentifier
    | pronoun                   #NamePronoun
    | primitiveType             #NamePrimitive
;

primitiveType: BOOLEAN    | CHAR    | BYTE    | SHORT    | INT    | LONG    | FLOAT    | DOUBLE    | STRING     | VOID  | CLASSREF;

pronoun:
    THIS                # ThisPrimary
    | SUPER             # SuperPrimary
    | CLASS '.' THIS    # ClassThisPrimary
    | CLASS '.' SUPER   # ClassSuperPrimary
    | FUN '.' THIS      # FunThisPrimary
    | FUN '.' SUPER     # FunSuperPrimary
    | TRAIT '.' THIS    # TraitThisPrimary      // in the trait, class.this ref to permit class, trait.this is itself
    | TRAIT '.' SUPER   # TraitSuperPrimary
    ;

parameterizedType: typeIdentifier typeArguments? classCreatorArguments?;

typeRange:    '[' from=typeOrAny (TO to=typeOrAny)? ']';

typeOrAny:  '_' | declarationType;      // _  not works, for it already ate in token IDENTIFIER

typeArguments:
        '<' '>'                                         #EmptyTypeArgs
    |   '<' typeArgument (',' typeArgument)* '>'        #TypeArgsList
    ;

classCreatorArguments:  '::' arguments;         // VarChar::(200)

//superSuffix
//    : arguments
//    | '.' typeArguments? identifier arguments?
//    ;
//
//explicitGenericInvocationSuffix
//    : SUPER superSuffix
//    | identifier arguments
//    ;

arguments: '(' expressionList? ')';

eos
    : ';'
    | EOF
    | {this.lineTerminatorAhead()}?
    | {this.closeBrace()}?
    ;