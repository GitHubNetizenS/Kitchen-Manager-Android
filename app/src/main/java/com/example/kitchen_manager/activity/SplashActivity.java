package com.example.kitchen_manager.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.kitchen_manager.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 5000; // 5秒
    private static final long COUNT_DOWN_INTERVAL = 1000; // 每秒更新一次

    private CountDownTimer countDownTimer;
    private Button btnSkip;
    private TextView tvCountDown;
    private ProgressBar pbSplash;
    private ImageView ivLogo;
    private TextView tvTitle, tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startAnimations();
        startCountDown();
    }

    private void initViews() {
        btnSkip = findViewById(R.id.btn_skip);
        tvCountDown = findViewById(R.id.tv_count_down);
        pbSplash = findViewById(R.id.pb_splash);
        ivLogo = findViewById(R.id.iv_splash_logo);
        tvTitle = findViewById(R.id.tv_splash_title);
        tvSubtitle = findViewById(R.id.tv_splash_subtitle);

        btnSkip.setOnClickListener(v -> {
            cancelCountDown();
            navigateToNextActivity();
        });
    }

    private void startAnimations() {
        // Logo淡入动画
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivLogo.startAnimation(fadeIn);

        // 标题和副标题的上移动画
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        tvTitle.startAnimation(slideUp);
        tvSubtitle.startAnimation(slideUp);
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(SPLASH_DURATION, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvCountDown.setText(String.format("跳过 %ds", secondsRemaining));
            }

            @Override
            public void onFinish() {
                navigateToNextActivity();
            }
        }.start();
    }

    private void cancelCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void navigateToNextActivity() {
        // 检查用户是否已登录
        if (isUserLoggedIn()) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
        // 添加淡入淡出过渡效果
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private boolean isUserLoggedIn() {
        // 从SharedPreferences读取用户会话
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);

        // 检查是否存在有效的用户ID
        int userId = prefs.getInt("user_id", -1);
        boolean isLoggedIn = (userId != -1);

        return isLoggedIn;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountDown();
    }
}