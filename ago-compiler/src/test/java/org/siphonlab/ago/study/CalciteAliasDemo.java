package org.siphonlab.ago.study;// CalciteAliasDemo.java
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.rex.*;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.SqlCollation;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.validate.SqlValidatorImpl;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;

public class CalciteAliasDemo {
    public static void main(String[] args) throws Exception {
        // 1. 构造最小化的 Schema
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);
        addTable(rootSchema, "table1", Map.of(
                "id", SqlTypeName.INTEGER,
                "col1", SqlTypeName.VARCHAR,
                "col2", SqlTypeName.DOUBLE));
        addTable(rootSchema, "table2", Map.of(
                "id", SqlTypeName.INTEGER,
                "t1_id", SqlTypeName.INTEGER,
                "col3", SqlTypeName.VARCHAR));

        // 2. 准备要解析的 SQL
        String sql = """
            WITH c AS (SELECT id, col1, col2 FROM table1)
            SELECT c.col1 AS c1, t2.col3
            FROM c
            JOIN table2 t2 ON c.id = t2.t1_id
            WHERE c.col2 > 10
            """;

        // 3. 解析 + 验证
        SqlParser parser = SqlParser.create(sql);
        SqlNode parsed = parser.parseStmt();

        Planner planner = Frameworks.getPlanner(
                Frameworks.newConfigBuilder()
                        .defaultSchema(rootSchema)
                        .parserConfig(SqlParser.config().withLex(Lex.MYSQL))
                        .build());

        SqlNode root = planner.parse(sql);

        SqlNode validated = planner.validate(root);

        // 4. 打印逻辑计划（可选）
        System.out.println("\n=== Logical Plan ===");

        RelNode rel = planner.rel(validated).rel;
        System.out.println(RelOptUtil.toString(rel));

