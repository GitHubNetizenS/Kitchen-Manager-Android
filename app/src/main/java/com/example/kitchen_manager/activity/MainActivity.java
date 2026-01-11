package com.example.kitchen_manager.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.databinding.ActivityMainBinding;
import com.example.kitchen_manager.fragment.KitchenFragment;
import com.example.kitchen_manager.fragment.MineFragment;
import com.example.kitchen_manager.fragment.RecommendFragment;
import com.example.kitchen_manager.fragment.ToBuyFragment;
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
    private FrameLayout navFragmentContainer;  // 新增：侧边栏中的Fragment容器
    private boolean isToBuyFragmentShowing = false;

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

        // 在侧边栏中动态添加一个Fragment容器
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            navFragmentContainer = new FrameLayout(this);
            navFragmentContainer.setId(R.id.nav_fragment_container);
            navFragmentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,  // 高度设为0，通过weight控制
                    1.0f  // weight为1，占满剩余空间
            ));

            // 将Fragment容器添加到头部布局下面
            ViewGroup parent = (ViewGroup) headerView.getParent();
            int headerIndex = parent.indexOfChild(headerView);
            parent.addView(navFragmentContainer, headerIndex + 1);
        }
    }

    public ApiService getApiService() {
        return apiService;
    }

    private void setupListeners() {
        //打开侧边栏
        tobuy.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                // 如果侧边栏已经打开，关闭它
                drawerLayout.closeDrawer(navigationView);
            } else {
                // 打开侧边栏并显示ToBuyFragment
                drawerLayout.openDrawer(navigationView);

                // 延迟一小段时间确保侧边栏完全打开后再显示Fragment
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isToBuyFragmentShowing) {
                        showToBuyFragmentInDrawer();
                    }
                }, 100);
            }
        });

        // 添加侧边栏监听器
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // 滑动时
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                // 侧边栏打开时显示ToBuyFragment
                if (drawerView == navigationView) {
                    if (!isToBuyFragmentShowing) {
                        showToBuyFragmentInDrawer();
                    }
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // 侧边栏关闭时隐藏ToBuyFragment
                if (drawerView == navigationView) {
                    hideToBuyFragment();
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // 状态改变时
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
        // 设置侧边栏的头部视图点击事件
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            View shoppingCartHeader = headerView.findViewById(R.id.tobuy);
            if (shoppingCartHeader != null) {
                shoppingCartHeader.setOnClickListener(v -> {
                    // 点击标题时，确保ToBuyFragment显示
                    if (!isToBuyFragmentShowing) {
                        showToBuyFragmentInDrawer();
                    }
                });
            }
        }
    }

    private void switchToFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // 新增：在侧边栏中显示ToBuyFragment
    private void showToBuyFragmentInDrawer() {
        if (navFragmentContainer == null || isToBuyFragmentShowing) {
            return;
        }

        // 创建并显示ToBuyFragment到侧边栏的容器中
        ToBuyFragment toBuyFragment = new ToBuyFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_fragment_container, toBuyFragment);
        transaction.commit();

        isToBuyFragmentShowing = true;
        tvTitle.setText("购买清单");
    }

    // 新增：隐藏侧边栏中的ToBuyFragment
    private void hideToBuyFragment() {
        if (navFragmentContainer == null || !isToBuyFragmentShowing) {
            return;
        }

        // 从侧边栏容器中移除Fragment
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_fragment_container);
        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(fragment);
            transaction.commit();
        }

        isToBuyFragmentShowing = false;
    }

    // 新增：关闭侧边栏并跳转到推荐页面
    public void closeDrawerAndNavigateToRecommend() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        }
    }
}