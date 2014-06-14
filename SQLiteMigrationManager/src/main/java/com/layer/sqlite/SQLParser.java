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

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
public class SQLParser {
    public static void execute(SQLiteDatabase db, Schema schema)
            throws IOException, StandardException {
        execute(db, schema.getStream());
    }

    public static void execute(SQLiteDatabase db, Migration migration)
            throws IOException, StandardException {
        execute(db, migration.getStream());
    }

    private static void execute(SQLiteDatabase db, InputStream in)
            throws IOException, SQLException, StandardException {
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
    public static List<String> toStatements(InputStream in) throws StandardException {
        return toStatements(new Scanner(in, "UTF-8").useDelimiter("\\A").next());
    }

    /**
     * Parses a SQL string into a list of individual SQL statements.
     *
     * @param sqlText Sting to parse into Statements.
     * @return A list of Statements parsed from the given String.
     * @throws StandardException
     */
    public static List<String> toStatements(String sqlText) throws StandardException {
        List<StatementNode> nodes = (new com.foundationdb.sql.parser.SQLParser())
                .parseStatements(sqlText);
        List<String> statements = new LinkedList<String>();
        for (StatementNode node : nodes) {
            statements.add(sqlText.substring(node.getBeginOffset(), node.getEndOffset() + 1));
        }
        return statements;
    }

    /**
     * Executes each SQL statement in the statements list.
     *
     * @param db         The database on which to execute statements.
     * @param statements The list of SQL statement strings to execute.
     * @throws java.io.IOException
     * @throws IllegalArgumentException If a statement cannot be parsed.
     */
    public static void execute(SQLiteDatabase db, List<String> statements)
            throws IOException, SQLException {
        for (String statement : statements) {
            db.execSQL(statement);
        }
    }
}