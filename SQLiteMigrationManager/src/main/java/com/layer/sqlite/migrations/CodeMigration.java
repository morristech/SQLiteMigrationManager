package com.layer.sqlite.migrations;

import com.layer.sqlite.SQLDatabase;

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
    public abstract void execute(SQLDatabase db) throws IOException;
}
