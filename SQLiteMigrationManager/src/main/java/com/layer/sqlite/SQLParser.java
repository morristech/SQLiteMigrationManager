/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/8/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite;

import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.schema.Schema;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
public class SQLParser {
    public static void execute(SQLiteDatabase db, Schema schema) throws IOException {
        execute(db, schema.getStream());
    }

    public static void execute(SQLiteDatabase db, Migration migration) throws IOException {
        execute(db, migration.getStream());
    }

    private static void execute(SQLiteDatabase db, InputStream in)
            throws IOException, SQLException {
        try {
            Execute.statements(db, Statements.fromStream(in));
        } finally {
            in.close();
        }
    }

    /**
     * Generates lists of statements for execution from various sources.
     */
    public static class Statements {
        public static List<String> fromStream(InputStream in) {
            String contents = new Scanner(in, "UTF-8").useDelimiter("\\A").next();

            // Remove block comments
            Pattern blockComment = Pattern.compile("/\\*.*?\\*/",
                    Pattern.MULTILINE | Pattern.DOTALL);
            contents = blockComment.matcher(contents).replaceAll("");

            // Remove line comments
            Pattern lineComment = Pattern.compile("^\\s*--.*?$",
                    Pattern.MULTILINE | Pattern.DOTALL);
            contents = lineComment.matcher(contents).replaceAll("");

            // TODO: make a parser smarter than simply looking for empty lines to split on
            return Arrays.asList(contents.split("(\\s*\\r?\\n\\s*){2,}"));
        }
    }

    /**
     * Executes lists of statements.
     */
    private static class Execute {
        private final static Set<String> COMMENT_PREFIXES = new HashSet<String>(
                Arrays.asList("--"));
        private final static Set<String> EXEC_PREFIXES = new HashSet<String>(
                Arrays.asList("ALTER", "ANALYZE", "CREATE", "DELETE", "DROP", "INSERT", "UPDATE"));
        private final static Set<String> QUERY_PREFIXES = new HashSet<String>(
                Arrays.asList("PRAGMA"));

        /**
         * Returns true if the provided statement begins with a prefix from a given prefixes set.
         *
         * @param prefixes
         * @param statement
         * @return
         */
        private static boolean isPrefixMatch(Set<String> prefixes, String statement) {
            String upper = statement.toUpperCase();
            for (String prefix : prefixes) {
                if (upper.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Executes each statement in the statements list.  The actual SQLiteDatabase method used
         * to execute each statement is determined by comparing the beginning of the statement to
         * prefixes in the COMMENT_PREFIXES, EXEC_PREFIXES, and QUERY_PREFIXES sets.
         *
         * @param db         The database on which to execute statements.
         * @param statements The list of statements to execute.
         * @throws java.io.IOException
         * @throws IllegalArgumentException If a statement cannot be parsed.
         */
        public static void statements(SQLiteDatabase db, List<String> statements)
                throws IOException, SQLException {
            for (String statement : statements) {
                statement = statement.trim();

                if (statement.isEmpty()) {
                    // Skip empty statements.
                    continue;
                }

                if (isPrefixMatch(COMMENT_PREFIXES, statement)) {
                    // Skip comments.
                    continue;
                }

                if (isPrefixMatch(EXEC_PREFIXES, statement)) {
                    // Execute.
                    db.execSQL(statement);
                    continue;
                }

                if (isPrefixMatch(QUERY_PREFIXES, statement)) {
                    // Query.
                    db.rawQuery(statement, null);
                    continue;
                }

                throw new IllegalArgumentException("Cannot parse statement: " + statement);
            }
        }
    }
}