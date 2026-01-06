package com.example.kitchen_manager.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
    private TextView tvName, tvExpiryDate, tvRemainingDays, tvNutrition, tvBenefit;
    private Spinner spCategory;
    private EditText etStorageDate, etExpiryDays; // 新增保质期编辑框
    private Button btnEdit, btnDelete;
    private Ingredient ingredient;

    // 分类选项
    private static final String[] CATEGORIES = {"蔬菜", "肉类", "药食", "果品类", "鱼类", "五谷杂粮","水产","蛋、奶","其他"};

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
    private String formatNutrition(String nutritionJson) {
        if (nutritionJson == null || nutritionJson.isEmpty()) {
            return "暂无数据";
        }

        try {
            // 尝试解析JSON
            JSONObject json = new JSONObject(nutritionJson);
            JSONArray components = json.getJSONArray("components");

            // 将数组元素连接为逗号分隔的字符串
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < components.length(); i++) {
                if (i > 0) {
                    builder.append("、"); // 使用中文顿号分隔
                }
                builder.append(components.getString(i));
            }

            return builder.toString();
        } catch (JSONException e) {
            // 如果解析失败，尝试直接处理字符串
            if (nutritionJson.startsWith("{") && nutritionJson.endsWith("}")) {
                // 看起来像JSON但解析失败，返回原始字符串
                return nutritionJson;
            }

            // 如果不是JSON格式，直接返回
            return nutritionJson;
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivIngredient = findViewById(R.id.iv_ingredient_large);
        tvName = findViewById(R.id.tv_detail_name);
        spCategory = findViewById(R.id.sp_category);
        etStorageDate = findViewById(R.id.et_storage_date);
        etExpiryDays = findViewById(R.id.et_expiry_days); // 新增
        tvExpiryDate = findViewById(R.id.tv_detail_expiry_date);
        tvRemainingDays = findViewById(R.id.tv_detail_remaining_days);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvNutrition = findViewById(R.id.tv_nutrition);
        tvBenefit = findViewById(R.id.tv_benefit);

        // 设置分类下拉菜单
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
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
        // 启用编辑控件
        spCategory.setEnabled(true);
        etStorageDate.setEnabled(true);
        etExpiryDays.setEnabled(true); // 新增：启用保质期编辑

        // 更改按钮文本
        btnEdit.setText("完成");
    }


    private void saveChanges() {
        // 获取修改后的值
        String newCategory = spCategory.getSelectedItem().toString();
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

        // 更新本地对象
        ingredient.setCategory(newCategory);
        ingredient.setStorageDate(newStorageDate);
        ingredient.setExpiryDate(newExpiryDate);

        // 可能需要添加保质期天数到Ingredient模型中
        //ingredient.setExpiryDays(newExpiryDays);

        // 调用API更新服务器
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

    private String calculateExpiryDate(String storageDate, int newExpiryDays) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(storageDate);

            // 假设默认保质期3天（实际应从服务器获取）
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, 3);

            return sdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return ingredient.getExpiryDate(); // 出错时返回原值
        }
    }

    private void updateIngredientOnServer(int expiryDays) {
        int userId = getCurrentUserId();
        if (userId == -1) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加日志
        Log.d("IngredientDetail", "发送更新请求 - userId: " + userId +
                ", name: " + ingredient.getName() +
                ", category: " + ingredient.getCategory() +
                ", storageDate: " + ingredient.getStorageDate() +
                ", expiryDays: " + expiryDays);

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<Void>> call = apiService.updateIngredient(
                userId,
                ingredient.getName(),
                ingredient.getCategory(),
                ingredient.getStorageDate(),
                expiryDays
        );

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d("IngredientDetail", "收到响应 - 状态码: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("IngredientDetail", "响应体: " + response.body().toString());

                    if (response.body().getCode() == 200) {
                        Toast.makeText(IngredientDetailActivity.this, "更新成功", Toast.LENGTH_SHORT).show();

                        // 更新食用期限显示
                        updateUI();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("updated", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(IngredientDetailActivity.this, "更新失败: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("IngredientDetail", "更新失败: " + response.body().getMessage());
                    }
                } else {
                    Toast.makeText(IngredientDetailActivity.this, "更新失败: 响应异常", Toast.LENGTH_SHORT).show();
                    Log.e("IngredientDetail", "响应异常: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(IngredientDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("IngredientDetail", "网络错误: ", t);
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
            // 如果输入无效，使用模型中的保质期天数
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

            // 设置分类选择
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(ingredient.getCategory())) {
                    spCategory.setSelection(i);
                    break;
                }
            }

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
        tvRemainingDays.setText(String.format("%d天", remainingDays));

        // 设置颜色
        if (remainingDays > 7) {
            tvRemainingDays.setTextColor(getResources().getColor(R.color.green));
        } else if (remainingDays >= 3) {
            tvRemainingDays.setTextColor(getResources().getColor(R.color.orange));
        } else {
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

    // 获取当前用户ID
    private int getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }
}