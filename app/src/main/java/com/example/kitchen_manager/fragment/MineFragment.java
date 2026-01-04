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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.kitchen_manager.activity.FavoriteActivity;
import com.example.kitchen_manager.activity.HistoryActivity;
import com.example.kitchen_manager.activity.LoginActivity;
import com.example.kitchen_manager.activity.MainActivity;
import com.example.kitchen_manager.activity.ProfileEditActivity;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.Tag;
import com.example.kitchen_manager.models.User;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.TagResponse;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MineFragment extends Fragment {
    private TextView tvName, tvTitle, tvFavAmount, tvHistoryAmount;
    private ImageView ivAvatar;
    private SharedPreferences prefs;
    private ApiService apiService;
    private CardView cardFavorites, cardHistory, cardTaste, cardPeople, cardSpecial;

    // 添加 userId 成员变量
    private int userId = -1;

    // 存储用户选择的标签
    private Map<String, Set<Integer>> selectedTags = new HashMap<>();
    private Map<String, List<Tag>> categoryTags = new HashMap<>();

    private static final int PROFILE_EDIT_REQUEST = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tv_mine_name);
        tvTitle = view.findViewById(R.id.tv_mine_title);
        ivAvatar = view.findViewById(R.id.iv_mine_avatar);
        tvFavAmount = view.findViewById(R.id.fav_amount);
        tvHistoryAmount = view.findViewById(R.id.history_amount);
        cardFavorites = view.findViewById(R.id.card_favorites);
        cardHistory = view.findViewById(R.id.card_history);
        cardTaste = view.findViewById(R.id.taste);
        cardPeople = view.findViewById(R.id.people);
        cardSpecial = view.findViewById(R.id.special);

        prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);

        // 初始化API服务
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            apiService = ((MainActivity) requireActivity()).getApiService();
        }

        // 获取用户ID
        userId = prefs.getInt("user_id", -1);

        // 初始化标签分类
        selectedTags.put("菜品口味", new HashSet<>());
        selectedTags.put("适用人群", new HashSet<>());
        selectedTags.put("特殊需求", new HashSet<>());

        // 先显示加载状态
        tvName.setText("加载中...");
        tvTitle.setText("");
        ivAvatar.setImageResource(R.drawable.ic_logo_orange);
        tvFavAmount.setText("0");
        tvHistoryAmount.setText("0");

        // 先显示本地缓存数据
        updateUserInfoDisplay();

        // 获取最新用户信息
        fetchUserProfile();

        // 获取收藏数量
        fetchFavoriteCount();

        // 获取烹饪记录数量
        fetchHistoryCount();

        // 加载标签数据
        loadTags();

        // 用户信息卡片点击事件
        CardView cardUserInfo = view.findViewById(R.id.card_user_info);
        cardUserInfo.setOnClickListener(v -> {
            if (userId != -1) {
                Intent intent = new Intent(getContext(), ProfileEditActivity.class);
                startActivityForResult(intent, PROFILE_EDIT_REQUEST);
            } else {
                // 如果未登录，跳转到登录页面
                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        // 收藏菜谱卡片点击事件
        cardFavorites.setOnClickListener(v -> {
            if (userId != -1) {
                // 跳转到收藏页面
                Intent intent = new Intent(getContext(), FavoriteActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        });

        // 烹饪记录卡片点击事件
        cardHistory.setOnClickListener(v -> {
            if (userId != -1) {
                // 跳转到历史记录页面
                Intent intent = new Intent(getContext(), HistoryActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        });

        // 菜品口味卡片点击事件
        cardTaste.setOnClickListener(v -> showTagSelectionDialog("菜品口味"));

        // 适用人群卡片点击事件
        cardPeople.setOnClickListener(v -> showTagSelectionDialog("适用人群"));

        // 特殊需求卡片点击事件
        cardSpecial.setOnClickListener(v -> showTagSelectionDialog("特殊需求"));
    }

    // 显示标签选择对话框
    private void showTagSelectionDialog(String category) {
        if (userId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Tag> tags = categoryTags.get(category);
        if (tags == null || tags.isEmpty()) {
            Toast.makeText(getContext(), "标签数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建标签名称数组
        CharSequence[] tagNames = new CharSequence[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagNames[i] = tags.get(i).getName();
        }

        // 获取当前选中的标签
        Set<Integer> selectedIds = selectedTags.get(category);
        boolean[] checkedItems = new boolean[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            checkedItems[i] = selectedIds.contains(tags.get(i).getId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择" + category);
        builder.setMultiChoiceItems(tagNames, checkedItems, (dialog, which, isChecked) -> {
            // 不需要在这里处理，在确定按钮中处理
        });

        builder.setPositiveButton("确定", (dialog, which) -> {
            // 获取选中的标签
            Set<Integer> newSelectedIds = new HashSet<>();
            AlertDialog alertDialog = (AlertDialog) dialog;
            for (int i = 0; i < tags.size(); i++) {
                if (alertDialog.getListView().isItemChecked(i)) {
                    newSelectedIds.add(tags.get(i).getId());
                }
            }

            // 更新选择
            selectedTags.put(category, newSelectedIds);

            // 保存到服务器
            saveUserTags(category, newSelectedIds);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 保存用户选择的标签
    private void saveUserTags(String category, Set<Integer> tagIds) {
        List<Integer> tagIdList = new ArrayList<>(tagIds);
        Gson gson = new Gson();
        String tagIdsJson = gson.toJson(tagIdList);

        Call<ApiResponse<Void>> call = apiService.saveUserTags(
                userId,
                category,
                tagIdsJson
        );

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "保存失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "保存失败: 服务器错误", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 加载标签数据
    private void loadTags() {
        Call<ApiResponse<List<TagResponse>>> call = apiService.getAllTags();
        call.enqueue(new Callback<ApiResponse<List<TagResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TagResponse>>> call, Response<ApiResponse<List<TagResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<TagResponse>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        List<TagResponse> tagResponses = apiResponse.getData();
                        processTags(tagResponses);

                        // 加载用户已选择的标签
                        loadUserSelectedTags();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TagResponse>>> call, Throwable t) {
                Log.e("MineFragment", "加载标签失败", t);
            }
        });
    }

    // 处理标签数据
    private void processTags(List<TagResponse> tagResponses) {
        categoryTags.clear();
        for (TagResponse tag : tagResponses) {
            String category = tag.getCategory();
            if (!categoryTags.containsKey(category)) {
                categoryTags.put(category, new ArrayList<>());
            }
            categoryTags.get(category).add(new Tag(tag.getId(), tag.getName()));
        }
    }

    // 加载用户已选择的标签
    private void loadUserSelectedTags() {
        if (userId == -1) return;

        Call<ApiResponse<Map<String, List<Integer>>>> call = apiService.getUserTags(userId);
        call.enqueue(new Callback<ApiResponse<Map<String, List<Integer>>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, List<Integer>>>> call, Response<ApiResponse<Map<String, List<Integer>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, List<Integer>>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Map<String, List<Integer>> userTags = apiResponse.getData();
                        for (String category : userTags.keySet()) {
                            Set<Integer> tagIds = new HashSet<>(userTags.get(category));
                            selectedTags.put(category, tagIds);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, List<Integer>>>> call, Throwable t) {
                Log.e("MineFragment", "加载用户标签失败", t);
            }
        });
    }

    // 更新用户信息显示
    private void updateUserInfoDisplay() {
        if (userId != -1) {
            String name = prefs.getString("username", "");
            String title = prefs.getString("title", "");
            String avatarUrl = prefs.getString("avatar_url", "");
            int favCount = prefs.getInt("favorite_count", 0);
            int historyCount = prefs.getInt("history_count", 0);

            tvName.setText(name);
            tvTitle.setText(title);
            tvFavAmount.setText(String.valueOf(favCount));
            tvHistoryAmount.setText(String.valueOf(historyCount));

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Picasso.get()
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_logo_orange)
                        .error(R.drawable.ic_logo_orange)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_logo_orange);
            }
        } else {
            tvName.setText("请登录");
            tvTitle.setText("");
            ivAvatar.setImageResource(R.drawable.ic_logo_orange);
            tvFavAmount.setText("0");
            tvHistoryAmount.setText("0");
        }
    }

    // 获取用户资料
    private void fetchUserProfile() {
        if (userId != -1) {
            // 显示加载指示器
            tvName.setText("加载中...");
            tvTitle.setText("");

            Call<ApiResponse<User>> call = apiService.getUserProfile(userId);
            call.enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<User> apiResponse = response.body();
                        if (apiResponse.getCode() == 200) {
                            User user = apiResponse.getData();
                            // 保存到SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("username", user.getUsername());
                            editor.putString("phone", user.getPhone());
                            editor.putString("avatar_url", user.getAvatarUrl());
                            editor.putString("title", user.getTitle());

                            editor.apply();

                            // 更新显示
                            updateUI(user);
                        }
                    } else {
                        // 使用缓存数据
                        updateUserInfoDisplay();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    // 使用缓存数据
                    updateUserInfoDisplay();
                }
            });
        } else {
            updateUserInfoDisplay();
        }
    }

    // 获取收藏数量
    private void fetchFavoriteCount() {
        if (userId != -1) {
            Call<ApiResponse<Integer>> call = apiService.getFavoriteCount(userId);
            call.enqueue(new Callback<ApiResponse<Integer>>() {
                @Override
                public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Integer> apiResponse = response.body();
                        if (apiResponse.getCode() == 200) {
                            int count = apiResponse.getData();
                            tvFavAmount.setText(String.valueOf(count));

                            // 保存到缓存
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("favorite_count", count);
                            editor.apply();
                        }
                    } else {
                        // 使用缓存数据
                        int cachedCount = prefs.getInt("favorite_count", 0);
                        tvFavAmount.setText(String.valueOf(cachedCount));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                    // 使用缓存数据
                    int cachedCount = prefs.getInt("favorite_count", 0);
                    tvFavAmount.setText(String.valueOf(cachedCount));
                }
            });
        } else {
            tvFavAmount.setText("0");
        }
    }

    // 获取历史记录数量
    private void fetchHistoryCount() {
        if (userId != -1) {
            Call<ApiResponse<Integer>> call = apiService.getHistoryCount(userId);
            call.enqueue(new Callback<ApiResponse<Integer>>() {
                @Override
                public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Integer> apiResponse = response.body();
                        if (apiResponse.getCode() == 200) {
                            int count = apiResponse.getData();
                            tvHistoryAmount.setText(String.valueOf(count));

                            // 保存到缓存
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("history_count", count);
                            editor.apply();
                        }
                    } else {
                        // 使用缓存数据
                        int cachedCount = prefs.getInt("history_count", 0);
                        tvHistoryAmount.setText(String.valueOf(cachedCount));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                    // 使用缓存数据
                    int cachedCount = prefs.getInt("history_count", 0);
                    tvHistoryAmount.setText(String.valueOf(cachedCount));
                }
            });
        } else {
            tvHistoryAmount.setText("0");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROFILE_EDIT_REQUEST && resultCode == Activity.RESULT_OK) {
            // 直接更新本地数据
            updateUserInfoDisplay();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次显示时刷新数据
        fetchUserProfile();
        fetchFavoriteCount();
        fetchHistoryCount();
    }

    private void updateUI(User user) {
        if (user != null) {
            tvName.setText(user.getUsername());
            tvTitle.setText(user.getTitle());

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                // 添加时间戳避免缓存问题
                String urlWithTimestamp = user.getAvatarUrl() + "?t=" + System.currentTimeMillis();

                Picasso.get()
                        .load(urlWithTimestamp)
                        .placeholder(R.drawable.ic_logo_orange)
                        .error(R.drawable.ic_logo_orange)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_logo_orange);
            }
        } else {
            tvName.setText("获取信息失败");
            tvTitle.setText("");
            ivAvatar.setImageResource(R.drawable.ic_logo_orange);
        }
    }
}