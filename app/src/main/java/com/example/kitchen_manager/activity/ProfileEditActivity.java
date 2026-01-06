package com.example.kitchen_manager.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.User;
import com.example.kitchen_manager.response.ApiResponse;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileEditActivity extends AppCompatActivity {
    private EditText etName, etTitle, etPhone, etPassword;
    private TextView tvUserId;
    private ImageView ivAvatar,ivBack;
    private Button btnSave;
    private Button btnLogout;
    private SharedPreferences prefs;
    private int userId;
    private String avatarUrl;
    private ApiService apiService;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private boolean isUploadingAvatar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        // 初始化Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        initViews();
        setupListeners();
        loadUserInfo();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        tvUserId = findViewById(R.id.tv_user_id);
        etTitle = findViewById(R.id.et_title);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        ivAvatar = findViewById(R.id.iv_avatar);
        ivBack=findViewById(R.id.iv_back);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);

        // 从SharedPreferences获取用户ID
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            finish();
        }
        tvUserId.setText(String.valueOf(userId));
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveChanges());
        // 退出登录按钮点击事件
        btnLogout.setOnClickListener(v -> logout());
        // 头像点击事件
        ivAvatar.setOnClickListener(v -> openImageChooser());
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // 更换头像文字点击事件
        findViewById(R.id.tv_change_avatar).setOnClickListener(v -> openImageChooser());
    }
    private void logout() {
        // 创建确认对话框
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performLogout() {
        // 清除用户会话
        clearUserSession();

        // 显示退出成功提示
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();

        // 跳转到登录页面，并清除所有activity
        Intent intent = new Intent(ProfileEditActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // 关闭当前页面
        finish();
    }

    // 这个 clearUserSession() 方法你已经有了，不需要重复添加
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择头像"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivAvatar.setImageURI(selectedImageUri);
            uploadImageToServer();
        }
    }

    private void uploadImageToServer() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            isUploadingAvatar = true;
            btnSave.setEnabled(false);
            btnSave.setText("上传头像中...");

            ContentResolver contentResolver = getContentResolver();
            String mimeType = contentResolver.getType(selectedImageUri);
            if (mimeType == null) {
                mimeType = "image/*";
            }

            InputStream inputStream = contentResolver.openInputStream(selectedImageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法获取图片数据", Toast.LENGTH_SHORT).show();
                isUploadingAvatar = false;
                btnSave.setEnabled(true);
                btnSave.setText("保存修改");
                return;
            }

            // 创建临时文件
            String extension = getFileExtension(selectedImageUri);
            File tempFile = File.createTempFile("avatar_", "." + extension, getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            inputStream.close();

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(mimeType),
                    tempFile
            );

            // 创建用户ID的请求体
            RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));

            // 创建多部分请求
            MultipartBody.Part userIdPart = MultipartBody.Part.createFormData("user_id", String.valueOf(userId));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("avatar", tempFile.getName(), requestBody);

            Call<ApiResponse<String>> call = apiService.uploadAvatar(userIdPart, filePart);
            call.enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                    isUploadingAvatar = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("保存修改");

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<String> apiResponse = response.body();
                        if (apiResponse.getCode() == 200) {
                            avatarUrl = apiResponse.getData();
                            Picasso.get()
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_logo_orange)
                                    .error(R.drawable.ic_logo_orange)
                                    .into(ivAvatar);
                            Toast.makeText(ProfileEditActivity.this, "头像上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileEditActivity.this, "头像上传失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileEditActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                    isUploadingAvatar = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("保存修改");
                    Toast.makeText(ProfileEditActivity.this, "上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            isUploadingAvatar = false;
            btnSave.setEnabled(true);
            btnSave.setText("保存修改");
            Toast.makeText(this, "上传过程中发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserInfo(String name, String title, String phone, String password) {
        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        // 确保头像URL不为空
        if (avatarUrl == null) {
            avatarUrl = prefs.getString("avatar_url", "");
        }

        Call<ApiResponse<User>> call = apiService.updateUserProfile(
                "update_profile",
                userId,
                name,
                phone,
                title,
                avatarUrl,
                password.isEmpty() ? null : password
        );

        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                btnSave.setEnabled(true);
                btnSave.setText("保存修改");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    if (apiResponse.getCode() == 200) {
                        Toast.makeText(ProfileEditActivity.this, "信息更新成功", Toast.LENGTH_SHORT).show();

                        User updatedUser = apiResponse.getData();
                        // 保存到SharedPreferences
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("username", updatedUser.getUsername());
                        editor.putString("phone", updatedUser.getPhone());
                        editor.putString("avatar_url", updatedUser.getAvatarUrl());
                        editor.putString("title", updatedUser.getTitle());
                        editor.apply();

                        // 检查是否修改了密码
                        if (!TextUtils.isEmpty(password)) {
                            // 清除登录状态
                            clearUserSession();

                            // 跳转到登录页面
                            Intent intent = new Intent(ProfileEditActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            // 没有修改密码，正常返回
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    } else {
                        Toast.makeText(ProfileEditActivity.this, "更新失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileEditActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("保存修改");
                Toast.makeText(ProfileEditActivity.this, "更新失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearUserSession() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    // 获取文件扩展名
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
        return extension != null ? extension : "jpg";
    }

    private void loadUserInfo() {
        if (prefs.contains("user_id")) {
            String name = prefs.getString("username", "");
            String title = prefs.getString("title", "");
            String phone = prefs.getString("phone", "");
            avatarUrl = prefs.getString("avatar_url", "");

            etName.setText(name);
            etTitle.setText(title);
            etPhone.setText(phone);

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
            etName.setText("");
            etTitle.setText("");
            etPhone.setText("");
            ivAvatar.setImageResource(R.drawable.ic_logo_orange);
        }
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validateInput(name, phone)) {
            // 检查是否正在上传头像
            if (isUploadingAvatar) {
                Toast.makeText(this, "请等待头像上传完成", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserInfo(name, title, phone, password);
        }
    }

    private boolean validateInput(String name, String phone) {
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, "请输入有效的手机号", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}