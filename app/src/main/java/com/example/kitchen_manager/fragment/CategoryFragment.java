package com.example.kitchen_manager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.adapters.RecipeAdapter;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeResponse;
import com.google.android.material.appbar.AppBarLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

public class CategoryFragment extends Fragment {

    private View rootView;
    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ApiService apiService;
    private int userId = -1;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final int pageSize = 20;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // 折叠控件
    private AppBarLayout appBarLayout;
    private ImageView ivExpandIndicator;
    private FrameLayout flExpandContainer;
    private boolean isExpanded = true;

    // 筛选状态
    private Set<String> selectedTastes = new HashSet<>();
    private String selectedMethod = "";
    private String selectedDifficulty = "";
    private String selectedSort = "all"; // 默认综合

    // 按钮映射 (ID 到 值)
    private Map<Integer, String> tasteButtons = new HashMap<>();
    private Map<Integer, String> methodButtons = new HashMap<>();
    private Map<Integer, String> difficultyButtons = new HashMap<>();
    private Map<Integer, String> sortButtons = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_category, container, false);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        apiService = ApiClient.getApiService();

        initViews();
        setupButtonMaps();
        setupListeners();
        setupRecyclerView();
        setupAppBarListener(); // 添加AppBar滚动监听
        updateButtonStates();
        loadRecipes();

        return rootView;
    }

    private void initViews() {
        rvRecipes = rootView.findViewById(R.id.rv_recipes);
        progressBar = rootView.findViewById(R.id.progressBar);
        emptyView = rootView.findViewById(R.id.emptyView);
        appBarLayout = rootView.findViewById(R.id.app_bar);
        ivExpandIndicator = rootView.findViewById(R.id.iv_expand_indicator);
        flExpandContainer = rootView.findViewById(R.id.fl_expand_container);

        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE); // 初始显示加载中
        rvRecipes.setVisibility(View.VISIBLE);
    }

    private void setupButtonMaps() {
        // 口味 (多选)
        tasteButtons.put(R.id.btn_taste_all, "");
        tasteButtons.put(R.id.btn_taste_sour, "酸");
        tasteButtons.put(R.id.btn_taste_sweet, "甜");
        tasteButtons.put(R.id.btn_taste_bitter, "苦");
        tasteButtons.put(R.id.btn_taste_spicy, "辣");
        tasteButtons.put(R.id.btn_taste_salty, "咸");
        tasteButtons.put(R.id.btn_taste_umami, "鲜");

        // 工艺 (单选) - 注意：现在按钮分布在两行
        methodButtons.put(R.id.btn_method_all, "");
        methodButtons.put(R.id.btn_method_fry, "煎");
        methodButtons.put(R.id.btn_method_stir, "炒");
        methodButtons.put(R.id.btn_method_boil, "煮");
        methodButtons.put(R.id.btn_method_deepfry, "炸");
        methodButtons.put(R.id.btn_method_stew, "炖");
        methodButtons.put(R.id.btn_method_pickle, "腌");
        methodButtons.put(R.id.btn_method_roast, "烧");
        methodButtons.put(R.id.btn_method_mix, "拌");
        methodButtons.put(R.id.btn_method_other, "其他");

        // 难度 (单选)
        difficultyButtons.put(R.id.btn_diff_all, "");
        difficultyButtons.put(R.id.btn_diff_easy, "简单");
        difficultyButtons.put(R.id.btn_diff_normal, "普通");
        difficultyButtons.put(R.id.btn_diff_hard, "高级");
        difficultyButtons.put(R.id.btn_diff_god, "神级");

        // 排序 (单选)
        sortButtons.put(R.id.btn_sort_all, "all");
        sortButtons.put(R.id.btn_sort_match, "match");
        sortButtons.put(R.id.btn_sort_popular, "popularity");
    }

    private void setupListeners() {
        if (rootView == null) return;

        // 折叠点击 - 修复点击事件
        if (flExpandContainer != null) {
            flExpandContainer.setOnClickListener(v -> {
                if (isExpanded) {
                    // 收起筛选栏
                    appBarLayout.setExpanded(false, true);
                    isExpanded = false;
                } else {
                    // 展开筛选栏
                    appBarLayout.setExpanded(true, true);
                    isExpanded = true;
                }
                updateExpandIndicator();
            });
        }

        // 口味按钮（多选）
        for (Map.Entry<Integer, String> entry : tasteButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                btn.setOnClickListener(v -> handleTasteClick((Button) v));
            }
        }

        // 工艺按钮（单选） - 现在需要从两行中查找
        for (Map.Entry<Integer, String> entry : methodButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                btn.setOnClickListener(v -> handleSingleSelectClick((Button) v, "method"));
            }
        }

        // 难度按钮（单选）
        for (Map.Entry<Integer, String> entry : difficultyButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                btn.setOnClickListener(v -> handleSingleSelectClick((Button) v, "difficulty"));
            }
        }

        // 排序按钮（单选）
        for (Map.Entry<Integer, String> entry : sortButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                btn.setOnClickListener(v -> handleSingleSelectClick((Button) v, "sort"));
            }
        }
    }

    private void setupAppBarListener() {
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    int totalScrollRange = appBarLayout.getTotalScrollRange();
                    if (totalScrollRange > 0) {
                        // 计算折叠百分比
                        float percentage = Math.abs(verticalOffset) / (float) totalScrollRange;

                        // 更新状态但不触发动画
                        boolean newExpandedState = percentage < 0.5f;
                        if (newExpandedState != isExpanded) {
                            isExpanded = newExpandedState;
                            // 只更新角度，不播放动画
                            updateIndicatorAngleWithoutAnimation();
                        }
                    }
                }
            });
        }
    }

    private void updateIndicatorAngleWithoutAnimation() {
        if (ivExpandIndicator != null) {
            ivExpandIndicator.clearAnimation();
            ivExpandIndicator.setRotation(isExpanded ? 0 : 180);
        }
    }

    private void updateExpandIndicator() {
        if (ivExpandIndicator != null) {
            // 清除之前的动画
            ivExpandIndicator.clearAnimation();

            // 直接从当前角度旋转到目标角度
            float startAngle = ivExpandIndicator.getRotation();
            float endAngle = isExpanded ? 0 : 180;

            // 如果角度已经相同，不执行动画
            if (Math.abs(startAngle - endAngle) < 1) {
                ivExpandIndicator.setRotation(endAngle);
                return;
            }

            RotateAnimation rotate = new RotateAnimation(
                    startAngle,
                    endAngle,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            ivExpandIndicator.startAnimation(rotate);
        }
    }


    private void handleTasteClick(Button btn) {
        String value = tasteButtons.get(btn.getId());
        if (value == null) return;

        if (value.isEmpty()) { // "全部"
            if (selectedTastes.isEmpty()) {
                return; // 已为全部，不响应
            }
            selectedTastes.clear();
        } else {
            if (selectedTastes.contains(value)) {
                selectedTastes.remove(value);
            } else {
                selectedTastes.add(value);
            }
        }
        updateButtonStates();
        resetAndLoad();
    }

    private void handleSingleSelectClick(Button btn, String type) {
        String value = getButtonValue(btn.getId(), type);
        if (value == null) return;

        boolean changed = false;
        switch (type) {
            case "method":
                if (value.equals(selectedMethod)) return;
                selectedMethod = value;
                changed = true;
                break;
            case "difficulty":
                if (value.equals(selectedDifficulty)) return;
                selectedDifficulty = value;
                changed = true;
                break;
            case "sort":
                if (value.equals(selectedSort)) return;
                selectedSort = value;
                changed = true;
                break;
        }
        if (changed) {
            updateButtonStates();
            resetAndLoad();
        }
    }

    private String getButtonValue(int buttonId, String type) {
        switch (type) {
            case "method": return methodButtons.get(buttonId);
            case "difficulty": return difficultyButtons.get(buttonId);
            case "sort": return sortButtons.get(buttonId);
            case "taste": return tasteButtons.get(buttonId);
            default: return null;
        }
    }

    private void updateButtonStates() {
        if (rootView == null) return;

        // 更新口味按钮 - 多选逻辑
        boolean hasSelectedTaste = !selectedTastes.isEmpty();
        for (Map.Entry<Integer, String> entry : tasteButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                String value = entry.getValue();
                boolean selected;
                if (value.isEmpty()) {
                    // "全部"按钮：当没有任何具体口味被选中时选中
                    selected = !hasSelectedTaste;
                } else {
                    // 具体口味按钮：如果在选中集合中
                    selected = selectedTastes.contains(value);
                }
                updateButtonStyle(btn, selected);
            }
        }

        // 更新工艺按钮 - 单选逻辑
        for (Map.Entry<Integer, String> entry : methodButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                String value = entry.getValue();
                boolean selected = value.equals(selectedMethod);
                updateButtonStyle(btn, selected);
            }
        }

        // 更新难度按钮 - 单选逻辑
        for (Map.Entry<Integer, String> entry : difficultyButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                String value = entry.getValue();
                boolean selected = value.equals(selectedDifficulty);
                updateButtonStyle(btn, selected);
            }
        }

        // 更新排序按钮 - 单选逻辑
        for (Map.Entry<Integer, String> entry : sortButtons.entrySet()) {
            Button btn = rootView.findViewById(entry.getKey());
            if (btn != null) {
                String value = entry.getValue();
                boolean selected = value.equals(selectedSort);
                updateButtonStyle(btn, selected);
            }
        }
    }

    private void updateButtonStyle(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundResource(R.drawable.bg_tag_selected);
            btn.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            btn.setBackgroundResource(R.drawable.bg_tag_normal);
            btn.setTextColor(getResources().getColor(R.color.gray));
        }
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
     * 切换购物车状态
     */
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

    /**
     * 检查购物车状态
     */
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
                Log.e("CategoryFragment", "检查购物车状态失败", t);
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
                Log.d("CategoryFragment", "更新收藏状态: recipeId=" + recipeId + ", isFavorite=" + isFavorite);
                break;
            }
        }
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
                Log.d("CategoryFragment", "更新购物车状态: recipeId=" + recipeId + ", inShoppingCart=" + inShoppingCart);
                break;
            }
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        rvRecipes.setLayoutManager(layoutManager);
        adapter = new RecipeAdapter(getContext(), new ArrayList<>(), new RecipeAdapter.OnItemClickListener() {
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
                Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
                intent.putExtra("recipe_id", recipeId);
                startActivity(intent);
            }

            @Override
            public void onCartClick(int recipeId, boolean isCurrentlyInCart) {
                if (userId == -1) {
                    Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleCart(recipeId);
            }
        }, RecipeAdapter.PAGE_TYPE_NORMAL);
        rvRecipes.setAdapter(adapter);

        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMore && (firstVisibleItemPosition + visibleItemCount >= totalItemCount - 5)) {
                    loadRecipes();
                }

                // 自动收起功能：当向下滑动时，如果筛选栏是展开的，则自动收起
                if (dy > 10 && isExpanded) { // dy > 10 表示向下滑动
                    appBarLayout.setExpanded(false, true);
                    isExpanded = false;
                    updateExpandIndicator();
                }
            }
        });
    }

    private void resetAndLoad() {
        currentPage = 1;
        hasMore = true;
        adapter.setRecipes(new ArrayList<>());
        loadRecipes();
    }

    private void loadRecipes() {
        if (isLoading || !hasMore) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        // 构建口味参数：使用逗号分隔多个口味
        String tasteParam = String.join(",", selectedTastes);

        Call<ApiResponse<Map<String, Object>>> call = apiService.getFilteredRecipes(
                tasteParam.isEmpty() ? "" : tasteParam,
                selectedMethod,
                selectedDifficulty,
                userId,
                currentPage,
                pageSize);

        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Map<String, Object> data = apiResponse.getData();
                        List<RecipeResponse> newRecipes = parseRecipes(data.get("recipes"));

                        // 从data中获取总数，如果没有则使用recipes.size()
                        int total = 0;
                        if (data.containsKey("total_count")) {
                            total = ((Number) data.get("total_count")).intValue();
                        } else {
                            total = newRecipes.size();
                        }

                        // 客户端排序
                        if ("popularity".equals(selectedSort)) {
                            // 按热度排序
                            newRecipes.sort((r1, r2) -> Integer.compare(r2.getPopularity(), r1.getPopularity()));
                        } else if ("match".equals(selectedSort)) {
                            // 匹配度排序 - 需要后端支持，这里暂时按热度排序
                            newRecipes.sort((r1, r2) -> Integer.compare(r2.getPopularity(), r1.getPopularity()));
                        }
                        // "all" 使用默认排序

                        if (currentPage == 1) {
                            adapter.setRecipes(newRecipes);
                        } else {
                            adapter.getRecipes().addAll(newRecipes);
                            adapter.notifyDataSetChanged();
                        }

                        hasMore = adapter.getItemCount() < total;
                        currentPage++;

                        updateEmptyView();
                    } else {
                        Toast.makeText(getContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        updateEmptyView();
                    }
                } else {
                    Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                    updateEmptyView();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                String errorMsg = "网络错误: " + t.getMessage();
                if (t instanceof SocketTimeoutException) {
                    errorMsg = "请求超时，请检查网络";
                } else if (t instanceof ConnectException) {
                    errorMsg = "无法连接到服务器";
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                updateEmptyView();
            }
        });
    }

    private List<RecipeResponse> parseRecipes(Object recipesObj) {
        if (recipesObj instanceof List) {
            Gson gson = new Gson();
            List<Map<String, Object>> recipeMaps = gson.fromJson(
                    gson.toJson(recipesObj),
                    new TypeToken<List<Map<String, Object>>>() {}.getType()
            );

            List<RecipeResponse> recipes = new ArrayList<>();
            for (Map<String, Object> recipeMap : recipeMaps) {
                RecipeResponse recipe = new RecipeResponse();

                // 设置基本字段
                if (recipeMap.containsKey("recipe_id")) {
                    recipe.setRecipeId(((Number) recipeMap.get("recipe_id")).intValue());
                }
                if (recipeMap.containsKey("name")) {
                    recipe.setName((String) recipeMap.get("name"));
                }
                if (recipeMap.containsKey("image_url")) {
                    recipe.setImageUrl((String) recipeMap.get("image_url"));
                }
                if (recipeMap.containsKey("taste")) {
                    recipe.setTaste((String) recipeMap.get("taste"));
                }
                if (recipeMap.containsKey("method")) {
                    recipe.setMethod((String) recipeMap.get("method"));
                }
                if (recipeMap.containsKey("time")) {
                    recipe.setTime((String) recipeMap.get("time"));
                }
                if (recipeMap.containsKey("difficulty")) {
                    recipe.setDifficulty((String) recipeMap.get("difficulty"));
                }
                if (recipeMap.containsKey("needs")) {
                    recipe.setNeeds((String) recipeMap.get("needs"));
                }

                // 处理收藏状态（可能是数字或布尔值）
                if (recipeMap.containsKey("isFavorite")) {
                    Object favoriteObj = recipeMap.get("isFavorite");
                    if (favoriteObj instanceof Boolean) {
                        recipe.setFavorite((Boolean) favoriteObj);
                    } else if (favoriteObj instanceof Number) {
                        recipe.setFavorite(((Number) favoriteObj).intValue() == 1);
                    }
                }

                // 处理购物车状态（可能是数字或布尔值）
                if (recipeMap.containsKey("inShoppingCart")) {
                    Object cartObj = recipeMap.get("inShoppingCart");
                    if (cartObj instanceof Boolean) {
                        recipe.setInShoppingCart((Boolean) cartObj);
                    } else if (cartObj instanceof Number) {
                        recipe.setInShoppingCart(((Number) cartObj).intValue() == 1);
                    }
                }

                recipes.add(recipe);
            }
            return recipes;
        }
        return new ArrayList<>();
    }

    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("暂无菜谱");
            rvRecipes.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            rvRecipes.setVisibility(View.VISIBLE);
        }
    }
}