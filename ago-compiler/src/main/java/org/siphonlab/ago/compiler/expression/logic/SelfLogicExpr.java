package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.expression.Assign;
import org.siphonlab.ago.compiler.expression.Cast;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

import java.util.Objects;

import static org.siphonlab.ago.opcode.logic.And.KIND_AND;
import static org.siphonlab.ago.opcode.logic.Or.KIND_OR;

public class SelfLogicExpr extends SelfOpExpr {

    private final Expression site;
    private final Expression change;
    private final Type type;

    public enum Type{
        And(KIND_AND),
        Or(KIND_OR)
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }

    public SelfLogicExpr(Expression site, Expression change, Type type) throws CompilationError {
        super(site, change);
        this.site = site.transform().setParent(this);
        this.change = new Cast(change.setParent(this), site.inferType()).transform();
        this.type = type;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.site.inferType();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if (!(site instanceof Assign.Assignee)) {
            throw new SyntaxError("bad target for self logic operation", this.site.getSourceLocation());
        }
        if(change instanceof Literal<?> literal){
            switch (this.type){
                case And: {
                    if(BooleanLiteral.isTrue(literal)){     // an= true
                        return this.site;
                    } else {
                        return Assign.to((Assign.Assignee) site,literal).setSourceLocation(this.getSourceLocation()).transform();
                    }
                }
                case Or:{
                    if(BooleanLiteral.isFalse(literal)){
                        return this.site;
                    }
                }
            }
        }
        return this;
    }

    @Override
    Expression expr(Expression left, Expression right) throws CompilationError {
        return (type == Type.And ? new AndExpr(site,change) : new OrExpr(site,change));
    }

    @Override
    public SelfLogicExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(this.type, this.site, this.change);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SelfLogicExpr that)) return false;
        return Objects.equals(site, that.site) && Objects.equals(change, that.change) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, change, type);
    }
}
