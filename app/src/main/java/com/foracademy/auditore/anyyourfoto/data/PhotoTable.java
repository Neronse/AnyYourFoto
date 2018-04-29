package com.foracademy.auditore.anyyourfoto.data;

import android.database.sqlite.SQLiteDatabase;

public class PhotoTable {
    public static final String TABLE_PHOTOS = "photos";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_TERM = "term";

    public static final String PHOTOS_CREATE = "create table "
            + TABLE_PHOTOS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_URL
            + " text not null); ";

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(PHOTOS_CREATE);

    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        onCreate(sqLiteDatabase);
    }
}
