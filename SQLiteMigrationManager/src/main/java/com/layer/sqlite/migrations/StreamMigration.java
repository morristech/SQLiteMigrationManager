package com.layer.sqlite.migrations;

import java.io.InputStream;

public abstract class StreamMigration extends Migration {

    protected StreamMigration(String path) {
        super(path);
    }

    /**
     * Returns an input stream of SQL to execute.
     */
    public abstract InputStream getStream();
}
