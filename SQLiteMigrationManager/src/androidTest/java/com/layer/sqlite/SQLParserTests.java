package com.layer.sqlite;

import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class SQLParserTests extends AndroidTestCase {
    public void testParseSingleNoComments() throws Exception {
        String statement = "SELECT\n* FROM \n apples;";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.toStatements(in);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isEqualTo("SELECT\n* FROM \n apples");
    }

    public void testParseMultipleNoComments() throws Exception {
        String statement = "SELECT\n* FROM \n apples;SELECT 1 FROM oranges;\n"
                + "\n\nINSERT INTO v (x, y) VALUES\n(SELECT a, b FROM q);";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.toStatements(in);
        assertThat(parsed).hasSize(3);
        assertThat(parsed.get(0)).isEqualTo("SELECT\n* FROM \n apples");
        assertThat(parsed.get(1)).isEqualTo("SELECT 1 FROM oranges");
        assertThat(parsed.get(2)).isEqualTo("INSERT INTO v (x, y) VALUES\n(SELECT a, b FROM q)");
    }

    public void testParseLineComment() throws Exception {
        String statement = "--LINE COMMENT\n--LINE COMMENT\nSELECT\n* FROM \n apples;\n"
                + "--LINE COMMENT\nSELECT 1 FROM oranges;\n\n\nINSERT INTO v (x, y) VALUES\n"
                + "(SELECT a, b FROM q);--LINE COMMENT\n--LINE COMMENT";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.toStatements(in);
        assertThat(parsed).hasSize(3);
        assertThat(parsed.get(0)).isEqualTo("SELECT\n* FROM \n apples");
        assertThat(parsed.get(1)).isEqualTo("SELECT 1 FROM oranges");
        assertThat(parsed.get(2)).isEqualTo("INSERT INTO v (x, y) VALUES\n(SELECT a, b FROM q)");
    }

    public void testParseBlockComment() throws Exception {
        String statement = "/*BLOCK COMMENT*/\n/*******\n * BLOCK COMMENT\n *******/\n"
                + "SELECT\n* FROM \n apples;/*BLOCK COMMENT*/SELECT 1 FROM oranges;"
                + "/* INSERT INTO x (x, z) VALUES (1, 2, 3);*/"
                + "\n\n\nINSERT INTO v (x, y) VALUES\n(SELECT a, b FROM q);";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.toStatements(in);
        assertThat(parsed).hasSize(3);
        assertThat(parsed.get(0)).isEqualTo("SELECT\n* FROM \n apples");
        assertThat(parsed.get(1)).isEqualTo("SELECT 1 FROM oranges");
        assertThat(parsed.get(2)).isEqualTo("INSERT INTO v (x, y) VALUES\n(SELECT a, b FROM q)");
    }
}
