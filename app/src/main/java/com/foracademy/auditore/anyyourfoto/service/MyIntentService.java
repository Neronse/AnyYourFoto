package com.foracademy.auditore.anyyourfoto.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.widget.Toast;

import com.foracademy.auditore.anyyourfoto.FlickrContentProvider;
import com.foracademy.auditore.anyyourfoto.MainActivity;
import com.foracademy.auditore.anyyourfoto.data.JSONpack.Result;
import com.foracademy.auditore.anyyourfoto.data.PhotoTable;
import com.foracademy.auditore.motofoto.BuildConfig;
import com.foracademy.auditore.motofoto.R;
import com.foracademy.auditore.anyyourfoto.data.JSONpack.Photo;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyIntentService extends IntentService {
    private static FlickrService service;

    public static final String ACTION_LOAD = "com.foracademy.auditore.motofoto.action.LOAD";

    private static final String SEARCH = "com.foracademy.auditore.motofoto.extra.SEARCH";
    private static final String PAGE = "com.foracademy.auditore.motofoto.extra.PAGE";
    public static final String FLICKR_METHOD_SEARCH = "flickr.photos.search";
    public static final String RETROFIT_EXECUTE_ERROR = "RETROFIT_EXECUTE_ERROR";
    public static final String SERVICE_COMPLETE = "SERVICE_COMPLETE";
    private Handler mHandler;

    public MyIntentService() {
        super("MyIntentService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }



    public static void startAction(Context context, String search, int page, boolean refresh) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Cursor cursor =  context.getContentResolver().query(FlickrContentProvider.CONTENT_URI,null,null,null,null);
       //если база пустая или запрашивается следующая страница или нажата кнопка обновления, то запускаем сервис загрузки
        if (cursor != null && (cursor.getCount() < 1 || page > MainActivity.FIRST_PAGE || refresh)){
            Intent intent = new Intent(context, MyIntentService.class);
            intent.setAction(ACTION_LOAD);
            intent.putExtra(SEARCH, search);
            intent.putExtra(PAGE, page);
            service = retrofit.create(FlickrService.class);
            context.startService(intent);
        }
        if (cursor != null) {
            cursor.close();
        }


    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            final String action = intent.getAction();
            if (ACTION_LOAD.equals(action)) {
                final String search = intent.getStringExtra(SEARCH);
                final int page = intent.getIntExtra(PAGE, 1);

                Call<Result> call = service.search(
                        FLICKR_METHOD_SEARCH,
                        BuildConfig.API_KEY,
                        search,
                        "relevance",
                        "json",
                        1,
                        page
                );
                try {
                    Response<Result> resultResponse = call.execute();
                    handleAction(resultResponse, page);
                } catch (IOException e) {
                    mHandler.post(()-> Toast.makeText(getApplicationContext(), R.string.retrifitExecuteAlert,Toast.LENGTH_SHORT).show());
                    Intent responseIntent = new Intent(ACTION_LOAD);
                    responseIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    responseIntent.putExtra(RETROFIT_EXECUTE_ERROR, e.getMessage());
                    sendBroadcast(responseIntent);
                }
            }
        }
    }


    private void handleAction(Response<Result> resultResponse, int currentPage) {

        Result r = resultResponse.body();
        if (r != null && r.getStat().equals("ok")) {
            //если нет фото по запросу
            if(r.getPhotos().getTotal() == 0) {
                mHandler.post(()->Toast.makeText(getApplicationContext(), R.string.anotherRequest, Toast.LENGTH_LONG).show());
                serviceComplete();
                stopSelf();
                return;
            }

            List<Photo> photoList = r.getPhotos().getPhoto();
            ContentValues[] contentValues = new ContentValues[photoList.size()];
            for (int i = 0; i < contentValues.length; i++) {
                ContentValues cv = new ContentValues();
                Photo p = photoList.get(i);
                cv.put(PhotoTable.COLUMN_URL, createUrl(p));
                contentValues[i] = cv;
                if(currentPage >= r.getPhotos().getPages()) mHandler.post(()->Toast.makeText(getApplicationContext(), R.string.lastPageAlert, Toast.LENGTH_LONG).show());
            }
            getContentResolver().bulkInsert(FlickrContentProvider.CONTENT_URI, contentValues);
            serviceComplete();
        } else if(r != null && r.getStat().equals("fail")){
            mHandler.post(()->Toast.makeText(getApplicationContext(), r.getMessage(),Toast.LENGTH_SHORT).show());
            serviceComplete();
        }
    }

    private void serviceComplete(){
        Intent responseIntent = new Intent(ACTION_LOAD);
        responseIntent.addCategory(Intent.CATEGORY_DEFAULT);
        responseIntent.putExtra(SERVICE_COMPLETE, 1);
        sendBroadcast(responseIntent);
    }

    private String createUrl(Photo p) {
        // Сервисная функция для получения URL картинки по объекту https://www.flickr.com/services/api/misc.urls.html
        return String.format(
                "https://farm%s.staticflickr.com/%s/%s_%s_q.jpg",
                p.getFarm(),
                p.getServer(),
                p.getId(),
                p.getSecret()
        );
    }

}

