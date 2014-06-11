/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/6/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.migrations;

import com.layer.sqlite.datasource.ResourceDataSource;

import java.io.InputStream;

public class ResourceMigration extends Migration {
    public ResourceMigration(String path) {
        super(path);
        if (!ResourceDataSource.resourceExists(path)) {
            throw new IllegalArgumentException("Could not find '" + path + "'");
        }
    }

    @Override
    public InputStream getStream() {
        return ResourceDataSource.getResourceAsStream(getPath());
    }
}
