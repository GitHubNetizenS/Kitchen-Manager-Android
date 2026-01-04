package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Button btnSortTime, btnSortMatch;

    private int userId = -1; // 初始化为-1表示未登录
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

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
            loadHistoryRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
            // 改为调用历史记录API，但使用match排序
            loadHistoryRecipes(false); // false 表示按匹配值排序
        });

        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        // 创建适配器并实现收藏点击事件
        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId) {
                // 只有登录用户才能收藏
                if (userId != -1) {
                    favoriteRecipe(recipeId);
                } else {
                    Toast.makeText(HistoryActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDetailClick(int recipeId) {
                showRecipeDetail(recipeId);
            }
        });
        rvRecipes.setAdapter(adapter);

        // 默认加载按时间排序
        loadHistoryRecipes(true);
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

    private void loadHistoryRecipes(boolean sortByTime) {
        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 获取历史记录API
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getHistoryRecipes(
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
        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
                    adapter.setRecipes(recipes);
                    rvRecipes.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setText("暂无烹饪记录");
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

    // 收藏菜谱
    private void favoriteRecipe(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.favoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        // 收藏成功，更新本地状态
                        updateLocalFavoriteStatus(recipeId, true);
                        Toast.makeText(HistoryActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复收藏
                        updateLocalFavoriteStatus(recipeId, true); // 确保图标更新为实心
                        Toast.makeText(HistoryActivity.this, "您已经收藏过这个菜谱", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HistoryActivity.this, "收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                String errorMsg = "网络错误: " + t.getMessage();
                if (t instanceof SocketTimeoutException) {
                    errorMsg = "请求超时，请检查网络";
                } else if (t instanceof ConnectException) {
                    errorMsg = "无法连接到服务器";
                }
                Toast.makeText(HistoryActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 更新本地收藏状态
    private void updateLocalFavoriteStatus(int recipeId, boolean isFavorite) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setFavorite(isFavorite);
                adapter.notifyItemChanged(i); // 更新单个项目
                break;
            }
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