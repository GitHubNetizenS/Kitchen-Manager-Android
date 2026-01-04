package com.example.kitchen_manager.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.User;
import com.example.kitchen_manager.response.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginId, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        initViews();
        setupListeners();

        //检查是否已登录
        if (isLoggedIn()) {
          navigateToMainActivity();}
    }

    private void initViews() {
        etLoginId = findViewById(R.id.et_username); // 修改为登录ID（用户名或手机号）
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String loginId = etLoginId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateLogin(loginId, password)) {
                performLogin(loginId, password);
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private boolean validateLogin(String loginId, String password) {
        if(loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名/手机号和密码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void performLogin(String loginId, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        // 添加日志
        android.util.Log.d("LOGIN", "开始登录");
        android.util.Log.d("LOGIN", "URL: " + ApiService.BASE_URL);
        android.util.Log.d("LOGIN", "LoginId: " + loginId);

        Call<ApiResponse<User>> call = apiService.login("login", loginId, password);

        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");

                // 详细日志
                android.util.Log.d("LOGIN", "响应码: " + response.code());
                android.util.Log.d("LOGIN", "响应消息: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    android.util.Log.d("LOGIN", "API Code: " + apiResponse.getCode());
                    android.util.Log.d("LOGIN", "API Message: " + apiResponse.getMessage());

                    if (apiResponse.getCode() == 200) {
                        android.util.Log.d("LOGIN", "登录成功");
                        saveUserSession(apiResponse.getData());
                        navigateToMainActivity();
                    } else {
                        android.util.Log.e("LOGIN", "登录失败: " + apiResponse.getMessage());
                        Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 打印错误响应体
                    try {
                        String errorBody = response.errorBody().string();
                        android.util.Log.e("LOGIN", "错误响应: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.e("LOGIN", "无法读取错误响应");
                    }
                    Toast.makeText(LoginActivity.this, "登录失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");
                android.util.Log.e("LOGIN", "网络错误", t);
                android.util.Log.e("LOGIN", "错误消息: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // 保存用户信息到SharedPreferences
    private void saveUserSession(User user) {
        SharedPreferences sharedPref = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("user_id", user.getUserId());
        editor.putString("username", user.getUsername());
        editor.putString("phone", user.getPhone());
        editor.putString("avatar_url", user.getAvatarUrl());
        editor.putString("title", user.getTitle()); // 保存称号
        editor.putBoolean("is_admin", user.isAdmin());

        editor.apply();
    }

    // 检查是否已登录
    private boolean isLoggedIn() {
        SharedPreferences sharedPref = getSharedPreferences("user_session", MODE_PRIVATE);
        return sharedPref.contains("user_id"); // 检查是否存在用户ID
    }

    // 导航到主界面
    private void navigateToMainActivity() {
        SharedPreferences sharedPref = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isAdmin = sharedPref.getBoolean("is_admin", false);

        if (isAdmin) {//是管理员
            //startActivity(new Intent(LoginActivity.this, AdminActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        finish();
    }
}