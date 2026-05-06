package org.siphonlab.ago.compiler.expression.dynamic;

import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DynamicCreator extends ExpressionInFunctionBody {

    private Expression typeExpr;
    private final List<Expression> arguments;
    private ClassDef tupleClass;

    public DynamicCreator(FunctionDef ownerFunction, Expression typeExpr, List<Expression> arguments) {
        super(ownerFunction);
        this.typeExpr = typeExpr.setParent(this);
        this.arguments = arguments;
    }


    @Override
    protected Expression transformInner() throws CompilationError {
        this.typeExpr = typeExpr.transform();
        if(this.arguments != null) {
            List<Expression> expressions = this.arguments;
            for (int i = 0; i < expressions.size(); i++) {
                Expression argument = expressions.get(i);
                this.arguments.set(i, argument.setParent(this).transform());
            }
        }

        var tuple = getRoot().getTupleClass(this.arguments.size());
        if(tuple == null) throw new IllegalStateException("Tuple%d not defined".formatted(this.arguments.size()));

        List<ClassRefLiteral> list = new ArrayList<>(this.arguments.size());
        for (var arg : this.arguments) {
            ClassRefLiteral refLiteral = arg.inferType().toClassRefLiteral();
            list.add(refLiteral);
        }
        var args = list.toArray(new ClassRefLiteral[0]);
        var instantiated = tuple.instantiateAsReferenceClass(new InstantiationArguments(tuple.getTypeParamsContext(), args), null);
        if(instantiated instanceof ConcreteType c) ownerFunction.registerConcreteType(c);
        this.tupleClass = instantiated;
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getRoot().getObjectClass();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) typeExpr.visit(blockCompiler);

            blockCompiler.lockRegister(instance);

            if(arguments == null || arguments.isEmpty()){
                blockCompiler.getCode().new_dynamic(localVar.getVariableSlot(), instance.getVariableSlot());
            } else {
                Var.LocalVar tupleInstance = (Var.LocalVar) new Creator(ownerFunction, new ConstClass(tupleClass), arguments, getSourceLocation()).visit(blockCompiler);
                blockCompiler.getCode().new_dynamic(localVar.getVariableSlot(), instance.getVariableSlot(), tupleInstance.getVariableSlot());
            }

            blockCompiler.releaseRegister(instance);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DynamicCreator that = (DynamicCreator) o;
        return Objects.equals(typeExpr, that.typeExpr) && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeExpr, arguments);
    }
}
