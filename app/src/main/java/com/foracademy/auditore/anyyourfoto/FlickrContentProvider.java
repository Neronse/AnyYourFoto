package com.foracademy.auditore.anyyourfoto;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.foracademy.auditore.anyyourfoto.data.PhotoDBHelper;
import com.foracademy.auditore.anyyourfoto.data.PhotoTable;

public class FlickrContentProvider extends ContentProvider {

    public static final String CONTENT_AUTHORITY = "com.foracademy.auditore.motofoto.flickrfotos";
    // По этому Uri можно будет запросить таблицу через
    // ContentResolver
    public static final Uri CONTENT_URI = Uri.parse(
            "content://" + CONTENT_AUTHORITY + "/elements"
    );

    // Метод UriMatcher.match(URI) Используется, чтобы определить по URI,
    // запрос происходит к одной записи или к таблице целиком
    private static final UriMatcher uriMatcher;

    // результат UriMatcher.match(URI):
    // вся таблица
    private static final int ALL_ROWS = 100;
    // одна строка
    private static final int SINGLE_ROW = 101;


    // Статическая инициализация
    // Если URI соответствует какому-нибудь из нежеперечисленных,
    // возвратить соответствующий int
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(CONTENT_AUTHORITY, "elements", ALL_ROWS);
        // # это число
        uriMatcher.addURI(CONTENT_AUTHORITY, "elements/#", SINGLE_ROW);
    }

    private PhotoDBHelper helper;
    private SQLiteStatement insertStatement;

    @Override
    public boolean onCreate() {
        helper = new PhotoDBHelper(getContext());

        insertStatement = helper.getReadableDatabase().compileStatement(
                "insert into photos (url) values (?)"
        );
        // Инициализация провайдера выполнена успешно
        return true;
    }

    private String getSelection(Uri uri, String selection)
    {
        switch (uriMatcher.match(uri))
        {
            case SINGLE_ROW:
                String rowID = uri.getPathSegments().get(1);

                // selection может быть ненулевым, поэтому, если он не нулевой
                // его нужно сохранить - добавляем к нашему selection
                selection = PhotoTable.COLUMN_ID + "=" + rowID
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                break;
        }
        return selection;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = helper.getReadableDatabase();

        selection = getSelection(uri, selection);

        Cursor cursor = db.query(PhotoTable.TABLE_PHOTOS, projection,selection,selectionArgs,null,null,sortOrder);
        //подписка курсора на изменение таблицы
        cursor.setNotificationUri(getContext().getContentResolver(), CONTENT_URI);

        return cursor;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();

        selection = getSelection(uri, selection);

        int deleteCount = db.delete(PhotoTable.TABLE_PHOTOS,selection,selectionArgs);
        if(deleteCount > 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return deleteCount;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri))
        {
            case ALL_ROWS:
                return "vnd.android.cursor.dir/vnd.flickrfotos.elemental";
            case SINGLE_ROW:
                return "vnd.android.cursor.item/vnd.flickrfotos.elemental";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(PhotoTable.TABLE_PHOTOS, null,values);
        if(id > -1){
            // Добавим идентификатор вставленнной строки к Uri таблицы
            Uri inserted = ContentUris.withAppendedId(uri,id);
            getContext().getContentResolver().notifyChange(inserted, null);
            return uri;
        }
        else return null;
    }




    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();

        selection = getSelection(uri, selection);


        int updateCount = db.update( PhotoTable.TABLE_PHOTOS, values, selection, selectionArgs );

        if(updateCount > 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int numInserted = 0;
        try{
            db.beginTransaction();
            for(ContentValues v : values){
                insertStatement.bindString(1, v.getAsString(PhotoTable.COLUMN_URL));
                insertStatement.executeInsert();
                numInserted++;
            }
            db.setTransactionSuccessful();

        }catch (Exception e){
            numInserted = 0;
        }finally {
            db.endTransaction();
        }
        if(numInserted > 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return numInserted;
    }
}