        // 5. 提取别名 → 原始表+字段 的映射
        Map<String, String> aliasMap = extractAliasMapping(rel);
        System.out.println("\n=== Alias Mapping ===");
        aliasMap.forEach((alias, origin) -> System.out.printf("%s => %s%n", alias, origin));
    }

    /* ------------------------------------------------------------------ */
    /* Helper: 建表到 Schema  */
    private static void addTable(SchemaPlus schema,
                                 String name,
                                 Map<String, SqlTypeName> cols) {
        schema.add(name, new AbstractTable() {
            @Override public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                RelDataTypeFactory.Builder b = typeFactory.builder();
                cols.forEach(b::add);
                return b.build();
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /* 解析 SELECT 列别名 → 原始表+字段 的核心逻辑 */
    private static Map<String, String> extractAliasMapping(RelNode root) {
        // 如果根节点是 LogicalProject，则每个表达式对应一列
        if (!(root instanceof LogicalProject)) {
            // 直接把输出列映射到叶子表字段（没有别名的情况）
            return mapDirect(root);
        }

        LogicalProject project = (LogicalProject) root;
        List<RexNode> projects = project.getProjects();
        List<RelDataTypeField> fields   = project.getRowType().getFieldList();

        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            String alias      = fields.get(i).getName(); // SELECT 列名（可能是别名）
            RexNode expr      = projects.get(i);         // 对应的表达式
            String origin     = resolveOrigin(expr, project.getInput());
            map.put(alias, origin != null ? origin : "<unknown>");
        }
        return map;
    }

    /* ------------------------------------------------------------------ */
    /* 直接映射（无别名）: 输出列就等于叶子表字段 */
    private static Map<String, String> mapDirect(RelNode node) {
        List<RelDataTypeField> fields = node.getRowType().getFieldList();
        Map<String, String> map = new LinkedHashMap<>();
        for (RelDataTypeField f : fields) {
            // 只要是 LogicalTableScan 就能得到原始字段
            if (node instanceof LogicalTableScan) {
                RelOptTable tbl = ((LogicalTableScan) node).getTable();
                String tableName = String.join(".", tbl.getQualifiedName());
                map.put(f.getName(), tableName + "." + f.getName());
            } else {
                // 对于 Filter / Sort 等不改变字段顺序的节点，直接下探
                RelNode input = node.getInputs().get(0);
                if (input instanceof LogicalTableScan) {
                    RelOptTable tbl = ((LogicalTableScan) input).getTable();
                    String tableName = String.join(".", tbl.getQualifiedName());
                    map.put(f.getName(), tableName + "." + f.getName());
                } else {
                    // 复杂情况：递归调用
                    Map<String, String> sub = extractAliasMapping(input);
                    if (!sub.isEmpty()) {
                        // 用字段名覆盖原始值
                        map.putAll(sub);
                    }
                }
            }
        }
        return map;
    }

    /* ------------------------------------------------------------------ */
    /* 递归解析表达式，找到最底层的 RexInputRef 并返回 table.column 字符串 */
    private static String resolveOrigin(RexNode expr, RelNode input) {
        if (expr instanceof RexInputRef) {
            // 最直接：已经是叶子字段引用
            return getTableColumn((RexInputRef) expr, input);
        } else if (expr instanceof RexCall) {
            // 计算表达式（+、-、* 等）：递归检查 operands
            for (RexNode op : ((RexCall) expr).getOperands()) {
                String res = resolveOrigin(op, input);
                if (res != null) return res;
            }
        } else if (expr instanceof RexFieldAccess) {
            // 结构体字段访问：先解析引用
            return resolveOrigin(((RexFieldAccess) expr).getReferenceExpr(), input);
        }
        // 其它类型（如 RexDynamicParam、RexLiteral 等）不涉及表字段，直接返回 null
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* 从 RexInputRef + 当前节点 找到原始表+列名 */
    private static String getTableColumn(RexInputRef ref, RelNode node) {
        int index = ref.getIndex();
        // 逐层向下寻找叶子 LogicalTableScan
        while (true) {
            if (node instanceof LogicalProject) {
                // Project 本身不改变字段顺序，直接往下走即可
                node = node.getInput(0);
            } else if (node instanceof Join) {
                // 需要把 index 分配到左/右子树
                int leftCount = node.getInput(0).getRowType().getFieldCount();
                if (index < leftCount) {
                    node = node.getInput(0);          // 左边
                } else {
                    index -= leftCount;
                    node = node.getInput(1);          // 右边
                }
            } else if (node instanceof LogicalTableScan) {
                RelOptTable tbl = ((LogicalTableScan) node).getTable();
                String tableName = String.join(".", tbl.getQualifiedName());
                String columnName = node.getRowType().getFieldList().get(index).getName();
                return tableName + "." + columnName;
            } else if (node instanceof LogicalFilter ||
                       node instanceof LogicalSort ||
                       node instanceof LogicalAggregate) {
                // 这些节点不改变字段顺序，直接下探
                node = node.getInput(0);
            } else {
                // 其它未知节点：尝试下探第一个输入
                if (!node.getInputs().isEmpty()) {
                    node = node.getInput(0);
                } else {
                    break;
                }
            }
        }
        return null;   // 没找到叶子表，说明表达式不是直接列引用
    }

    static class OrmSchema extends AbstractSchema {
        @Override
        protected Map<String, Table> getTableMap() {
            return super.getTableMap();
        }
    }
//    static class OrmTable extends AbstractTable {
//        @Override
//        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
//            RelDataTypeFactory.Builder builder = typeFactory.builder();
//            builder.add("test", new RelDataType() {
//                @Override
//                public boolean isStruct() {
//                    return false;
//                }
//
//                @Override
//                public List<RelDataTypeField> getFieldList() {
//                    return List.of();
//                }
//
//                @Override
//                public List<String> getFieldNames() {
//                    return List.of();
//                }
//
//                @Override
//                public int getFieldCount() {
//                    return 0;
//                }
//
//                @Override
//                public StructKind getStructKind() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable RelDataTypeField getField(String fieldName, boolean caseSensitive, boolean elideRecord) {
//                    return null;
//                }
//
//                @Override
//                public boolean isNullable() {
//                    return false;
//                }
//
//                @Override
//                public @Nullable RelDataType getComponentType() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable RelDataType getKeyType() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable RelDataType getValueType() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable Charset getCharset() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable SqlCollation getCollation() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable SqlIntervalQualifier getIntervalQualifier() {
//                    return null;
//                }
//
//                @Override
//                public int getPrecision() {
//                    return 0;
//                }
//
//                @Override
//                public int getScale() {
//                    return 0;
//                }
//
//                @Override
//                public SqlTypeName getSqlTypeName() {
//                    return null;
//                }
//
//                @Override
//                public @Nullable SqlIdentifier getSqlIdentifier() {
//                    return null;
//                }
//
//                @Override
//                public String getFullTypeString() {
//                    return "";
//                }
//
//                @Override
//                public RelDataTypeFamily getFamily() {
//                    return null;
//                }
//
//                @Override
//                public RelDataTypePrecedenceList getPrecedenceList() {
//                    return null;
//                }
//
//                @Override
//                public RelDataTypeComparability getComparability() {
//                    return null;
//                }
//
//                @Override
//                public boolean isDynamicStruct() {
//                    return false;
//                }
//            });
//
//            // 遍历 Java 类的所有字段，将其转化为 Calcite 的列名（Field Name）
//            for (Field field : entityClass.getDeclaredFields()) {
//                String fieldName = field.getName(); // 这里拿到的就是类里的字段名，如 userId
//                SqlTypeName sqlTypeName = mapJavaTypeToSqlType(field.getType());
//
//                // 将字段名和对应的 SQL 类型放入元数据
//                builder.add(fieldName, typeFactory.createSqlType(sqlTypeName));
//            }
//
//            return builder.build();
//        }
//    }
}
