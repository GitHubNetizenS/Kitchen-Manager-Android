package com.example.kitchen_manager.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.activity.FavoriteActivity;
import com.example.kitchen_manager.activity.HistoryActivity;
import com.example.kitchen_manager.activity.LoginActivity;
import com.example.kitchen_manager.activity.MainActivity;
import com.example.kitchen_manager.activity.PreferenceActivity;
import com.example.kitchen_manager.activity.ProfileEditActivity;
import com.example.kitchen_manager.activity.RecipeDetailActivity;
import com.example.kitchen_manager.adapters.RecipeAdapter;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeResponse;
import com.example.kitchen_manager.utils.ImageLoader;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MineFragment extends Fragment {

    private TextView tvName, tvTitle, tvFavAmount, tvHistoryAmount, tvPreferenceSummary;
    private ImageView ivAvatar;
    private SharedPreferences prefs;
    private ApiService apiService;
    private int userId = -1;

    // Tab相关
    private TextView tabFavorite, tabHistory;
    private View tabIndicator;
    private LinearLayout favoriteContainer, historyContainer;
    private RecyclerView rvFavorites, rvHistory;
    private ProgressBar favProgress, historyProgress;
    private TextView favEmpty, historyEmpty;
    private View sortScroll;
    private TextView btnSortTime, btnSortMatch;

    // Adapter
    private RecipeAdapter favoriteAdapter, historyAdapter;
    private List<RecipeResponse> favoriteList = new ArrayList<>();
    private List<RecipeResponse> historyList = new ArrayList<>();

    private boolean isFavoriteTab = true;
    private boolean isTimeSort = true;

    private static final int PROFILE_EDIT_REQUEST = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initData();
        setupListeners(view);
        setupAdapters();
        loadUserData();
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_mine_name);
        tvTitle = view.findViewById(R.id.tv_mine_title);
        ivAvatar = view.findViewById(R.id.iv_mine_avatar);
        tvFavAmount = view.findViewById(R.id.fav_amount);
        tvHistoryAmount = view.findViewById(R.id.history_amount);
        tvPreferenceSummary = view.findViewById(R.id.tv_preference_summary);

        // Tab相关
        tabFavorite = view.findViewById(R.id.tab_favorite);
        tabHistory = view.findViewById(R.id.tab_history);
        tabIndicator = view.findViewById(R.id.tab_indicator);
        favoriteContainer = view.findViewById(R.id.favorite_container);
        historyContainer = view.findViewById(R.id.history_container);
        rvFavorites = view.findViewById(R.id.rv_favorites);
        rvHistory = view.findViewById(R.id.rv_history);
        favProgress = view.findViewById(R.id.fav_progress);
        historyProgress = view.findViewById(R.id.history_progress);
        favEmpty = view.findViewById(R.id.fav_empty);
        historyEmpty = view.findViewById(R.id.history_empty);
        sortScroll = view.findViewById(R.id.sort_scroll);
        btnSortTime = view.findViewById(R.id.btn_sort_time);
        btnSortMatch = view.findViewById(R.id.btn_sort_match);
    }

    private void initData() {
        prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        if (getActivity() != null && getActivity() instanceof MainActivity) {
            apiService = ((MainActivity) requireActivity()).getApiService();
        }
    }

    private void setupListeners(View view) {
        // 编辑资料按钮
        ImageView ivEdit = view.findViewById(R.id.iv_mine_avatar).getRootView().findViewById(R.id.iv_mine_avatar);
        View editButton = view.findViewById(R.id.iv_mine_avatar).getRootView().findViewById(R.id.iv_mine_avatar);
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                if (userId != -1) {
                    startActivityForResult(new Intent(getContext(), ProfileEditActivity.class), PROFILE_EDIT_REQUEST);
                } else {
                    startActivity(new Intent(getContext(), LoginActivity.class));
                }
            });
        }

        // 偏好设置
        CardView cardPreference = view.findViewById(R.id.card_preference);
        cardPreference.setOnClickListener(v -> {
            if (userId != -1) {
                startActivity(new Intent(getContext(), PreferenceActivity.class));
            } else {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        });

        // Tab切换
        tabFavorite.setOnClickListener(v -> switchTab(true));
        tabHistory.setOnClickListener(v -> switchTab(false));

        // 排序按钮
        btnSortTime.setOnClickListener(v -> {
            updateSortButtonState(true);
            isTimeSort = true;
            if (isFavoriteTab) {
                loadFavoriteRecipes(true);
            } else {
                loadHistoryRecipes(true);
            }
        });

        btnSortMatch.setOnClickListener(v -> {
            updateSortButtonState(false);
            isTimeSort = false;
            if (isFavoriteTab) {
                loadFavoriteRecipes(false);
            } else {
                loadHistoryRecipes(false);
            }
        });
    }

    private void setupAdapters() {
        // 收藏Adapter
        favoriteAdapter = new RecipeAdapter(requireContext(), favoriteList,
                new RecipeAdapter.OnItemClickListener() {
                    @Override
                    public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
                        unfavoriteRecipe(recipeId);
                    }

                    @Override
                    public void onDetailClick(int recipeId) {
                        showRecipeDetail(recipeId);
                    }

                    @Override
                    public void onCartClick(int recipeId, boolean isCurrentlyInCart) {
                        handleCartClick(recipeId, isCurrentlyInCart);
                    }
                }, RecipeAdapter.PAGE_TYPE_FAVORITE);
        rvFavorites.setLayoutManager(new GridLayoutManager(getContext(), 1));
        rvFavorites.setAdapter(favoriteAdapter);

        // 历史Adapter
        historyAdapter = new RecipeAdapter(requireContext(), historyList,
                new RecipeAdapter.OnItemClickListener() {
                    @Override
                    public void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite) {
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
                        handleCartClick(recipeId, isCurrentlyInCart);
                    }
                }, RecipeAdapter.PAGE_TYPE_HISTORY);
        rvHistory.setLayoutManager(new GridLayoutManager(getContext(), 1));
        rvHistory.setAdapter(historyAdapter);
    }

    private void switchTab(boolean showFavorite) {
        isFavoriteTab = showFavorite;

        if (showFavorite) {
            tabFavorite.setTextColor(getResources().getColor(R.color.orange));
            tabHistory.setTextColor(getResources().getColor(R.color.gray_text));
            favoriteContainer.setVisibility(View.VISIBLE);
            historyContainer.setVisibility(View.GONE);
            // 更新排序按钮文字
            btnSortTime.setText("收藏时间");
            if (favoriteList.isEmpty()) {
                loadFavoriteRecipes(isTimeSort);
            }
        } else {
            tabFavorite.setTextColor(getResources().getColor(R.color.gray_text));
            tabHistory.setTextColor(getResources().getColor(R.color.orange));
            favoriteContainer.setVisibility(View.GONE);
            historyContainer.setVisibility(View.VISIBLE);
            btnSortTime.setText("烹饪时间");
            if (historyList.isEmpty()) {
                loadHistoryRecipes(isTimeSort);
            }
        }

        // 移动指示器
        tabIndicator.animate()
                .translationX(showFavorite ? 0 : tabFavorite.getWidth())
                .setDuration(200)
                .start();

        sortScroll.setVisibility(View.VISIBLE);
    }

    private void updateSortButtonState(boolean isTime) {
        if (isTime) {
            btnSortTime.setBackgroundResource(R.drawable.bg_tab_selected);
            btnSortTime.setTextColor(getResources().getColor(android.R.color.white));
            btnSortMatch.setBackgroundResource(R.drawable.bg_tab_normal);
            btnSortMatch.setTextColor(getResources().getColor(R.color.gray));
        } else {
            btnSortTime.setBackgroundResource(R.drawable.bg_tab_normal);
            btnSortTime.setTextColor(getResources().getColor(R.color.gray));
            btnSortMatch.setBackgroundResource(R.drawable.bg_tab_selected);
            btnSortMatch.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void loadUserData() {
        if (userId != -1) {
            fetchUserProfile();
            fetchFavoriteCount();
            fetchHistoryCount();
            loadFavoriteRecipes(true);
        } else {
            tvName.setText("请登录");
            tvTitle.setText("");
            tvFavAmount.setText("0");
            tvHistoryAmount.setText("0");
            tvPreferenceSummary.setText("登录后设置偏好");
        }
    }

    private void fetchUserProfile() {
        if (userId == -1) return;

        Call<ApiResponse<com.example.kitchen_manager.models.User>> call = apiService.getUserProfile(userId);
        call.enqueue(new Callback<ApiResponse<com.example.kitchen_manager.models.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.example.kitchen_manager.models.User>> call,
                                   Response<ApiResponse<com.example.kitchen_manager.models.User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.kitchen_manager.models.User user = response.body().getData();
                    if (user != null) {
                        updateUI(user);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.example.kitchen_manager.models.User>> call, Throwable t) {
                updateUserInfoDisplay();
            }
        });
    }

    private void updateUI(com.example.kitchen_manager.models.User user) {
        if (user != null) {
            tvName.setText(user.getUsername());
            tvTitle.setText(user.getTitle());

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                // 使用 ImageLoader 工具类加载图片
                ImageLoader.loadImage(user.getAvatarUrl(), ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_logo_orange);
            }
        }
    }

    private void updateUserInfoDisplay() {
        if (userId != -1) {
            String name = prefs.getString("username", "");
            String title = prefs.getString("title", "");
            tvName.setText(name);
            tvTitle.setText(title);
        }
    }

    private void fetchFavoriteCount() {
        if (userId == -1) return;
        Call<ApiResponse<Integer>> call = apiService.getFavoriteCount(userId);
        call.enqueue(new Callback<ApiResponse<Integer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    tvFavAmount.setText(String.valueOf(response.body().getData()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                tvFavAmount.setText("0");
            }
        });
    }

    private void fetchHistoryCount() {
        if (userId == -1) return;
        Call<ApiResponse<Integer>> call = apiService.getHistoryCount(userId);
        call.enqueue(new Callback<ApiResponse<Integer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    tvHistoryAmount.setText(String.valueOf(response.body().getData()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                tvHistoryAmount.setText("0");
            }
        });
    }

    private void loadFavoriteRecipes(boolean isTimeSort) {
        if (userId == -1) return;

        favProgress.setVisibility(View.VISIBLE);
        favEmpty.setVisibility(View.GONE);

        String sortType = isTimeSort ? "time" : "match";
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getFavoriteRecipes(userId, sortType);
        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                favProgress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<RecipeResponse> recipes = response.body().getData();
                    if (recipes != null && !recipes.isEmpty()) {
                        for (RecipeResponse recipe : recipes) {
                            recipe.setFavorite(true);
                        }
                        favoriteList.clear();
                        favoriteList.addAll(recipes);
                        favoriteAdapter.notifyDataSetChanged();
                        rvFavorites.setVisibility(View.VISIBLE);
                        favEmpty.setVisibility(View.GONE);
                        checkCartStatusForRecipes(recipes, true);
                    } else {
                        favoriteList.clear();
                        favoriteAdapter.notifyDataSetChanged();
                        rvFavorites.setVisibility(View.GONE);
                        favEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    favEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                favProgress.setVisibility(View.GONE);
                favEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadHistoryRecipes(boolean isTimeSort) {
        if (userId == -1) return;

        historyProgress.setVisibility(View.VISIBLE);
        historyEmpty.setVisibility(View.GONE);

        String sortType = isTimeSort ? "time" : "match";
        Call<ApiResponse<List<RecipeResponse>>> call = apiService.getHistoryRecipes(userId, sortType);
        call.enqueue(new Callback<ApiResponse<List<RecipeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeResponse>>> call,
                                   Response<ApiResponse<List<RecipeResponse>>> response) {
                historyProgress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<RecipeResponse> recipes = response.body().getData();
                    if (recipes != null && !recipes.isEmpty()) {
                        historyList.clear();
                        historyList.addAll(recipes);
                        historyAdapter.notifyDataSetChanged();
                        rvHistory.setVisibility(View.VISIBLE);
                        historyEmpty.setVisibility(View.GONE);
                        checkCartStatusForRecipes(recipes, false);
                    } else {
                        historyList.clear();
                        historyAdapter.notifyDataSetChanged();
                        rvHistory.setVisibility(View.GONE);
                        historyEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    historyEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeResponse>>> call, Throwable t) {
                historyProgress.setVisibility(View.GONE);
                historyEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkCartStatusForRecipes(List<RecipeResponse> recipes, boolean isFavorite) {
        if (userId == -1) return;
        for (RecipeResponse recipe : recipes) {
            checkSingleCartStatus(recipe, isFavorite);
        }
    }

    private void checkSingleCartStatus(RecipeResponse recipe, boolean isFavorite) {
        Call<ApiResponse<Boolean>> call = apiService.checkIfInCart(userId, recipe.getRecipeId());
        call.enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    recipe.setInShoppingCart(response.body().getData());
                    if (isFavorite) {
                        int index = favoriteList.indexOf(recipe);
                        if (index >= 0) favoriteAdapter.notifyItemChanged(index);
                    } else {
                        int index = historyList.indexOf(recipe);
                        if (index >= 0) historyAdapter.notifyItemChanged(index);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                Log.e("MineFragment", "检查购物车状态失败");
            }
        });
    }

    private void unfavoriteRecipe(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.unfavoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    removeFromFavoriteList(recipeId);
                    Toast.makeText(getContext(), "已取消收藏", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void favoriteRecipe(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.favoriteRecipe(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    updateHistoryFavoriteStatus(recipeId, true);
                    Toast.makeText(getContext(), "已收藏", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "收藏失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFromFavoriteList(int recipeId) {
        for (int i = 0; i < favoriteList.size(); i++) {
            if (favoriteList.get(i).getRecipeId() == recipeId) {
                favoriteList.remove(i);
                favoriteAdapter.notifyItemRemoved(i);
                fetchFavoriteCount();
                break;
            }
        }
        if (favoriteList.isEmpty()) {
            favEmpty.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        }
    }

    private void updateHistoryFavoriteStatus(int recipeId, boolean isFavorite) {
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getRecipeId() == recipeId) {
                historyList.get(i).setFavorite(isFavorite);
                historyAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void handleCartClick(int recipeId, boolean isCurrentlyInCart) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCurrentlyInCart) {
            removeFromCart(recipeId);
        } else {
            addToCart(recipeId);
        }
    }

    private void addToCart(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.addToCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getCode() == 200 || response.body().getCode() == 409) {
                        updateCartStatus(recipeId, true);
                        Toast.makeText(getContext(), "已加入购物车", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFromCart(int recipeId) {
        Call<ApiResponse<Void>> call = apiService.removeFromCart(userId, recipeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    updateCartStatus(recipeId, false);
                    Toast.makeText(getContext(), "已从购物车移除", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartStatus(int recipeId, boolean inCart) {
        for (int i = 0; i < favoriteList.size(); i++) {
            if (favoriteList.get(i).getRecipeId() == recipeId) {
                favoriteList.get(i).setInShoppingCart(inCart);
                favoriteAdapter.notifyItemChanged(i);
                break;
            }
        }
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getRecipeId() == recipeId) {
                historyList.get(i).setInShoppingCart(inCart);
                historyAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void showRecipeDetail(int recipeId) {
        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROFILE_EDIT_REQUEST && resultCode == Activity.RESULT_OK) {
            fetchUserProfile();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            fetchUserProfile();
            fetchFavoriteCount();
            fetchHistoryCount();
            if (isFavoriteTab) {
                loadFavoriteRecipes(isTimeSort);
            } else {
                loadHistoryRecipes(isTimeSort);
            }
        }
    }
}