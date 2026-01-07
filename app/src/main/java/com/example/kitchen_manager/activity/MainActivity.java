package com.example.kitchen_manager.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.databinding.ActivityMainBinding;
import com.example.kitchen_manager.fragment.KitchenFragment;
import com.example.kitchen_manager.fragment.MineFragment;
import com.example.kitchen_manager.fragment.RecommendFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private ImageView  ivSearch;
    private TextView tvTitle;
    private NavigationView navigationView;
    private ActivityMainBinding binding;
    private ApiService apiService;
    private ImageView tobuy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        initViews();
        setupListeners();
        setupBottomNavigation();
        setupNavigationView();

        // 默认显示首页
        switchToFragment(new RecommendFragment());
        tvTitle.setText("推荐食谱");
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
        ivSearch = findViewById(R.id.iv_search);
        tvTitle = findViewById(R.id.tv_title);
        navigationView = findViewById(R.id.navigation_view);
        tobuy = findViewById(R.id.tobuy);
    }

    public ApiService getApiService() {
        return apiService;
    }

    // 在 setupListeners() 方法中修改搜索点击事件
    private void setupListeners() {
        tobuy.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        // 修改为启动 SearchActivity 并传递搜索意图
        ivSearch.setOnClickListener(v -> {
            Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
            searchIntent.putExtra("search_mode", "normal"); // 标识普通搜索模式
            startActivity(searchIntent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";

            if(item.getItemId()==R.id.nav_home) {
                selectedFragment = new RecommendFragment();
                title="推荐食谱";
            }
            else if(item.getItemId()==R.id.nav_kitchen) {
                selectedFragment = new KitchenFragment();
                title="我的厨房";
            }
            else if(item.getItemId()==R.id.nav_profile){
                selectedFragment = new MineFragment();
                title="我的";
            }

            if (selectedFragment != null) {
                switchToFragment(selectedFragment);
                tvTitle.setText(title);
                return true;
            }
            return false;
        });
    }

    private void setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(item -> {
            // 关闭侧边栏
            drawerLayout.closeDrawer(navigationView);

            int itemId = item.getItemId();
            if (itemId == R.id.nav_favorites) {
                // 我的收藏逻辑
                Toast.makeText(MainActivity.this, "我的收藏", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_settings) {
                // 设置逻辑
                Toast.makeText(MainActivity.this, "设置", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_help) {
                // 帮助与反馈逻辑
                Toast.makeText(MainActivity.this, "帮助与反馈", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void switchToFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}