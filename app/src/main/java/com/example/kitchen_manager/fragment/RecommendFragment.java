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
import java.util.Objects;

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

        // 添加以下优化配置
        rvRecipes.setHasFixedSize(true);  // 如果item高度固定
        rvRecipes.setItemViewCacheSize(20);  // 增加视图缓存数量
        rvRecipes.setDrawingCacheEnabled(true);  // 启用绘图缓存
        rvRecipes.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);  // 设置缓存质量

// 启用子项预取（Android 5.0+）
        Objects.requireNonNull(rvRecipes.getItemAnimator()).setAddDuration(0);  // 添加动画时间为0
        rvRecipes.getItemAnimator().setRemoveDuration(0);  // 移除动画时间为0
        rvRecipes.getItemAnimator().setChangeDuration(0);  // 变更动画时间为0

        // 更新适配器接口实现
        adapter = new RecipeAdapter(getContext(), new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
            @Override
            public void onFavoriteClick(int recipeId) {
                // 调用收藏方法
                favoriteRecipe(recipeId);
            }

            @Override
            public void onDetailClick(int recipeId) {
                showRecipeDetail(recipeId);
            }
        });
        rvRecipes.setAdapter(adapter);

// 替换原来的上拉加载逻辑
        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private static final int VISIBLE_THRESHOLD = 5; // 距离底部5个item时预加载
            private static final int MAX_LOAD_PAGES = 5;   // 最多加载10页（200条数据）
            private static final int ITEM_THRESHOLD = 100;  // 最多保持100个item在内存中

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || !hasMore || isLoading) return;

                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();

                // 条件1：距离底部还有5个item时加载下一页
                // 条件2：最多只加载10页数据
                // 条件3：当前内存中item数少于100个时才加载更多
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

                // 当停止滚动时，清理过时数据
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int firstVisible = layoutManager.findFirstVisibleItemPosition();
                        int lastVisible = layoutManager.findLastVisibleItemPosition();

                        // 延迟500ms执行清理，避免卡顿
                        recyclerView.postDelayed(() -> {
                            cleanupOldItems(firstVisible, lastVisible);
                        }, 500);
                    }
                }

                super.onScrollStateChanged(recyclerView, newState);

                Context context = getContext();
                if (context == null) return;

                // 滑动时暂停Glide加载，停止时恢复
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING ||
                        newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    // 滑动中：只加载当前屏幕可见的图片
                    Glide.with(context).pauseRequests();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 停止时：恢复加载
                    Glide.with(context).resumeRequests();
                }
            }

            // 添加一个数据清理方法
            private void cleanupOldItems(int currentFirstVisible, int currentLastVisible) {
                List<RecipeResponse> allRecipes = adapter.getRecipes();
                if (allRecipes.size() <= ITEM_THRESHOLD) {
                    return; // 数据量不大，不需要清理
                }

                // 保留可见区域及其前后各20个item的数据
                int keepStart = Math.max(0, currentFirstVisible - 20);
                int keepEnd = Math.min(allRecipes.size() - 1, currentLastVisible + 20);

                // 如果要清理的数据太多，才执行清理
                if (allRecipes.size() > ITEM_THRESHOLD * 1.5) {
                    List<RecipeResponse> newList = new ArrayList<>();
                    for (int i = keepStart; i <= keepEnd && i < allRecipes.size(); i++) {
                        newList.add(allRecipes.get(i));
                    }

                    // 使用新的列表替换旧的，并通知适配器
                    adapter.setRecipes(newList);
                    Log.d("RecommendFragment", "数据清理完成: 保留" + newList.size() + "条数据");
                }
            }
        });

        loadRecipesByTag(currentTag);

        return view;
    }

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

    private void initTagMap() {
        tagMap.put("全部", 0);
        tagMap.put("早餐", 1);
        tagMap.put("正餐", 2);
        tagMap.put("加餐", 3);
    }

    private void loadRecipesByTag(String tag) {
        currentTag = tag;
        currentPage = 1;  // 重置到第一页
        hasMore = true;

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 调用新的分页接口
        loadRecipes(currentPage);
    }

    private void loadRecipes(int page) {
        if (isLoading || !hasMore) return;

        isLoading = true;
        if (page == 1) {
            requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        }
        int tagId = tagMap.get(currentTag);

        Log.d("RecommendFragment", "请求参数: tagId=" + tagId + ", page=" + page + ", pageSize=" + pageSize);

        // 调用新的分页接口，page和page_size参数生效
        Call<ApiResponse<Map<String, Object>>> call = apiService.getRecipeList(tagId, page, pageSize);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        Map<String, Object> data = apiResponse.getData();

                        // 解析分页数据
                        List<RecipeResponse> recipes = new ArrayList<>();
                        int total = 0;
                        int totalPages = 0;

                        try {
                            // 使用Gson转换recipes列表
                            Object recipesObj = data.get("recipes");
                            if (recipesObj instanceof List) {
                                Gson gson = new Gson();
                                String json = gson.toJson(recipesObj);
                                Type recipeListType = new TypeToken<List<RecipeResponse>>(){}.getType();
                                recipes = gson.fromJson(json, recipeListType);
                            }

                            // 安全转换数字类型
                            Object totalObj = data.get("total");
                            Object totalPagesObj = data.get("totalPages");

                            if (totalObj instanceof Number) {
                                total = ((Number) totalObj).intValue();
                            }
                            if (totalPagesObj instanceof Number) {
                                totalPages = ((Number) totalPagesObj).intValue();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError("数据解析错误");
                            return;
                        }

                        if (recipes != null && !recipes.isEmpty()) {
                            // 在主线程中更新UI
                            List<RecipeResponse> finalRecipes = recipes;
                            requireActivity().runOnUiThread(() -> {
                                if (currentPage == 1) {
                                    // 第一页：直接设置新数据
                                    adapter.setRecipes(new ArrayList<>(finalRecipes));
                                } else {
                                    // 加载更多：创建新的列表，避免直接修改原列表
                                    List<RecipeResponse> currentRecipes = new ArrayList<>(adapter.getRecipes());
                                    currentRecipes.addAll(finalRecipes);
                                    adapter.setRecipes(currentRecipes);
                                }

                                rvRecipes.setVisibility(View.VISIBLE);
                            });

                            hasMore = currentPage < totalPages;
                            currentPage++;
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                if (currentPage == 1) {
                                    emptyView.setText("暂无菜谱");
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                            });
                            hasMore = false;
                        }
                    } else {
                        showError("加载失败: " + apiResponse.getMessage());
                    }
                } else {
                    showError("服务器响应错误");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                mainHandler.post(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    showError("网络错误: " + t.getMessage());
                });
            }
        });
    }

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
                        updateLocalFavoriteStatus(recipeId, true); // 确保图标更新为实心
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
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    private void showError(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }
}