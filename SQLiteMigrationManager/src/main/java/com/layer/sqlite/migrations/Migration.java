/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 6/4/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.sqlite.migrations;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Migration implements Comparable<Migration> {
    /**
     * Valid names:
     * 0123.sql
     * 0123_some_description.sql
     *
     * Invalid names:
     * 0123_description
     * 0123_.sql
     * some_description.sql
     * .sql
     * sql
     */
    public static final Pattern MIGRATION_PATTERN =
            Pattern.compile("^(\\d+)_?((?<=_)[\\w\\s-]+)?(?<!_)\\.sql$");

    private final String mPath;
    private final Long mVersion;
    private final String mDescription;

    protected Migration(String path) {
        path = path.trim();
        int index = path.lastIndexOf(File.separatorChar);
        String fileName = (index == -1) ? path : path.substring(index + 1);

        Matcher matcher = MIGRATION_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid migration name: " + fileName);
        }

        mPath = path;

        if (matcher.groupCount() == 1) {
            mVersion = Long.parseLong(matcher.group(1));
            mDescription = null;
        } else {
            mVersion = Long.parseLong(matcher.group(1));
            mDescription = matcher.group(2);
        }
    }

    protected String getPath() {
        return mPath;
    }

    public Long getVersion() {
        return mVersion;
    }

    public String getDescription() {
        return mDescription;
    }

    /**
     * Migrations are equal when their versions are equal.
     *
     * @param o The other object to compare to.
     * @return True of the other object is a Migration and has equal versions.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Migration)) {
            return false;
        }

        Migration other = (Migration) o;

        return getVersion().equals(other.getVersion());
    }

    /**
     * Migrations are sorted by version.
     *
     * @param migration The other Migration with which to compare versions.
     * @return this.getVersion().com
     */
    @Override
    public int compareTo(Migration migration) {
        return getVersion().compareTo(migration.getVersion());
    }

    /**
     * Migrations are equal when their versions are equal.
     *
     * @return The hashcode of this Migration's version.
     */
    @Override
    public int hashCode() {
        return getVersion().hashCode();
    }
}
