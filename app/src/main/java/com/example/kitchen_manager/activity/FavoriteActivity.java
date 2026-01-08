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
import java.util.Map;
import java.util.Set;

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
            loadFavoriteRecipes(true);
        });

        btnSortMatch.setOnClickListener(v -> {
            updateButtonState(false);
            isTimeSort = false;
            loadFavoriteRecipes(false);
        });

        tvEdit.setOnClickListener(v -> enterEditMode());

        tvSelectAll.setOnClickListener(v -> {
            adapter.selectAllFavorite(true);
            adapter.notifyDataSetChanged();
        });

        tvCancel.setOnClickListener(v -> exitEditMode());

        tvDelete.setOnClickListener(v -> deleteSelectedItems());

        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
                // 实现取消收藏逻辑
                unfavoriteRecipe(recipeId);
            }

            @Override
            public void onDetailClick(int recipeId) {
                // 实现查看详情逻辑
                showRecipeDetail(recipeId);
            }

            @Override
            public void onCartClick(int recipeId, boolean isCurrentlyInCart) {
                if (userId == -1) {
                    Toast.makeText(FavoriteActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isCurrentlyInCart) {
                    // 实现从购物车移除逻辑
                    removeFromCart(recipeId);
                } else {
                    // 实现加入购物车逻辑
                    addToCart(recipeId);
                }
            }
        }, RecipeAdapter.PAGE_TYPE_FAVORITE);
        rvRecipes.setAdapter(adapter);

        // 默认加载按时间排序
        loadFavoriteRecipes(true);
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
            Toast.makeText(this, "请选择要删除的收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingDeletes = selectedIds.size();
        progressBar.setVisibility(View.VISIBLE);

        for (int recipeId : selectedIds) {
            unfavoriteRecipe(recipeId);
        }
    }

    private void unfavoriteRecipe(int recipeId) {
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
                        adapter.removeSelectedId(recipeId);
                        Toast.makeText(FavoriteActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FavoriteActivity.this, "操作失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoriteActivity.this, "操作失败: 服务器错误 " + response.code(), Toast.LENGTH_SHORT).show();
                }
                checkIfAllDeleted();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                String errorMsg = "网络错误: " + t.getMessage();
                if (t instanceof SocketTimeoutException) {
                    errorMsg = "请求超时，请检查网络";
                } else if (t instanceof ConnectException) {
                    errorMsg = "无法连接到服务器";
                }
                Toast.makeText(FavoriteActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                checkIfAllDeleted();
            }
        });
    }

    private void checkIfAllDeleted() {
        pendingDeletes--;
        if (pendingDeletes <= 0) {
            progressBar.setVisibility(View.GONE);
            exitEditMode();
            loadFavoriteRecipes(isTimeSort);
            if (adapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                rvRecipes.setVisibility(View.GONE);
            }
        }
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

    // 加入购物车
    private void addToCart(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.addToCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        // 加入购物车成功，更新本地状态
                        updateLocalCartStatus(recipeId, true);
                        Toast.makeText(FavoriteActivity.this, "已加入购物车", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复添加
                        updateLocalCartStatus(recipeId, true);
                        Toast.makeText(FavoriteActivity.this, "已在购物车中", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FavoriteActivity.this, "加入购物车失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoriteActivity.this, "加入购物车失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(FavoriteActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 从购物车移除
    private void removeFromCart(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.removeFromCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        // 移除成功，更新本地状态
                        updateLocalCartStatus(recipeId, false);
                        Toast.makeText(FavoriteActivity.this, "已从购物车移除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FavoriteActivity.this, "移除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoriteActivity.this, "移除失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(FavoriteActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 更新本地购物车状态
    private void updateLocalCartStatus(int recipeId, boolean inShoppingCart) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setInShoppingCart(inShoppingCart);
                adapter.notifyItemChanged(i);
                Log.d("FavoriteActivity", "更新购物车状态: recipeId=" + recipeId + ", inShoppingCart=" + inShoppingCart);
                break;
            }
        }
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

        // 刷新数据
        loadFavoriteRecipes(true);
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

    private void loadFavoriteRecipes(boolean isTimeSort) {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        String sortType = isTimeSort ? "time" : "match";
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getFavoriteRecipes(userId, sortType);
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
                                recipe.setFavorite(true);
                            }

                            adapter.setRecipes(recipes);
                            rvRecipes.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);

                            // 关键：逐一检查购物车状态
                            checkCartStatusOneByOne(recipes);
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

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "网络错误: " + t.getMessage();
                showError(errorMsg);
                Log.e("FavoriteActivity", errorMsg, t);
            }
        });
    }


    /**
     * 逐一检查购物车状态
     */
    private void checkCartStatusOneByOne(List<RecipeResponse> recipes) {
        if (userId == -1) return;

        for (RecipeResponse recipe : recipes) {
            checkSingleCartStatus(recipe);
        }
    }

    /**
     * 检查单个菜谱的购物车状态
     */
    private void checkSingleCartStatus(RecipeResponse recipe) {
        Call<ApiResponse<Boolean>> call = apiService.checkIfInCart(userId, recipe.getRecipeId());
        call.enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        boolean inCart = apiResponse.getData();
                        recipe.setInShoppingCart(inCart);

                        // 更新对应位置的UI
                        List<RecipeResponse> adapterRecipes = adapter.getRecipes();
                        for (int i = 0; i < adapterRecipes.size(); i++) {
                            if (adapterRecipes.get(i).getRecipeId() == recipe.getRecipeId()) {
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                Log.e("FavoriteActivity", "检查购物车状态失败: " + t.getMessage());
            }
        });
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