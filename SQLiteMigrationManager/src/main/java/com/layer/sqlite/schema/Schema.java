/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/6/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.schema;

import java.io.InputStream;
public abstract class Schema {
    private String mPath;

    protected Schema(String path) {
        mPath = path;
    }

    protected String getPath() {
        return mPath;
    }

    public abstract InputStream getStream();
}
