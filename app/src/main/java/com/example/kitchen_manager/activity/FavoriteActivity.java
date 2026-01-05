package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.adapters.RecipeAdapter;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Button btnSortTime, btnSortMatch;

    private int userId = 1; // 示例ID
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        apiService = ApiClient.getApiService();

        rvRecipes = findViewById(R.id.rv_recipes);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        btnSortTime = findViewById(R.id.btn_sort_time);
        btnSortMatch = findViewById(R.id.btn_sort_match);

        // 初始化按钮状态
        updateButtonState(true);

        btnSortTime.setOnClickListener(v -> {
            updateButtonState(true);
            loadFavoriteRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
            // 改为调用收藏列表API，但使用match排序
            loadFavoriteRecipes(false); // false 表示按匹配值排序
        });

        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId) {
                unfavoriteRecipe(recipeId);
            }

            @Override
            public void onDetailClick(int recipeId) {
                showRecipeDetail(recipeId);
            }
        });
        rvRecipes.setAdapter(adapter);

        // 默认加载按时间排序
        loadFavoriteRecipes(true);
    }

    private void updateButtonState(boolean isTimeSort) {
        int orangeLight = getResources().getColor(R.color.orange_light);
        int lightGray = getResources().getColor(R.color.light_gray);
        int white = getResources().getColor(android.R.color.white);
        int black = getResources().getColor(android.R.color.black);

        btnSortTime.setBackgroundColor(isTimeSort ? orangeLight : lightGray);
        btnSortTime.setTextColor(isTimeSort ? white : black);

        btnSortMatch.setBackgroundColor(!isTimeSort ? orangeLight : lightGray);
        btnSortMatch.setTextColor(!isTimeSort ? white : black);
    }

    private void loadFavoriteRecipes(boolean sortByTime) {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 获取收藏列表API（按时间或匹配值排序）
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getFavoriteRecipes(
                userId,
                sortByTime ? "time" : "match" // 使用同一个API，但参数不同
        );

        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                handleResponse(response);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                handleFailure(t);
            }
        });
    }

    private void loadRecipesByMatchValue() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 获取匹配值排序的菜谱
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getRecipesByMatchValue(userId);

        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                handleResponse(response);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                handleFailure(t);
            }
        });
    }

    private void handleResponse(Response<ApiResponse<List<RecipeResponse>>> response) {
        progressBar.setVisibility(View.GONE);

        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<RecipeResponse>> apiResponse = response.body();

            if (apiResponse.getCode() == 200) {
                List<RecipeResponse> recipes = apiResponse.getData();

                if (recipes != null && !recipes.isEmpty()) {
                    // 标记所有菜谱为已收藏状态
                    for (RecipeResponse recipe : recipes) {
                        recipe.setFavorite(true);
                    }
                    adapter.setRecipes(recipes);
                    rvRecipes.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setText("暂无收藏内容");
                    emptyView.setVisibility(View.VISIBLE);
                }
            } else {
                showError("加载失败: " + apiResponse.getMessage());
            }
        } else {
            showError("服务器响应错误");
        }
    }

    private void handleFailure(Throwable t) {
        progressBar.setVisibility(View.GONE);
        showError("网络错误: " + t.getMessage());
    }

    private void unfavoriteRecipe(int recipeId) {
        // 取消收藏API
        Call<ApiResponse<Void>> call = apiService.unfavoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        removeRecipeFromList(recipeId);
                        Toast.makeText(FavoriteActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FavoriteActivity.this, "操作失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoriteActivity.this, "操作失败: 服务器错误", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(FavoriteActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeRecipeFromList(int recipeId) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getRecipeId() == recipeId) {
                recipes.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }

        // 检查是否为空
        if (recipes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            rvRecipes.setVisibility(View.GONE);
        }
    }

    private void showRecipeDetail(int recipeId) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    private void showError(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }
}