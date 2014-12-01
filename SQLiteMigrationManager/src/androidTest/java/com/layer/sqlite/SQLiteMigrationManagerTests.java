package com.layer.sqlite;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;

import com.layer.sqlite.datasource.ResourceDataSource;
import com.layer.sqlite.migrations.Migration;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.layer.sqlite.Fixtures.assertStreamNotNull;
import static com.layer.sqlite.Fixtures.getDatabase;
import static com.layer.sqlite.Fixtures.getMigrationManagerMockDataSource;
import static com.layer.sqlite.Fixtures.mockBananaDataSourceNoSchemaCreatesTable;
import static com.layer.sqlite.Fixtures.mockBananaDataSourceNoSchemaNoTable;
import static com.layer.sqlite.Fixtures.mockBananaDataSourceNoSchemaNoTable2;
import static com.layer.sqlite.Fixtures.mockBananaDataSourceSchemaNoTable;
import static com.layer.sqlite.SQLiteMigrationManager.BootstrapAction;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

public class SQLiteMigrationManagerTests extends AndroidTestCase {
    public void testAddDataSource() throws Exception {
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        assertFalse(migrationManager.hasSchema());
        ResourceDataSource source = new ResourceDataSource(getContext(), "schema/schema.sql", "migrations");
        migrationManager.addDataSource(source);
        assertTrue(migrationManager.hasSchema());
    }

