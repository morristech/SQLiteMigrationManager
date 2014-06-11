package com.layer.sqlite;

import com.layer.sqlite.datasource.ResourceDataSource;
import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.migrations.ResourceMigration;
import com.layer.sqlite.schema.ResourceSchema;
import com.layer.sqlite.schema.Schema;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import static com.layer.sqlite.Fixtures.assertStreamNotNull;
import static com.layer.sqlite.Fixtures.getDatabase;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

public class ResourceDataSourceTests extends AndroidTestCase {
    public void testInvalidMigrationName() throws Exception {
        try {
            new ResourceMigration("migrations/bad");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/bad.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/_bad.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/0");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/0_");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/0_.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/_.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }

        try {
            new ResourceMigration("migrations/0_bad");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Invalid migration name");
        }
    }

    public void testNonexistentSchema() throws Exception {
        assertFalse(ResourceDataSource.resourceExists("wrong/schema.sql"));
        try {
            new ResourceSchema("wrong/schema.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Could not find");
        }
    }

    public void testValidSchema() throws Exception {
        assertTrue(ResourceDataSource.resourceExists("schema/schema.sql"));
        Schema schema = new ResourceSchema("schema/schema.sql");
        assertStreamNotNull(schema);
    }

    public void testNonexistentNoDescriptionMigration() throws Exception {
        assertFalse(ResourceDataSource.resourceExists("wrong/1402070000.sql"));
        try {
            new ResourceMigration("wrong/1402070000.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Could not find");
        }
    }

    public void testNonexistentDescriptionMigration() throws Exception {
        assertFalse(ResourceDataSource.resourceExists("wrong/1402070000_Origin.sql"));
        try {
            new ResourceMigration("wrong/1402070000_Origin.sql");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Could not find");
        }
    }

    public void testValidMigration() throws Exception {
        assertTrue(ResourceDataSource.resourceExists("migrations/1402070000_Origin.sql"));
        Migration migration = new ResourceMigration("migrations/1402070000_Origin.sql");
        assertThat(migration.getVersion()).isEqualTo(1402070000L);
        assertThat(migration.getDescription()).isEqualTo("Origin");
        assertStreamNotNull(migration);
    }

    public void testManageSchemaResourceDataSource() throws Exception {
        SQLiteDatabase db = getDatabase(getContext());
        SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();

        try {
            migrationManager.manageSchema(db, SQLiteMigrationManager.NoMigrationsTableAction.NONE);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("No DataSources added");
        }

        // Create a DataSource with a schema and no table-creating migration.
        migrationManager.addDataSource(new ResourceDataSource("schema/schema.sql", "migrations"));
        assertThat(migrationManager
                .manageSchema(db, SQLiteMigrationManager.NoMigrationsTableAction.APPLY_SCHEMA))
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
                .manageSchema(db, SQLiteMigrationManager.NoMigrationsTableAction.APPLY_SCHEMA))
                .isEqualTo(0);
    }
}
