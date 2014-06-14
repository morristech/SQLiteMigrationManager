/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/8/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite;

import com.foundationdb.sql.StandardException;
import com.foundationdb.sql.parser.StatementNode;
import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.schema.Schema;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
public class SQLParser {
    public static void execute(SQLiteDatabase db, Schema schema)
            throws IOException, SQLParserException {
        execute(db, schema.getStream());
    }

    public static void execute(SQLiteDatabase db, Migration migration)
            throws IOException, SQLParserException {
        execute(db, migration.getStream());
    }

    private static void execute(SQLiteDatabase db, InputStream in)
            throws IOException, SQLParserException {
        try {
            execute(db, toStatements(in));
        } finally {
            in.close();
        }
    }

    /**
     * Reads the entire InputStream into a String, and returns the list of individual SQL
     * statements within.
     *
     * @param in InputStream to parse into Statements.
     * @return A list of Statements parsed from the given InputStream.
     * @throws StandardException
     */
    public static List<String> toStatements(InputStream in) throws SQLParserException {
        return toStatements(new Scanner(in, "UTF-8").useDelimiter("\\A").next());
    }

    /**
     * Parses a SQL string into a list of individual SQL statements.
     *
     * @param sqlText Sting to parse into Statements.
     * @return A list of Statements parsed from the given String.
     * @throws StandardException
     */
    public static List<String> toStatements(String sqlText) throws SQLParserException {
        try {
            List<StatementNode> nodes = (new com.foundationdb.sql.parser.SQLParser())
                    .parseStatements(sqlText);
            List<String> statements = new LinkedList<String>();
            for (StatementNode node : nodes) {
                statements.add(sqlText.substring(node.getBeginOffset(), node.getEndOffset() + 1));
            }
            return statements;
        } catch (StandardException e) {
            e.printStackTrace();
            throw new SQLParserException(e.getMessage());
        }
    }

    /**
     * Executes each SQL statement in the statements list.
     *
     * @param db         The database on which to execute statements.
     * @param statements The list of SQL statement strings to execute.
     * @throws IllegalArgumentException If a statement cannot be parsed.
     */
    public static void execute(SQLiteDatabase db, List<String> statements) {
        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    public static class SQLParserException extends Exception {
        public SQLParserException(String msg) {
            super(msg);
        }
    }
}