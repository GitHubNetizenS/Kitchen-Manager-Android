package com.example.kitchen_manager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.kitchen_manager.activity.IngredientDetailActivity;
import com.example.kitchen_manager.activity.PhotoRecognitionActivity;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.adapters.IngredientAdapter;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.response.ApiResponse;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.models.Ingredient;
import com.example.kitchen_manager.response.IngredientResponse;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KitchenFragment extends Fragment implements IngredientAdapter.OnItemClickListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageView ivAdd;
    private IngredientAdapter adapter;
    private List<Ingredient> allIngredients = new ArrayList<>();
    private List<Ingredient> filteredIngredients = new ArrayList<>();
    private String[] categories = {
            "全部",
            "五谷杂粮",
            "蔬菜",
            "果品类",
            "肉类",
            "水产",
            "蛋、奶",
            "豆类、豆制品",
            "鱼类",
            "药食",
            "调味品",
            "其他"
    };
    //修改数组内容，可以直接映射修改页面中的分类
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kitchen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabLayout();
        setupRecyclerView();
        setupSearchBar();
        setupAddButton();
        loadUserIngredients(); // 从服务器加载食材
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tl_categories);
        recyclerView = view.findViewById(R.id.rv_ingredients);
        etSearch = view.findViewById(R.id.et_search);
        ivAdd = view.findViewById(R.id.iv_add);
    }

    private void setupTabLayout() {
        // 添加分类标签
        for (String category : categories) {//动态添加分类标签
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }

        // 设置标签选择监听器
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 选中标签时更新分类筛选
                String category = tab.getText().toString();
                String searchQuery = etSearch.getText().toString().toLowerCase();
                filterIngredients(category, searchQuery);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 默认选中第一个标签
        if (tabLayout.getTabCount() > 0) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
            filterIngredients(categories[0], "");
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new IngredientAdapter(filteredIngredients, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 搜索文本变化时更新筛选结果
                String searchQuery = s.toString().toLowerCase();
                if (tabLayout.getSelectedTabPosition() >= 0) {
                    String category = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
                    filterIngredients(category, searchQuery);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAddButton() {
        ivAdd.setOnClickListener(v -> {
            // 跳转到添加食材页面
            Intent intent = new Intent(requireContext(), PhotoRecognitionActivity.class);
            startActivityForResult(intent, 1001);
        });
    }

    private void loadUserIngredients() {
        // 获取当前用户ID
        int userId = getCurrentUserId();
        if (userId == 0) {
            Toast.makeText(requireContext(), "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse<List<IngredientResponse>>> call = apiService.getUserIngredients(userId);

        call.enqueue(new Callback<ApiResponse<List<IngredientResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<IngredientResponse>>> call,
                                   Response<ApiResponse<List<IngredientResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<IngredientResponse> serverIngredients = response.body().getData();

                    allIngredients.clear();

                    // 修改for循环，添加expiryDays参数
                    for (IngredientResponse item : serverIngredients) {
                        // 使用服务器返回的保质期天数，如果为空则使用默认值3
                        int expiryDays = item.getExpiryDays() != null ? item.getExpiryDays() : 3;

                        allIngredients.add(new Ingredient(
                                item.getName(),
                                item.getCategory(),
                                item.getStorageDate(),
                                item.getExpiryDate(),
                                expiryDays, // 保质期天数
                                R.drawable.ic_logo_orange, // 图片资源ID
                                item.getNutrition(),
                                item.getBenefit(),
                                item.getImageUrl()
                        ));
                    }

                    // 更新UI
                    if (tabLayout.getTabCount() > 0) {
                        String selectedCategory = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
                        filterIngredients(selectedCategory, etSearch.getText().toString().toLowerCase());
                    }
                } else {
                    String errorMsg = "获取食材失败: ";
                    if (response.body() != null) {
                        errorMsg += response.body().getMessage();
                    } else {
                        errorMsg += "响应体为空";
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<IngredientResponse>>> call, Throwable t) {
                Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("KitchenFragment", "加载食材失败", t);
            }
        });
    }

    // 获取当前登录用户ID
    private int getCurrentUserId() {
        SharedPreferences prefs = requireContext().
                getSharedPreferences("user_session", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    private void filterIngredients(String category, String searchQuery) {
        filteredIngredients.clear();

        if ("全部".equals(category)) {
            filteredIngredients.addAll(allIngredients);
        } else {
            for (Ingredient ingredient : allIngredients) {
                // 使用真实分类进行匹配
                if (category.equals(ingredient.getCategory())) {
                    filteredIngredients.add(ingredient);
                }
            }
        }

        // 根据搜索词进一步筛选
        if (!searchQuery.isEmpty()) {
            List<Ingredient> searchResults = new ArrayList<>();
            for (Ingredient ingredient : filteredIngredients) {
                if (ingredient.getName().toLowerCase().contains(searchQuery)) {
                    searchResults.add(ingredient);
                }
            }
            filteredIngredients.clear();
            filteredIngredients.addAll(searchResults);
        }

        // 通知适配器更新数据
        if (adapter != null) {
            adapter.updateList(filteredIngredients);
        }
    }

    //7.4 4:30pm修改
    @Override
    public void onItemClick(int position) {
        if (position >= 0 && position < filteredIngredients.size()) {
            Ingredient ingredient = filteredIngredients.get(position);
            Intent intent = new Intent(requireContext(), IngredientDetailActivity.class);
            intent.putExtra("ingredient", ingredient);
            // 使用startActivityForResult并设置请求码
            startActivityForResult(intent, 1002);
        }
    }


    //7.4 4pm修改方法
    @Override
    public void onDeleteClick(int position) {
        if (position >= 0 && position < filteredIngredients.size()) {
            Ingredient deleted = filteredIngredients.get(position);
            int userId = getCurrentUserId();

            if (userId == -1) {
                Toast.makeText(requireContext(), "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用API删除关联
            ApiService apiService = ApiClient.getApiService();
            Call<ApiResponse<Void>> call = apiService.deleteUserIngredient(userId, deleted.getName());
            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                        filteredIngredients.remove(position);
                        allIngredients.remove(deleted);
                        adapter.notifyItemRemoved(position);
                        Toast.makeText(requireContext(), "已删除: " + deleted.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    //7.4 4:30修改
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理添加食材的返回
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK) {
            loadUserIngredients();
        }
        // 处理详情页的返回
        else if (requestCode == 1002 && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                if (data.getBooleanExtra("deleted", false) ||
                        data.getBooleanExtra("updated", false)) {
                    // 如果删除了食材或更新了食材，刷新列表
                    loadUserIngredients();
                }
            }
        }
    }
}