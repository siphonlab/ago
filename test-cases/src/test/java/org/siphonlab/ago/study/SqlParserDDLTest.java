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

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

import java.util.List;

public class SqlParserDDLTest {
    public static void main(String[] args) {

        Table table = new Table().withName("dual").withAlias(new Alias("t", false));

        CreateTable createTable = new CreateTable();

        Column columnA = new Column().withColumnName("a");
        Column columnB = new Column().withColumnName("b");

        createTable.withTable(table).addColumnDefinitions(
                List.of(new ColumnDefinition("name",
                                new ColDataType().withDataType("varchar").addArgumentsStringList("200"))
                            .addColumnSpecs("not null"),

                        new ColumnDefinition("id",
                                new ColDataType().withDataType("bigint"))
                            .addColumnSpecs("not null")
                        ));
//        Expression whereExpression = new EqualsTo().withLeftExpression(columnA).withRightExpression(columnB);
//
//        PlainSelect select = new PlainSelect().addSelectItem(new LongValue(1)).withFromItem(table).withWhere(whereExpression);
//
//        StringBuilder builder = new StringBuilder();
//        StatementDeParser deParser = new StatementDeParser(builder);
//        deParser.visit(select);

        StringBuilder sb= new StringBuilder();
        StatementDeParser deParser = new StatementDeParser(sb);
        deParser.visit(createTable);
        System.out.println(sb);
    }
}
