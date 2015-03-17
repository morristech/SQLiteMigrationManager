package com.layer.sqlite.migrations.impl;

import android.content.Context;

import com.layer.sqlite.datasource.ResourceDataSource;
import com.layer.sqlite.migrations.StreamMigration;

import java.io.InputStream;

public class ResourceMigration extends StreamMigration {
    private final Context mContext;

    public ResourceMigration(Context context, String path) {
        super(path);
        mContext = context;
        if (!ResourceDataSource.resourceExists(mContext, path)) {
            throw new IllegalArgumentException("Could not find '" + path + "'");
        }
    }

    @Override
    public InputStream getStream() {
        return ResourceDataSource.getResourceAsStream(mContext, getPath());
    }
}
