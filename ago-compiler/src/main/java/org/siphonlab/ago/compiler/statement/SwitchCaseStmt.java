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
package org.siphonlab.ago.compiler.statement;



import org.siphonlab.ago.compiler.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.DuplicatedError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.logic.InstanceOf;

import java.util.*;

public class SwitchCaseStmt extends Statement{

    private Expression condition;
    private final List<SwitchGroup> groups;
    private Mode mode;
    private int switchTableFirstKey;
    private int switchTableLastKey;
    private LinkedHashMap<Integer, SwitchGroup> intCasesMap;
    private SwitchGroup defaultGroup;
    private Label exitLabel;

    public static class SwitchGroup{
        List<Case> cases = new ArrayList<>();
        List<Statement> actions = new ArrayList<>();
        Label entranceLabel;
        boolean hasDefault = false;

        public void addCase(Case aCase){
            this.cases.add(aCase);
            aCase.group = this;
            if(aCase.caseKind == CaseKind.Default){
                hasDefault = true;
            }
        }

        public void addStatement(Statement statement) {
            this.actions.add(statement);
        }

        public void setEntranceLabel(Label entranceLabel) {
            this.entranceLabel = entranceLabel;
        }

        public void visitActions(BlockCompiler blockCompiler) throws CompilationError {
            for (Statement action : this.actions) {
                action.termVisit(blockCompiler);
            }
        }
    }

    public enum CaseKind {
        ConstExpression, EnumConst, TypeDispatch, Default
    }

    enum Mode{
        DenseInts,
        SparseInts,
        Branches
    }

    public static final class Case {
        private final CaseKind caseKind;
        public SwitchGroup group;
        private Expression expression;

        public Case(CaseKind caseKind, Expression expression) {
            this.caseKind = caseKind;
            this.expression = expression;
        }

        private SourceLocation sourceLocation;

