package com.example.kitchen_manager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.adapters.RecipeAdapter;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendFragment extends Fragment {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Button btnAll, btnBreakfast, btnOther, btnSnack;
    private Map<Button, String> buttonTagMap = new HashMap<>();
    private Map<String, Integer> tagMap = new HashMap<>();
    private String currentTag = "全部";
    private int userId = -1;
    private ApiService apiService;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int pageSize = 20;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommend, container, false);

        // 获取SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        apiService = ApiClient.getApiService();
        initTagMap();

        rvRecipes = view.findViewById(R.id.rv_recipes);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        btnAll = view.findViewById(R.id.btn_category_all);
        btnBreakfast = view.findViewById(R.id.btn_category_breakfast);
        btnOther = view.findViewById(R.id.btn_category_other);
        btnSnack = view.findViewById(R.id.btn_category_snack);

        buttonTagMap.put(btnAll, "全部");
        buttonTagMap.put(btnBreakfast, "早餐");
        buttonTagMap.put(btnOther, "正餐");
        buttonTagMap.put(btnSnack, "加餐");

        updateButtonState(btnAll);

        View.OnClickListener categoryClickListener = v -> {
            Button clickedButton = (Button) v;
            String tag = buttonTagMap.get(clickedButton);
            loadRecipesByTag(tag);
            updateButtonState(clickedButton);
        };

        btnAll.setOnClickListener(categoryClickListener);
        btnBreakfast.setOnClickListener(categoryClickListener);
        btnOther.setOnClickListener(categoryClickListener);
        btnSnack.setOnClickListener(categoryClickListener);

        rvRecipes.setLayoutManager(new GridLayoutManager(getContext(), 1));

        // 添加优化配置
        rvRecipes.setHasFixedSize(true);
        rvRecipes.setItemViewCacheSize(20);
        rvRecipes.setDrawingCacheEnabled(true);
        rvRecipes.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        if (rvRecipes.getItemAnimator() != null) {
            rvRecipes.getItemAnimator().setAddDuration(0);
            rvRecipes.getItemAnimator().setRemoveDuration(0);
            rvRecipes.getItemAnimator().setChangeDuration(0);
        }

        // 创建适配器 - 修改：使用正确的页面类型
        adapter = new RecipeAdapter(getContext(), new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
                if (userId == -1) {
                    Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 根据当前状态决定是收藏还是取消收藏
                if (isCurrentlyFavorite) {
                    // 如果已收藏，则取消收藏
                    unfavoriteRecipe(recipeId);
                } else {
                    // 如果未收藏，则收藏
                    favoriteRecipe(recipeId);
                }
            }

            @Override
            public void onDetailClick(int recipeId) {
                showRecipeDetail(recipeId);
            }
        }, RecipeAdapter.PAGE_TYPE_NORMAL); // 明确指定页面类型
        rvRecipes.setAdapter(adapter);

        // 设置滚动监听
        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private static final int VISIBLE_THRESHOLD = 5;
            private static final int MAX_LOAD_PAGES = 5;
            private static final int ITEM_THRESHOLD = 100;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || !hasMore || isLoading) return;

                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();

                if (lastVisibleItemPosition >= totalItemCount - VISIBLE_THRESHOLD
                        && currentPage <= MAX_LOAD_PAGES
                        && totalItemCount < ITEM_THRESHOLD) {
                    Log.d("RecommendFragment", "触发预加载: page=" + currentPage);
                    loadRecipes(currentPage);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                Context context = getContext();
                if (context == null) return;

                // 滑动时暂停Glide加载，停止时恢复
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING ||
                        newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    Glide.with(context).pauseRequests();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(context).resumeRequests();
                }
            }
        });

        // 加载初始数据
        loadRecipesByTag(currentTag);

        return view;
    }

    /**
     * 根据标签加载菜谱
     */
    private void loadRecipesByTag(String tag) {
        currentTag = tag;
        currentPage = 1;  // 重置到第一页
        hasMore = true;

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        loadRecipes(currentPage);
    }

    /**
     * 加载指定页码的菜谱数据
     */
    private void loadRecipes(int page) {
        if (isLoading || !hasMore) return;

        isLoading = true;
        if (page == 1) {
            requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        }
        int tagId = tagMap.get(currentTag);

        Log.d("RecommendFragment", "请求参数: tagId=" + tagId + ", page=" + page + ", pageSize=" + pageSize + ", userId=" + userId);

        // 确保 userId 有效
        int effectiveUserId = userId != -1 ? userId : 0;

        Call<ApiResponse<Map<String, Object>>> call = apiService.getRecipeList(tagId, page, pageSize, effectiveUserId);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                isLoading = false;
                requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    Log.d("RecommendFragment", "响应码: " + apiResponse.getCode());
                    Log.d("RecommendFragment", "响应消息: " + apiResponse.getMessage());

                    if (apiResponse.getCode() == 200) {
                        Map<String, Object> data = apiResponse.getData();

                        if (data == null) {
                            Log.e("RecommendFragment", "数据为空");
                            showError("数据为空");
                            return;
                        }

                        // 解析菜谱数据
                        List<RecipeResponse> recipes = parseRecipesFromData(data);
                        int totalPages = getTotalPagesFromData(data);

                        if (recipes != null && !recipes.isEmpty()) {
                            Log.d("RecommendFragment", "成功解析 " + recipes.size() + " 条菜谱数据");
                            // 打印每条菜谱的收藏状态
                            for (RecipeResponse recipe : recipes) {
                                Log.d("RecommendFragment", "菜谱ID: " + recipe.getRecipeId() + ", 收藏状态: " + recipe.isFavorite());
                            }

                            // 在主线程中更新UI
                            List<RecipeResponse> finalRecipes = recipes;
                            requireActivity().runOnUiThread(() -> {
                                if (currentPage == 1) {
                                    // 第一页：直接设置新数据
                                    adapter.setRecipes(new ArrayList<>(finalRecipes));
                                } else {
                                    // 加载更多：创建新的列表
                                    List<RecipeResponse> currentRecipes = new ArrayList<>(adapter.getRecipes());
                                    currentRecipes.addAll(finalRecipes);
                                    adapter.setRecipes(currentRecipes);
                                }

                                rvRecipes.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                            });

                            hasMore = currentPage < totalPages;
                            if (hasMore) {
                                currentPage++;
                            }
                        } else {
                            Log.d("RecommendFragment", "没有数据");
                            requireActivity().runOnUiThread(() -> {
                                if (currentPage == 1) {
                                    emptyView.setText("暂无菜谱");
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                                rvRecipes.setVisibility(View.GONE);
                            });
                            hasMore = false;
                        }
                    } else {
                        showError("加载失败: " + apiResponse.getMessage());
                    }
                } else {
                    String errorMsg = "服务器响应错误: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                mainHandler.post(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    String errorMsg = "网络错误: " + t.getMessage();
                    if (t instanceof SocketTimeoutException) {
                        errorMsg = "请求超时，请检查网络";
                    } else if (t instanceof ConnectException) {
                        errorMsg = "无法连接到服务器";
                    }
                    showError(errorMsg);
                });
            }
        });
    }

    /**
     * 从数据中解析菜谱列表（增强收藏状态解析）- 关键修复点
     */
    private List<RecipeResponse> parseRecipesFromData(Map<String, Object> data) {
        List<RecipeResponse> recipes = new ArrayList<>();

        try {
            if (data.containsKey("recipes")) {
                Object recipesObj = data.get("recipes");

                if (recipesObj instanceof List) {
                    List<?> rawList = (List<?>) recipesObj;

                    Gson gson = new Gson();
                    for (Object item : rawList) {
                        if (item instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) item;

                            // 创建RecipeResponse对象
                            RecipeResponse recipe = new RecipeResponse();

                            // 设置基本字段
                            if (map.containsKey("recipe_id")) {
                                recipe.setRecipeId(((Number) map.get("recipe_id")).intValue());
                            }
                            if (map.containsKey("name")) {
                                recipe.setName((String) map.get("name"));
                            }
                            if (map.containsKey("image_url")) {
                                recipe.setImageUrl((String) map.get("image_url"));
                            }
                            if (map.containsKey("taste")) {
                                recipe.setTaste((String) map.get("taste"));
                            }
                            if (map.containsKey("method")) {
                                recipe.setMethod((String) map.get("method"));
                            }
                            if (map.containsKey("time")) {
                                recipe.setTime((String) map.get("time"));
                            }
                            if (map.containsKey("difficulty")) {
                                recipe.setDifficulty((String) map.get("difficulty"));
                            }
                            if (map.containsKey("needs")) {
                                recipe.setNeeds((String) map.get("needs"));
                            }

                            // 关键修复：正确解析收藏状态
                            boolean isFavorite = false;
                            if (map.containsKey("isFavorite")) {
                                Object favoriteObj = map.get("isFavorite");
                                if (favoriteObj instanceof Boolean) {
                                    isFavorite = (Boolean) favoriteObj;
                                } else if (favoriteObj instanceof Number) {
                                    isFavorite = ((Number) favoriteObj).intValue() == 1;
                                } else if (favoriteObj instanceof String) {
                                    String favStr = ((String) favoriteObj).trim().toLowerCase();
                                    isFavorite = favStr.equals("true") || favStr.equals("1") || favStr.equals("y");
                                }
                            }
                            // 同时检查小写key
                            else if (map.containsKey("isfavorite")) {
                                Object favoriteObj = map.get("isfavorite");
                                if (favoriteObj instanceof Boolean) {
                                    isFavorite = (Boolean) favoriteObj;
                                } else if (favoriteObj instanceof Number) {
                                    isFavorite = ((Number) favoriteObj).intValue() == 1;
                                }
                            }
                            // 检查数据库返回的常见格式
                            else if (map.containsKey("ISFAVORITE")) {
                                Object favoriteObj = map.get("ISFAVORITE");
                                if (favoriteObj instanceof Boolean) {
                                    isFavorite = (Boolean) favoriteObj;
                                } else if (favoriteObj instanceof Number) {
                                    isFavorite = ((Number) favoriteObj).intValue() == 1;
                                }
                            }

                            recipe.setFavorite(isFavorite);
                            recipes.add(recipe);

                            Log.d("RecommendFragment", "解析结果: recipeId=" + recipe.getRecipeId() + ", isFavorite=" + isFavorite);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RecommendFragment", "数据解析错误", e);
        }

        return recipes;
    }

    /**
     * 从数据中获取总页数
     */
    private int getTotalPagesFromData(Map<String, Object> data) {
        try {
            if (data.containsKey("totalPages")) {
                Object totalPagesObj = data.get("totalPages");
                if (totalPagesObj instanceof Number) {
                    return ((Number) totalPagesObj).intValue();
                }
            }
        } catch (Exception e) {
            Log.e("RecommendFragment", "获取总页数错误", e);
        }
        return 1;
    }

    /**
     * 收藏菜谱
     */
    private void favoriteRecipe(int recipeId) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(requireContext(), "收藏成功", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复收藏
                        updateLocalFavoriteStatus(recipeId, true);
                        Toast.makeText(requireContext(), "您已经收藏过这个菜谱", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 取消收藏菜谱
     */
    private void unfavoriteRecipe(int recipeId) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "取消收藏失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "取消收藏失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 更新本地收藏状态
     */
    private void updateLocalFavoriteStatus(int recipeId, boolean isFavorite) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setFavorite(isFavorite);
                adapter.notifyItemChanged(i);
                Log.d("RecommendFragment", "更新本地状态: recipeId=" + recipeId + ", isFavorite=" + isFavorite);
                break;
            }
        }
    }

    /**
     * 显示菜谱详情
     */
    private void showRecipeDetail(int recipeId) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonState(Button selectedButton) {
        int orangeLight = ContextCompat.getColor(requireContext(), R.color.orange_light);
        int lightGray = ContextCompat.getColor(requireContext(), R.color.light_gray);

        for (Button button : buttonTagMap.keySet()) {
            if (button == selectedButton) {
                button.setBackgroundColor(orangeLight);
                button.setTextColor(Color.WHITE);
            } else {
                button.setBackgroundColor(lightGray);
                button.setTextColor(Color.BLACK);
            }
        }
    }

    /**
     * 初始化标签映射
     */
    private void initTagMap() {
        tagMap.put("全部", 0);
        tagMap.put("早餐", 1);
        tagMap.put("正餐", 2);
        tagMap.put("加餐", 3);
    }
}