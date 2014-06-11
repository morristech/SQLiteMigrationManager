SQLiteMigrationManager
======================

**A lightweight schema management system for Android [SQLiteDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html) databases.**

## Features

* Supports the creation and management of a dedicated migrations table within the host database.
* Applies migrations safely using SQLite transactions.
* Basic migrations are implemented as flat SQL files with a naming convention that encodes the version and name.
* Manages migrations loaded from extensible data sources.
* Similar the [FMDBMigrationManager](https://github.com/layerhq/FMDBMigrationManager) for iOS.

## Implementation Details

SQLiteMigrationManager works by introducing a simple `schema_migrations` table into the database under management. This table has a schema of:

```sql
CREATE TABLE schema_migrations(
	version INTEGER UNIQUE NOT NULL
);
```

Each row in `schema_migrations` corresponds to a single migration that has been applied and represents a unique version of the schema. This schema supports any versioning scheme that is based on integers, but it is recommended that you utilize an integer that encodes a timestamp. 

### Timestamped Versions

Timestamps are preferable to a monotonically incrementing integers because they better support branched workflows as you do not need to re-sequence migrations when multiple lines of development are brought together. Timestamps with sufficient levels of precision are ensured a very lowpotential for conflict and are trivially sortable.

The recommended format for timestamped migrations uses sub-second precision and can be generated via the `date` utility on platforms that provide GNU coreutils via `date +"%Y%m%d%M%S%3N"`. Unfortunately the build of `date` that ships with Mac OS X does not natively support this format. It can instead be generated via an invocation of Ruby: `ruby -e "puts Time.now.strftime('%Y%m%d%M%S%3N').to_i"`.

### Migration Naming

SQLiteMigrationManager favors migrations that are expressed as flat SQL files. These files can then be included into the host project via any Java resource bundle. In order for SQLiteMigrationManager to be able to identify migration files within the bundle and interpret the version they represent, the filename must encode the versioning data and may optionally include a descriptive name for the migration. Migrations filenames are matched with a regular expression that will recognize filenames of the form: `(<Numeric Version Number>)_?(<Descriptive Name)?.sql`. The description is optional, but if included, must be delimited by an underscore.  The version and the .sql file extension are mandatory.

Examples of valid migration names include:

* 1.sql
* 201406063106474_create_mb-demo-schema
* 9999_ChangeTablesToNewFormat.sql
* 2014324_This is the Description.sql

### Computing Origin and Current Version

Before SQLiteMigrationManager can determine what migrations should be applied to a given database, it must be able to asses details about the current version of the schema.

To compute the "origin version" (the version of the schema at the time the database was created), select the minimum value for the `version` column in the `schema_migrations` table:

```sql
SELECT MIN(version) FROM schema_migrations
```

The current version of the database is computable by selecting the maximum value for the `version` column present in the `schema_migrations` table:

```sql
SELECT MAX(version) FROM schema_migrations
```

Note that knowing the current version is not sufficient for computing if the database is fully migrated. This is because migrations that were created in the past may not yet have been merged, released and applied yet.

### Computing Unapplied Migrations

Determining what migrations should be applied to a given database is done using the following algorithm:

1. Compute the origin version of the database.
1. Build an array containing the `version` for all migrations within a given bundle.
1. Build an array of all migration versions that have already been applied to the database (`SELECT version FROM schema_migrations`)
1. Remove any migrations from the list with a version less than the origin version of the database.
1. Diff the arrays of migrations. The set that remains is the set of pending migrations.
1. Order the set of unapplied migrations into an array of ascending values and apply them in order from oldest to newest.

## Usage

SQLiteMigrationManager is designed to be very straightforward to use. The extensive unit test coverage that accompanies the library provides a great body of reference code. The sections below quickly sketch out how the most common tasks are accomplished with the library.

### Supplying DataSources for Loading Schemas and Migrations

Instances of SQLiteMigrationManager use sets of DataSource objects to load schemas and migrations.  Multiple DataSource objects can be added to a single manager, allowing migrations to come from a number of sources.

In its current form, only the ResourceDataSource is implemented, with the following signature:

```java
public ResourceDataSource(String schemaPath, String migrationsPath);
```

At runtime, the ResourceDataSource looks in bundled Java resources for a schema file matching the `schemaPath` and any resources whose path is prefixed with `migrationsPath`.  See `ResourceDataSourceTests` and the bundled test JAR in `/src/androidTest/libs` for examples.

The DataSource interface can be implemented by other classes to supply Schema and Migration objects from other sources (e.g. compiled classes or http).

### Migrating a Database

Usually, the only code modification required to get your SQLiteDatabase under managed migrations is to add `SQLiteMigrationManager.manageSchema()` to your `SQLiteOpenHelper.onCreate()` as follows:

```java
public class Persistence extends SQLiteOpenHelper {
    @Override
    public void onCreate(SQLiteDatabase db) {
        SQLiteMigrationManager manager = new SQLiteMigrationManager();
        manager.addDataSource(new ResourceDataSource("schema/schema.sql", "migrations"));
        try {
            manager.manageSchema(db, BootstrapAction.APPLY_SCHEMA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ...
}
```

#### BootstrapAction

`SQLiteMigrationManager.manageSchema()` takes an BootstrapAction enum object that tells manageSchema what to do when no schema_migrations table exists.

* **BootstrapAction.NONE**: Do nothing.  This is appropriate when the first Migration creates the schema_migrations table.
* **BootstrapAction.APPLY_SCHEMA**: Call `applySchema()`.  This is appropriate when the Schema object creates the schema_migrations table.
* **BootstrapAction.CREATE_MIGRATIONS_TABLE**: Call `createMigrationsTable()`.  This is appropriate when neither Schema nor Migration creates the schema_migrations table.

#### Chaining

Methods useful for initialization can also be chained for convenience, e.g.:

```java
(new SQLiteMigrationManager())
    .addDataSource(source1, source2)
    .manageSchema(db, BootstrapAction.CREATE_MIGRATIONS_TABLE);
```

Chainable methods:

```java
addDataSource(DataSource... dataSource)
createMigrationsTable(SQLiteDatabase db)
applySchema(SQLiteDatabase db)
```

### Manually Creating the Migrations Table

```java
SQLiteMigrationManager migrationManager = new SQLiteMigrationManager();
migrationManager.createMigrationsTable(db);
```

### Creating a SQL File Migration

```sh
$ touch "`ruby -e "puts Time.now.strftime('%Y%m%d%M%S%3N').to_i"`"_CreateMyAwesomeTable.sql
```

Now edit the file `*_CreateMyAwesomeTable.sql` in your editor of choice and add it to your JAR.


### Inspecting Schema State

The `SQLiteMigrationManager` includes a number of methods for investigating the state of your database. Here's a quick tour:

```java
boolean hasMigrationsTable(SQLiteDatabase db);
long getOriginVersion(SQLiteDatabase db);
long getCurrentVersion(SQLiteDatabase db);
List<Migration> getMigrations();
HashSet<Long> getAppliedVersions(SQLiteDatabase db);
List<Migration> getPendingMigrations(SQLiteDatabase db);
```

## Installation Via Gradle / Maven

Without parameters, `gradle publish` will publish a jar to your local Maven.  After doing so, you can include SQLiteMigrationManager in another project by adding the following dependency to its `build.gradle`:

```gradle
dependencies {
    compile 'com.layer.sqlite:migrationmanager:0.0.1'
}
```

## Unit Tests

SQLiteMigrationManager can be tested by running `gradle connectedAndroidTest` with an active simulator or device attached to your system.

## Credits

SQLiteMigrationManager was created in San Francisco by Steven Jones in collaboration with Blake Watters during their work at [Layer](http://layer.com). At Layer, we are building the Communications Layer for the Internet. We value, support, and create works of Open Source engineering excellence.

## License

SQLiteMigrationManager is available under the Apache 2 License. See the LICENSE file for more info.