        public void setSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
        }

        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }

        public CaseKind caseKind() {
            return caseKind;
        }

        public Expression expression() {
            return expression;
        }

        @Override
        public String toString() {
            return "Case[" +
                    "caseKind=" + caseKind + ", " +
                    "expression=" + expression + ']';
        }
    }

    List<Label> cases = new ArrayList<>();
    public SwitchCaseStmt(Expression condition, List<SwitchGroup> groups) throws CompilationError {
        this.condition = condition.transform();
        if(condition instanceof EnumValue enumValue){
            this.condition = enumValue.toLiteral();
        } else if(condition instanceof ConstValue constValue){
            this.condition = constValue.toLiteral();
        }
        this.groups = groups;
        condition.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        var allCasesIt = groups.stream().flatMap(g -> g.cases.stream()).iterator();
        LinkedHashMap<Integer, SwitchGroup> intCasesMap = new LinkedHashMap<>();
        Case defaultCase = null;
        Mode mode = TypeCode.INT.isHigherThan(condition.inferType().getTypeCode()) ? Mode.Branches : Mode.DenseInts;
        List<CompilationError> errors = new ArrayList<>();
        List<Case> processed = new ArrayList<>();
        while (allCasesIt.hasNext()) {
            Case cs = allCasesIt.next();
            if(defaultCase != null){
                throw new SyntaxError("cases after 'default' case", cs.sourceLocation);
            }
            var duplicated = processed.stream().anyMatch(cs2 ->{
                if(cs2.caseKind == cs.caseKind){
                    switch (cs2.caseKind){
                        case Default: return true;
                        case ConstExpression:
                        case CaseKind.EnumConst:
                            if(cs.expression.equals(cs2.expression)){
                                return true;
                            }
                            break;
                        case CaseKind.TypeDispatch:
                            Var.LocalVar v1 = (Var.LocalVar) cs.expression;
                            Var.LocalVar v2 = (Var.LocalVar) cs2.expression;
                            try {
                                if(v1.inferType() == v2.inferType()){
                                    return true;
                                }
                            } catch (CompilationError e) {
                                throw new RuntimeException(e);
                            }
                    }
                    return false;
                } else {
                    return false;
                }
            });
            if(duplicated){
                throw new DuplicatedError("case duplicated",cs.sourceLocation);
            }
            processed.add(cs);

            if(cs.expression != null) cs.expression.setParent(this);
            for (Statement action : cs.group.actions) {
                action.setParent(this).transform();
            }

            switch (cs.caseKind){
                case ConstExpression:
                case EnumConst:
                    cs.expression = cs.expression.transform().setParent(this);
                    Literal<?> literal;
                    if(cs.expression instanceof Literal<?> l){
                        literal = l;
                    } else if(cs.expression instanceof EnumValue enumValue){
                        literal = enumValue.toLiteral();
                        cs.expression = literal;
                    } else if(cs.expression instanceof ConstValue constValue){
                        literal = constValue.toLiteral();
                        cs.expression = literal;
                    } else {
                        literal = null;
                    }
                    if(literal != null){
                        if(mode != Mode.Branches) {
                            TypeCode typeCode = literal.inferType().getTypeCode();
                            if(typeCode.isNumber()) {
                                if (typeCode.isHigherThan(TypeCode.INT)) {
                                    errors.add(new TypeMismatchError("only int value allowed", cs.sourceLocation));
                                } else {
                                    var value = (IntLiteral) new Cast(literal, PrimitiveClassDef.INT).setSourceLocation(cs.sourceLocation).transform();
                                    cs.expression = value;
                                    intCasesMap.put(value.value, cs.group);
                                }
                            } else {
                                mode = Mode.Branches;  // compileBranches will cast case.expression to condition.type too
                            }
                        }
                    } else {
                        mode = Mode.Branches;
                    }
                    break;
                case Default:
                    defaultCase = cs;
                    break;
                case TypeDispatch:
                    mode = Mode.Branches;
                    break;
            }
        }

        if(mode == Mode.DenseInts){
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Integer key : intCasesMap.keySet()) {
                if(key < min) min = key;
                if(key > max) max = key;
            }
            int cnt = intCasesMap.size();
            if(max - min > cnt * 1.44 || cnt >= 256){
                mode = Mode.SparseInts;
            }
            this.switchTableFirstKey = min;
            this.switchTableLastKey = max;
            this.intCasesMap = intCasesMap;
            this.condition = new Cast(this.condition,PrimitiveClassDef.INT).transform();
        } else {
            if(!errors.isEmpty()){
                throw errors.get(0);
            }
        }
        this.mode = mode;
        if(defaultCase != null)
            this.defaultGroup = defaultCase.group;
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var code = blockCompiler.getCode();
            FunctionDef functionDef = blockCompiler.getFunctionDef();

            var r = this.condition.visit(blockCompiler);
            if (r instanceof Literal<?> literal) {
                switch (mode) {
                    case DenseInts:
                    case SparseInts:
                        var group = this.intCasesMap.getOrDefault(((IntLiteral) literal).value, this.defaultGroup);
                        if (group != null)
                            group.visitActions(blockCompiler);
                        return;
                }
                var v = new PipeToTempVar(literal).visit(blockCompiler);
                compileBranches(v, blockCompiler);
            } else {
                Var.LocalVar v = (Var.LocalVar) r;
                switch (mode) {
                    case DenseInts:
                        DenseSwitchTable denseTable = functionDef.createDenseSwitchTable();
                        denseTable.setFirstKey(switchTableFirstKey);
                        code.jumpByDenseSwitchTable(v.getVariableSlot(), denseTable.getId());
                        compileSwitchTables(denseTable, blockCompiler);
                        break;
                    case Mode.SparseInts:
                        SparseSwitchTable sparseSwitchTable = functionDef.createSparseSwitchTable();
                        code.jumpBySparseSwitchTable(v.getVariableSlot(), sparseSwitchTable.getId());
                        compileSwitchTables(sparseSwitchTable, blockCompiler);
                        break;
                    case Branches:
                        compileBranches(v, blockCompiler);
                }
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private void compileSwitchTables(SwitchTable switchTable, BlockCompiler blockCompiler) throws CompilationError {
        this.exitLabel = blockCompiler.createLabel();
        for (Map.Entry<Integer, SwitchGroup> entry : intCasesMap.sequencedEntrySet()) {
            Integer caseValue = entry.getKey();
            SwitchGroup group = entry.getValue();
            if(group.entranceLabel != null){
                switchTable.addLabel(caseValue, group.entranceLabel);
            } else {
                var entranceLabel = blockCompiler.createLabel().here();
                group.setEntranceLabel(entranceLabel);
                switchTable.addLabel(caseValue, entranceLabel);
                group.visitActions(blockCompiler);
            }
        }

        Label defaultLabel;
        if(defaultGroup != null) {
            if (defaultGroup.entranceLabel != null) {
                defaultLabel = defaultGroup.entranceLabel;
            } else {
                var entranceLabel = blockCompiler.createLabel().here();
                defaultGroup.setEntranceLabel(entranceLabel);
                defaultLabel = entranceLabel;
                defaultGroup.visitActions(blockCompiler);

            }
        } else {
            defaultLabel = exitLabel;
        }

        int defaultKey = switchTableLastKey + 1;
        if (mode == Mode.SparseInts) {
            ((SparseSwitchTable) switchTable).setDefaultEntrance(defaultLabel);
        }
        switchTable.addLabel(defaultKey,defaultLabel);

        this.exitLabel.here();     // for break stmt
        switchTable.composeBlob();
    }

    private void compileBranches(Var.LocalVar conditionResult, BlockCompiler blockCompiler) throws CompilationError {
        this.exitLabel = blockCompiler.createLabel();
        CodeBuffer code = blockCompiler.getCode();

        Case alwaysTrueBranch = this.findAlwaysTrueBranch(conditionResult);     // TODO always false
        if(alwaysTrueBranch != null){
            if(alwaysTrueBranch.caseKind == CaseKind.TypeDispatch){
                Var.LocalVar var = (Var.LocalVar) alwaysTrueBranch.expression;
                Assign.to(var,conditionResult).termVisit(blockCompiler);
            }
            alwaysTrueBranch.group.visitActions(blockCompiler);
            this.exitLabel.here();
            return;
        }

        blockCompiler.lockRegister(conditionResult);
        Label defaultBranch = null;
        // compile conditions
        Map<Case, Label> variableAssignments = new LinkedHashMap<>();
        Var.LocalVar r = null;
        for (int i = 0; i < groups.size(); i++) {
            SwitchGroup group = groups.get(i);
            Label groupEntrance = blockCompiler.createLabel();
            group.setEntranceLabel(groupEntrance);
            if(group.hasDefault) {
                defaultBranch = groupEntrance;
            }
            for (Case cs : group.cases) {
                switch (cs.caseKind) {
                    case ConstExpression:
                    case EnumConst:
                        var eq = new Equals(condition, new Cast(cs.expression, condition.inferType()).transform(), Equals.Type.Equals).transform().visit(blockCompiler);
                        if(eq instanceof Literal<?>){
                            r = new PipeToTempVar(eq).visit(blockCompiler);
                        } else {
                            r = (Var.LocalVar) eq;
                        }
                        code.jumpIf(r.getVariableSlot(), groupEntrance);
                        break;
                    case TypeDispatch:
                        Var.LocalVar typeDispatch = (Var.LocalVar) cs.expression;
                        var instanceOF = new InstanceOf(conditionResult, typeDispatch.inferType(), typeDispatch).transform();
                        if(instanceOF instanceof Literal<?>){
                            r = new PipeToTempVar(instanceOF).visit(blockCompiler);
                        } else {
                            r = (Var.LocalVar) instanceOF;
                        }
                        var assignment = blockCompiler.createLabel();
                        code.jumpIf(r.getVariableSlot(), assignment);       // jump to assignment
                        variableAssignments.put(cs, assignment);
                        break;
                }
            }
        }
        blockCompiler.releaseRegister(conditionResult);
        if(r != null){
            code.jumpIfNot(r.getVariableSlot(), defaultBranch != null ? defaultBranch : exitLabel);
        }
        /*
        *  condition segment, some match will jump to variable assignment segment
        *      if ... jump to my group entrance
        *      if ... jump to variable entrance
        *
        *  variable assign segment
        *       jump to exit        // avoid enter
        *       v as Foo = cond
        *       jump to my group entrance
        *       v2 as Bar = cond
        *       jump to my group entrance
        * */

        // ------------------- variable assignment segment -------------------------
        if(!variableAssignments.isEmpty()){
            code.jump(exitLabel);       // avoid normal statement enter here
            for (Map.Entry<Case, Label> entry : variableAssignments.entrySet()) {
                Case cs = entry.getKey();
                Var.LocalVar v = (Var.LocalVar) cs.expression;

                var lb = entry.getValue();
                lb.here();

                Assign.to(v, conditionResult).termVisit(blockCompiler);
                code.jump(cs.group.entranceLabel);
            }
        }

        // ------------------- actions -------------------------
        for (int i = 0; i < groups.size(); i++) {
            SwitchGroup group = groups.get(i);
            group.entranceLabel.here();
            group.visitActions(blockCompiler);
        }
        this.exitLabel.here();
    }

    private Case findAlwaysTrueBranch(TermExpression condResult) throws CompilationError {
        for (SwitchGroup group : this.groups) {
            for (Case cs : group.cases) {
                switch (cs.caseKind){
                    case ConstExpression:
                        if(Equals.isLiteralEquals(cs.expression,condition) || condResult.equals(cs.expression)) return cs;
                        break;
                    case EnumConst:
                        if(Equals.isLiteralEquals(cs.expression,condition) || condResult.equals(cs.expression)) return cs;
                        break;
                    case TypeDispatch:
                        if(cs.expression.inferType().isThatOrSuperOfThat(condResult.inferType())){
                            return cs;
                        }
                        break;
                }
            }
        }
        return null;
    }

    @Override
    public SwitchCaseStmt setSourceLocation(org.siphonlab.ago.SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Label getExitLabel() {
        return this.exitLabel;
    }
}
