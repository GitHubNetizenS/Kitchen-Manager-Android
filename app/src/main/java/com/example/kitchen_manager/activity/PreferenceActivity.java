package com.example.kitchen_manager.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.models.Tag;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.TagResponse;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferenceActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvTasteSummary, tvPeopleSummary, tvSpecialSummary;
    private LinearLayout tasteTagsContainer, peopleTagsContainer, specialTagsContainer;
    private ApiService apiService;
    private int userId;
    private SharedPreferences prefs;

    private Map<String, List<Tag>> categoryTags = new HashMap<>();
    private Map<String, Set<Integer>> selectedTags = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getApiService();

        initViews();
        setupListeners();
        loadTags();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvTasteSummary = findViewById(R.id.tv_taste_summary);
        tvPeopleSummary = findViewById(R.id.tv_people_summary);
        tvSpecialSummary = findViewById(R.id.tv_special_summary);
        tasteTagsContainer = findViewById(R.id.taste_tags_container);
        peopleTagsContainer = findViewById(R.id.people_tags_container);
        specialTagsContainer = findViewById(R.id.special_tags_container);

        selectedTags.put("菜品口味", new HashSet<>());
        selectedTags.put("适用人群", new HashSet<>());
        selectedTags.put("特殊需求", new HashSet<>());
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        findViewById(R.id.card_taste).setOnClickListener(v ->
                showTagSelectionDialog("菜品口味"));
        findViewById(R.id.card_people).setOnClickListener(v ->
                showTagSelectionDialog("适用人群"));
        findViewById(R.id.card_special).setOnClickListener(v ->
                showTagSelectionDialog("特殊需求"));
    }

    private void loadTags() {
        Call<ApiResponse<List<TagResponse>>> call = apiService.getAllTags();
        call.enqueue(new Callback<ApiResponse<List<TagResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TagResponse>>> call,
                                   Response<ApiResponse<List<TagResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<TagResponse>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        processTags(apiResponse.getData());
                        loadUserSelectedTags();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TagResponse>>> call, Throwable t) {
                Toast.makeText(PreferenceActivity.this, "加载标签失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void loadUserSelectedTags() {
        Call<ApiResponse<Map<String, List<Integer>>>> call = apiService.getUserTags(userId);
        call.enqueue(new Callback<ApiResponse<Map<String, List<Integer>>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, List<Integer>>>> call,
                                   Response<ApiResponse<Map<String, List<Integer>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, List<Integer>>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        for (Map.Entry<String, List<Integer>> entry : apiResponse.getData().entrySet()) {
                            selectedTags.put(entry.getKey(), new HashSet<>(entry.getValue()));
                        }
                    }
                }
                updateUIWithSelectedTags();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, List<Integer>>>> call, Throwable t) {
                updateUIWithSelectedTags();
            }
        });
    }

    private void updateUIWithSelectedTags() {
        updateCategoryUI("菜品口味", tvTasteSummary, tasteTagsContainer);
        updateCategoryUI("适用人群", tvPeopleSummary, peopleTagsContainer);
        updateCategoryUI("特殊需求", tvSpecialSummary, specialTagsContainer);
    }

    private void updateCategoryUI(String category, TextView summaryView, LinearLayout container) {
        Set<Integer> selected = selectedTags.get(category);
        List<Tag> allTags = categoryTags.get(category);

        if (selected == null || selected.isEmpty()) {
            summaryView.setText("未选择");
            container.setVisibility(View.GONE);
            return;
        }

        // 构建标签名称列表
        List<String> selectedNames = new ArrayList<>();
        if (allTags != null) {
            for (Tag tag : allTags) {
                if (selected.contains(tag.getId())) {
                    selectedNames.add(tag.getName());
                }
            }
        }

        if (selectedNames.isEmpty()) {
            summaryView.setText("未选择");
            container.setVisibility(View.GONE);
        } else {
            summaryView.setText(selectedNames.size() + "项已选");
            container.setVisibility(View.VISIBLE);
            container.removeAllViews();

            // 创建Chip展示选中的标签
            for (String name : selectedNames) {
                Chip chip = new Chip(this);
                chip.setText(name);
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setTextColor(getResources().getColor(R.color.orange));
                chip.setChipStrokeColorResource(R.color.orange);
                chip.setChipStrokeWidth(1f);
                chip.setClickable(false);

                // 使用 LinearLayout.LayoutParams 设置 margin
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 16, 8);  // 右 margin 16dp，下 margin 8dp
                chip.setLayoutParams(params);

                container.addView(chip);
            }
        }
    }

    private void showTagSelectionDialog(String category) {
        List<Tag> tags = categoryTags.get(category);
        if (tags == null || tags.isEmpty()) {
            Toast.makeText(this, "标签数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] tagNames = new CharSequence[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagNames[i] = tags.get(i).getName();
        }

        Set<Integer> selectedIds = selectedTags.get(category);
        boolean[] checkedItems = new boolean[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            checkedItems[i] = selectedIds != null && selectedIds.contains(tags.get(i).getId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择" + category);
        builder.setMultiChoiceItems(tagNames, checkedItems, null);

        builder.setPositiveButton("确定", (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            Set<Integer> newSelectedIds = new HashSet<>();
            for (int i = 0; i < tags.size(); i++) {
                if (alertDialog.getListView().isItemChecked(i)) {
                    newSelectedIds.add(tags.get(i).getId());
                }
            }

            selectedTags.put(category, newSelectedIds);
            saveUserTags(category, newSelectedIds);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void saveUserTags(String category, Set<Integer> tagIds) {
        List<Integer> tagIdList = new ArrayList<>(tagIds);
        Gson gson = new Gson();
        String tagIdsJson = gson.toJson(tagIdList);

        Call<ApiResponse<Void>> call = apiService.saveUserTags(userId, category, tagIdsJson);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        updateUIWithSelectedTags();
                        Toast.makeText(PreferenceActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PreferenceActivity.this, "保存失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PreferenceActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(PreferenceActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}