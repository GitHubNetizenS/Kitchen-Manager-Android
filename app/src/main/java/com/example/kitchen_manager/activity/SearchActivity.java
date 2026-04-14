package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class SearchActivity extends AppCompatActivity {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText etSearch;
    private ImageView ivBack, ivSearchAction;
    private Button btnAll, btnTagMatch, btnIngredientMatch;

    private int userId = -1;
    private ApiService apiService;
    private String currentKeyword = "";
    private String currentSortType = "all"; // 默认排序类型
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabScrollTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        apiService = ApiClient.getApiService();

        initViews();
        setupListeners();
        setupRecyclerView();
    }

    private void initViews() {
        rvRecipes = findViewById(R.id.rv_recipes);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        etSearch = findViewById(R.id.et_search);
        ivBack = findViewById(R.id.iv_back);
        ivSearchAction = findViewById(R.id.iv_search_action);
        btnAll = findViewById(R.id.btn_all);
        btnTagMatch = findViewById(R.id.btn_tag_match);
        btnIngredientMatch = findViewById(R.id.btn_ingredient_match);
        fabScrollTop = findViewById(R.id.fab_scroll_top);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 搜索按钮点击事件
        ivSearchAction.setOnClickListener(v -> performSearch());

        // 键盘搜索键监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // 排序按钮监听
        btnAll.setOnClickListener(v -> {
            updateButtonState(0);
            currentSortType = "all";
            if (!currentKeyword.isEmpty()) {
                searchRecipes(currentKeyword, currentSortType);
            }
        });

        btnTagMatch.setOnClickListener(v -> {
            updateButtonState(1);
            currentSortType = "tag_match";
            if (!currentKeyword.isEmpty()) {
                searchRecipes(currentKeyword, currentSortType);
            }
        });

        btnIngredientMatch.setOnClickListener(v -> {
            updateButtonState(2);
            currentSortType = "ingredient_match";
            if (!currentKeyword.isEmpty()) {
                searchRecipes(currentKeyword, currentSortType);
            }
        });

        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (rv.computeVerticalScrollOffset() > 300) {
                    fabScrollTop.setVisibility(View.VISIBLE);
                } else {
                    fabScrollTop.setVisibility(View.GONE);
                }
            }
        });

        fabScrollTop.setOnClickListener(v ->
                rvRecipes.smoothScrollToPosition(0)
        );
    }

    private void setupRecyclerView() {
        rvRecipes.setLayoutManager(new GridLayoutManager(this, 1));

        adapter = new RecipeAdapter(this, new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
                if (userId != -1) {
                    // 根据当前状态决定是收藏还是取消收藏
                    if (isCurrentlyFavorite) {
                        // 如果已收藏，则取消收藏
                        unfavoriteRecipe(recipeId);
                    } else {
                        // 如果未收藏，则收藏
                        favoriteRecipe(recipeId);
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDetailClick(int recipeId) {
                showRecipeDetail(recipeId);
            }

            @Override
            public void onCartClick(int recipeId, boolean isCurrentlyInCart) {
                if (userId != -1) {
                    // 根据当前状态决定是加入购物车还是移除
                    if (isCurrentlyInCart) {
                        removeFromCart(recipeId);
                    } else {
                        addToCart(recipeId);
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                }
            }
        }, RecipeAdapter.PAGE_TYPE_NORMAL);
        rvRecipes.setAdapter(adapter);
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        currentKeyword = keyword;
        searchRecipes(keyword, currentSortType);
    }

    private void searchRecipes(String keyword, String sortType) {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 确保用户ID有效
        if (userId == -1) {
            Toast.makeText(this, "用户未登录，使用默认排序", Toast.LENGTH_SHORT).show();
            userId = 0; // 使用默认值
        }

        Call<ApiResponse<List<RecipeResponse>>> call = apiService.searchRecipes(
                keyword,
                sortType,
                userId
        );

        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<RecipeResponse>> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        List<RecipeResponse> recipes = apiResponse.getData();

                        // 处理可能的空数据
                        if (recipes != null && !recipes.isEmpty()) {
                            adapter.setRecipes(recipes);
                            rvRecipes.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        } else {
                            emptyView.setText("没有找到相关食谱");
                            emptyView.setVisibility(View.VISIBLE);
                            rvRecipes.setVisibility(View.GONE);
                        }
                    } else {
                        showError("搜索失败: " + apiResponse.getMessage());
                    }
                } else {
                    String errorMsg = "服务器响应错误";
                    if (response != null) {
                        errorMsg += " - HTTP " + response.code();
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "网络错误: " + t.getMessage();
                if (t instanceof SocketTimeoutException) {
                    errorMsg = "请求超时，请检查网络";
                } else if (t instanceof ConnectException) {
                    errorMsg = "无法连接到服务器";
                } else if (t instanceof NullPointerException) {
                    errorMsg = "数据解析错误";
                }
                showError(errorMsg);
            }
        });
    }

    private void updateButtonState(int selectedIndex) {
        btnAll.setBackgroundResource(selectedIndex == 0 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_normal_new);
        btnAll.setTextColor(selectedIndex == 0 ? 0xFFFFFFFF : 0xFFFF8C00);

        btnTagMatch.setBackgroundResource(selectedIndex == 1 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_normal_new);
        btnTagMatch.setTextColor(selectedIndex == 1 ? 0xFFFFFFFF : 0xFFFF8C00);

        btnIngredientMatch.setBackgroundResource(selectedIndex == 2 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_normal_new);
        btnIngredientMatch.setTextColor(selectedIndex == 2 ? 0xFFFFFFFF : 0xFFFF8C00);
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
                        Toast.makeText(SearchActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复收藏
                        updateLocalFavoriteStatus(recipeId, true); // 确保图标更新为实心
                        Toast.makeText(SearchActivity.this, "您已经收藏过这个菜谱", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this, "收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 取消收藏菜谱
    private void unfavoriteRecipe(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.unfavoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        // 取消收藏成功，更新本地状态
                        updateLocalFavoriteStatus(recipeId, false);
                        Toast.makeText(SearchActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this, "取消收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "取消收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
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
                        Toast.makeText(SearchActivity.this, "已加入购物车", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复添加
                        updateLocalCartStatus(recipeId, true);
                        Toast.makeText(SearchActivity.this, "已在购物车中", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this, "加入购物车失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "加入购物车失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(SearchActivity.this, "已从购物车移除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this, "移除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "移除失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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

    // 更新本地购物车状态
    private void updateLocalCartStatus(int recipeId, boolean inShoppingCart) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setInShoppingCart(inShoppingCart);
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
        emptyView.setText(message + "\n\n点击重试");
        emptyView.setVisibility(View.VISIBLE);
        rvRecipes.setVisibility(View.GONE);

        emptyView.setOnClickListener(v -> {
            if (!currentKeyword.isEmpty()) {
                searchRecipes(currentKeyword, currentSortType);
            }
        });
    }
}