/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/4/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.layer.sqlite.datasource.DataSource;
import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.schema.Schema;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SQLiteMigrationManager {
    public static final long NO_VERSIONS = -1;

    /**
     * `BootstrapAction` tells SQLiteMigrationManager which action to take when no
     * `schema_migrations` table is found during a call to manageSchema().
     * <p><ul>
     * <li>NONE: Do nothing.  A SQLException will get thrown if no `schema_migrations` table is
     * created by the first migration.</li>
     * <li>APPLY_SCHEMA: Load and apply a Schema from the DataSource set.</li>
     * <li>CREATE_MIGRATIONS_TABLE: Create the `schema_migrations` table.</li>
     * </ul></p>
     */
    public static enum BootstrapAction {
        NONE,
        APPLY_SCHEMA,
        CREATE_MIGRATIONS_TABLE
    }

    /**
     * DataSources from which to find Schemas and Migrations
     */
    private final Set<DataSource> mDataSources = new HashSet<DataSource>();

    /**
     * Applies pending Migrations in order.  If a migration throws an SQLException, the process is
     * halted at that point, but all previous migrations remain applied.  Behavior when no
     * migrations table is present is controlled by the `BootstrapAction action` parameter.
     *
     * @param db     Database on which to operate.
     * @param action NoSchemaAction action to take when hasMigrationsTable() returns false.
     * @return The number of migrations applied.
     * @see #getOriginVersion(android.database.sqlite.SQLiteDatabase)
     * @see #getMigrations()
     * @see #getAppliedVersions(android.database.sqlite.SQLiteDatabase)
     * @see com.layer.sqlite.SQLiteMigrationManager.BootstrapAction
     */
    public int manageSchema(SQLiteDatabase db, BootstrapAction action) throws IOException {
        int numApplied = 0;

        // Begin an outer transaction.
        db.beginTransaction();

        // Bootstrap if no `schema_migrations` is present.
        if (!hasMigrationsTable(db)) {
            switch (action) {
                case APPLY_SCHEMA:
                    applySchema(db);
                    break;
                case CREATE_MIGRATIONS_TABLE:
                    createMigrationsTable(db);
                    break;
                case NONE:
                default:
                    break;
            }
        }

        // Apply Migrations.
        try {
            for (Migration migration : getPendingMigrations(db)) {
                try {
                    // Begin an inner Migration transaction.
                    db.beginTransaction();

                    // Apply the Migration.
                    SQLParser.execute(db, migration);
                    insertVersion(db, migration.getVersion());
                    numApplied++;

                    // Set the inner Migration transaction successful.
                    db.setTransactionSuccessful();
                } catch (SQLException e) {
                    // Halt the migration process on error.
                    e.printStackTrace();
                    break;
                } finally {
                    // End the inner Migration transaction.
                    db.endTransaction();
                }
            }
            // Set the outer transaction successful.
            db.setTransactionSuccessful();
        } finally {
            // End the outer transaction.
            db.endTransaction();
        }
        return numApplied;
    }

    /**
     * Adds a DataSource to the set of available sources for providing Schema and Migrations.
     *
     * @param dataSource DataSource to add to the set of managed sources.
     * @return `this` for chaining.
     */
    public SQLiteMigrationManager addDataSource(DataSource... dataSource) {
        mDataSources.addAll(Arrays.asList(dataSource));
        return this;
    }

    /**
     * Returns true if the `schema_migrations` table exists.
     *
     * @param db Database to query for the `schema_migrations` table.
     * @return true if the `schema_migrations` table exists.
     */
    public boolean hasMigrationsTable(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{"schema_migrations"});
            return (c.getCount() > 0);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Creates the managed `schema_migrations` table used for tracking applied migrations.
     *
     * @param db Database to create the `schema_migrations` table in.
     * @return `this` for chaining.
     */
    public SQLiteMigrationManager createMigrationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS schema_migrations (" +
                "version INTEGER UNIQUE NOT NULL)");
        return this;
    }

    /**
     * Returns true if any DataSource has a Schema.
     *
     * @return True if any DataSource has a Schema.
     */
    public boolean hasSchema() {
        for (DataSource dataSource : mDataSources) {
            if (dataSource.hasSchema()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first available Schema contained within the DataSource set.
     *
     * @return The first available Schema contained within the DataSource set.
     * @throws java.lang.IllegalStateException When no DataSources have been added.
     */
    public Schema getSchema() throws IllegalStateException {
        if (mDataSources.isEmpty()) {
            throw new IllegalStateException("No DataSources added");
        }

        for (DataSource dataSource : mDataSources) {
            if (dataSource.hasSchema()) {
                return dataSource.getSchema();
            }
        }
        return null;
    }

    /**
     * Bootstraps from the first available Schema found in the DataSource set.
     *
     * @param db Database to bootstrap.
     * @return `this` for chaining.
     * @throws IllegalStateException         If no schemas were found in the DataSource set.
     * @throws java.io.IOException
     * @throws android.database.SQLException
     */
    public SQLiteMigrationManager applySchema(SQLiteDatabase db) throws IOException {
        if (!hasSchema()) {
            throw new IllegalStateException("No schemas in DataSource set.");
        }
        SQLParser.execute(db, getSchema());
        return this;
    }

    /**
     * Generates a sorted list of Migration objects from all DataSources.
     *
     * @return A sorted list of Migrations from the set of DataSources.
     * @throws java.lang.IllegalStateException When no DataSources have been added.
     */
    public List<Migration> getMigrations() throws IllegalStateException {
        if (mDataSources.isEmpty()) {
            throw new IllegalStateException("No DataSources added");
        }

        // Use Sets to prevent duplicate Migrations.
        Set<Migration> migrations = new HashSet<Migration>();
        for (DataSource dataSource : mDataSources) {
            migrations.addAll(dataSource.getMigrations());
        }

        // Return a sorted List.
        List<Migration> sortedMigrations = new LinkedList<Migration>(migrations);
        Collections.sort(sortedMigrations);
        return sortedMigrations;
    }

    /**
     * Returns a sorted list of Migrations available in the set of DataSources which have not been
     * applied to the given SQLiteDatabase.  The list is generated as follows:
     *
     * 0) If the database isn't managed, return the entire list of available migrations; else:
     * 1) Get the origin version with getOrigin().
     * 2) Get the list of applied versions with getAppliedVersions().
     * 3) Loop through available Migrations with getMigrations(), and:
     * 3.1) If the migration has a version less than or equal to origin, skip;
     * 3.2) If the migration is in the applied list, skip;
     * 3.3) Otherwise, the migration is pending.
     *
     * @param db Database on which to compare migration versions.
     * @return The list of available Migrations which have not been applied.
     */
    public List<Migration> getPendingMigrations(SQLiteDatabase db) throws IOException {
        // If this database isn't yet managed, just return the list of available Migrations.
        if (!hasMigrationsTable(db)) {
            return getMigrations();
        }

        // (1) Get the origin version of this database.
        long originVersion = getOriginVersion(db);

        // (2) Get a list of currently-applied versions.
        HashSet<Long> appliedVersions = getAppliedVersions(db);

        // (3) Generate the list of pending migrations.
        List<Migration> pendingMigrations = new LinkedList<Migration>();
        for (Migration migration : getMigrations()) {
            long version = migration.getVersion();

            if (version <= originVersion) {
                // Our origin already had this migration applied, continue.
                continue;
            }

            if (appliedVersions.contains(version)) {
                // We've already applied this migration, continue.
                continue;
            }

            // This Migration is pending.
            pendingMigrations.add(migration);
        }
        Collections.sort(pendingMigrations);
        return pendingMigrations;
    }

    /**
     * Loads the lowest version number from the `schema_migrations` table, returns NO_VERSIONS if
     * the migrations table is empty, or throws an SQLException if the table isn't present.  The
     * origin version number tells the manageSchema() method which old migrations it can safely
     * ignore (because they were already applied on the bootstrapped schema deployed on this
     * device).
     *
     * @param db Database from which to load the origin version.
     * @return The lowest version present or NO_VERSIONS of the `schema_migrations` table is empty.
     * @throws android.database.SQLException When no `schema_migrations` table is present.
     */
    public long getOriginVersion(SQLiteDatabase db) throws SQLException {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT MIN(version) FROM schema_migrations", null);
            cursor.moveToNext();
            if (cursor.isNull(0)) {
                return NO_VERSIONS;
            }
            return cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Loads the current version number from the `schema_migrations` table, returns NO_VERSIONS if
     * the migrations table is empty, or throws an SQLException if the table isn't present.  The
     * current version number is the maximum version in the `schema_migrations` table.
     *
     * @param db Database from which to load the current version.
     * @return The highest version present or NO_VERSIONS of the `schema_migrations` table is
     * empty.
     * @throws android.database.SQLException When no `schema_migrations` table is present.
     */
    public long getCurrentVersion(SQLiteDatabase db) throws SQLException {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT MAX(version) FROM schema_migrations", null);
            cursor.moveToNext();
            if (cursor.isNull(0)) {
                return NO_VERSIONS;
            }
            return cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Loads an ordered set of all versions currently applied on this database.  This set is used
     * by manageSchema() to determine which available migrations have already been applied.
     *
     * @param db Database from which to load versions.
     * @return An ordered set of all versions applied.
     * @throws android.database.SQLException When no `schema_migrations` table is present.
     */
    public LinkedHashSet<Long> getAppliedVersions(SQLiteDatabase db) throws SQLException {
        Cursor cursor = null;
        try {
            LinkedHashSet<Long> versions = new LinkedHashSet<Long>();
            cursor = db.rawQuery("SELECT version FROM schema_migrations ORDER BY version", null);
            while (cursor.moveToNext()) {
                versions.add(cursor.getLong(0));
            }
            return versions;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Records a successfully-applied migration in the `schema_migrations` table.
     *
     * @param db      Database to record a successful version in.
     * @param version Migration version to record.
     * @throws SQLException
     */
    public void insertVersion(SQLiteDatabase db, Long version) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("version", version);
        db.insert("schema_migrations", null, values);
    }

    /**
     * Returns true if the provided database's current version is not contained in the migrations,
     * or false if it does.
     *
     * @param db Database to check for downgrading.
     * @return true if the provided database has a higher version than the known migrations.
     */
    public boolean isDowngrade(SQLiteDatabase db) {
        Long max = getCurrentVersion(db);
        List<Migration> migrations = getMigrations();

        boolean isCurrentVersionInMigrations = false;
        for (Migration migration : migrations) {
            if (migration.getVersion().equals(max)) {
                isCurrentVersionInMigrations = true;
            }
        }
        return !isCurrentVersionInMigrations;
    }
}
