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
