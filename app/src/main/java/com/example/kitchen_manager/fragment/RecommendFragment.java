package com.example.kitchen_manager.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

    private int userId = 1; // 示例ID
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommend, container, false);

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
        int tagId = tagMap.get(tag);

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getRecipeList(tagId, 1, 100);
        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<RecipeResponse>> apiResponse = response.body();

                    if (apiResponse.getCode() == 200) {
                        List<RecipeResponse> recipes = apiResponse.getData();

                        if (recipes != null && !recipes.isEmpty()) {
                            adapter.setRecipes(recipes);
                            rvRecipes.setVisibility(View.VISIBLE);
                        } else {
                            emptyView.setText("暂无菜谱");
                            emptyView.setVisibility(View.VISIBLE);
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