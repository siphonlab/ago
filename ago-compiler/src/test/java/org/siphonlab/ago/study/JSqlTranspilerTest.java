package org.siphonlab.ago.study;

import ai.starlake.transpiler.JSQLColumResolver;
import ai.starlake.transpiler.JSQLReplacer;
import ai.starlake.transpiler.schema.JdbcResultSetMetaData;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class JSqlTranspilerTest {
    @Test
    public void test1() throws JSQLParserException {
        String sql = """
                SELECT
                  DATE(2016, 12, 25) AS date_ymd,
                  DATE(DATETIME '2016-12-25 23:59:59') AS date_dt,
                  DATE(TIMESTAMP '2016-12-25 05:30:00+07', 'America/Los_Angeles') AS date_tstz;
                
                """;
        String[][] schemaDefinition = {
                // Table A with Columns col1, col2, col3, colAA, colAB
                {"a", "col1", "col2", "col3", "colAA", "colAB"},

                // Table B with Columns col1, col2, col3, colBA, colBB
                {"b", "col1", "col2", "col3", "colBA", "colBB"}
        };

        String sqlStr =
                "SELECT Case when Sum(colBA + colBB)=0 then c.col1 else a.col2 end AS total FROM a INNER JOIN (SELECT * FROM b) c ON a.col1 = c.col1";

        JdbcResultSetMetaData resultSetMetaData = new JSQLColumResolver(schemaDefinition).getResultSetMetaData(sqlStr);
        System.out.println(resultSetMetaData);
    }

    @Test
    public void test2() throws JSQLParserException {
        String sql = """
            SELECT a.*
            FROM (  SELECT  a.col3
                            , Sum( a.col2 )
                    FROM a inner join b on a.col1=b.col1
                    WHERE a.col1 = b.col1
                    GROUP BY a.col3
                    HAVING Sum( a.col2 ) > 0 ) AS a
        """;
        JSQLReplacer replacer = new JSQLReplacer(new String[][]{{"a", "col1", "col2", "col3"}, {"b", "col1", "col2", "col3"}});
        var s = replacer.replace(sql, java.util.Map.of("a", "b", "b", "a"));
        System.out.println(s);
    }
}
