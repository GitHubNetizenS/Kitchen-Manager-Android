package com.example.kitchen_manager.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.Recipe;
import com.example.kitchen_manager.response.ApiResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeDetailFragment extends Fragment {

    private static final String ARG_RECIPE = "recipe";
    private FloatingActionButton fabCook;
    public static RecipeDetailFragment newInstance(Recipe recipe) {
        RecipeDetailFragment fragment = new RecipeDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECIPE, (Serializable) recipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detail, container, false);

        fabCook = view.findViewById(R.id.fab_cook);
        fabCook.setOnClickListener(v -> {
            int userId = getCurrentUserId(); // 从SharedPreferences获取
            Recipe recipe = (Recipe) getArguments().getSerializable(ARG_RECIPE);

            if (userId != -1 && recipe != null) {
                addUserHistory(userId, recipe.getRecipe_id());
            } else {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            }
        });

        if (getArguments() != null) {
            Recipe recipe = (Recipe) getArguments().getSerializable(ARG_RECIPE);
            if (recipe != null) {
                displayRecipeDetails(view, recipe);
            }
        }

        return view;
    }

    // 获取当前登录用户ID（需要根据你的用户系统实现）
    private int getCurrentUserId() {
        // 示例：从SharedPreferences获取用户ID
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1); // -1表示未登录
    }

    // 添加用户历史记录
    private void addUserHistory(int userId, int recipeId) {
        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.addUserHistory(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getCode())) {
                        Toast.makeText(getContext(), "已添加到烹饪历史", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "添加失败: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRecipeDetails(View view, Recipe recipe) {
        ImageView imageView = view.findViewById(R.id.recipe_image);
        TextView nameView = view.findViewById(R.id.recipe_name);
        TextView attributesView = view.findViewById(R.id.recipe_attributes);
        TextView ingredientsView = view.findViewById(R.id.recipe_ingredients);
        TextView stepsView = view.findViewById(R.id.recipe_steps);

        Glide.with(this).load(recipe.getImage_url()).into(imageView);
        nameView.setText(recipe.getName());

        String attributes = String.format("口味: %s\n烹饪方法: %s\n所需时间: %s\n难度: %s",
                recipe.getTaste(), recipe.getMethod(), recipe.getTime(), recipe.getDifficulty());
        attributesView.setText(attributes);

        try {
            JSONArray needsArray = new JSONArray(recipe.getNeeds());
            StringBuilder ingredients = new StringBuilder("原料:\n");
            for (int i = 0; i < needsArray.length(); i++) {
                ingredients.append("· ").append(needsArray.getString(i)).append("\n");
            }
            ingredientsView.setText(ingredients.toString());
        } catch (JSONException e) {
            ingredientsView.setText("原料信息加载失败");
        }

        try {
            JSONArray stepsArray = new JSONArray(recipe.getSteps());
            StringBuilder steps = new StringBuilder("步骤:\n");
            for (int i = 0; i < stepsArray.length(); i++) {
                steps.append(i + 1).append(". ").append(stepsArray.getString(i)).append("\n\n");
            }
            stepsView.setText(steps.toString());
        } catch (JSONException e) {
            stepsView.setText("步骤信息加载失败");
        }
    }
}