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
    private ImageView ivBack;
    private int userId = -1;
    private ApiService apiService;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 初始化SharedPreferences
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        // 获取当前用户ID
        userId = prefs.getInt("user_id", -1);
        Log.d("HistoryActivity", "当前用户ID: " + userId);

        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getApiService();

        ivBack = findViewById(R.id.iv_back);
        rvRecipes = findViewById(R.id.rv_recipes);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        btnSortTime = findViewById(R.id.btn_sort_time);
        btnSortMatch = findViewById(R.id.btn_sort_match);

        // 初始化按钮状态
        updateButtonState(true);

        ivBack.setOnClickListener(v -> finish());

        btnSortTime.setOnClickListener(v -> {
            updateButtonState(true);
            loadHistoryRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
            loadHistoryRecipes(false); // false 表示按匹配值排序
        });

        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        // 创建适配器 - 注意：历史记录页面不提供取消收藏功能，只提供收藏功能
        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId) {
                // 历史记录页面只能收藏，不能取消收藏
                // 因为历史记录中的菜谱可能没有被收藏过
                favoriteRecipe(recipeId);
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

    @Override
    protected void onResume() {
        super.onResume();
        // 每次活动恢复时重新获取用户ID
        userId = prefs.getInt("user_id", -1);
        Log.d("HistoryActivity", "onResume - 当前用户ID: " + userId);

        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查当前排序状态
        boolean isTimeSort = btnSortTime.getCurrentTextColor() ==
                getResources().getColor(android.R.color.white);
        loadHistoryRecipes(isTimeSort);
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
            showError("用户未登录");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        Log.d("HistoryActivity", "加载历史记录 - userId: " + userId + ", sortByTime: " + sortByTime);

        // 获取历史记录API
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getHistoryRecipes(
                userId,
                sortByTime ? "time" : "match"
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

    private void handleResponse(Response<ApiResponse<List<RecipeResponse>>> response) {
        progressBar.setVisibility(View.GONE);

        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<RecipeResponse>> apiResponse = response.body();
            Log.d("HistoryActivity", "API响应码: " + apiResponse.getCode() + ", 消息: " + apiResponse.getMessage());

            if (apiResponse.getCode() == 200) {
                List<RecipeResponse> recipes = apiResponse.getData();
                Log.d("HistoryActivity", "获取到历史记录数量: " + (recipes != null ? recipes.size() : 0));

                if (recipes != null && !recipes.isEmpty()) {
                    // 历史记录中的菜谱需要检查是否已收藏
                    for (RecipeResponse recipe : recipes) {
                        // 由于历史记录中的菜谱可能没有被收藏，这里需要从服务器获取收藏状态
                        // 简化处理：假设历史记录中的菜谱都不是收藏状态
                        recipe.setFavorite(false);
                        Log.d("HistoryActivity", "菜谱ID: " + recipe.getRecipeId() + ", 名称: " + recipe.getName());
                    }
                    adapter.setRecipes(recipes);
                    rvRecipes.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setText("暂无烹饪记录");
                    emptyView.setVisibility(View.VISIBLE);
                    Log.d("HistoryActivity", "历史记录列表为空");
                }
            } else {
                String errorMsg = "加载失败: " + apiResponse.getMessage();
                showError(errorMsg);
                Log.e("HistoryActivity", errorMsg);
            }
        } else {
            String errorMsg = "服务器响应错误: " + response.code();
            showError(errorMsg);
            Log.e("HistoryActivity", errorMsg);
        }
    }

    private void handleFailure(Throwable t) {
        progressBar.setVisibility(View.GONE);
        String errorMsg = "网络错误: " + t.getMessage();
        showError(errorMsg);
        Log.e("HistoryActivity", errorMsg, t);
    }

    // 收藏菜谱
    private void favoriteRecipe(int recipeId) {
        if (userId == -1) {
            Toast.makeText(HistoryActivity.this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("HistoryActivity", "收藏菜谱 - 菜谱ID: " + recipeId);

        // 立即更新UI（乐观更新）
        updateLocalFavoriteStatus(recipeId, true);

        // 调用API收藏菜谱
        Call<ApiResponse<Void>> call = apiService.favoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    Log.d("HistoryActivity", "收藏响应码: " + apiResponse.getCode());

                    if (apiResponse.getCode() == 200) {
                        Toast.makeText(HistoryActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复收藏
                        Toast.makeText(HistoryActivity.this, "您已经收藏过这个菜谱", Toast.LENGTH_SHORT).show();
                        // 确保图标保持实心状态
                        updateLocalFavoriteStatus(recipeId, true);
                    } else {
                        Toast.makeText(HistoryActivity.this, "收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        // 如果API调用失败，恢复收藏状态
                        updateLocalFavoriteStatus(recipeId, false);
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
                    // 如果API调用失败，恢复收藏状态
                    updateLocalFavoriteStatus(recipeId, false);
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
                // 如果网络错误，恢复收藏状态
                updateLocalFavoriteStatus(recipeId, false);
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
                // 更新单个项目
                if (adapter != null) {
                    adapter.notifyItemChanged(i);
                }
                Log.d("HistoryActivity", "更新菜谱收藏状态 - ID: " + recipeId + ", 是否收藏: " + isFavorite);
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
        rvRecipes.setVisibility(View.GONE);
    }
}