package com.foracademy.auditore.anyyourfoto;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.foracademy.auditore.anyyourfoto.data.PhotoTable;
import com.foracademy.auditore.motofoto.R;
import com.squareup.picasso.Picasso;

public class CursorPhotoAdapter extends CursorAdapter {

    public CursorPhotoAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_view_item, parent, false);
        Holder holder = new Holder();
        holder.picture = (ImageView) view.findViewById(R.id.image);
        populateView(holder, cursor);
        view.setTag(holder);
        return view;
    }

    // Для CursorAdapter эта функция вызывается для изменения View
    // Нужно только получить Holder из Tag и поменять ImageView, накоторый он хранит ссылку
    //
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Holder holder = (Holder) view.getTag();
        populateView(holder, cursor);
    }

    // Загрузка картинки в ImageView, на который хранит ссылку Holder
    private void populateView(final Holder holder, Cursor cursor) {
        String url = cursor.getString(cursor.getColumnIndex(PhotoTable.COLUMN_URL));
        Picasso.get().load(url).placeholder(R.drawable.ic_photo_placeholder_24dp).into(holder.picture);
    }

    private class Holder {
        ImageView picture;
    }
}
