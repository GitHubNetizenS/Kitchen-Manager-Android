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
import com.example.kitchen_manager.activity.MainActivity;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.User;
import com.example.kitchen_manager.response.ApiResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ToBuyFragment extends Fragment {
    private RecyclerView tobuy;
    private ShoppingCartAdapter adapter;
    private List<Map<String, Object>> shoppingCartRecipes = new ArrayList<>();
    private ApiService apiService;
    private User currentUser;
    private SharedPreferences prefs;
    private int userId = -1;
    private ImageView add;
    private ProgressBar progressBar;
    private TextView empty;

    public ToBuyFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_cart, container, false);
        apiService = ApiClient.getApiService();

        // 获取用户ID
        prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        add = view.findViewById(R.id.iv_add);
        progressBar = view.findViewById(R.id.progressBar);
        empty = view.findViewById(R.id.emptyView);

        // 初始化RecyclerView
        adapter = new ShoppingCartAdapter();
        tobuy.setLayoutManager(new LinearLayoutManager(getContext()));
        tobuy.setAdapter(adapter);

        // 设置初始状态
        progressBar.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        tobuy.setVisibility(View.VISIBLE);

        // 设置添加按钮点击事件 - 由于在侧边栏，可能需要不同的处理
        add.setOnClickListener(v -> {
            // 关闭侧边栏并跳转到推荐页面
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.closeDrawerAndNavigateToRecommend();
            }
        });
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
            tobuy.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);

        // 使用正确的 API 接口
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
                                empty.setText("购物车为空");
                                tobuy.setVisibility(View.GONE);
                            } else {
                                empty.setVisibility(View.GONE);
                                tobuy.setVisibility(View.VISIBLE);
                                adapter.setRecipes(shoppingCartRecipes);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            empty.setVisibility(View.VISIBLE);
                            empty.setText("加载失败");
                            tobuy.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        empty.setVisibility(View.VISIBLE);
                        empty.setText("网络错误");
                        tobuy.setVisibility(View.GONE);
                        t.printStackTrace();
                    }
                });
    }

    private class ShoppingCartAdapter extends RecyclerView.Adapter<ShoppingCartAdapter.ViewHolder> {
        private List<Map<String, Object>> recipes = new ArrayList<>();

        public void setRecipes(List<Map<String, Object>> recipes) {
            this.recipes = recipes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tobuy_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

            // 设置图片点击事件 - 跳转到菜谱详情
            holder.recipeImage.setOnClickListener(v -> {
                Object recipeIdObj = recipe.get("recipeId");
                if (recipeIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    navigateToRecipeDetail(recipeId);
                }
            });

            // 设置复选框状态
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

            boolean allPurchased = totalIngredients > 0 && purchasedCount == totalIngredients;

            holder.checkboxBought.setChecked(allPurchased);
            holder.checkboxBought.setText(allPurchased ?
                    "已购买 (" + purchasedCount + "/" + totalIngredients + ")" :
                    "已购买 (" + purchasedCount + "/" + totalIngredients + ")");

            // 复选框点击事件
            holder.checkboxBought.setOnClickListener(v -> {
                boolean isChecked = holder.checkboxBought.isChecked();
                String newStatus = isChecked ? "purchased" : "pending";
                Object recipeIdObj = recipe.get("recipeId");

                if (recipeIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    updateAllIngredientsStatus(recipeId, newStatus, position);
                }
            });

            // 删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> {
                Object recipeIdObj = recipe.get("recipeId");
                if (recipeIdObj instanceof Number) {
                    int recipeId = ((Number) recipeIdObj).intValue();
                    deleteRecipeFromCart(recipeId, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return recipes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView recipeName;
            ImageView recipeImage;
            CheckBox checkboxBought;
            ImageView btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                recipeName = itemView.findViewById(R.id.recipe_name);
                recipeImage = itemView.findViewById(R.id.imageView);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    private void navigateToRecipeDetail(int recipeId) {
        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    private void updateAllIngredientsStatus(int recipeId, String status, int position) {
        if (userId == -1 || apiService == null) return;

        apiService.updateAllCartIngredientsStatus(userId, recipeId, status)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // 更新本地数据
                            if (position >= 0 && position < shoppingCartRecipes.size()) {
                                Map<String, Object> recipe = shoppingCartRecipes.get(position);
                                Object totalObj = recipe.get("totalIngredients");
                                int totalIngredients = 0;
                                if (totalObj instanceof Number) {
                                    totalIngredients = ((Number) totalObj).intValue();
                                }

                                if ("purchased".equals(status)) {
                                    recipe.put("purchasedCount", totalIngredients);
                                } else {
                                    recipe.put("purchasedCount", 0);
                                }
                                adapter.notifyItemChanged(position);
                                Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "更新失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteRecipeFromCart(int recipeId, int position) {
        if (userId == -1 || apiService == null) return;

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
                                    tobuy.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}