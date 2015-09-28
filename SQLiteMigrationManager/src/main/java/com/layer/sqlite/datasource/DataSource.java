/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/6/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.datasource;

import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.schema.Schema;

import java.util.List;

public interface DataSource {
    boolean hasSchema();

    Schema getSchema();

    List<Migration> getMigrations();
}
