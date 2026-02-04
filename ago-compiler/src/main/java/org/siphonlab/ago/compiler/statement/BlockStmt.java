package org.siphonlab.ago.compiler.statement;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.List;

public class BlockStmt extends Statement{

    private final List<Statement> statements;

    public BlockStmt(List<Statement> statements){
        this.statements = statements;
        for (Statement statement : this.statements) {
            statement.setParent(this);
        }
    }

    @Override
    public Statement transformInner() throws CompilationError {
        List<Statement> statementList = this.statements;
        for (int i = 0; i < statementList.size(); i++) {
            Statement statement = statementList.get(i);
            var n = statement.transform();
            if (n != statement) {
                statementList.set(i, statement);
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        for (Statement statement : statements) {
            statement.termVisit(blockCompiler);
        }
    }

    @Override
    public BlockStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "{" + StringUtils.join(statements, '\n') + "}";
    }
}
