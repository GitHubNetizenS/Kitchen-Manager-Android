package com.example.kitchen_manager.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.Ingredient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.RecipeDetailResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
    private ProgressBar progressBar;
    private List<Ingredient> recipeIngredientsList;
    private AlertDialog depletionDialog;
    private AlertDialog selectionDialog;
    private ImageView ivBack;
    private List<CheckBox> ingredientCheckboxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

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
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.iv_back);

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 结束当前Activity，返回上一页面
            }
        });
        initDialogs();
        loadRecipeDetail();
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

        // 不再在此处初始化selectionDialog
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭所有对话框
        if (depletionDialog != null && depletionDialog.isShowing()) {
            depletionDialog.dismiss();
        }
        if (selectionDialog != null && selectionDialog.isShowing()) {
            selectionDialog.dismiss();
        }
    }

    private void showDepletionDialog() {
        if (userId == -1) {
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

                        // 添加小延迟确保UI准备就绪
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
        //关闭可能存在的旧对话框
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

        // 创建新对话框
        AlertDialog newSelectionDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        ivBack.setOnClickListener(v -> finish());

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
        selectionDialog = newSelectionDialog; // 更新引用
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

        // 将List转换为JSON字符串
        Gson gson = new Gson();
        String ingredientIdsJson = gson.toJson(selectedIds);
        Log.d("Delete", "删除的食材JSON: " + ingredientIdsJson);

        // 禁用按钮防止多次点击
        Button btnConfirm = selectionDialog.findViewById(R.id.btnConfirm);
        Button btnCancel = selectionDialog.findViewById(R.id.btnCancel);
        if (btnConfirm != null) btnConfirm.setEnabled(false);
        if (btnCancel != null) btnCancel.setEnabled(false);

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.deleteUserIngredients(userId, ingredientIdsJson);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                progressBar.setVisibility(View.GONE);
                Log.d("Delete", "收到删除响应");

                // 重新启用按钮
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

                // 重新启用按钮
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

        // 先增加热度
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

        StringBuilder formattedNeeds = new StringBuilder("原料:\n");
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
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addUserHistory() {
        if (userId == -1) {
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