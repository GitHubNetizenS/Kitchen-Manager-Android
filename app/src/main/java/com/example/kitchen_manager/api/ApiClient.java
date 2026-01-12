package com.example.kitchen_manager.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// ApiClient.java
public class ApiClient {
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {

            // 创建Gson实例，配置正确的反序列化选项
            Gson gson = new GsonBuilder()
                    .setLenient() // 设置宽松模式，便于调试
                    .create();

            // 创建带超时设置的OkHttpClient
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // 连接超时30秒
                    .readTimeout(30, TimeUnit.SECONDS)    // 读取超时30秒
                    .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时30秒
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiService.BASE_URL)
                    .client(okHttpClient) // 添加自定义的OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

}