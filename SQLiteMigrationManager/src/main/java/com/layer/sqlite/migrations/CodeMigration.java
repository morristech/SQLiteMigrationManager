package com.layer.sqlite.migrations;

import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;

public abstract class CodeMigration extends Migration {

    protected CodeMigration(String path) {
        super(path);
    }

    /**
     * Executes this migration on the given database.
     *
     * @return `true` for success, `false` for fail.
     */
    public abstract void execute(SQLiteDatabase db) throws IOException;
}
