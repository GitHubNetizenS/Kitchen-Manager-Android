package com.example.kitchen_manager.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.api.DoubaoApiClient;
import com.example.kitchen_manager.api.DoubaoApiService;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.response.DoubaoApiResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoRecognitionActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private Button btnUpload;
    private LinearLayout llResultContainer;
    private ProgressBar progressBar;
    private LinearLayout textInputLayout;
    private EditText etTextInput;
    private Button btnSubmitText;
    private Button btnFood;
    private Button btnReceipt;
    private Button btnText;
    private Button btnAddItem;
    private Button btnDone;

    // 当前模式
    private static final int MODE_FOOD = 0;
    private static final int MODE_RECEIPT = 1;
    private static final int MODE_TEXT = 2;
    private int currentMode = MODE_FOOD;

    // 火山引擎豆包API配置
    private static final String API_KEY = "4f9b835d-b78b-4643-8d4d-8d33fe3fe4f1";
    private static final String MODEL_ID = "ep-20250701144300-4gmgs";

    // 使用Retrofit客户端
    private DoubaoApiService doubaoApiService;

    // 存储食材列表
    private List<String> ingredientsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_recognition);

        // 初始化豆包API服务
        doubaoApiService = DoubaoApiClient.getDoubaoApiService();

        // 初始化视图
        btnFood = findViewById(R.id.btn_food);
        btnReceipt = findViewById(R.id.btn_receipt);
        btnText = findViewById(R.id.btn_text);
        btnUpload = findViewById(R.id.btn_upload);
        llResultContainer = findViewById(R.id.ll_result_container);
        progressBar = findViewById(R.id.progress_bar);
        textInputLayout = findViewById(R.id.text_input_layout);
        etTextInput = findViewById(R.id.et_text_input);
        btnSubmitText = findViewById(R.id.btn_submit_text);
        btnAddItem = findViewById(R.id.btn_add_item);
        btnDone = findViewById(R.id.btn_done);

        // 设置模式切换监听
        btnFood.setOnClickListener(v -> {
            currentMode = MODE_FOOD;
            updateModeUI();
        });

        btnReceipt.setOnClickListener(v -> {
            currentMode = MODE_RECEIPT;
            updateModeUI();
        });

        btnText.setOnClickListener(v -> {
            currentMode = MODE_TEXT;
            updateModeUI();
            textInputLayout.setVisibility(View.VISIBLE);
            btnUpload.setVisibility(View.GONE);
        });

        // 上传按钮
        btnUpload.setOnClickListener(v -> {
            // 如果当前是文本模式，切换到图片模式
            if (btnUpload.getVisibility() == View.GONE) {
                currentMode = MODE_FOOD;
                updateModeUI();
            }
            showImagePickerDialog();
        });

        // 文本提交按钮
        btnSubmitText.setOnClickListener(v -> processTextInput());

        // 添加食材按钮
        btnAddItem.setOnClickListener(v -> addIngredientItem("", true));

        // 完成按钮
        btnDone.setOnClickListener(v -> {
            if (ingredientsList.isEmpty()) {
                Toast.makeText(this, "请添加食材", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取用户ID
            int userId = getCurrentUserId();
            if (userId == -1) {
                Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示进度条
            progressBar.setVisibility(View.VISIBLE);

            // 创建食材JSON数组 - 发送原始食材名称
            JSONArray jsonArray = new JSONArray();
            for (String ingredient : ingredientsList) {
                jsonArray.put(ingredient);
            }
            String ingredientsJson = jsonArray.toString();

            // 调用API添加食材
            ApiService apiService = ApiClient.getApiService();
            Call<ApiResponse<Void>> call = apiService.addIngredients(userId, ingredientsJson);
            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Void> apiResponse = response.body();
                        if (apiResponse.getCode() == 200) {
                            Toast.makeText(PhotoRecognitionActivity.this, "食材添加成功", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(PhotoRecognitionActivity.this, "添加失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PhotoRecognitionActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PhotoRecognitionActivity.this, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 初始化UI
        updateModeUI();
    }

    private int getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    private void updateModeUI() {
        btnFood.setBackgroundTintList(null);
        btnReceipt.setBackgroundTintList(null);
        btnText.setBackgroundTintList(null);

        btnFood.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_dark));
        btnReceipt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_dark));
        btnText.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_dark));

        if (currentMode == MODE_FOOD) {  
            btnFood.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_light));
            btnUpload.setText("选择食材图片");
            btnUpload.setVisibility(View.VISIBLE);
            textInputLayout.setVisibility(View.GONE);
        } else if (currentMode == MODE_RECEIPT) {
            btnReceipt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_light));
            btnUpload.setText("选择小票图片");
            btnUpload.setVisibility(View.VISIBLE);
            textInputLayout.setVisibility(View.GONE);
        } else if (currentMode == MODE_TEXT) {
            btnText.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_light));
            btnUpload.setVisibility(View.GONE);
            textInputLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择图片来源")
                .setItems(new String[]{"拍照上传", "相册上传"}, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkStoragePermission();
                    }
                })
                .show();
    }

    private void processTextInput() {
        String inputText = etTextInput.getText().toString().trim();
        if (!TextUtils.isEmpty(inputText)) {
            // 清空输入框
            etTextInput.setText("");

            // 处理用户输入（支持逗号分隔的多个食材）
            String[] items = inputText.split("[,，]");
            for (String item : items) {
                String trimmedItem = item.trim();
                if (!trimmedItem.isEmpty()) {
                    addIngredientItem(trimmedItem, true);
                }
            }
        } else {
            Toast.makeText(this, "请输入食材名称", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "需要存储权限访问相册", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap bitmap = null;

            if (requestCode == REQUEST_CAMERA && data != null) {
                // 从相机获取图片
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data");
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                // 从相册获取图片
                Uri imageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bitmap != null) {
                // 显示加载状态
                progressBar.setVisibility(View.VISIBLE);
                textInputLayout.setVisibility(View.GONE);

                // 压缩图片并转换为Base64
                Bitmap compressedBitmap = compressImage(bitmap);
                String imageBase64 = bitmapToBase64(compressedBitmap);

                // 调用豆包API识别
                recognizeWithDoubao(imageBase64);
            }
        }
    }

    private Bitmap compressImage(Bitmap original) {
        // 压缩图片到合适的大小
        int maxWidth = 1024;
        int maxHeight = 1024;
        int width = original.getWidth();
        int height = original.getHeight();

        if (width > maxWidth || height > maxHeight) {
            float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
            width = Math.round(ratio * width);
            height = Math.round(ratio * height);
            return Bitmap.createScaledBitmap(original, width, height, true);
        }
        return original;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // 使用PNG格式（避免JPEG压缩损失）
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // 使用NO_WRAP选项避免换行问题
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void recognizeWithDoubao(String imageBase64) {
        // 网络连接检查
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PhotoRecognitionActivity.this, "网络服务不可用", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PhotoRecognitionActivity.this, "无网络连接", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();

            // 1. 消息数组
            JSONArray messagesArray = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            // 2. 内容数组（文本+图片）
            JSONArray contentArray = new JSONArray();

            // 文本指令
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");

            // 根据模式设置不同指令
            String instruction;
            if (currentMode == MODE_FOOD || currentMode == MODE_TEXT) {
                instruction = "请识别这张图片中的食材，只回答食材名称，多个食材用逗号分隔。如果没有食材，回答'未识别到食材'";
            } else {
                instruction = "请识别这张超市小票中的食材项，按以下规则输出："
                        + "1. 只列出食材名称（如'西红柿'，不要带数量/价格）"
                        + "2. 每行一个食材"
                        + "3. 非食材内容不要输出"
                        + "4. 没有食材时回答'未识别到食材'";
            }

            textContent.put("text", instruction);
            contentArray.put(textContent);

            // 图片数据
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");

            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/png;base64," + imageBase64);

            imageContent.put("image_url", imageUrl);
            contentArray.put(imageContent);

            userMessage.put("content", contentArray);
            messagesArray.put(userMessage);
            requestBody.put("messages", messagesArray);

            // 3. 模型ID
            requestBody.put("model", MODEL_ID);

            // 4. 可选参数
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.1);

            // 创建请求体
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody.toString()
            );

            // 使用Retrofit调用API
            Call<DoubaoApiResponse> call = doubaoApiService.recognizeImage(
                    "Bearer " + API_KEY,
                    body
            );

            call.enqueue(new Callback<DoubaoApiResponse>() {
                @Override
                public void onResponse(Call<DoubaoApiResponse> call, Response<DoubaoApiResponse> response) {
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                    if (response.isSuccessful() && response.body() != null) {
                        DoubaoApiResponse apiResponse = response.body();
                        String result = apiResponse.getContent();

                        if (result != null) {
                            processRecognitionResult(result);
                        } else {
                            String errorMsg = apiResponse.getErrorMessage();
                            if (errorMsg == null) errorMsg = "未识别到食材";
                            Toast.makeText(PhotoRecognitionActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMsg = "API错误: " + response.code();
                        Toast.makeText(PhotoRecognitionActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DoubaoApiResponse> call, Throwable t) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PhotoRecognitionActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    Log.e("API_FAILURE", "Network error", t);
                }
            });

        } catch (JSONException e) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PhotoRecognitionActivity.this, "构建请求失败", Toast.LENGTH_SHORT).show();
            });
            Log.e("API_JSON", "Request build error", e);
        }
    }

    private void processRecognitionResult(String result) {
        // 处理识别结果
        if (result.contains("未识别") || result.isEmpty()) {
            Toast.makeText(this, "未识别到食材", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据模式处理结果
        if (currentMode == MODE_FOOD || currentMode == MODE_TEXT) {
            // 食材模式：逗号分隔
            String[] items = result.split("[,，]");
            for (String item : items) {
                String trimmedItem = item.trim();
                if (!trimmedItem.isEmpty()) {
                    addIngredientItem(trimmedItem, true);
                }
            }
        } else {
            // 小票模式：按行分割
            String[] lines = result.split("\\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() &&
                        !trimmedLine.contains("未识别") &&
                        trimmedLine.length() > 1) {
                    addIngredientItem(trimmedLine, true);
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private void addIngredientItem(String ingredient, boolean scrollToBottom) {
        // 创建食材项视图
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_photo_recognition, llResultContainer, false);

        EditText etIngredient = itemView.findViewById(R.id.et_ingredient);
        ImageButton btnDelete = itemView.findViewById(R.id.btn_delete);

        etIngredient.setText(ingredient);

        // 删除按钮点击事件
        btnDelete.setOnClickListener(v -> llResultContainer.removeView(itemView));

        // 添加文本变化监听，允许用户编辑
        etIngredient.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 更新食材列表
                updateIngredientsList();
            }
        });

        // 添加视图到容器
        llResultContainer.addView(itemView);

        // 滚动到底部
        if (scrollToBottom) {
            llResultContainer.post(() -> {
                View lastChild = llResultContainer.getChildAt(llResultContainer.getChildCount() - 1);
                lastChild.requestFocus();
            });
        }

        // 更新食材列表
        updateIngredientsList();
    }

    private void updateIngredientsList() {
        ingredientsList.clear();
        for (int i = 0; i < llResultContainer.getChildCount(); i++) {
            View itemView = llResultContainer.getChildAt(i);
            EditText et = itemView.findViewById(R.id.et_ingredient);
            String ingredient = et.getText().toString().trim();
            if (!ingredient.isEmpty()) {
                ingredientsList.add(ingredient);
            }
        }

        Log.d("INGREDIENTS", "当前食材列表: " + ingredientsList);
    }
}