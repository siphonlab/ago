package org.siphonlab.ago.study;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.HashMap;
import java.util.Map;

public class AliasTracker {

    public static void main(String[] args) throws Exception {
//        String sql = "SELECT u.id AS user_id, u.name AS user_name, o.order_no FROM users AS u JOIN orders o ON u.id = o.user_id";
        String sql = """
            WITH c AS (SELECT id, col1, col2 FROM table1)
            SELECT c.col1 AS c1, t2.col3 -> 'abc'
            FROM c
            JOIN table2 t2 ON c.id = t2.t1_id
            WHERE c.col2 > :name
            """;

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = select.getPlainSelect();

        // 1. 存储表别名映射： Alias -> Real Table Name (例如: u -> users)
        Map<String, String> tableAliasMap = new HashMap<>();
        
        // 解析主表
        if (plainSelect.getFromItem() instanceof Table) {
            Table mainTable = (Table) plainSelect.getFromItem();
            String tableName = mainTable.getName();
            String alias = mainTable.getAlias() != null ? mainTable.getAlias().getName() : tableName;
            tableAliasMap.put(alias, tableName);
        }
        
        // 解析 JOIN 表
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() instanceof Table) {
                    Table joinTable = (Table) join.getRightItem();
                    String tableName = joinTable.getName();
                    String alias = joinTable.getAlias() != null ? joinTable.getAlias().getName() : tableName;
                    tableAliasMap.put(alias, tableName);
                }
            }
        }

        // 2. 存储并追踪字段别名： Alias -> Original Field (例如: user_id -> users.id)
        Map<String, String> columnAliasMap = new HashMap<>();

        for (SelectItem<?> exprItem : plainSelect.getSelectItems()) {
                // 如果有别名
                if (exprItem.getAlias() != null) {
                    String aliasName = exprItem.getAlias().getName(); // 别名：user_id
                    
                    // 获取原始表达式
                    if (exprItem.getExpression() instanceof Column) {
                        Column column = (Column) exprItem.getExpression();
                        String columnName = column.getColumnName(); // 原始列名：id
                        String tableAlias = column.getTable() != null ? column.getTable().getName() : null; // 表别名：u
                        
                        // 还原真实的 表名.列名
                        String realTableName = tableAliasMap.getOrDefault(tableAlias, tableAlias);
                        String originalSource = (realTableName != null ? realTableName + "." : "") + columnName;
                        
                        columnAliasMap.put(aliasName, originalSource);
                    }
                }
        }

        // 3. 输出追踪结果
        System.out.println("--- 表别名追踪 ---");
        tableAliasMap.forEach((k, v) -> System.out.println("别名 [" + k + "] -> 原始表 [" + v + "]"));

        System.out.println("\n--- 字段别名追踪 ---");
        columnAliasMap.forEach((k, v) -> System.out.println("别名 [" + k + "] -> 原始字段 [" + v + "]"));
    }
}
