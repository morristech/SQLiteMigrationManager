package com.layer.sqlite;

import android.os.StrictMode;
import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class SQLParserTests extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    public void testParseSingleNoComments() throws Exception {
        String statement = "statement1;\ncontinued;";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.Statements.fromStream(in);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isEqualTo("statement1;\ncontinued;");
    }

    public void testParseMultipleNoComments() throws Exception {
        String statement = "statement1;\n\nstatement2;";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.Statements.fromStream(in);
        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0)).isEqualTo("statement1;");
        assertThat(parsed.get(1)).isEqualTo("statement2;");
    }

// TODO: make SQLParser handle statements not divided by empty newlines
//    public void testParseMultipleNoCommentsNoNewlines() throws Exception {
//        String statement = "statement1;\nstatement2;";
//        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));
//
//        List<String> parsed = SQLParser.Statements.fromStream(in);
//        assertThat(parsed).hasSize(2);
//        assertThat(parsed.get(0)).isEqualTo("statement1;");
//        assertThat(parsed.get(1)).isEqualTo("statement2;");
//    }

    public void testParseLineComment() throws Exception {
        String statement = "statement1;\n  --comment1 \n statement2;";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.Statements.fromStream(in);
        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0)).isEqualTo("statement1;");
        assertThat(parsed.get(1)).isEqualTo("statement2;");
    }

    public void testParseBlockComment() throws Exception {
        String statement
                = "statement1;\n/* comment1 */ /* comment2\n ** continued */\n statement2; /*comment3 /* */continued";
        InputStream in = new ByteArrayInputStream(statement.getBytes("UTF-8"));

        List<String> parsed = SQLParser.Statements.fromStream(in);
        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0)).isEqualTo("statement1;");
        assertThat(parsed.get(1)).isEqualTo("statement2; continued");
    }
}
