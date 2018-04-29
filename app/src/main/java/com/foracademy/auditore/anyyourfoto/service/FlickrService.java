package com.foracademy.auditore.anyyourfoto.service;

import com.foracademy.auditore.anyyourfoto.data.JSONpack.Result;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface FlickrService {

    @GET("/services/rest/")
    Call<Result> search(
            @Query("method") String method,
            @Query("api_key") String key,
            @Query("tags") String text,
            @Query("sort") String type,
            @Query("format") String format,
            @Query("nojsoncallback") int nojasoncallback,
            @Query("page") int page
    );
}
