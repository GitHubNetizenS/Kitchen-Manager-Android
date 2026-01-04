package com.example.kitchen_manager.api;

import com.example.kitchen_manager.response.DoubaoApiResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface DoubaoApiService {
    @POST("api/v3/chat/completions")
    Call<DoubaoApiResponse> recognizeImage(
            @Header("Authorization") String authorization,
            @Body RequestBody body
    );
}