    public void testHasMigrationsTableCreateMigrationsTable() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = getMigrationManagerMockDataSource();
        assertFalse(migrationManager.hasMigrationsTable(db));
        migrationManager.createMigrationsTable(db);
        assertTrue(migrationManager.hasMigrationsTable(db));
    }

    public void testHasSchemaGetSchema() throws Exception {
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        assertFalse(migrationManager.hasSchema());
        ResourceDataSource source = new ResourceDataSource(getContext(), "schema/schema.sql", "migrations");
        migrationManager.addDataSource(source);
        assertTrue(migrationManager.hasSchema());
        assertThat(migrationManager.getSchema()).isNotNull();
        assertStreamNotNull(migrationManager.getSchema());
    }

    public void testApplySchema() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        assertFalse(migrationManager.hasSchema());

        try {
            migrationManager.applySchema(db);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No schemas in DataSource set.");
        }

        ResourceDataSource source = new ResourceDataSource(getContext(), "schema/schema.sql", "migrations");
        migrationManager.addDataSource(source);
        assertTrue(migrationManager.hasSchema());

        try {
            migrationManager.getCurrentVersion(db);
            failBecauseExceptionWasNotThrown(SQLException.class);
        } catch (SQLException e) {
            assertThat(e.getMessage()).contains("schema_migrations");
        }

        migrationManager.applySchema(db);
        assertThat(migrationManager.getCurrentVersion(db)).isEqualTo(1402070000);
    }

    public void testGetMigrations() throws Exception {
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        try {
            migrationManager.getMigrations();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }
        ResourceDataSource source = new ResourceDataSource(getContext(), "schema/schema.sql", "migrations");
        migrationManager.addDataSource(source);

        List<Migration> migrations = migrationManager.getMigrations();
        assertThat(migrations).hasSize(7);

        assertThat(migrations.get(0).getVersion()).isEqualTo(1402070000);
        assertThat(migrations.get(1).getVersion()).isEqualTo(1402070001);
        assertThat(migrations.get(2).getVersion()).isEqualTo(1402070002);
        assertThat(migrations.get(3).getVersion()).isEqualTo(1402070003);
        assertThat(migrations.get(4).getVersion()).isEqualTo(1402070004);
        assertThat(migrations.get(5).getVersion()).isEqualTo(1402070005);
        assertThat(migrations.get(6).getVersion()).isEqualTo(1402070006);

        assertThat(migrations.get(0).getDescription()).isEqualTo("Origin");
        assertThat(migrations.get(1).getDescription()).isEqualTo("CreateTableBananas");
        assertThat(migrations.get(2).getDescription()).isEqualTo("InsertWhiteYellowIntoBananas");
        assertThat(migrations.get(3).getDescription()).isEqualTo("AlterBananasAddRipeness");
        assertThat(migrations.get(4).getDescription()).isEqualTo("UpdateBananasSetRipeness");
        assertThat(migrations.get(5).getDescription()).isEqualTo("InsertGreenBrownIntoBananas");
        assertThat(migrations.get(6).getDescription()).isEqualTo("DeleteWhiteFromBananas");

        assertStreamNotNull(migrations.get(0));
        assertStreamNotNull(migrations.get(1));
        assertStreamNotNull(migrations.get(2));
        assertStreamNotNull(migrations.get(3));
        assertStreamNotNull(migrations.get(4));
        assertStreamNotNull(migrations.get(5));
        assertStreamNotNull(migrations.get(6));
    }

    public void testGetOriginVersion() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = getMigrationManagerMockDataSource();

        try {
            migrationManager.getOriginVersion(db);
            failBecauseExceptionWasNotThrown(SQLException.class);
        } catch (SQLException e) {
            assertThat(e.getMessage()).contains("schema_migrations");
        }

        migrationManager.createMigrationsTable(db);
        assertThat(migrationManager.getOriginVersion(db)).isEqualTo(
                SQLiteMigrationManager.NO_VERSIONS);
        migrationManager.insertVersion(db, 100L);
        assertThat(migrationManager.getOriginVersion(db)).isEqualTo(100L);
        migrationManager.insertVersion(db, 50L);
        assertThat(migrationManager.getOriginVersion(db)).isEqualTo(50L);
        migrationManager.insertVersion(db, 500L);
        assertThat(migrationManager.getOriginVersion(db)).isEqualTo(50L);
    }

    public void testGetCurrentVersion() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = getMigrationManagerMockDataSource();

        try {
            migrationManager.getCurrentVersion(db);
            failBecauseExceptionWasNotThrown(SQLException.class);
        } catch (SQLException e) {
            assertThat(e.getMessage()).contains("schema_migrations");
        }

        migrationManager.createMigrationsTable(db);
        assertThat(migrationManager.getCurrentVersion(db))
                .isEqualTo(SQLiteMigrationManager.NO_VERSIONS);
        migrationManager.insertVersion(db, 100L);
        assertThat(migrationManager.getCurrentVersion(db)).isEqualTo(100L);
        migrationManager.insertVersion(db, 50L);
        assertThat(migrationManager.getCurrentVersion(db)).isEqualTo(100L);
        migrationManager.insertVersion(db, 500L);
        assertThat(migrationManager.getCurrentVersion(db)).isEqualTo(500L);
    }

    public void testInsertVersionGetAppliedVersions() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = getMigrationManagerMockDataSource();

        try {
            migrationManager.getAppliedVersions(db);
            failBecauseExceptionWasNotThrown(SQLException.class);
        } catch (SQLException e) {
            assertThat(e.getMessage()).contains("schema_migrations");
        }

        migrationManager.createMigrationsTable(db);
        assertThat(migrationManager.getAppliedVersions(db)).isEmpty();

        migrationManager.insertVersion(db, 100L);
        migrationManager.insertVersion(db, 50L);
        migrationManager.insertVersion(db, 500L);
        Set<Long> appliedVersions = migrationManager.getAppliedVersions(db);
        assertThat(appliedVersions).hasSize(3);
        assertThat(appliedVersions.toArray()[0]).isEqualTo(50L);
        assertThat(appliedVersions.toArray()[1]).isEqualTo(100L);
        assertThat(appliedVersions.toArray()[2]).isEqualTo(500L);
    }

    public void testGetPendingVersions() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        try {
            migrationManager.getPendingMigrations(db);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }

        ResourceDataSource source = new ResourceDataSource(getContext(), "schema/schema.sql", "migrations");
        migrationManager.addDataSource(source);

        // With a blank table, all migrations are pending.
        migrationManager.createMigrationsTable(db);
        final List<Migration> m1 = migrationManager.getPendingMigrations(db);
        assertThat(m1).hasSize(7);
        assertThat(m1.get(0).getVersion()).isEqualTo(1402070000);
        assertThat(m1.get(1).getVersion()).isEqualTo(1402070001);
        assertThat(m1.get(2).getVersion()).isEqualTo(1402070002);
        assertThat(m1.get(3).getVersion()).isEqualTo(1402070003);
        assertThat(m1.get(4).getVersion()).isEqualTo(1402070004);
        assertThat(m1.get(5).getVersion()).isEqualTo(1402070005);
        assertThat(m1.get(6).getVersion()).isEqualTo(1402070006);

        assertThat(m1.get(0).getDescription()).isEqualTo("Origin");
        assertThat(m1.get(1).getDescription()).isEqualTo("CreateTableBananas");
        assertThat(m1.get(2).getDescription()).isEqualTo("InsertWhiteYellowIntoBananas");
        assertThat(m1.get(3).getDescription()).isEqualTo("AlterBananasAddRipeness");
        assertThat(m1.get(4).getDescription()).isEqualTo("UpdateBananasSetRipeness");
        assertThat(m1.get(5).getDescription()).isEqualTo("InsertGreenBrownIntoBananas");
        assertThat(m1.get(6).getDescription()).isEqualTo("DeleteWhiteFromBananas");

        assertStreamNotNull(m1.get(0));
        assertStreamNotNull(m1.get(1));
        assertStreamNotNull(m1.get(2));
        assertStreamNotNull(m1.get(3));
        assertStreamNotNull(m1.get(4));
        assertStreamNotNull(m1.get(5));
        assertStreamNotNull(m1.get(6));

        // With a non-empty migrations table, only greater than origin and not applied are pending.
        migrationManager.insertVersion(db, 1402070001L); // origin
        migrationManager.insertVersion(db, 1402070004L); // applied
        final List<Migration> m2 = migrationManager.getPendingMigrations(db);
        assertThat(m2).hasSize(4);
        assertThat(m2.get(0).getVersion()).isEqualTo(1402070002);
        assertThat(m2.get(1).getVersion()).isEqualTo(1402070003);
        assertThat(m2.get(2).getVersion()).isEqualTo(1402070005);
        assertThat(m2.get(3).getVersion()).isEqualTo(1402070006);

        assertThat(m2.get(0).getDescription()).isEqualTo("InsertWhiteYellowIntoBananas");
        assertThat(m2.get(1).getDescription()).isEqualTo("AlterBananasAddRipeness");
        assertThat(m2.get(2).getDescription()).isEqualTo("InsertGreenBrownIntoBananas");
        assertThat(m2.get(3).getDescription()).isEqualTo("DeleteWhiteFromBananas");

        assertStreamNotNull(m2.get(0));
        assertStreamNotNull(m2.get(1));
        assertStreamNotNull(m2.get(2));
        assertStreamNotNull(m2.get(3));
    }

    public void testManageSchemaActionNone() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        try {
            migrationManager.manageSchema(db, BootstrapAction.NONE);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }

        // Create a DataSource with no schema but with a table-creating migration.
        migrationManager.addDataSource(mockBananaDataSourceNoSchemaCreatesTable());
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.NONE))
                .isEqualTo(7);
        assertTrue(migrationManager.hasMigrationsTable(db));

        // Verify applied versions.
        Cursor c = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c.getCount()).isEqualTo(7);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070000);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070001);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070002);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070003);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070004);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070005);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070006);
        c.close();

        //Verify inserted data
        Cursor c2 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c2.getCount()).isEqualTo(3);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("yellow");
        assertThat(c2.getLong(1)).isEqualTo(50);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("brown");
        assertThat(c2.getLong(1)).isEqualTo(80);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("green");
        assertThat(c2.getLong(1)).isEqualTo(0);
        c2.close();

        assertThat(migrationManager.getOriginVersion(db))
                .isEqualTo(1402070000);

        assertThat(migrationManager.getCurrentVersion(db))
                .isEqualTo(1402070006);

        // Verify that 0 migrations remain.
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.NONE))
                .isEqualTo(0);
    }

    public void testManageSchemaActionApplySchema() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        try {
            migrationManager.manageSchema(db, BootstrapAction.NONE);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }

        // Create a DataSource with a schema and no table-creating migration.
        migrationManager.addDataSource(mockBananaDataSourceSchemaNoTable());
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.APPLY_SCHEMA))
                .isEqualTo(6);
        assertTrue(migrationManager.hasMigrationsTable(db));

        // Verify applied versions.
        Cursor c = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c.getCount()).isEqualTo(7);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070000);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070001);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070002);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070003);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070004);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070005);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070006);
        c.close();

        //Verify inserted data
        Cursor c2 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c2.getCount()).isEqualTo(3);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("yellow");
        assertThat(c2.getLong(1)).isEqualTo(50);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("brown");
        assertThat(c2.getLong(1)).isEqualTo(80);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("green");
        assertThat(c2.getLong(1)).isEqualTo(0);
        c2.close();

        assertThat(migrationManager.getOriginVersion(db))
                .isEqualTo(1402070000);

        assertThat(migrationManager.getCurrentVersion(db))
                .isEqualTo(1402070006);

        // Verify that 0 migrations remain.
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.APPLY_SCHEMA))
                .isEqualTo(0);
    }

    public void testManageSchemaActionCreateTable() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        try {
            migrationManager.manageSchema(db, BootstrapAction.NONE);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }

        // Create a DataSource with neither schema nor table-creating migration.
        migrationManager.addDataSource(mockBananaDataSourceNoSchemaNoTable());
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.CREATE_MIGRATIONS_TABLE))
                .isEqualTo(6);
        assertTrue(migrationManager.hasMigrationsTable(db));

        // Verify applied versions.
        Cursor c = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c.getCount()).isEqualTo(6);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070001);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070002);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070003);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070004);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070005);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070006);
        c.close();

        //Verify inserted data
        Cursor c2 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c2.getCount()).isEqualTo(3);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("yellow");
        assertThat(c2.getLong(1)).isEqualTo(50);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("brown");
        assertThat(c2.getLong(1)).isEqualTo(80);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("green");
        assertThat(c2.getLong(1)).isEqualTo(0);
        c2.close();

        assertThat(migrationManager.getOriginVersion(db))
                .isEqualTo(1402070001);

        assertThat(migrationManager.getCurrentVersion(db))
                .isEqualTo(1402070006);

        // Verify that 0 migrations remain.
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.CREATE_MIGRATIONS_TABLE))
                .isEqualTo(0);
    }

    public void testChainedManage() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());

        (new SQLiteMigrationManager())
                .addDataSource(mockBananaDataSourceNoSchemaNoTable())
                .manageSchema(db, BootstrapAction.CREATE_MIGRATIONS_TABLE);

        // Verify applied versions.
        Cursor c = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c.getCount()).isEqualTo(6);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070001);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070002);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070003);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070004);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070005);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070006);
        c.close();

        //Verify inserted data
        Cursor c2 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c2.getCount()).isEqualTo(3);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("yellow");
        assertThat(c2.getLong(1)).isEqualTo(50);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("brown");
        assertThat(c2.getLong(1)).isEqualTo(80);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("green");
        assertThat(c2.getLong(1)).isEqualTo(0);
        c2.close();
    }

    public void testUpgrade() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        migrationManager.addDataSource(mockBananaDataSourceNoSchemaNoTable());

        assertThat(migrationManager.manageSchema(
                db, BootstrapAction.CREATE_MIGRATIONS_TABLE)).isEqualTo(6);

        // Verify applied versions.
        Cursor c = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c.getCount()).isEqualTo(6);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070001);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070002);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070003);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070004);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070005);
        c.moveToNext();
        assertThat(c.getLong(0)).isEqualTo(1402070006);
        c.close();

        //Verify inserted data
        Cursor c2 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c2.getCount()).isEqualTo(3);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("yellow");
        assertThat(c2.getLong(1)).isEqualTo(50);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("brown");
        assertThat(c2.getLong(1)).isEqualTo(80);
        c2.moveToNext();
        assertThat(c2.getString(0)).isEqualTo("green");
        assertThat(c2.getLong(1)).isEqualTo(0);
        c2.close();

        assertThat(migrationManager.manageSchema(
                db, BootstrapAction.CREATE_MIGRATIONS_TABLE)).isEqualTo(0);

        // "Upgrade" with an additional migration
        SQLiteMigrationManager migrationManager2 = new SQLiteMigrationManager();
        migrationManager2.addDataSource(mockBananaDataSourceNoSchemaNoTable2());

        assertThat(migrationManager2.manageSchema(
                db, BootstrapAction.CREATE_MIGRATIONS_TABLE)).isEqualTo(1);

        // Verify applied versions.
        Cursor c3 = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
        assertThat(c3.getCount()).isEqualTo(7);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070001);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070002);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070003);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070004);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070005);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070006);
        c3.moveToNext();
        assertThat(c3.getLong(0)).isEqualTo(1402070007);
        c3.close();

        //Verify inserted data
        Cursor c4 = db.rawQuery("SELECT name, ripeness FROM bananas ORDER BY _ROWID_", null);
        assertThat(c4.getCount()).isEqualTo(4);
        c4.moveToNext();
        assertThat(c4.getString(0)).isEqualTo("yellow");
        assertThat(c4.getLong(1)).isEqualTo(50);
        c4.moveToNext();
        assertThat(c4.getString(0)).isEqualTo("brown");
        assertThat(c4.getLong(1)).isEqualTo(80);
        c4.moveToNext();
        assertThat(c4.getString(0)).isEqualTo("green");
        assertThat(c4.getLong(1)).isEqualTo(0);
        c4.moveToNext();
        assertThat(c4.getString(0)).isEqualTo("spotted");
        assertThat(c4.getLong(1)).isEqualTo(75);
        c4.close();
    }

    public void testIsDowngrade() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        // Verify not a downgrade when there is no schema table
        assertFalse(migrationManager.isDowngrade(db));

        // Create a DataSource with a schema and no table-creating migration.
        migrationManager.addDataSource(mockBananaDataSourceSchemaNoTable());
        assertThat(migrationManager
                .manageSchema(db, BootstrapAction.APPLY_SCHEMA))
                .isEqualTo(6);
        assertTrue(migrationManager.hasMigrationsTable(db));

        // Verify applied versions.
        assertThat(migrationManager.getCurrentVersion(db)).isEqualTo(1402070006L);
        assertFalse(migrationManager.isDowngrade(db));

        // Add a version and verify that this manager's migrations are a downgrade.
        migrationManager.insertVersion(db, 1402070007L);
        assertTrue(migrationManager.isDowngrade(db));
    }

    public void testUpgradeOpenHelper() throws Exception {

        final SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
        final String dbName = UUID.randomUUID().toString();

        SQLiteOpenHelper openHelper = new SQLiteOpenHelper(getContext(), dbName, null, 1) {
            @Override
            public void onConfigure(SQLiteDatabase db) {
                try {
                    migrationManager.manageSchema(db, BootstrapAction.CREATE_MIGRATIONS_TABLE);
                } catch (IOException e) {
                    fail("IOException thrown: " + e.getMessage());
                }
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

            }
        };

        migrationManager.addDataSource(mockBananaDataSourceNoSchemaNoTable());
        SQLiteDatabase db1 = openHelper.getWritableDatabase();
        assertThat(migrationManager.getMigrations()).hasSize(6);
        db1.close();
        openHelper.close();

        // "Upgrade" with an additional migration
        migrationManager.addDataSource(mockBananaDataSourceNoSchemaNoTable2());
        SQLiteDatabase db2 = openHelper.getWritableDatabase();
        assertThat(migrationManager.getMigrations()).hasSize(7);
        db2.close();
        openHelper.close();
    }
}
