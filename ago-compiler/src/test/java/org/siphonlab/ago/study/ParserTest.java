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
package org.siphonlab.ago.study;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ParserTest {
    @Test
    public void classDef() throws IOException, ExecutionException, InterruptedException {
//        String code = IOUtils.toString(new FileInputStream("D:\\ficfio\\ago\\examples\\bootstrap\\20.boxer.ago"));
        String code = """
                fun main(){
                                     var c = new Contact()
                                     c.name = "Tom"
                                     c.phone = "13333334444"
                
                                     var s as string = c.name
                                     Trace.print("name:" + s)
                
                                     var str as String = s;
                                     Trace.print("str:" + str)
                
                                     var v as VarChar::(300) = str
                                     Trace.print("v:" + v)
                                 }
                """;
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        AgoParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();

        showAstTreeview(parser, compilationUnitContext);
    }

    @Test
    public void genericOfGeneric() throws IOException, ExecutionException, InterruptedException {
//        ParserATNSimulator.debug = true;
//        ParserATNSimulator.trace_atn_sim = true;
//        ParserATNSimulator.dfa_debug = true;
        ParserATNSimulator.retry_debug = true;

        String code = """
                    fun listUsers() as QueryResult<List<User>> {
                    }
                """;
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        parser.setTrace(true);
        parser.setErrorHandler(new DefaultErrorStrategy(){
            @Override
            public void reportError(Parser recognizer, RecognitionException e) {
                super.reportError(recognizer, e);
            }

            @Override
            protected void reportFailedPredicate(Parser recognizer, FailedPredicateException e) {
                super.reportFailedPredicate(recognizer, e);
            }
        });
        AgoParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();

        showAstTreeview(parser, compilationUnitContext);
    }

    @Test
    public void strings() throws IOException, ExecutionException, InterruptedException {
        String code = IOUtils.toString(new FileInputStream("D:\\My Work\\ago\\examples\\strings.ago"));
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));

//        Map<Integer, String> types = new HashMap<>();
//        for (Map.Entry<String, Integer> entry : lexer.getTokenTypeMap().entrySet()) {
//            types.put(entry.getValue(), entry.getKey());
//        }
//
//        List<? extends Token> allTokens = lexer.getAllTokens();
//        System.err.println(allTokens.stream().map(t -> types.get( t.getType()) + ": " + t.getText() + " " + t.getChannel()).collect(Collectors.joining("\n")));

//        lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        AgoParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();

        showAstTreeview(parser, compilationUnitContext);
    }

    @Test
    public void eos() throws IOException, ExecutionException, InterruptedException {
        String code = """
                    {
                        print(add(5));
                    }
                """;
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        var ast = parser.methodBody();
        showAstTreeview(parser, ast);
    }

    @Test
    public void lang() throws IOException, ExecutionException, InterruptedException {
        String code = FileUtils.readFileToString(new File("src/main/ago/lang.ago"), "utf-8");
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        var ast = parser.compilationUnit();
        showAstTreeview(parser, ast);
    }

    @Test
    public void base_types() throws IOException, ExecutionException, InterruptedException {
        String code = FileUtils.readFileToString(new File("examples/bootstrap/base_types.ago"), "utf-8");
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        var ast = parser.compilationUnit();
        showAstTreeview(parser, ast);
    }

    @Test
    public void interface_test() throws IOException, ExecutionException, InterruptedException {
        String code = FileUtils.readFileToString(new File("examples/bootstrap/13.interface.ago"), "utf-8");
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        var ast = parser.compilationUnit();
        showAstTreeview(parser, ast);
    }

    public void parse(String filename) throws IOException, ExecutionException, InterruptedException {
        String code = FileUtils.readFileToString(new File(filename), "utf-8");
        AgoLexer lexer = new AgoLexer(CharStreams.fromString(code));

        if(ParserATNSimulator.debug) {
            Map<Integer, String> types = new HashMap<>();
            for (Map.Entry<String, Integer> entry : lexer.getTokenTypeMap().entrySet()) {
                types.put(entry.getValue(), entry.getKey());
            }

            List<? extends Token> allTokens = lexer.getAllTokens();
            System.err.println(allTokens.stream().map(t -> types.get(t.getType()) + ": " + t.getText() + " " + t.getChannel()).collect(Collectors.joining("\n")));

            lexer = new AgoLexer(CharStreams.fromString(code));
        }

        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        var ast = parser.compilationUnit();
        showAstTreeview(parser, ast);
    }

    @Test
    public void test() throws IOException, ExecutionException, InterruptedException {
//        ParserATNSimulator.debug = true;
//        ParserATNSimulator.trace_atn_sim = true;
//        ParserATNSimulator.dfa_debug = true;
//        ParserATNSimulator.retry_debug = true;
        //parse("examples/bootstrap/6.array.ago");
        //parse("examples/bootstrap/9.parameterized_class.ago");

        parse("../test-cases/examples/bootstrap/29.literals.ago");
    }

    private static void showAstTreeview(AgoParser parser, ParserRuleContext ast) throws InterruptedException, ExecutionException {
        var loginSignal = new CountDownLatch(1);

        List<String> list = Arrays.asList(parser.getRuleNames());
        TreeViewer viewr = new TreeViewer(list, ast);
        viewr.setTreeTextProvider(new AltLabelTextProvider(parser));
        var frame = viewr.open().get();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loginSignal.countDown();
            }
        });
        try {
            loginSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
