/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/6/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.schema;

import android.content.Context;

import com.layer.sqlite.datasource.ResourceDataSource;

import java.io.InputStream;

public class ResourceSchema extends Schema {
    private final Context mContext;

    public ResourceSchema(Context context, String path) {
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
