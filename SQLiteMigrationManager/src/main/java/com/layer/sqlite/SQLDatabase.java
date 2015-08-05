package com.layer.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by kushaankumar on 7/15/15.
 */

public interface SQLDatabase {
        void beginTransaction();

        void endTransaction();

        void setTransactionSuccessful();

        Cursor rawQuery(String sql, String[] selectionArgs);

        void execSQL(String sql);

        void execSQL(String sql, Object[] bindArgs);

        long insert(String table, String nullColumnHack, ContentValues values);

        void close();
    }