package com.example.kitchen_manager.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class RegisterActivity extends AppCompatActivity {

    private EditText Username, Phone, Code, Password, ConfirmPassword;
    private TextView Policy;
    private Button btnRegister;
    private ImageView ConfirmImg, btnBack;
    private String realCode;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        Username = findViewById(R.id.et_register_username);
        Phone = findViewById(R.id.et_register_phone);
        Code = findViewById(R.id.et_register_code);
        Password = findViewById(R.id.et_register_password);
        ConfirmPassword = findViewById(R.id.et_register_confirm_password);
        Policy = findViewById(R.id.tv_agree_policy);
        btnRegister = findViewById(R.id.btn_register);
        btnBack = findViewById(R.id.iv_back);
        ConfirmImg = findViewById(R.id.identifyingcode_image);
        ConfirmImg.setImageBitmap(IdentifyingCode.getInstance().createBitmap());
        realCode = IdentifyingCode.getInstance().getCode().toLowerCase();
    }

    private void setupListeners() {
        // 验证码图片点击事件
        ConfirmImg.setOnClickListener(v -> {
            ConfirmImg.setImageBitmap(IdentifyingCode.getInstance().createBitmap());
            realCode = IdentifyingCode.getInstance().getCode().toLowerCase();
        });

        // 注册按钮点击事件
        btnRegister.setOnClickListener(v -> {
            String username = Username.getText().toString().trim();
            String phone = Phone.getText().toString().trim();
            String password = Password.getText().toString().trim();
            String confirmPassword = ConfirmPassword.getText().toString().trim();
            String code = Code.getText().toString().trim().toLowerCase();

            if (validateRegistration(username, phone, password, confirmPassword, code)) {
                performRegistration(username, phone, password);
            }
        });

        // 返回登录页点击事件
        btnBack.setOnClickListener(v -> finish());
    }

    private boolean validateRegistration(String username, String phone, String password, String confirmPassword, String code) {
        // 输入验证逻辑
        if (username.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!code.equals(realCode)) {
            Toast.makeText(this, "请输入正确的验证码", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performRegistration(String username, String phone, String password) {
        // 显示加载状态
        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        // 调用后端注册API
        Call<ApiResponse<User>> call = apiService.register(
                "register",  // action参数
                username,
                password,
                phone
        );

        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                // 恢复按钮状态
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        // 注册成功
                        Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                        finish(); // 返回登录页面
                    } else {
                        // 注册失败
                        Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 网络响应错误
                    Toast.makeText(RegisterActivity.this, "网络错误: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                // 恢复按钮状态
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");

                // 请求失败
                Toast.makeText(RegisterActivity.this, "注册失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }
}