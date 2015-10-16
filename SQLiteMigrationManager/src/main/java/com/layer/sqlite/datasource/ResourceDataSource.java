/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/6/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.datasource;

import android.content.Context;

import com.layer.sqlite.migrations.Migration;
import com.layer.sqlite.migrations.ResourceMigration;
import com.layer.sqlite.schema.ResourceSchema;
import com.layer.sqlite.schema.Schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceDataSource implements DataSource {
    private final Context mContext;
    private final String mSchemaPath;
    private final String mMigrationsPath;

    public ResourceDataSource(Context context, String schemaPath, String migrationsPath) {
        if (schemaPath != null && !resourceExists(context, schemaPath)) {
            throw new IllegalArgumentException("Could not find schema path: '" + schemaPath + "'");
        }
        mContext = context;
        mSchemaPath = schemaPath;
        mMigrationsPath = migrationsPath;
    }

    /**
     * Returns whether this DataSource has a Schema entry.
     *
     * @return A boolean representing the presence of a Schema entry in this DataSource.
     */
    @Override
    public boolean hasSchema() {
        return (getSchema() != null);
    }

    /**
     * Returns the Schema entry if it exists, or `null` if it does not.
     *
     * @return The Schema entry if it exists, or `null` if it does not.
     */
    @Override
    public Schema getSchema() {
        if (mSchemaPath == null) return null;
        return new ResourceSchema(mContext, mSchemaPath);
    }

    /**
     * Returns a list of Migrations bundled in the java resources.
     *
     * We get resource migrations by first getting a known entry in the resource bundle (the
     * schema) and then backing out to find migrations that may also be bundled in the resources.
     * The JAR may either be accessed directly or after expansion on the filesystem, and each case
     * needs its own migration search technique.
     *
     * JAR: iterate through all JAR entries, capturing those whose name starts with
     * `resources/migrations`
     *
     * Filesystem: from the schema entry, back out two levels (to resources), then jump to the
     * migrations directory and iterate over the files in that directory.
     *
     * @return a list of Migrations bundled in the java resources
     */
    @Override
    public List<Migration> getMigrations() throws URISyntaxException, IOException {
        if (mMigrationsPath == null) return new ArrayList<Migration>();
        LinkedHashMap<String, Migration> migrations = new LinkedHashMap<String, Migration>();
        Enumeration<URL> target = mContext.getClassLoader().getResources(mSchemaPath);
        while (target.hasMoreElements()) {
            URL url = target.nextElement();
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            if (connection instanceof JarURLConnection) {
                // The schema resource is in a JAR; search within this JAR for migrations.
                JarURLConnection urlcon = (JarURLConnection) connection;
                JarFile jar = null;
                try {
                    jar = urlcon.getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        // Path is the item name in the JAR
                        String path = entries.nextElement().getName();
                        if (path.startsWith(mMigrationsPath)) {
                            if (migrations.containsKey(path)) continue;
                            migrations.put(path, new ResourceMigration(mContext, path));
                        }
                    }
                } finally {
                    if (jar != null) jar.close();
                }
            } else {
                // The schema resource is expanded onto the filesystem; jump to the migrations
                String[] schemaDirs = mSchemaPath.split("[/]");
                File baseDir = new File(url.toURI());
                for (int i = 0; i < schemaDirs.length; i++) {
                    baseDir = baseDir.getParentFile();
                }
                File migrationsDir = new File(baseDir, mMigrationsPath);
                for (File file : migrationsDir.listFiles()) {
                    // Resource path is still relative to the JAR (not the filesystem)
                    String path = mMigrationsPath + "/" + file.getName();
                    if (migrations.containsKey(path)) {
                        continue;
                    }
                    migrations.put(path, new ResourceMigration(mContext, path));
                }
            }
        }
        return new ArrayList<Migration>(migrations.values());
    }

    public static boolean resourceExists(Context context, String path) {
        return context.getClassLoader().getResource(path) != null;
    }

    public static InputStream getResourceAsStream(Context context, String path) {
        return context.getClassLoader().getResourceAsStream(path);
    }
}
