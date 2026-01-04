package com.example.kitchen_manager.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DoubaoApiClient {
    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/";
    private static Retrofit retrofit = null;

    public static DoubaoApiService getDoubaoApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(DoubaoApiService.class);
    }
}