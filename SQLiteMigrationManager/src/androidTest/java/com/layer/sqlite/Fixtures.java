/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/10/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.layer.sqlite.datasource.DataSource;
import com.layer.sqlite.migrations.CodeMigration;
import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.migrations.StreamMigration;
import com.layer.sqlite.schema.Schema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class Fixtures {
    public static void assertStreamNotNull(Schema schema) throws Exception {
        InputStream in = null;
        try {
            in = schema.getStream();
            assertThat(in).isNotNull();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void assertStreamNotNull(Migration migration) throws Exception {
        assertThat(migration).isInstanceOf(StreamMigration.class);
        InputStream in = null;
        try {
            in = ((StreamMigration) migration).getStream();
            assertThat(in).isNotNull();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static SQLiteDatabase getDatabase(Context context) {
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, null, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {

            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

            }
        };

        return helper.getWritableDatabase();
    }

    public static SQLiteMigrationManager getMigrationManagerMockDataSource() {
        return (new SQLiteMigrationManager()).addDataSource(mockBananaDataSource());
    }

    public static DataSource mockDataSource(final String schemaSql,
                                            final String[] migrationNames, final String[] migrationSql) {

        return new DataSource() {
            @Override
            public boolean hasSchema() {
                return schemaSql != null;
            }

            @Override
            public Schema getSchema() {
                if (schemaSql == null) {
                    return null;
                }

                return new Schema("schema.sql") {
                    @Override
                    public InputStream getStream() {
                        try {
                            return new ByteArrayInputStream(schemaSql.getBytes("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }

            @Override
            public List<Migration> getMigrations() {
                LinkedList<Migration> ms = new LinkedList<Migration>();
                for (int i = 0; i < migrationNames.length; i++) {
                    final String name = migrationNames[i];
                    final String sql = migrationSql[i];

                    ms.add(new StreamMigration(name) {
                        @Override
                        public InputStream getStream() {
                            try {
                                return new ByteArrayInputStream(sql.getBytes("UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
                }
                return ms;
            }
        };
    }

    public static DataSource mockBananaDataSource() {
        String schemaSql = "-- Versioning\n"
                + "\n"
                + "CREATE TABLE schema_migrations (\n"
                + "  version INTEGER UNIQUE NOT NULL\n"
                + ");\n"
                + "\n"
                + "INSERT INTO schema_migrations(version) VALUES (1402070000);";

        String name1 = "1402070000_Origin.sql";
        String migration1 = "-- Versioning\n"
                + "\n"
                + "CREATE TABLE schema_migrations (\n"
                + "  version INTEGER UNIQUE NOT NULL\n"
                + ");";

        String name2 = "1402070001_CreateTableBananas.sql";
        String migration2 = "CREATE TABLE bananas (\n"
                + "    name TEXT\n"
                + ");";

        String name3 = "1402070002_InsertWhiteYellowIntoBananas.sql";
        String migration3 = "INSERT INTO bananas (name) VALUES ('white');\n"
                + "\n"
                + "INSERT INTO bananas (name) VALUES ('yellow');";

        String name4 = "1402070003_AlterBananasAddRipeness.sql";
        String migration4 = "ALTER TABLE bananas ADD ripeness NUMERIC;";

        String name5 = "1402070004_UpdateBananasSetRipeness.sql";
        String migration5 = "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n"
                + "\n"
                + "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';";

        String name6 = "1402070005_InsertGreenBrownIntoBananas.sql";
        String migration6 = "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n"
                + "\n"
                + "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);";

        String name7 = "1402070006_DeleteWhiteFromBananas.sql";
        String migration7 = "DELETE FROM bananas WHERE name = 'white';";

        return mockDataSource(schemaSql,
                new String[]{name1, name2, name3, name4, name5, name6, name7},
                new String[]{migration1, migration2, migration3, migration4, migration5,
                        migration6, migration7}
        );
    }

    public static DataSource mockBananaDataSourceNoSchemaCreatesTable() {
        String schemaSql = null;

        String name1 = "1402070000_Origin.sql";
        String migration1 = "-- Versioning\n"
                + "\n"
                + "CREATE TABLE schema_migrations (\n"
                + "  version INTEGER UNIQUE NOT NULL\n"
                + ");";

        String name2 = "1402070001_CreateTableBananas.sql";
        String migration2 = "CREATE TABLE bananas (\n"
                + "    name TEXT\n"
                + ");";

        String name3 = "1402070002_InsertWhiteYellowIntoBananas.sql";
        String migration3 = "INSERT INTO bananas (name) VALUES ('white');\n"
                + "\n"
                + "INSERT INTO bananas (name) VALUES ('yellow');";

        String name4 = "1402070003_AlterBananasAddRipeness.sql";
        String migration4 = "ALTER TABLE bananas ADD ripeness NUMERIC;";

        String name5 = "1402070004_UpdateBananasSetRipeness.sql";
        String migration5 = "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n"
                + "\n"
                + "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';";

        String name6 = "1402070005_InsertGreenBrownIntoBananas.sql";
        String migration6 = "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n"
                + "\n"
                + "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);";

        String name7 = "1402070006_DeleteWhiteFromBananas.sql";
        String migration7 = "DELETE FROM bananas WHERE name = 'white';";

        return mockDataSource(schemaSql,
                new String[]{name1, name2, name3, name4, name5, name6, name7},
                new String[]{migration1, migration2, migration3, migration4, migration5,
                        migration6, migration7}
        );
    }

    public static DataSource mockBananaDataSourceSchemaNoTable() {
        String schemaSql = "-- Versioning\n"
                + "\n"
                + "CREATE TABLE schema_migrations (\n"
                + "  version INTEGER UNIQUE NOT NULL\n"
                + ");\n"
                + "\n"
                + "INSERT INTO schema_migrations(version) VALUES (1402070000);";

        String name2 = "1402070001_CreateTableBananas.sql";
        String migration2 = "CREATE TABLE bananas (\n"
                + "    name TEXT\n"
                + ");";

        String name3 = "1402070002_InsertWhiteYellowIntoBananas.sql";
        String migration3 = "INSERT INTO bananas (name) VALUES ('white');\n"
                + "\n"
                + "INSERT INTO bananas (name) VALUES ('yellow');";

        String name4 = "1402070003_AlterBananasAddRipeness.sql";
        String migration4 = "ALTER TABLE bananas ADD ripeness NUMERIC;";

        String name5 = "1402070004_UpdateBananasSetRipeness.sql";
        String migration5 = "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n"
                + "\n"
                + "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';";

        String name6 = "1402070005_InsertGreenBrownIntoBananas.sql";
        String migration6 = "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n"
                + "\n"
                + "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);";

        String name7 = "1402070006_DeleteWhiteFromBananas.sql";
        String migration7 = "DELETE FROM bananas WHERE name = 'white';";

        return mockDataSource(schemaSql,
                new String[]{name2, name3, name4, name5, name6, name7},
                new String[]{migration2, migration3, migration4, migration5,
                        migration6, migration7}
        );
    }

    public static DataSource mockBananaDataSourceNoSchemaNoTable() {
        String schemaSql = null;

        String name2 = "1402070001_CreateTableBananas.sql";
        String migration2 = "CREATE TABLE bananas (\n"
                + "    name TEXT\n"
                + ");";

        String name3 = "1402070002_InsertWhiteYellowIntoBananas.sql";
        String migration3 = "INSERT INTO bananas (name) VALUES ('white');\n"
                + "\n"
                + "INSERT INTO bananas (name) VALUES ('yellow');";

        String name4 = "1402070003_AlterBananasAddRipeness.sql";
        String migration4 = "ALTER TABLE bananas ADD ripeness NUMERIC;";

        String name5 = "1402070004_UpdateBananasSetRipeness.sql";
        String migration5 = "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n"
                + "\n"
                + "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';";

        String name6 = "1402070005_InsertGreenBrownIntoBananas.sql";
        String migration6 = "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n"
                + "\n"
                + "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);";

        String name7 = "1402070006_DeleteWhiteFromBananas.sql";
        String migration7 = "DELETE FROM bananas WHERE name = 'white';";

        return mockDataSource(schemaSql,
                new String[]{name2, name3, name4, name5, name6, name7},
                new String[]{migration2, migration3, migration4, migration5,
                        migration6, migration7}
        );
    }

    public static DataSource mockBananaDataSourceNoSchemaNoTable2() {
        String schemaSql = null;

        String name2 = "1402070001_CreateTableBananas.sql";
        String migration2 = "CREATE TABLE bananas (\n"
                + "    name TEXT\n"
                + ");";

        String name3 = "1402070002_InsertWhiteYellowIntoBananas.sql";
        String migration3 = "INSERT INTO bananas (name) VALUES ('white');\n"
                + "\n"
                + "INSERT INTO bananas (name) VALUES ('yellow');";

        String name4 = "1402070003_AlterBananasAddRipeness.sql";
        String migration4 = "ALTER TABLE bananas ADD ripeness NUMERIC;";

        String name5 = "1402070004_UpdateBananasSetRipeness.sql";
        String migration5 = "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n"
                + "\n"
                + "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';";

        String name6 = "1402070005_InsertGreenBrownIntoBananas.sql";
        String migration6 = "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n"
                + "\n"
                + "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);";

        String name7 = "1402070006_DeleteWhiteFromBananas.sql";
        String migration7 = "DELETE FROM bananas WHERE name = 'white';";

        String name8 = "1402070007_InsertSpottedIntoBananas.sql";
        String migration8 = "INSERT INTO bananas (name, ripeness) VALUES ('spotted', 75);";

        return mockDataSource(schemaSql,
                new String[]{name2, name3, name4, name5, name6, name7, name8},
                new String[]{migration2, migration3, migration4, migration5,
                        migration6, migration7, migration8}
        );
    }

    public static CodeMigration codeMigration(String name, final String sql) {
        return new CodeMigration(name) {
            @Override
            public void execute(SQLDatabase db) throws IOException {
                List<String> statements = SQLParser.Statements.fromStream(new ByteArrayInputStream(sql.getBytes()));
                SQLParser.Execute.statements(db, statements);
            }
        };
    }

    public static DataSource mockCodeBananaDataSource() {
        return new DataSource() {
            @Override
            public boolean hasSchema() {
                return false;
            }

            @Override
            public Schema getSchema() {
                return null;
            }

            @Override
            public List<Migration> getMigrations() {
                List<Migration> codeMigrations = new LinkedList<Migration>();
                codeMigrations.add(codeMigration("1402070000_Origin.sql", "CREATE TABLE schema_migrations (\n" +
                        "  version INTEGER UNIQUE NOT NULL\n" +
                        ");"));
                codeMigrations.add(codeMigration("1402070001_CreateTableBananas.sql", "CREATE TABLE bananas (\n" +
                        "    name TEXT\n" +
                        ");"));
                codeMigrations.add(codeMigration("1402070002_InsertWhiteYellowIntoBananas.sql", "INSERT INTO bananas (name) VALUES ('white');\n" +
                        "\n" +
                        "INSERT INTO bananas (name) VALUES ('yellow');"));
                codeMigrations.add(codeMigration("1402070003_AlterBananasAddRipeness.sql", "ALTER TABLE bananas ADD ripeness NUMERIC;"));
                codeMigrations.add(codeMigration("1402070004_UpdateBananasSetRipeness.sql", "UPDATE bananas SET ripeness = 1 WHERE name = 'white';\n" +
                        "\n" +
                        "UPDATE bananas SET ripeness = 50 WHERE name = 'yellow';"));
                codeMigrations.add(codeMigration("1402070005_InsertGreenBrownIntoBananas.sql", "INSERT INTO bananas (name, ripeness) VALUES ('brown', 80);\n" +
                        "\n" +
                        "INSERT INTO bananas (name, ripeness) VALUES ('green', 0);"));
                codeMigrations.add(codeMigration("1402070006_DeleteWhiteFromBananas.sql", "DELETE FROM bananas WHERE name = 'white';"));
                return codeMigrations;
            }
        };

    }

}
