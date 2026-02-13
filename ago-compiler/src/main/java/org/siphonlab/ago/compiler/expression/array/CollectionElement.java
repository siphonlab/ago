package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.TermExpression;
import org.siphonlab.ago.compiler.expression.Var;

public interface CollectionElement extends Expression{

    Var.LocalVar getProcessedCollection();

    TermExpression getProcessedIndex();

    Expression toPutElement(Expression processedCollection, TermExpression processedIndex, Expression value) throws CompilationError;

}
