package com.example.kitchen_manager.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.models.Ingredient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IngredientDetailActivity extends AppCompatActivity {

    private ImageView ivBack, ivIngredient;
    private TextView tvName, tvCategory, tvExpiryDate, tvRemainingDays, tvNutrition, tvBenefit; // 修改：添加 tvCategory
    private EditText etStorageDate, etExpiryDays;
    private Button btnEdit, btnDelete;
    private Ingredient ingredient;
    private TextView tvRemainingDaysLabel; // 新增

    // 移除分类选项数组

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_detail);

        getIngredientData();
        initViews();
        setupListeners();
        updateUI();
    }

    private void getIngredientData() {
        ingredient = getIntent().getParcelableExtra("ingredient");
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivIngredient = findViewById(R.id.iv_ingredient_large);
        tvName = findViewById(R.id.tv_detail_name);
        tvCategory = findViewById(R.id.tv_category); // 修改：绑定分类TextView
        etStorageDate = findViewById(R.id.et_storage_date);
        etExpiryDays = findViewById(R.id.et_expiry_days);
        tvExpiryDate = findViewById(R.id.tv_detail_expiry_date);
        tvRemainingDays = findViewById(R.id.tv_detail_remaining_days);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvNutrition = findViewById(R.id.tv_nutrition);
        tvBenefit = findViewById(R.id.tv_benefit);
        tvRemainingDaysLabel = findViewById(R.id.tv_remaining_days_label);

        // 移除分类下拉菜单的设置代码
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 编辑/完成按钮
        btnEdit.setOnClickListener(v -> {
            if (btnEdit.getText().toString().equals("编辑")) {
                // 进入编辑模式
                enterEditMode();
            } else {
                // 提交修改
                saveChanges();
            }
        });

        // 删除按钮
        btnDelete.setOnClickListener(v -> deleteIngredient());

        // 入库时间选择器
        etStorageDate.setOnClickListener(v -> showDatePicker());
    }

    private void enterEditMode() {
        // 只启用入库时间和保质期编辑
        etStorageDate.setEnabled(true);
        etExpiryDays.setEnabled(true);
        // 分类不可编辑，所以不需要任何操作

        // 更改按钮文本
        btnEdit.setText("完成");
    }

    private void saveChanges() {
        // 获取修改后的值
        String newStorageDate = etStorageDate.getText().toString();
        String expiryDaysStr = etExpiryDays.getText().toString();

        // 验证日期格式
        if (!isValidDate(newStorageDate)) {
            Toast.makeText(this, "日期格式不正确，请使用YYYY-MM-DD格式", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证保质期
        if (expiryDaysStr.isEmpty()) {
            Toast.makeText(this, "请输入保质期天数", Toast.LENGTH_SHORT).show();
            return;
        }

        int newExpiryDays;
        try {
            newExpiryDays = Integer.parseInt(expiryDaysStr);
            if (newExpiryDays <= 0) {
                Toast.makeText(this, "保质期天数必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "保质期必须是数字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算新的食用期限
        String newExpiryDate = calculateExpiryDate(newStorageDate, newExpiryDays);

        // 更新本地对象（分类保持不变）
        ingredient.setStorageDate(newStorageDate);
        ingredient.setExpiryDate(newExpiryDate);
        ingredient.setExpiryDays(newExpiryDays);

        // 调用API更新服务器（仍然传递分类，但它是原来的值，没有变化）
        updateIngredientOnServer(newExpiryDays);
    }

    private boolean isValidDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String calculateExpiryDate(String storageDate, int expiryDays) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(storageDate);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, expiryDays);

            return sdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return ingredient.getExpiryDate();
        }
    }

    private void updateIngredientOnServer(int expiryDays) {
        int userId = getCurrentUserId();
        if (userId == -1) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.updateIngredient(
                userId,
                ingredient.getName(),
                ingredient.getCategory(), // 传递原来的分类，保持不变
                ingredient.getStorageDate(),
                expiryDays
        );

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Toast.makeText(IngredientDetailActivity.this, "更新成功", Toast.LENGTH_SHORT).show();

                    // 更新UI
                    updateUI();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(IngredientDetailActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(IngredientDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteIngredient() {
        int userId = getCurrentUserId();
        if (userId == -1) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.deleteUserIngredient(
                userId,
                ingredient.getName()
        );

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    // 设置删除成功的标志
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("deleted", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(IngredientDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(IngredientDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        // 解析当前日期
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(etStorageDate.getText().toString());
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // 设置选择的日期
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    etStorageDate.setText(selectedDate);

                    // 获取保质期天数
                    int expiryDays = getExpiryDaysFromInput();

                    // 自动更新食用期限
                    String expiryDate = calculateExpiryDate(selectedDate, expiryDays);
                    tvExpiryDate.setText(expiryDate);

                    // 更新剩余天数显示
                    updateRemainingDays(expiryDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private int getExpiryDaysFromInput() {
        try {
            String expiryDaysStr = etExpiryDays.getText().toString();
            return Integer.parseInt(expiryDaysStr);
        } catch (NumberFormatException e) {
            return ingredient.getExpiryDays();
        }
    }

    private void updateUI() {
        if (ingredient != null) {
            // 使用 Glide 加载大图
            Glide.with(this)
                    .load(ingredient.getImageUrl())
                    .placeholder(R.drawable.ic_logo_orange)
                    .error(R.drawable.ic_logo_orange)
                    .into(ivIngredient);

            tvName.setText(ingredient.getName());

            // 设置分类（使用TextView显示，不可编辑）
            tvCategory.setText(ingredient.getCategory());

            etStorageDate.setText(ingredient.getStorageDate());
            tvExpiryDate.setText(ingredient.getExpiryDate());

            // 设置保质期天数
            etExpiryDays.setText(String.valueOf(ingredient.getExpiryDays()));

            // 设置营养和健康益处
            String formattedNutrition = formatNutrition(ingredient.getNutrition());
            tvNutrition.setText(formattedNutrition);
            tvBenefit.setText(ingredient.getBenefit());

            // 计算并显示剩余天数
            updateRemainingDays(ingredient.getExpiryDate());
        }
    }

    private void updateRemainingDays(String expiryDateStr) {
        long remainingDays = calculateRemainingDays(expiryDateStr);

        // 根据剩余天数的正负设置标签文本
        if (remainingDays >= 0) {
            tvRemainingDaysLabel.setText("剩余天数");
            tvRemainingDays.setText(String.format("%d天", remainingDays));
        } else {
            tvRemainingDaysLabel.setText("过期天数");
            // 显示过期天数的绝对值（正数）
            tvRemainingDays.setText(String.format("%d天", Math.abs(remainingDays)));
        }

        // 设置颜色
        if (remainingDays > 7) {
            tvRemainingDays.setTextColor(getResources().getColor(R.color.green));
        } else if (remainingDays >= 3) {
            tvRemainingDays.setTextColor(getResources().getColor(R.color.orange));
        } else if (remainingDays >= 0) {
            tvRemainingDays.setTextColor(getResources().getColor(R.color.red));
        } else {
            // 过期的情况，可以设置一个特殊的颜色，比如深红色
            tvRemainingDays.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private long calculateRemainingDays(String expiryDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date expiryDate = sdf.parse(expiryDateStr);
            Date currentDate = new Date();
            long diffInMillis = expiryDate.getTime() - currentDate.getTime();
            return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String formatNutrition(String nutritionJson) {
        if (nutritionJson == null || nutritionJson.isEmpty()) {
            return "暂无数据";
        }

        try {
            JSONObject json = new JSONObject(nutritionJson);
            JSONArray components = json.getJSONArray("components");

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < components.length(); i++) {
                if (i > 0) {
                    builder.append("、");
                }
                builder.append(components.getString(i));
            }

            return builder.toString();
        } catch (JSONException e) {
            if (nutritionJson.startsWith("{") && nutritionJson.endsWith("}")) {
                return nutritionJson;
            }
            return nutritionJson;
        }
    }

    // 获取当前用户ID
    private int getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }
}