package com.foracademy.auditore.anyyourfoto;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foracademy.auditore.anyyourfoto.data.PhotoTable;
import com.foracademy.auditore.anyyourfoto.service.MyIntentService;
import com.foracademy.auditore.motofoto.R;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Константа для передачи URL в детальную Activity через интент
    public static final String IMAGE_URL = "IMAGE_URL";
    //Константы для сохранения состояний
    public static final String CURRENT_PAGE = "CURRENT_PAGE";
    public static final String LAST_VISIBLE_ITEM = "LAST_VISIBLE_ITEM";
    public static final String LAST_SEARCH_TERM = "LAST_SEARCH_TERM";
    public static final String LOADING_STATE = "LOADING_STATE";
    public static final int FIRST_PAGE = 1;

    // Порог
    // Если разница между крайним видимым элементом GridView и количеством
    // элементов в GridView меньше порога, запросим еще картинки с сервера
    private static final int threshold = 6;
    //текщая страница запроса и просмотра, требует восстановления
    private int currentPage = 1;
    //остановка попыток загрузки в случае достижения последней страницы.
    private boolean stopLoad = false;

    //поисковый запрос по умолчанию
    private String term;

    private CursorAdapter mCursorAdapter;
    private GridView mGrid;
    private TextView mToolbarTitle;
    private FloatingActionButton mFab;
    private ProgressBar mProgressBar;

    private SharedPreferences sPref;
    private MyBroadcastReceiver mMyBroadcastReceiver;


    @Override
    protected void onResume() {
        IntentFilter intentFilter = new IntentFilter(MyIntentService.ACTION_LOAD);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mMyBroadcastReceiver, intentFilter);
        super.onResume();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);
        term = getString(R.string.defaultTerm);

        Toolbar bar = findViewById(R.id.top_toolbar);
        setSupportActionBar(bar);

        mToolbarTitle = findViewById(R.id.tvToolbarTitle);
        mProgressBar = findViewById(R.id.refreshProgressBar);

        mFab = findViewById(R.id.fab);
        mFab.show();


        mFab.setOnClickListener(v -> refresh());


        //загрузим прошлое состояние полей term & currentPage
        loadActivityData();

        mGrid = findViewById(R.id.grid);
        mCursorAdapter = new CursorPhotoAdapter(this, null, 0);
        mGrid.setAdapter(mCursorAdapter);

        // Чтобы скроллинг "вверх" грида вызывал
        // исчезновение тулбара.
        mGrid.setNestedScrollingEnabled(true);

        mGrid.setOnItemClickListener((AdapterView<?> adapter, View view, int position, long id) -> {
            if (isNetworkReady()) {
                Intent intent = new Intent(MainActivity.this, DetailFullscreenActivity.class);
                Cursor cursor = mCursorAdapter.getCursor();
                cursor.moveToPosition(position);
                String url = cursor.getString(cursor.getColumnIndex(PhotoTable.COLUMN_URL));
                intent.putExtra(IMAGE_URL, url);
                startActivity(intent);
            } else
                Toast.makeText(MainActivity.this, R.string.noInternetAlert, Toast.LENGTH_LONG).show();

        });

        mGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case SCROLL_STATE_IDLE:
                        mFab.show();
                        if (mGrid.getLastVisiblePosition() >= mGrid.getCount() - threshold && !stopLoad) {
                            if (isNetworkReady()) {
                                currentPage++;
                                MyIntentService.startAction(MainActivity.this, term, currentPage, false);
                            }
                        }
                        break;
                    case SCROLL_STATE_TOUCH_SCROLL:
                        mFab.hide();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        getSupportLoaderManager().initLoader(111, null, this);

        //Запускаем запрос на загрузку фотографий, все проверки внутри.
        MyIntentService.startAction(this, term, FIRST_PAGE, false);

        //создаем и регестрируем BroadcastReceiver для получения сообщения о проблемах с сервиса
        mMyBroadcastReceiver = new MyBroadcastReceiver();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_PAGE, currentPage);
        outState.putInt(LAST_VISIBLE_ITEM, mGrid.getLastVisiblePosition());
        outState.putBoolean(LOADING_STATE, stopLoad);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        stopLoad = savedInstanceState.getBoolean(LOADING_STATE, false);
        currentPage = savedInstanceState.getInt(CURRENT_PAGE, 1);
        mGrid.smoothScrollToPosition(savedInstanceState.getInt(LAST_VISIBLE_ITEM));

        super.onRestoreInstanceState(savedInstanceState);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(this, FlickrContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_search:
                handleSearch(item);
                return true;
            case R.id.about_app_menu:
                DialogAboutApp.newInstance().show(getFragmentManager(), "DialogAbout");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSearch(MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();
        item.expandActionView();
        searchView.setQuery(term, false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (isNetworkReady()) {
                    String currentTerm = term.toLowerCase();
                    if (!TextUtils.isEmpty(query) && !currentTerm.equals(query.toLowerCase())) {
                        term = query;
                        //чистим базу от прошлого запроса
                        getContentResolver().delete(FlickrContentProvider.CONTENT_URI, null, null);
                        //разрешаем загрузку и обновления
                        stopLoad = false;
                        //грузим новый запрос
                        MyIntentService.startAction(MainActivity.this, term, FIRST_PAGE, false);
                        //сбрасываем текущую страницу
                        currentPage = FIRST_PAGE;
                        //меняем заголовок тулбара
                        changeToolbarTitle(query);
                        //закрываем поиск
                        item.collapseActionView();
                        mProgressBar.setVisibility(View.VISIBLE);

                    }
                    return true;
                } else {
                    Snackbar.make(mGrid, "Need internet connection :(", Snackbar.LENGTH_LONG).show();
                    item.collapseActionView();
                    return false;
                }
            }

            // выполняется при вводе любого символа в строку поиска
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void changeToolbarTitle(String query) {
        String title = query.substring(0, 1).toUpperCase() + query.substring(1, query.length()).toLowerCase() + " Foto Or...";
        mToolbarTitle.setText(title);
    }

    private void saveActivityData() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putString(LAST_SEARCH_TERM, term);
        editor.putInt(CURRENT_PAGE, currentPage);
        editor.putBoolean(LOADING_STATE, stopLoad);
        editor.apply();
    }

    private void loadActivityData() {
        sPref = getPreferences(MODE_PRIVATE);
        term = sPref.getString(LAST_SEARCH_TERM, getString(R.string.defaultTerm));
        currentPage = sPref.getInt(CURRENT_PAGE, 1);
        stopLoad = sPref.getBoolean(LOADING_STATE,false);
        changeToolbarTitle(term);
    }

    private boolean isNetworkReady() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void refresh() {
        if (isNetworkReady()) {
            getContentResolver().delete(FlickrContentProvider.CONTENT_URI, null, null);
            MyIntentService.startAction(this, term, currentPage, true);
            mFab.hide();
            mProgressBar.setVisibility(View.VISIBLE);
        } else if(!isNetworkReady()){
            Toast.makeText(this, R.string.checkInternet, Toast.LENGTH_SHORT).show();
        }else if (stopLoad)
            Toast.makeText(this, R.string.anotherRequest, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onStop() {
        unregisterReceiver(mMyBroadcastReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saveActivityData();

        super.onDestroy();
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(MyIntentService.RETROFIT_EXECUTE_ERROR)) {
                Toast.makeText(context, R.string.inetConnectBad, Toast.LENGTH_LONG).show();
                mFab.show();
                mProgressBar.setVisibility(View.GONE);
                //сообщение из exception не прочитано, передается на будущее.
            }
            if (intent.hasExtra(MyIntentService.SERVICE_COMPLETE)) {
                int code = intent.getIntExtra(MyIntentService.SERVICE_COMPLETE,-1);
                switch (code){
                    case 0:
                        mFab.show();
                        mProgressBar.setVisibility(View.GONE);
                        break;
                    case 1:
                        mFab.show();
                        mProgressBar.setVisibility(View.GONE);
                        break;
                    case 2: // состояние когда больше нет страниц с фотками
                        stopLoad = true; //останавливаем попытки загрузить
                        currentPage--; //для того, чтобы можно было обновить последнюю страницу фоток возвращаем значение на предыдущее
                        mFab.show();
                        mProgressBar.setVisibility(View.GONE);
                        break;
                        default:
                            Toast.makeText(context, R.string.broadcastPerort, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
}
