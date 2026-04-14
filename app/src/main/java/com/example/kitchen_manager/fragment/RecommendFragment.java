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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.adapters.RecipeAdapter;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeResponse;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendFragment extends Fragment {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private View tabAll, tabBreakfast, tabOther, tabSnack;
    private Map<View, String> tabTagMap = new HashMap<>();
    private Map<String, Integer> tagMap = new HashMap<>();
    private String currentTag = "全部";
    private int userId = -1;
    private ApiService apiService;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final int pageSize = 20;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Integer> tapMap = new HashMap<>();    // 分类映射。
    private long currentRequestId = 0;                              // 当前请求标识，用于避免数据错乱。
    private int currentTagId = 0;   // 当前选择分类对应的ID。
    private int requestTagId = 0;   // 当前请求对应的分类ID（用于避免旧请求覆盖新数据）。
    private static final int PRELOAD_THRESHOLD = 5;       // 离底部多少条数据触发预加载。
    private int requestGeneration = 0;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabScrollTop;
    private boolean isDataLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recommend, container, false);

        SharedPreferences prefs =
                requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        apiService = ApiClient.getApiService();
        initTagMap();

        rvRecipes = view.findViewById(R.id.rv_recipes);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        // 设置初始状态
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.VISIBLE);

        tabAll = view.findViewById(R.id.tab_category_all);
        tabBreakfast = view.findViewById(R.id.tab_category_breakfast);
        tabOther = view.findViewById(R.id.tab_category_other);
        tabSnack = view.findViewById(R.id.tab_category_snack);
        fabScrollTop = view.findViewById(R.id.fab_scroll_top);

        tabTagMap.put(tabAll, "全部");
        tabTagMap.put(tabBreakfast, "早餐");
        tabTagMap.put(tabOther, "正餐");
        tabTagMap.put(tabSnack, "加餐");

        updateButtonState(tabAll);

        View.OnClickListener categoryClickListener = v -> {
            String tag = tabTagMap.get(v);
            loadRecipesByTag(tag);
            updateButtonState(v);
        };

        tabAll.setOnClickListener(categoryClickListener);
        tabBreakfast.setOnClickListener(categoryClickListener);
        tabOther.setOnClickListener(categoryClickListener);
        tabSnack.setOnClickListener(categoryClickListener);

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

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvRecipes.setLayoutManager(layoutManager);

        rvRecipes.setHasFixedSize(true);

        adapter = new RecipeAdapter(
                getContext(),
                new ArrayList<>(),
                new RecipeAdapter.OnItemClickListener() {
                    @Override
                    public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
                        if (userId == -1) {
                            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (isCurrentlyFavorite) {
                            unfavoriteRecipe(recipeId);
                        } else {
                            favoriteRecipe(recipeId);
                        }
                    }

                    @Override
                    public void onDetailClick(int recipeId) {
                        showRecipeDetail(recipeId);
                    }

                    @Override
                    public void onCartClick(int recipeId, boolean isCurrentlyInCart) {
                        if (userId == -1) {
                            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        toggleCart(recipeId);
                    }
                },
                RecipeAdapter.PAGE_TYPE_RECOMMEND
        );

        rvRecipes.setAdapter(adapter);

        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;

                // 获取瀑布流布局管理器
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                // 获取所有列的最后可见项位置（数组）
                int[] lastVisiblePositions = layoutManager.findLastVisibleItemPositions(null);
                // 取最大值作为最后可见项
                int lastVisible = -1;
                for (int pos : lastVisiblePositions) {
                    if (pos > lastVisible) lastVisible = pos;
                }
                int total = layoutManager.getItemCount();

                if (lastVisible >= total - PRELOAD_THRESHOLD) {
                    loadRecipes(currentPage, currentTagId, requestGeneration);
                }
            }
        });

        loadRecipesByTag(currentTag);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当从其他页面（如菜谱详情）返回时，同步购物车状态
        if (isDataLoaded && userId != -1 && adapter != null && adapter.getRecipes() != null) {
            refreshCartStatusForAllRecipes();
        }
    }

    /**
     * 刷新当前列表中所有菜谱的购物车状态（批量同步）
     */
    private void refreshCartStatusForAllRecipes() {
        if (userId == -1 || adapter == null) return;

        List<RecipeResponse> recipes = adapter.getRecipes();
        if (recipes == null || recipes.isEmpty()) return;

        // 调用批量获取购物车菜谱ID的接口
        apiService.getCartRecipes(userId).enqueue(new Callback<ApiResponse<List<Integer>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Integer>>> call,
                                   @NonNull Response<ApiResponse<List<Integer>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getCode() == 200
                        && response.body().getData() != null) {

                    List<Integer> cartRecipeIds = response.body().getData();
                    Set<Integer> cartSet = new HashSet<>(cartRecipeIds);

                    // 遍历当前适配器中的所有菜谱，更新购物车状态
                    boolean needRefresh = false;
                    for (int i = 0; i < recipes.size(); i++) {
                        RecipeResponse recipe = recipes.get(i);
                        boolean shouldBeInCart = cartSet.contains(recipe.getRecipeId());
                        if (recipe.isInShoppingCart() != shouldBeInCart) {
                            recipe.setInShoppingCart(shouldBeInCart);
                            needRefresh = true;
                        }
                    }
                    if (needRefresh) {
                        adapter.notifyDataSetChanged();  // 或者局部刷新所有变化的项
                        Log.d("RecommendFragment", "购物车状态已批量同步");
                    }
                } else {
                    Log.e("RecommendFragment", "获取购物车列表失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Integer>>> call, @NonNull Throwable t) {
                Log.e("RecommendFragment", "批量同步购物车状态失败", t);
            }
        });
    }


    /**
     * 根据标签加载菜谱
     */
    private void loadRecipesByTag(String tag) {
        currentTag = tag;
        currentTagId = tagMap.getOrDefault(tag, 0);

        currentPage = 1;
        hasMore = true;
        isLoading = false;

        requestGeneration++;
        int myGeneration = requestGeneration;

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        loadRecipes(currentPage, currentTagId, myGeneration);
    }

    /**
     * 加载指定页码的菜谱数据
     */
    private void loadRecipes(int page, int tagId, int generation) {
        if (isLoading || !hasMore) return;

        isLoading = true;
        if (page == 1) {
            progressBar.setVisibility(View.VISIBLE);
            isDataLoaded = true;
        }

        int effectiveUserId = userId != -1 ? userId : 0;

        apiService.getRecipeList(tagId, page, pageSize, effectiveUserId)
                .enqueue(new Callback<>() {

                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                           Response<ApiResponse<Map<String, Object>>> response) {

                        if (generation != requestGeneration) return;

                        isLoading = false;
                        progressBar.setVisibility(View.GONE);

                        if (!response.isSuccessful() || response.body() == null) {
                            hasMore = false;
                            return;
                        }

                        List<RecipeResponse> recipes =
                                parseRecipesFromData(response.body().getData());
                        int totalPages =
                                getTotalPagesFromData(response.body().getData());

                        if (page == 1) {
                            adapter.setRecipes(new ArrayList<>(recipes));
                        } else {
                            List<RecipeResponse> merged =
                                    new ArrayList<>(adapter.getRecipes());
                            merged.addAll(recipes);
                            adapter.setRecipes(merged);
                        }

                        rvRecipes.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();

                        hasMore = page < totalPages;
                        if (hasMore) currentPage = page + 1;

                        rvRecipes.post(() -> {
                            if (hasMore
                                    && !isLoading
                                    && !rvRecipes.canScrollVertically(1)) {

                                loadRecipes(currentPage, currentTagId, generation);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call,
                                          Throwable t) {
                        if (generation != requestGeneration) return;

                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        hasMore = false;
                    }
                });
    }

    /**
     * 从数据中解析菜谱列表（增强收藏状态和购物车状态解析）
     */
    private List<RecipeResponse> parseRecipesFromData(Map<String, Object> data) {
        List<RecipeResponse> recipes = new ArrayList<>();

        try {
            if (data.containsKey("recipes")) {
                Object recipesObj = data.get("recipes");

                if (recipesObj instanceof List) {
                    List<?> rawList = (List<?>) recipesObj;

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
                                isFavorite = parseBooleanValue(favoriteObj);
                            }
                            // 同时检查小写key
                            else if (map.containsKey("isfavorite")) {
                                Object favoriteObj = map.get("isfavorite");
                                isFavorite = parseBooleanValue(favoriteObj);
                            }
                            // 检查数据库返回的常见格式
                            else if (map.containsKey("ISFAVORITE")) {
                                Object favoriteObj = map.get("ISFAVORITE");
                                isFavorite = parseBooleanValue(favoriteObj);
                            }
                            recipe.setFavorite(isFavorite);

                            // 解析购物车状态
                            boolean inShoppingCart = false;
                            // 检查所有可能的键名
                            String[] possibleCartKeys = {"inShoppingCart", "inshoppingcart", "IN_SHOPPING_CART", "cart_status"};
                            for (String key : possibleCartKeys) {
                                if (map.containsKey(key)) {
                                    Object cartObj = map.get(key);
                                    inShoppingCart = parseBooleanValue(cartObj);
                                    Log.d("RecommendFragment", "找到购物车状态 key=" + key + ", value=" + cartObj + ", parsed=" + inShoppingCart);
                                    break;
                                }
                            }

                            // 如果没找到，默认false
                            recipe.setInShoppingCart(inShoppingCart);

                            recipes.add(recipe); // 重要：确保添加到列表中
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RecommendFragment", "数据解析错误", e);
        }

        Log.d("RecommendFragment", "解析出的菜谱数量: " + recipes.size());
        return recipes;
    }

    /**
     * 解析布尔值（支持多种类型）
     */
    private boolean parseBooleanValue(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue() == 1;
        } else if (obj instanceof String) {
            String str = ((String) obj).trim().toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("y");
        }
        return false;
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
    private void updateButtonState(View selectedTab) {
        int white = android.graphics.Color.WHITE;
        int orange = android.graphics.Color.parseColor("#FF8C00");

        for (View tab : tabTagMap.keySet()) {
            // 利用 tag 找出容器内的子控件 (如果该 Tab 没有图标，iv 将为 null)
            ImageView iv = tab.findViewWithTag("tab_icon");
            TextView tv = tab.findViewWithTag("tab_text");

            if (tab == selectedTab) {
                // 选中状态：背景变选中态，图文变白
                tab.setBackgroundResource(R.drawable.bg_tab_selected);
                if (tv != null) tv.setTextColor(white);
                if (iv != null) iv.setColorFilter(white);
            } else {
                // 未选中状态：背景变正常态，图文变橙色
                tab.setBackgroundResource(R.drawable.bg_tab_normal_new);
                if (tv != null) tv.setTextColor(orange);
                if (iv != null) iv.setColorFilter(orange);
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

    /**
     * 加入购物车
     */
    private void addToCart(int recipeId) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(requireContext(), "已加入购物车", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
                        // 重复添加
                        updateLocalCartStatus(recipeId, true);
                        Toast.makeText(requireContext(), "已在购物车中", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "加入购物车失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "加入购物车失败: 服务器错误", Toast.LENGTH_SHORT).show();
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
     * 从购物车移除
     */
    private void removeFromCart(int recipeId) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(requireContext(), "已从购物车移除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "移除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "移除失败: 服务器错误", Toast.LENGTH_SHORT).show();
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

    private void toggleCart(int recipeId) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiService.toggleCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        // 切换成功，需要重新检查购物车状态
                        checkCartStatus(recipeId);
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "操作失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "操作失败: 服务器错误", Toast.LENGTH_SHORT).show();
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

    // 添加检查购物车状态的方法
    private void checkCartStatus(int recipeId) {
        Call<ApiResponse<Boolean>> call = apiService.checkIfInCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call,
                                   Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        boolean inCart = apiResponse.getData();
                        updateLocalCartStatus(recipeId, inCart);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                Log.e("RecommendFragment", "检查购物车状态失败", t);
            }
        });
    }

    /**
     * 更新本地购物车状态
     */
    private void updateLocalCartStatus(int recipeId, boolean inShoppingCart) {
        List<RecipeResponse> recipes = adapter.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponse recipe = recipes.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setInShoppingCart(inShoppingCart);
                adapter.notifyItemChanged(i);
                Log.d("RecommendFragment", "更新购物车状态: recipeId=" + recipeId + ", inShoppingCart=" + inShoppingCart);
                break;
            }
        }
    }
}