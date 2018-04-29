package com.foracademy.auditore.anyyourfoto.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PhotoDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_FILE = "photos.db";
    private static final  int DATABASE_VERSION = 1;

    public PhotoDBHelper(Context context) {
        super(context, DATABASE_FILE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        PhotoTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        PhotoTable.onUpgrade(db, oldVersion, newVersion);
    }
}
