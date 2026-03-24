package com.example.kitchen_manager.fragment;

import android.graphics.Rect;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

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

    // 筛选状态
    private Set<String> selectedTastes = new HashSet<>();
    private String selectedMethod = "";
    private String selectedDifficulty = "";
    private String selectedSort = "all"; // 默认综合

    // 下拉框相关控件
    private TextView tvSort, tvMethod, tvTaste, tvDifficulty;
    private TextView tvReset; // 改为重置按钮
    private FrameLayout filterSort, filterMethod, filterTaste, filterDifficulty, filterReset; // 改为 filterReset
    private View mask;
    private FrameLayout dropdownContainer;
    private LinearLayout dropdownSort, dropdownMethod, dropdownTaste, dropdownDifficulty;
    private HorizontalScrollView hsvSelectedTags;
    private LinearLayout llSelectedTags;

    // 当前显示的下拉框
    private View activeDropdown = null;
    private TextView activeTextView = null;

    // 口味临时选择
    private Map<String, Boolean> tempTasteSelection = new HashMap<>();

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
        setupListeners();
        setupRecyclerView();
        updateButtonStates();
        loadRecipes();

        return rootView;
    }

    private void initViews() {
        rvRecipes = rootView.findViewById(R.id.rv_recipes);
        progressBar = rootView.findViewById(R.id.progressBar);
        emptyView = rootView.findViewById(R.id.emptyView);

        // 初始化筛选行控件
        tvSort = rootView.findViewById(R.id.tv_sort);
        tvMethod = rootView.findViewById(R.id.tv_method);
        tvTaste = rootView.findViewById(R.id.tv_taste);
        tvDifficulty = rootView.findViewById(R.id.tv_difficulty);
        tvReset = rootView.findViewById(R.id.tv_reset); // 改为 tvReset

        filterSort = rootView.findViewById(R.id.filter_sort);
        filterMethod = rootView.findViewById(R.id.filter_method);
        filterTaste = rootView.findViewById(R.id.filter_taste);
        filterDifficulty = rootView.findViewById(R.id.filter_difficulty);
        filterReset = rootView.findViewById(R.id.filter_reset); // 改为 filterReset

        // 初始化下拉框相关控件
        mask = rootView.findViewById(R.id.v_mask);
        dropdownContainer = rootView.findViewById(R.id.dropdown_container);
        dropdownSort = rootView.findViewById(R.id.dropdown_sort);
        dropdownMethod = rootView.findViewById(R.id.dropdown_method);
        dropdownTaste = rootView.findViewById(R.id.dropdown_taste);
        dropdownDifficulty = rootView.findViewById(R.id.dropdown_difficulty);
        hsvSelectedTags = rootView.findViewById(R.id.hsv_selected_tags);
        llSelectedTags = rootView.findViewById(R.id.ll_selected_tags);

        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        rvRecipes.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        if (rootView == null) return;

        // 点击筛选项显示下拉框
        filterSort.setOnClickListener(v -> showDropdown(dropdownSort, tvSort));
        filterMethod.setOnClickListener(v -> showDropdown(dropdownMethod, tvMethod));
        filterTaste.setOnClickListener(v -> showDropdown(dropdownTaste, tvTaste));
        filterDifficulty.setOnClickListener(v -> showDropdown(dropdownDifficulty, tvDifficulty));

        // 重置按钮点击事件
        filterReset.setOnClickListener(v -> resetAllFilters());

        // 遮罩层点击关闭下拉框
        mask.setOnClickListener(v -> hideDropdown());

        // 设置排序下拉框监听
        setupSortDropdownListeners();

        // 设置工艺下拉框监听
        setupMethodDropdownListeners();

        // 设置口味下拉框监听
        setupTasteDropdownListeners();

        // 设置难度下拉框监听
        setupDifficultyDropdownListeners();
    }

    /**
     * 重置所有筛选条件
     */
    private void resetAllFilters() {
        // 重置所有筛选条件为默认值
        selectedTastes.clear();
        selectedMethod = "";
        selectedDifficulty = "";
        selectedSort = "all";

        // 更新按钮显示状态
        updateButtonStates();

        // 隐藏下拉框（如果有打开的）
        hideDropdown();

        // 重置并重新加载数据
        resetAndLoad();

        Toast.makeText(getContext(), "已重置所有筛选条件", Toast.LENGTH_SHORT).show();
    }

    // 显示下拉框
    private void showDropdown(LinearLayout dropdown, TextView textView) {
        if (activeDropdown == dropdown) {
            hideDropdown();
            return;
        }

        hideDropdown();

        // 更新箭头方向
        resetAllArrows();
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);

        // 显示遮罩和下拉框
        mask.setVisibility(View.VISIBLE);
        dropdownContainer.setVisibility(View.VISIBLE);
        dropdown.setVisibility(View.VISIBLE);

        // 确保下拉框在遮罩层之上
        dropdownContainer.bringToFront();
        dropdown.bringToFront();

        activeDropdown = dropdown;
        activeTextView = textView;

        // 如果是口味下拉框，初始化临时选择并更新选择状态
        if (dropdown == dropdownTaste) {
            initTempTasteSelection();
            updateTasteDropdownSelection();
        }
    }

    // 隐藏下拉框
    private void hideDropdown() {
        mask.setVisibility(View.GONE);
        dropdownContainer.setVisibility(View.GONE);

        if (dropdownSort != null) dropdownSort.setVisibility(View.GONE);
        if (dropdownMethod != null) dropdownMethod.setVisibility(View.GONE);
        if (dropdownTaste != null) dropdownTaste.setVisibility(View.GONE);
        if (dropdownDifficulty != null) dropdownDifficulty.setVisibility(View.GONE);

        resetAllArrows();

        activeDropdown = null;
        activeTextView = null;
    }


    // 重置所有箭头
    private void resetAllArrows() {
        tvSort.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
        tvMethod.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
        tvTaste.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
        tvDifficulty.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
    }

    // 更新已选标签
    private void updateSelectedTags() {
        if (llSelectedTags == null) return;

        llSelectedTags.removeAllViews();

        boolean hasSelection = false;

        // 添加口味标签
        for (String taste : selectedTastes) {
            addTagView(taste, "taste");
            hasSelection = true;
        }

        // 添加工艺标签
        if (!selectedMethod.isEmpty() && !selectedMethod.equals("")) {
            addTagView(selectedMethod, "method");
            hasSelection = true;
        }

        // 添加难度标签
        if (!selectedDifficulty.isEmpty() && !selectedDifficulty.equals("")) {
            addTagView(selectedDifficulty, "difficulty");
            hasSelection = true;
        }

        hsvSelectedTags.setVisibility(hasSelection ? View.VISIBLE : View.GONE);

        // 刷新RecyclerView，更新第一个item的边距
        if (rvRecipes != null && rvRecipes.getAdapter() != null) {
            rvRecipes.getAdapter().notifyItemChanged(0);
        }
    }

    // 添加标签视图
    private void addTagView(String text, String type) {
        TextView tag = new TextView(getContext());
        tag.setText(text);
        tag.setTextSize(12);
        tag.setTextColor(getResources().getColor(R.color.orange));
        tag.setPadding(16, 6, 16, 6);
        tag.setBackgroundResource(R.drawable.bg_selected_tag);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 0);
        tag.setLayoutParams(params);

        // 点击移除
        tag.setOnClickListener(v -> {
            switch (type) {
                case "taste":
                    selectedTastes.remove(text);
                    break;
                case "method":
                    selectedMethod = "";
                    break;
                case "difficulty":
                    selectedDifficulty = "";
                    break;
            }
            updateButtonStates();
            updateSelectedTags();
            resetAndLoad();
        });

        llSelectedTags.addView(tag);
    }

    // 设置排序下拉框监听
    private void setupSortDropdownListeners() {
        rootView.findViewById(R.id.dropdown_sort_all).setOnClickListener(v -> {
            selectedSort = "all";
            tvSort.setText("综合");
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_sort_ingredient).setOnClickListener(v -> {
            selectedSort = "ingredient_match";
            tvSort.setText("食材匹配");
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_sort_tag).setOnClickListener(v -> {
            selectedSort = "tag_match";
            tvSort.setText("标签匹配");
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });
    }

    // 设置工艺下拉框监听
    private void setupMethodDropdownListeners() {
        rootView.findViewById(R.id.dropdown_method_all).setOnClickListener(v -> {
            selectedMethod = "";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_fry).setOnClickListener(v -> {
            selectedMethod = "煎";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_stir).setOnClickListener(v -> {
            selectedMethod = "炒";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_boil).setOnClickListener(v -> {
            selectedMethod = "煮";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_deepfry).setOnClickListener(v -> {
            selectedMethod = "炸";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_stew).setOnClickListener(v -> {
            selectedMethod = "炖";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_pickle).setOnClickListener(v -> {
            selectedMethod = "腌";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_roast).setOnClickListener(v -> {
            selectedMethod = "烧";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_mix).setOnClickListener(v -> {
            selectedMethod = "拌";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_method_other).setOnClickListener(v -> {
            selectedMethod = "其他";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });
    }

    // 设置口味下拉框监听
    private void setupTasteDropdownListeners() {
        rootView.findViewById(R.id.dropdown_taste_all).setOnClickListener(v -> {
            tempTasteSelection.clear();
            updateTasteDropdownSelection();
        });

        rootView.findViewById(R.id.dropdown_taste_sour).setOnClickListener(v -> {
            toggleTasteSelection("酸");
        });

        rootView.findViewById(R.id.dropdown_taste_sweet).setOnClickListener(v -> {
            toggleTasteSelection("甜");
        });

        rootView.findViewById(R.id.dropdown_taste_bitter).setOnClickListener(v -> {
            toggleTasteSelection("苦");
        });

        rootView.findViewById(R.id.dropdown_taste_spicy).setOnClickListener(v -> {
            toggleTasteSelection("辣");
        });

        rootView.findViewById(R.id.dropdown_taste_salty).setOnClickListener(v -> {
            toggleTasteSelection("咸");
        });

        rootView.findViewById(R.id.dropdown_taste_umami).setOnClickListener(v -> {
            toggleTasteSelection("鲜");
        });

        rootView.findViewById(R.id.dropdown_taste_confirm).setOnClickListener(v -> {
            // 应用口味选择
            selectedTastes.clear();
            selectedTastes.addAll(tempTasteSelection.keySet());
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });
    }

    // 设置难度下拉框监听
    private void setupDifficultyDropdownListeners() {
        rootView.findViewById(R.id.dropdown_diff_all).setOnClickListener(v -> {
            selectedDifficulty = "";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_diff_easy).setOnClickListener(v -> {
            selectedDifficulty = "简单";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_diff_normal).setOnClickListener(v -> {
            selectedDifficulty = "普通";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_diff_hard).setOnClickListener(v -> {
            selectedDifficulty = "高级";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });

        rootView.findViewById(R.id.dropdown_diff_god).setOnClickListener(v -> {
            selectedDifficulty = "神级";
            hideDropdown();
            updateButtonStates();
            resetAndLoad();
        });
    }

    // 初始化口味临时选择
    private void initTempTasteSelection() {
        tempTasteSelection.clear();
        for (String taste : selectedTastes) {
            tempTasteSelection.put(taste, true);
        }
    }

    // 切换口味选择
    private void toggleTasteSelection(String taste) {
        if (tempTasteSelection.containsKey(taste)) {
            tempTasteSelection.remove(taste);
        } else {
            tempTasteSelection.put(taste, true);
        }
        updateTasteDropdownSelection();
    }

    // 更新口味下拉框选择状态
    private void updateTasteDropdownSelection() {
        TextView tvAll = rootView.findViewById(R.id.dropdown_taste_all);
        TextView tvSour = rootView.findViewById(R.id.dropdown_taste_sour);
        TextView tvSweet = rootView.findViewById(R.id.dropdown_taste_sweet);
        TextView tvBitter = rootView.findViewById(R.id.dropdown_taste_bitter);
        TextView tvSpicy = rootView.findViewById(R.id.dropdown_taste_spicy);
        TextView tvSalty = rootView.findViewById(R.id.dropdown_taste_salty);
        TextView tvUmami = rootView.findViewById(R.id.dropdown_taste_umami);

        // 更新选择状态
        updateTasteItemSelection(tvAll, tempTasteSelection.isEmpty());
        updateTasteItemSelection(tvSour, tempTasteSelection.containsKey("酸"));
        updateTasteItemSelection(tvSweet, tempTasteSelection.containsKey("甜"));
        updateTasteItemSelection(tvBitter, tempTasteSelection.containsKey("苦"));
        updateTasteItemSelection(tvSpicy, tempTasteSelection.containsKey("辣"));
        updateTasteItemSelection(tvSalty, tempTasteSelection.containsKey("咸"));
        updateTasteItemSelection(tvUmami, tempTasteSelection.containsKey("鲜"));
    }

    // 更新口味项选择状态
    private void updateTasteItemSelection(TextView tv, boolean selected) {
        tv.setSelected(selected);
        if (selected) {
            tv.setTextColor(getResources().getColor(R.color.orange));
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
        } else {
            tv.setTextColor(getResources().getColor(android.R.color.black));
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    // 获取排序文本
    private String getSortText(String sort) {
        switch (sort) {
            case "all": return "综合";
            case "ingredient_match": return "食材匹配";
            case "tag_match": return "标签匹配";
            default: return "综合";
        }
    }

    private void updateButtonStates() {
        if (rootView == null) return;

        // 更新显示文本
        if (!selectedTastes.isEmpty()) {
            tvTaste.setText(selectedTastes.iterator().next() +
                    (selectedTastes.size() > 1 ? "等" + selectedTastes.size() + "种" : ""));
        } else {
            tvTaste.setText("口味");
        }

        tvMethod.setText(selectedMethod.isEmpty() ? "工艺" : selectedMethod);
        tvDifficulty.setText(selectedDifficulty.isEmpty() ? "难度" : selectedDifficulty);
        tvSort.setText(getSortText(selectedSort));

        // 更新已选标签
        updateSelectedTags();
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
                        updateLocalFavoriteStatus(recipeId, true);
                        Toast.makeText(requireContext(), "收藏成功", Toast.LENGTH_SHORT).show();
                    } else if (apiResponse.getCode() == 409) {
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

        // 添加一个透明的占位视图，确保第一个item不会被头部遮挡
        rvRecipes.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                // 注意：这里不要调用 super.getItemOffsets()

                // 为第一个item添加顶部边距，避免被已选标签遮挡
                int position = parent.getChildAdapterPosition(view);
                if (position == 0) {
                    // 获取已选标签的高度
                    if (hsvSelectedTags != null && hsvSelectedTags.getVisibility() == View.VISIBLE) {
                        // 如果已选标签可见，增加额外的边距
                        int tagHeight = hsvSelectedTags.getHeight();
                        if (tagHeight == 0) {
                            // 如果高度还没测量，使用一个默认值
                            tagHeight = dpToPx(30); // 默认30dp
                        }
                        outRect.top = tagHeight;
                    } else {
                        // 如果没有已选标签，添加一个较小的边距
                        outRect.top = dpToPx(2); // 2dp的间距
                    }
                } else {
                    // 非第一个item，添加一个较小的底部边距用于间隔
                    outRect.bottom = dpToPx(4);
                }
            }
        });

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

                // 滑动时隐藏下拉框
                if (dy != 0 && activeDropdown != null) {
                    hideDropdown();
                }
            }
        });
    }

    // 添加一个工具方法，将dp转换为px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
                selectedSort,
                userId,
                currentPage,
                pageSize);

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
                        List<RecipeResponse> newRecipes = parseRecipes(data.get("recipes"));

                        int total = 0;
                        if (data.containsKey("total_count")) {
                            total = ((Number) data.get("total_count")).intValue();
                        }

                        int totalPages = (int) Math.ceil((double) total / pageSize);

                        if (currentPage == 1) {
                            adapter.setRecipes(newRecipes);
                        } else {
                            adapter.getRecipes().addAll(newRecipes);
                            adapter.notifyDataSetChanged();
                        }

                        hasMore = currentPage < totalPages;
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

                if (recipeMap.containsKey("isFavorite")) {
                    Object favoriteObj = recipeMap.get("isFavorite");
                    if (favoriteObj instanceof Boolean) {
                        recipe.setFavorite((Boolean) favoriteObj);
                    } else if (favoriteObj instanceof Number) {
                        recipe.setFavorite(((Number) favoriteObj).intValue() == 1);
                    }
                }

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