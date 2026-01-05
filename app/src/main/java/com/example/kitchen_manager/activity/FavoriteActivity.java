package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private ImageView ivBack;
    private int userId = -1; // 不再硬编码，从SharedPreferences获取
    private ApiService apiService;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        // 初始化SharedPreferences
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        // 获取当前登录用户ID
        userId = prefs.getInt("user_id", -1);
        Log.d("FavoriteActivity", "当前用户ID: " + userId);

        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getApiService();

        rvRecipes = findViewById(R.id.rv_recipes);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        ivBack = findViewById(R.id.iv_back);
        btnSortTime = findViewById(R.id.btn_sort_time);
        btnSortMatch = findViewById(R.id.btn_sort_match);

        // 初始化按钮状态
        updateButtonState(true);

        ivBack.setOnClickListener(v -> finish());

        btnSortTime.setOnClickListener(v -> {
            updateButtonState(true);
            loadFavoriteRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
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

    @Override
    protected void onResume() {
        super.onResume();
        // 每次活动恢复时重新获取用户ID，防止用户切换
        userId = prefs.getInt("user_id", -1);
        Log.d("FavoriteActivity", "onResume - 当前用户ID: " + userId);

        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查当前是否有排序按钮被选中，如果有，按当前排序方式刷新
        boolean isTimeSort = btnSortTime.getCurrentTextColor() ==
                getResources().getColor(android.R.color.white);
        loadFavoriteRecipes(isTimeSort);
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
        if (userId == -1) {
            showError("用户未登录");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        Log.d("FavoriteActivity", "加载收藏菜谱 - userId: " + userId + ", sortByTime: " + sortByTime);

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
        if (userId == -1) {
            showError("用户未登录");
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
            Log.d("FavoriteActivity", "API响应码: " + apiResponse.getCode() + ", 消息: " + apiResponse.getMessage());

            if (apiResponse.getCode() == 200) {
                List<RecipeResponse> recipes = apiResponse.getData();
                Log.d("FavoriteActivity", "获取到收藏菜谱数量: " + (recipes != null ? recipes.size() : 0));

                if (recipes != null && !recipes.isEmpty()) {
                    // 标记所有菜谱为已收藏状态
                    for (RecipeResponse recipe : recipes) {
                        recipe.setFavorite(true);
                        Log.d("FavoriteActivity", "菜谱ID: " + recipe.getRecipeId() + ", 名称: " + recipe.getName());
                    }
                    adapter.setRecipes(recipes);
                    rvRecipes.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setText("暂无收藏内容");
                    emptyView.setVisibility(View.VISIBLE);
                    Log.d("FavoriteActivity", "收藏列表为空");
                }
            } else {
                String errorMsg = "加载失败: " + apiResponse.getMessage();
                showError(errorMsg);
                Log.e("FavoriteActivity", errorMsg);
            }
        } else {
            String errorMsg = "服务器响应错误: " + response.code();
            showError(errorMsg);
            Log.e("FavoriteActivity", errorMsg);
        }
    }

    private void handleFailure(Throwable t) {
        progressBar.setVisibility(View.GONE);
        String errorMsg = "网络错误: " + t.getMessage();
        showError(errorMsg);
        Log.e("FavoriteActivity", errorMsg, t);
    }

    private void unfavoriteRecipe(int recipeId) {
        if (userId == -1) {
            Toast.makeText(FavoriteActivity.this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 取消收藏API
        Call<ApiResponse<Void>> call = apiService.unfavoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    Log.d("FavoriteActivity", "取消收藏响应码: " + apiResponse.getCode());

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
                Log.d("FavoriteActivity", "从列表中移除菜谱ID: " + recipeId);
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
        rvRecipes.setVisibility(View.GONE);
    }
}