package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.Ingredient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.IngredientResponse;
import com.example.kitchen_manager.response.RecipeDetailResponse;
import com.example.kitchen_manager.response.RecipeVideoResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeDetailActivity extends AppCompatActivity {
    private FloatingActionButton fabCook;
    private int userId;
    private int recipeId;
    private ImageView recipeImage;
    private TextView recipeName;
    private TextView recipeAttributes;
    private TextView recipeIngredientsText;
    private TextView recipeSteps;
    private TextView ingredientComparationText;
    private ProgressBar progressBar;
    private List<Ingredient> recipeIngredientsList;
    private AlertDialog depletionDialog;
    private AlertDialog selectionDialog;
    private ImageView ivBack;
    private List<CheckBox> ingredientCheckboxes = new ArrayList<>();
    private List<IngredientResponse> userIngredients = new ArrayList<>();

    // 新增：视频相关组件
    private CardView videoCard;
    private TextView videoLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", 0);

        fabCook = findViewById(R.id.fabCook);
        fabCook.setOnClickListener(v -> showDepletionDialog());

        recipeId = getIntent().getIntExtra("recipe_id", 0);
        if (recipeId == 0) {
            Toast.makeText(this, "无效的菜谱ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeImage = findViewById(R.id.recipe_image);
        recipeName = findViewById(R.id.recipe_name);
        recipeAttributes = findViewById(R.id.recipe_attributes);
        recipeIngredientsText = findViewById(R.id.recipe_ingredients);
        recipeSteps = findViewById(R.id.recipe_steps);
        ingredientComparationText = findViewById(R.id.ingredient_comparation);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.iv_back);

        // 新增：初始化视频组件
        videoCard = findViewById(R.id.video_card);
        videoLink = findViewById(R.id.video_link);

        ivBack.setOnClickListener(v -> finish());

        initDialogs();
        loadRecipeDetail();

        if (userId != 0) {
            loadUserIngredients();
        }

        // 新增：加载视频信息
        loadRecipeVideo();
    }

    // 新增方法：加载菜谱视频
    private void loadRecipeVideo() {
        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<List<RecipeVideoResponse>>> call = apiService.getRecipeVideos(recipeId);

        call.enqueue(new Callback<ApiResponse<List<RecipeVideoResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecipeVideoResponse>>> call,
                                   Response<ApiResponse<List<RecipeVideoResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<RecipeVideoResponse>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null
                            && !apiResponse.getData().isEmpty()) {
                        // 获取第一个视频（根据你的需求，一个菜谱只有一个视频）
                        RecipeVideoResponse video = apiResponse.getData().get(0);
                        displayVideoLink(video);
                    } else {
                        Log.d("RecipeVideo", "该菜谱没有关联视频");
                    }
                } else {
                    Log.e("RecipeVideo", "加载视频失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecipeVideoResponse>>> call, Throwable t) {
                Log.e("RecipeVideo", "网络错误: " + t.getMessage());
            }
        });
    }

    // 新增方法：显示视频链接
    private void displayVideoLink(RecipeVideoResponse video) {
        if (video != null && video.getVideoUrl() != null && !video.getVideoUrl().isEmpty()) {
            videoCard.setVisibility(View.VISIBLE);

            // 设置视频标题（如果需要显示平台信息）
            String linkText = "点击观看视频教程";
            if (video.getPlatform() != null) {
                linkText = "点击观看 " + video.getPlatform() + " 视频教程";
            }
            videoLink.setText(linkText);

            // 设置点击事件，跳转到浏览器
            videoLink.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(RecipeDetailActivity.this,
                            "无法打开视频链接", Toast.LENGTH_SHORT).show();
                    Log.e("VideoLink", "打开链接失败: " + e.getMessage());
                }
            });
        }
    }

    private void loadUserIngredients() {
        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<List<IngredientResponse>>> call = apiService.getUserIngredients(userId);

        call.enqueue(new Callback<ApiResponse<List<IngredientResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<IngredientResponse>>> call,
                                   Response<ApiResponse<List<IngredientResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<IngredientResponse>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        userIngredients = apiResponse.getData();
                        Log.d("UserIngredients", "加载用户食材成功，数量: " + userIngredients.size());
                        loadRecipeIngredientsForComparation();
                    }
                } else {
                    Log.e("UserIngredients", "加载用户食材失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<IngredientResponse>>> call, Throwable t) {
                Log.e("UserIngredients", "网络错误: " + t.getMessage());
            }
        });
    }

    private void loadRecipeIngredientsForComparation() {
        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<List<Ingredient>>> call = apiService.getRecipeIngredients(recipeId);

        call.enqueue(new Callback<ApiResponse<List<Ingredient>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ingredient>>> call,
                                   Response<ApiResponse<List<Ingredient>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Ingredient>> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        List<Ingredient> recipeStandardIngredients = apiResponse.getData();
                        Log.d("RecipeIngredients", "加载菜谱食材成功，数量: " + recipeStandardIngredients.size());
                        updateIngredientComparation(recipeStandardIngredients);
                    }
                } else {
                    Log.e("RecipeIngredients", "加载菜谱食材失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ingredient>>> call, Throwable t) {
                Log.e("RecipeIngredients", "网络错误: " + t.getMessage());
            }
        });
    }

    private void updateIngredientComparation(List<Ingredient> recipeStandardIngredients) {
        if (recipeStandardIngredients == null || recipeStandardIngredients.isEmpty()) {
            ingredientComparationText.setText("暂无食材信息");
            return;
        }

        Set<String> userIngredientNames = new HashSet<>();
        for (IngredientResponse userIngredient : userIngredients) {
            userIngredientNames.add(userIngredient.getName());
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i < recipeStandardIngredients.size(); i++) {
            Ingredient ingredient = recipeStandardIngredients.get(i);
            String ingredientName = ingredient.getName();

            if (i > 0) {
                builder.append("\n");
            }

            String itemText = "· " + ingredientName;
            int start = builder.length();
            builder.append(itemText);
            int end = builder.length();

            int color;
            if (userIngredientNames.contains(ingredientName)) {
                color = Color.parseColor("#4CAF50");
            } else {
                color = Color.parseColor("#F44336");
            }

            builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        ingredientComparationText.setText(builder);
    }

    private void displayRecipe(RecipeDetailResponse recipe) {
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(recipeImage);
        } else {
            recipeImage.setImageResource(R.drawable.placeholder);
        }

        recipeName.setText(recipe.getName() != null ? recipe.getName() : "未知菜谱");

        String attributes = String.format("口味: %s · 方法: %s · 时间: %s · 难度: %s",
                recipe.getTaste() != null ? recipe.getTaste() : "未知",
                recipe.getMethod() != null ? recipe.getMethod() : "未知",
                recipe.getTime() != null ? recipe.getTime() : "未知",
                recipe.getDifficulty() != null ? recipe.getDifficulty() : "未知");
        recipeAttributes.setText(attributes);

        StringBuilder formattedNeeds = new StringBuilder("");
        if (recipe.getNeeds() != null && !recipe.getNeeds().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> ingredients = gson.fromJson(recipe.getNeeds(), listType);

                for (String ingredient : ingredients) {
                    formattedNeeds.append("· ").append(ingredient).append("\n");
                }
            } catch (Exception e) {
                String needsStr = recipe.getNeeds().trim();
                if (needsStr.startsWith("[") && needsStr.endsWith("]")) {
                    needsStr = needsStr.substring(1, needsStr.length()-1);
                }
                String[] ingredients = needsStr.split(",");
                for (String ingredient : ingredients) {
                    ingredient = ingredient.trim().replaceAll("^\"|\"$", "");
                    formattedNeeds.append("· ").append(ingredient).append("\n");
                }
            }
        } else {
            formattedNeeds.append("暂无原料信息");
        }
        recipeIngredientsText.setText(formattedNeeds.toString());

        StringBuilder formattedSteps = new StringBuilder("步骤:\n");
        if (recipe.getSteps() != null && !recipe.getSteps().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> stepList = gson.fromJson(recipe.getSteps(), listType);

                for (int i = 0; i < stepList.size(); i++) {
                    formattedSteps.append(stepList.get(i)).append("\n\n");
                }
            } catch (Exception e) {
                formattedSteps.append(recipe.getSteps());
            }
        } else {
            formattedSteps.append("暂无步骤信息");
        }
        recipeSteps.setText(formattedSteps.toString());

        if (userId != 0) {
            if (!userIngredients.isEmpty()) {
                loadRecipeIngredientsForComparation();
            }
        } else {
            ingredientComparationText.setText("请登录后查看食材比对");
            ingredientComparationText.setTextColor(Color.parseColor("#777777"));
        }
    }

    private void initDialogs() {
        View depletionView = LayoutInflater.from(this).inflate(R.layout.dialog_ingredient_depletion, null);
        depletionDialog = new AlertDialog.Builder(this)
                .setView(depletionView)
                .setCancelable(false)
                .create();

        Button btnYes = depletionView.findViewById(R.id.btnYes);
        Button btnNo = depletionView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            depletionDialog.dismiss();
            fetchRecipeIngredients();
        });

        btnNo.setOnClickListener(v -> {
            depletionDialog.dismiss();
            addUserHistory();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (depletionDialog != null && depletionDialog.isShowing()) {
            depletionDialog.dismiss();
        }
        if (selectionDialog != null && selectionDialog.isShowing()) {
            selectionDialog.dismiss();
        }
    }

    private void showDepletionDialog() {
        if (userId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        if (depletionDialog == null) {
            initDialogs();
        }
        if (!depletionDialog.isShowing()) {
            depletionDialog.show();
        }
    }

    private void fetchRecipeIngredients() {
        Log.d("API", "开始获取食材列表，recipeId=" + recipeId);
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<List<Ingredient>>> call = apiService.getRecipeIngredients(recipeId);
        call.enqueue(new Callback<ApiResponse<List<Ingredient>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ingredient>>> call,
                                   Response<ApiResponse<List<Ingredient>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Ingredient>> apiResponse = response.body();
                    Log.d("API", "响应代码: " + apiResponse.getCode());

                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        recipeIngredientsList = apiResponse.getData();
                        Log.d("API", "获取到食材数量: " + recipeIngredientsList.size());

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            showIngredientSelectionDialog();
                        }, 100);
                    } else {
                        String errorMsg = "获取食材失败: " + apiResponse.getMessage();
                        Log.e("API", errorMsg);
                        Toast.makeText(RecipeDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "服务器错误: " + response.code();
                    Log.e("API", errorMsg);
                    Toast.makeText(RecipeDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ingredient>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "网络错误: " + t.getMessage();
                Log.e("API", errorMsg, t);
                Toast.makeText(RecipeDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showIngredientSelectionDialog() {
        Log.d("Dialog", "显示食材选择对话框");
        if (selectionDialog != null && selectionDialog.isShowing()) {
            selectionDialog.dismiss();
        }

        if (recipeIngredientsList == null || recipeIngredientsList.isEmpty()) {
            Log.e("Dialog", "recipeIngredientsList为空");
            Toast.makeText(this, "该食谱没有关联食材", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ingredient_selection, null);
        LinearLayout container = dialogView.findViewById(R.id.ingredientsContainer);

        if (container == null) {
            Log.e("Dialog", "无法找到ingredientsContainer");
            Toast.makeText(this, "对话框布局错误", Toast.LENGTH_SHORT).show();
            return;
        }

        container.removeAllViews();
        ingredientCheckboxes.clear();

        for (Ingredient ingredient : recipeIngredientsList) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_ingredient_checkbox, container, false);
            CheckBox cbIngredient = itemView.findViewById(R.id.cbIngredient);
            TextView tvName = itemView.findViewById(R.id.tvIngredientName);

            tvName.setText(ingredient.getName());
            ingredientCheckboxes.add(cbIngredient);
            container.addView(itemView);
        }

        AlertDialog newSelectionDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(v -> {
            Log.d("Dialog", "确认按钮被点击");
            deleteSelectedIngredients();
            newSelectionDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            Log.d("Dialog", "取消按钮被点击");
            newSelectionDialog.dismiss();
            addUserHistory();
        });

        newSelectionDialog.show();
        selectionDialog = newSelectionDialog;
    }

    private void deleteSelectedIngredients() {
        Log.d("Delete", "开始删除选中的食材");
        List<Integer> selectedIds = new ArrayList<>();
        for (int i = 0; i < ingredientCheckboxes.size(); i++) {
            if (ingredientCheckboxes.get(i).isChecked()) {
                selectedIds.add(recipeIngredientsList.get(i).getIngredientId());
            }
        }

        if (selectedIds.isEmpty()) {
            Log.d("Delete", "没有选中任何食材");
            Toast.makeText(this, "请至少选择一种食材", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String ingredientIdsJson = gson.toJson(selectedIds);
        Log.d("Delete", "删除的食材JSON: " + ingredientIdsJson);

        Button btnConfirm = selectionDialog.findViewById(R.id.btnConfirm);
        Button btnCancel = selectionDialog.findViewById(R.id.btnCancel);
        if (btnConfirm != null) btnConfirm.setEnabled(false);
        if (btnCancel != null) btnCancel.setEnabled(false);

        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.deleteUserIngredients(userId, ingredientIdsJson);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                progressBar.setVisibility(View.GONE);
                Log.d("Delete", "收到删除响应");

                if (btnConfirm != null) btnConfirm.setEnabled(true);
                if (btnCancel != null) btnCancel.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Log.d("Delete", "删除成功");
                        Toast.makeText(RecipeDetailActivity.this, "已更新食材库存", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Delete", "删除失败: " + apiResponse.getMessage());
                        Toast.makeText(RecipeDetailActivity.this, "更新库存失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("Delete", "服务器错误: " + response.code());
                    Toast.makeText(RecipeDetailActivity.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show();
                }

                selectionDialog.dismiss();
                addUserHistory();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("Delete", "删除请求失败", t);

                if (btnConfirm != null) btnConfirm.setEnabled(true);
                if (btnCancel != null) btnCancel.setEnabled(true);

                Toast.makeText(RecipeDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                selectionDialog.dismiss();
                addUserHistory();
            }
        });
    }

    private void loadRecipeDetail() {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();

        Call<ApiResponse<Void>> popularityCall = apiService.incrementPopularity(recipeId);
        popularityCall.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Popularity", "热度增加成功");
                } else {
                    Log.e("Popularity", "热度增加失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("Popularity", "增加热度请求失败: " + t.getMessage());
            }
        });

        Call<ApiResponse<RecipeDetailResponse>> call = apiService.getRecipeDetail(recipeId);
        call.enqueue(new Callback<ApiResponse<RecipeDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<RecipeDetailResponse>> call,
                                   Response<ApiResponse<RecipeDetailResponse>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<RecipeDetailResponse> apiResponse = response.body();
                    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                        displayRecipe(apiResponse.getData());
                    } else {
                        showError("获取详情失败: " + apiResponse.getMessage());
                    }
                } else {
                    String errorMsg = "服务器响应错误";
                    if (response != null) {
                        errorMsg += " (HTTP " + response.code() + ")";
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RecipeDetailResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addUserHistory() {
        if (userId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.addUserHistory(userId, recipeId);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Toast.makeText(RecipeDetailActivity.this, "已记录您的动手经历", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RecipeDetailActivity.this, "操作失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RecipeDetailActivity.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(RecipeDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}