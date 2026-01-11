package com.example.kitchen_manager.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.User;
import com.example.kitchen_manager.response.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ToBuyFragment extends Fragment {
    private RecyclerView rvRecipes;
    private ShoppingCartAdapter adapter;
    private List<Map<String, Object>> shoppingCartRecipes = new ArrayList<>();
    private ApiService apiService;
    private User currentUser;
    private SharedPreferences prefs;
    private int userId = -1;
    private ProgressBar progressBar;
    private TextView empty;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_cart, container, false);
        apiService = ApiClient.getApiService();

        // 获取用户ID
        prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        progressBar = view.findViewById(R.id.progressBar);
        empty = view.findViewById(R.id.emptyView);

        // 初始化RecyclerView
        adapter = new ShoppingCartAdapter();
        rvRecipes = view.findViewById(R.id.rv_tobuy);
        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecipes.setAdapter(adapter);

        // 设置初始状态
        progressBar.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.VISIBLE);

        // 加载购物车数据
        loadShoppingCartData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当返回时重新加载数据
        if (userId != -1) {
            loadShoppingCartData();
        }
    }

    private void loadShoppingCartData() {
        if (userId == -1) {
            empty.setText("请先登录");
            empty.setVisibility(View.VISIBLE);
            rvRecipes.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        rvRecipes.setVisibility(View.GONE);

        // 使用 API 接口获取购物车数据
        apiService.getShoppingCartRecipes(userId)
                .enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call,
                                           Response<ApiResponse<List<Map<String, Object>>>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            shoppingCartRecipes = response.body().getData();

                            if (shoppingCartRecipes == null || shoppingCartRecipes.isEmpty()) {
                                empty.setVisibility(View.VISIBLE);
                                empty.setText("购物车为空，请先添加菜谱");
                                rvRecipes.setVisibility(View.GONE);
                            } else {
                                empty.setVisibility(View.GONE);
                                rvRecipes.setVisibility(View.VISIBLE);
                                adapter.setRecipes(shoppingCartRecipes);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            empty.setVisibility(View.VISIBLE);
                            empty.setText("加载失败，请重试");
                            rvRecipes.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        empty.setVisibility(View.VISIBLE);
                        empty.setText("网络错误，请检查网络连接");
                        rvRecipes.setVisibility(View.GONE);
                        t.printStackTrace();
                    }
                });
    }

    private class ShoppingCartAdapter extends RecyclerView.Adapter<ShoppingCartAdapter.RecipeViewHolder> {
        private List<Map<String, Object>> recipes = new ArrayList<>();

        public void setRecipes(List<Map<String, Object>> recipes) {
            this.recipes = recipes;
        }

        @NonNull
        @Override
        public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tobuy_card, parent, false);
            return new RecipeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
            Map<String, Object> recipe = recipes.get(position);

            // 设置菜谱名称
            String recipeName = (String) recipe.get("recipeName");
            if (recipeName != null) {
                holder.recipeName.setText(recipeName);
            } else {
                holder.recipeName.setText("未知菜谱");
            }

            // 设置图片
            String imageUrl = (String) recipe.get("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 确保 URL 是完整的
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "http://10.0.2.2:8080" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                }

                Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(holder.recipeImage);
            } else {
                holder.recipeImage.setImageResource(R.drawable.placeholder);
            }

            // 设置购买进度
            Object totalObj = recipe.get("totalIngredients");
            Object purchasedObj = recipe.get("purchasedCount");
            int totalIngredients = 0;
            int purchasedCount = 0;

            if (totalObj instanceof Number) {
                totalIngredients = ((Number) totalObj).intValue();
            }
            if (purchasedObj instanceof Number) {
                purchasedCount = ((Number) purchasedObj).intValue();
            }

            holder.tvProgress.setText("购买进度: " + purchasedCount + "/" + totalIngredients);

            // 设置图片点击事件 - 跳转到菜谱详情
            holder.recipeImage.setOnClickListener(v -> {
                Object recipeIdObj = recipe.get("recipeId");
                if (recipeIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    navigateToRecipeDetail(recipeId);
                }
            });

            // 设置删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> {
                Object recipeIdObj = recipe.get("recipeId");
                if (recipeIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    deleteRecipeFromCart(recipeId, position);
                }
            });

            // 设置食材列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ingredients = (List<Map<String, Object>>) recipe.get("ingredients");
            if (ingredients != null && !ingredients.isEmpty()) {
                IngredientAdapter ingredientAdapter = new IngredientAdapter(ingredients, recipe, holder);
                holder.rvIngredients.setLayoutManager(new LinearLayoutManager(getContext()));
                holder.rvIngredients.setAdapter(ingredientAdapter);
                holder.rvIngredients.setVisibility(View.VISIBLE);
            } else {
                holder.rvIngredients.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return recipes.size();
        }

        class RecipeViewHolder extends RecyclerView.ViewHolder {
            TextView recipeName;
            ImageView recipeImage;
            ImageView btnDelete;
            RecyclerView rvIngredients;
            TextView tvProgress;

            public RecipeViewHolder(@NonNull View itemView) {
                super(itemView);
                recipeName = itemView.findViewById(R.id.recipe_name);
                recipeImage = itemView.findViewById(R.id.imageView);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                rvIngredients = itemView.findViewById(R.id.rv_ingredients);
                tvProgress = itemView.findViewById(R.id.tv_progress);
            }
        }
    }

    private class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {
        private List<Map<String, Object>> ingredients;
        private Map<String, Object> recipe;
        private ShoppingCartAdapter.RecipeViewHolder parentHolder;

        public IngredientAdapter(List<Map<String, Object>> ingredients,
                                 Map<String, Object> recipe,
                                 ShoppingCartAdapter.RecipeViewHolder parentHolder) {
            this.ingredients = ingredients;
            this.recipe = recipe;
            this.parentHolder = parentHolder;
        }

        @NonNull
        @Override
        public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ingredient_item, parent, false);
            return new IngredientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
            Map<String, Object> ingredient = ingredients.get(position);

            // 设置食材名称
            String ingredientName = (String) ingredient.get("ingredientName");
            if (ingredientName != null) {
                holder.cbIngredient.setText(ingredientName);
            } else {
                holder.cbIngredient.setText("未知食材");
            }

            // 设置购买状态
            String status = (String) ingredient.get("status");
            boolean isPurchased = "purchased".equals(status);
            holder.cbIngredient.setChecked(isPurchased);
            holder.tvStatus.setText(isPurchased ? "已购买" : "待购买");
            holder.tvStatus.setTextColor(isPurchased ?
                    getResources().getColor(android.R.color.holo_green_dark) :
                    getResources().getColor(android.R.color.darker_gray));

            // 设置复选框点击事件
            holder.cbIngredient.setOnClickListener(v -> {
                boolean newStatus = holder.cbIngredient.isChecked();
                String statusStr = newStatus ? "purchased" : "pending";

                Object recipeIdObj = recipe.get("recipeId");
                Object ingredientIdObj = ingredient.get("ingredientId");

                if (recipeIdObj instanceof Number && ingredientIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    int ingredientId = ((Number) ingredientIdObj).intValue();
                    updateIngredientStatus(recipeId, ingredientId, statusStr,
                            getRecipePosition(recipeId), position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return ingredients.size();
        }

        class IngredientViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbIngredient;
            TextView tvStatus;

            public IngredientViewHolder(@NonNull View itemView) {
                super(itemView);
                cbIngredient = itemView.findViewById(R.id.cb_ingredient);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
    }

    private int getRecipePosition(int recipeId) {
        for (int i = 0; i < shoppingCartRecipes.size(); i++) {
            Object idObj = shoppingCartRecipes.get(i).get("recipeId");
            if (idObj instanceof Number && ((Number) idObj).intValue() == recipeId) {
                return i;
            }
        }
        return -1;
    }

    private void navigateToRecipeDetail(int recipeId) {
        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    private void updateIngredientStatus(int recipeId, int ingredientId, String status,
                                        int recipePosition, int ingredientPosition) {
        if (userId == -1 || apiService == null) return;

        apiService.updateIngredientStatus(userId, recipeId, ingredientId, status)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // 更新本地数据
                            if (recipePosition >= 0 && recipePosition < shoppingCartRecipes.size()) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> ingredients =
                                        (List<Map<String, Object>>) shoppingCartRecipes.get(recipePosition).get("ingredients");

                                if (ingredientPosition >= 0 && ingredientPosition < ingredients.size()) {
                                    ingredients.get(ingredientPosition).put("status", status);
                                    ingredients.get(ingredientPosition).put("isPurchased", "purchased".equals(status));

                                    // 重新计算购买进度
                                    int purchasedCount = 0;
                                    for (Map<String, Object> ing : ingredients) {
                                        if ("purchased".equals(ing.get("status"))) {
                                            purchasedCount++;
                                        }
                                    }

                                    shoppingCartRecipes.get(recipePosition).put("purchasedCount", purchasedCount);
                                    adapter.notifyItemChanged(recipePosition);

                                    Toast.makeText(getContext(), "状态已更新", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "更新失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(getContext(), "网络错误，请检查连接", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteRecipeFromCart(int recipeId, int position) {
        if (userId == -1 || apiService == null) return;

        // 显示确认对话框
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定要从购物车中删除这个菜谱吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    apiService.deleteRecipeFromCart(userId, recipeId)
                            .enqueue(new Callback<ApiResponse<Void>>() {
                                @Override
                                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                        // 从列表中移除
                                        if (position >= 0 && position < shoppingCartRecipes.size()) {
                                            shoppingCartRecipes.remove(position);
                                            adapter.notifyItemRemoved(position);
                                            Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();

                                            // 如果列表为空，显示空状态
                                            if (shoppingCartRecipes.isEmpty()) {
                                                empty.setVisibility(View.VISIBLE);
                                                empty.setText("购物车为空，请先添加菜谱");
                                                rvRecipes.setVisibility(View.GONE);
                                            }
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "删除失败，请重试", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                    Toast.makeText(getContext(), "网络错误，请检查连接", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("取消", null)
                .show();
    }
}