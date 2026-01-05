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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private TextView tvEdit, tvSelectAll, tvCancel, tvDelete;
    private int userId = -1;
    private ApiService apiService;
    private SharedPreferences prefs;
    private boolean isEditMode = false;
    private boolean isTimeSort = true;
    private int pendingDeletes = 0;

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
        tvEdit = findViewById(R.id.tv_edit);
        tvSelectAll = findViewById(R.id.tv_select_all);
        tvCancel = findViewById(R.id.tv_cancel);
        tvDelete = findViewById(R.id.tv_delete);


        // 初始化按钮状态
        updateButtonState(true);

        ivBack.setOnClickListener(v -> finish());

        btnSortTime.setOnClickListener(v -> {
            updateButtonState(true);
            isTimeSort = true;
            loadHistoryRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
            isTimeSort = false;
            loadHistoryRecipes(false); // false 表示按匹配值排序
        });

        tvEdit.setOnClickListener(v -> enterEditMode());

        tvSelectAll.setOnClickListener(v -> {
            adapter.selectAll(true);
            adapter.notifyDataSetChanged();
        });

        tvCancel.setOnClickListener(v -> exitEditMode());

        tvDelete.setOnClickListener(v -> deleteSelectedItems());

        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        // 创建适配器 - 注意：历史记录页面不提供取消收藏功能，只提供收藏功能
        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId) {
                if (!isEditMode) {
                    favoriteRecipe(recipeId);
                }
            }

            @Override
            public void onDetailClick(int recipeId) {
                if (!isEditMode) {
                    showRecipeDetail(recipeId);
                }
            }
        }, RecipeAdapter.PAGE_TYPE_HISTORY);
        rvRecipes.setAdapter(adapter);

        // 默认加载按时间排序
        loadHistoryRecipes(true);
    }

    private void enterEditMode() {
        isEditMode = true;
        tvEdit.setVisibility(View.GONE);
        tvSelectAll.setVisibility(View.VISIBLE);
        tvCancel.setVisibility(View.VISIBLE);
        tvDelete.setVisibility(View.VISIBLE);
        adapter.setEditMode(true);
        adapter.notifyDataSetChanged();
    }

    private void exitEditMode() {
        isEditMode = false;
        tvEdit.setVisibility(View.VISIBLE);
        tvSelectAll.setVisibility(View.GONE);
        tvCancel.setVisibility(View.GONE);
        tvDelete.setVisibility(View.GONE);
        adapter.setEditMode(false);
        adapter.clearSelection();
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedItems() {
        Set<Integer> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请选择要删除的记录", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingDeletes = selectedIds.size();
        progressBar.setVisibility(View.VISIBLE);

        for (int historyId : selectedIds) {
            deleteHistoryRecord(historyId);
        }
    }

    private void deleteHistoryRecord(int historyId) {
        Call<ApiResponse<Void>> call = apiService.deleteHistoryRecord(historyId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d("HistoryActivity", "删除响应码: " + response.code() + ", historyId: " + historyId);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    Log.d("HistoryActivity", "删除响应: " + apiResponse.getCode() + ", " + apiResponse.getMessage());
                    if (apiResponse.getCode() == 200) {
                        // 使用historyId从列表中移除
                        removeRecipeFromList(historyId);
                        adapter.removeSelectedId(historyId);
                        Toast.makeText(HistoryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HistoryActivity.this, "删除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 错误处理...
                }
                checkIfAllDeleted();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(HistoryActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                checkIfAllDeleted();
            }
        });
    }

    // 修改removeRecipeFromList方法，使用historyId查找
    private void removeRecipeFromList(int historyId) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getHistoryId() == historyId) { // 使用historyId匹配
                recipes.remove(i);
                adapter.notifyItemRemoved(i);
                Log.d("HistoryActivity", "从列表中移除历史记录ID: " + historyId);
                break;
            }
        }

        if (recipes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            rvRecipes.setVisibility(View.GONE);
        }
    }

    private void checkIfAllDeleted() {
        pendingDeletes--;
        if (pendingDeletes <= 0) {
            progressBar.setVisibility(View.GONE);
            exitEditMode();
            loadHistoryRecipes(isTimeSort);
            if (adapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                rvRecipes.setVisibility(View.GONE);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        userId = prefs.getInt("user_id", -1);
        Log.d("HistoryActivity", "onResume - 当前用户ID: " + userId);

        if (userId == -1) {
            finish();
            return;
        }

        // 刷新数据
        loadHistoryRecipes(true);
    }

    private void updateButtonState(boolean isTimeSort) {
        int selectedColor = getResources().getColor(android.R.color.white);
        int normalColor = getResources().getColor(R.color.gray);

        if (isTimeSort) {
            btnSortTime.setBackgroundResource(R.drawable.bg_tab_selected);
            btnSortTime.setTextColor(selectedColor);
            btnSortMatch.setBackgroundResource(R.drawable.bg_tab_normal);
            btnSortMatch.setTextColor(normalColor);
        } else {
            btnSortTime.setBackgroundResource(R.drawable.bg_tab_normal);
            btnSortTime.setTextColor(normalColor);
            btnSortMatch.setBackgroundResource(R.drawable.bg_tab_selected);
            btnSortMatch.setTextColor(selectedColor);
        }
    }

    private void loadHistoryRecipes(boolean isTimeSort) {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        String sortType = isTimeSort ? "time" : "match";
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getHistoryRecipes(userId, sortType);
        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call, Response<ApiResponse<List<RecipeResponse>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<RecipeResponse>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        List<RecipeResponse> recipes = apiResponse.getData();
                        if (recipes != null && !recipes.isEmpty()) {
                            for (RecipeResponse recipe : recipes) {
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
                        showError("加载失败: " + apiResponse.getMessage());
                    }
                } else {
                    showError("服务器响应错误");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

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