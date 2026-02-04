package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.statement.Label;

import java.util.Objects;

import static org.siphonlab.ago.compiler.PrimitiveClassDef.BOOLEAN;
import static org.siphonlab.ago.opcode.logic.Or.KIND_OR;

public class OrExpr extends ExpressionBase {
    public Expression left;
    public Expression right;

    public OrExpr(Expression left, Expression right) throws CompilationError {
        this.left = left.transform();
        this.right = right.transform();
        this.left.setParent(this);
        this.right.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        CastStrategy.UnifyTypeResult result = new CastStrategy(this.getSourceLocation(), false).unifyTypes(this.left, this.right);
        if (result.changed() || result.left() != this.left || result.right() != this.right) {
            this.left = result.left();
            this.right = result.right();
        }
        if(this.left instanceof Literal<?> l){
            return BooleanLiteral.isTrue(l) ? l : this.right;
        }
        return this;
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return this.left.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (this.left instanceof LiteralResultExpression lre) {
                Literal<?> literal = lre.visit(blockCompiler);
                if (BooleanLiteral.isFalse(literal)) {
                    Assign.to(localVar, this.right).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                } else {
                    Assign.to(localVar, literal).visit(blockCompiler);
                }
            } else {
                this.left.outputToLocalVar(localVar, blockCompiler);
            }
            if (this.right instanceof Var var) {
                if (this.left.inferType() == BOOLEAN) {
                    var v1 = localVar;
                    Var.LocalVar v2 = (Var.LocalVar) var.visit(blockCompiler);
                    blockCompiler.getCode().biOperate(KIND_OR, left.inferType().getTypeCode(), v1.getVariableSlot(), v2.getVariableSlot(), localVar.getVariableSlot());
                    return;
                }
            }

            // make shortcut
            Label skip = blockCompiler.createLabel();
            blockCompiler.getCode().jumpIf(localVar.getVariableSlot(), skip);
            right.outputToLocalVar(localVar, blockCompiler);
            skip.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(Or %s %s)".formatted(left, right);
    }

    @Override
    public OrExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrExpr orExpr)) return false;
        return Objects.equals(left, orExpr.left) && Objects.equals(right, orExpr.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
