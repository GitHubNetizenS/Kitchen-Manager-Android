package com.example.kitchen_manager.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.api.ApiClient;
import com.example.kitchen_manager.api.ApiService;
import com.example.kitchen_manager.response.RecipeResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onFavoriteClick(int recipeId, boolean isCurrentlyFavorite);
        void onDetailClick(int recipeId);
        void onCartClick(int recipeId, boolean isCurrentlyInCart);
    }

    public static final int PAGE_TYPE_NORMAL = 0;
    public static final int PAGE_TYPE_FAVORITE = 1;
    public static final int PAGE_TYPE_HISTORY = 2;

    private Context context;
    private List<RecipeResponse> recipeList;
    private OnItemClickListener listener;
    private int pageType;
    private boolean isSelectionMode = false;
    private boolean isEditMode = false;
    private List<Integer> selectedItems = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();
    private int userId = -1;
    private ApiService apiService;
    private Handler mainHandler;

    public RecipeAdapter(Context context, List<RecipeResponse> recipeList, OnItemClickListener listener, int pageType) {
        this.context = context;
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.listener = listener;
        this.pageType = pageType;
        this.apiService = ApiClient.getApiService();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 获取用户ID
        SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        this.userId = prefs.getInt("user_id", -1);
    }

    public void setRecipes(List<RecipeResponse> recipes) {
        this.recipeList = recipes != null ? recipes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<RecipeResponse> getRecipes() {
        return recipeList;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedItems.clear();
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedItems.clear();
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedIds() {
        return selectedIds;
    }

    public void removeSelectedId(int id) {
        selectedIds.remove(id);
        // 根据id找到对应的position并移除
        for (int i = 0; i < recipeList.size(); i++) {
            if (pageType == PAGE_TYPE_HISTORY) {
                if (recipeList.get(i).getHistoryId() == id) {
                    selectedItems.remove(Integer.valueOf(i));
                    break;
                }
            } else {
                if (recipeList.get(i).getRecipeId() == id) {
                    selectedItems.remove(Integer.valueOf(i));
                    break;
                }
            }
        }
    }

    public void clearSelection() {
        selectedItems.clear();
        selectedIds.clear();
        notifyDataSetChanged();
    }

    // 收藏页全选/取消全选
    public void selectAllFavorite(boolean selectAll) {
        if (selectAll) {
            selectedItems.clear();
            selectedIds.clear();
            for (int i = 0; i < recipeList.size(); i++) {
                selectedItems.add(i);
                selectedIds.add(recipeList.get(i).getRecipeId());
            }
        } else {
            selectedItems.clear();
            selectedIds.clear();
        }
    }

    // 历史页全选/取消全选
    public void selectAllHistory(boolean selectAll) {
        if (selectAll) {
            selectedItems.clear();
            selectedIds.clear();
            for (int i = 0; i < recipeList.size(); i++) {
                selectedItems.add(i);
                selectedIds.add(recipeList.get(i).getHistoryId());
            }
        } else {
            selectedItems.clear();
            selectedIds.clear();
        }
    }

    /**
     * 更新单个菜谱的购物车状态
     */
    public void updateCartStatus(int recipeId, boolean inShoppingCart) {
        for (int i = 0; i < recipeList.size(); i++) {
            RecipeResponse recipe = recipeList.get(i);
            if (recipe.getRecipeId() == recipeId) {
                recipe.setInShoppingCart(inShoppingCart);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * 格式化原料字符串，移除JSON数组的括号和引号
     * @param needs 原始原料字符串
     * @return 格式化后的原料字符串
     */
    private String formatNeeds(String needs) {
        if (needs == null || needs.isEmpty()) {
            return "";
        }

        // 如果包含JSON数组的特征（以[开头，以]结尾）
        if (needs.startsWith("[") && needs.endsWith("]")) {
            // 移除首尾的方括号
            String content = needs.substring(1, needs.length() - 1);

            // 移除所有引号
            content = content.replace("\"", "");

            // 按逗号分割并重新组合
            String[] ingredients = content.split(",");
            StringBuilder formatted = new StringBuilder();

            for (int i = 0; i < ingredients.length; i++) {
                String ingredient = ingredients[i].trim();
                if (!ingredient.isEmpty()) {
                    formatted.append(ingredient);
                    if (i < ingredients.length - 1) {
                        formatted.append(", ");
                    }
                }
            }

            // 限制显示长度，避免过长
            if (formatted.length() > 100) {
                return formatted.substring(0, 97) + "...";
            }

            return formatted.toString();
        }

        // 如果不是JSON格式，直接返回
        // 但可能还有引号，移除它们
        String cleaned = needs.replace("\"", "");
        cleaned = cleaned.replace("[", "").replace("]", "");

        // 限制显示长度
        if (cleaned.length() > 100) {
            return cleaned.substring(0, 97) + "...";
        }

        return cleaned;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipeResponse recipe = recipeList.get(position);

        // 设置选择模式可见性 - 根据不同的页面类型调整
        if (pageType == PAGE_TYPE_FAVORITE || pageType == PAGE_TYPE_HISTORY) {
            holder.cbSelect.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            // 根据页面类型选择正确的ID
            int targetId = (pageType == PAGE_TYPE_HISTORY) ? recipe.getHistoryId() : recipe.getRecipeId();
            holder.cbSelect.setChecked(selectedIds.contains(targetId));
        } else {
            holder.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            holder.cbSelect.setChecked(selectedItems.contains(position));
        }

        // 设置菜谱名称
        holder.tvRecipeName.setText(recipe.getName());

        // 设置菜谱属性：口味、方法、时间、难度
        String attributes = String.format("%s | %s | %s | %s",
                recipe.getTaste() != null ? recipe.getTaste() : "",
                recipe.getMethod() != null ? recipe.getMethod() : "",
                recipe.getTime() != null ? recipe.getTime() : "",
                recipe.getDifficulty() != null ? recipe.getDifficulty() : "");
        holder.tvRecipeAttributes.setText(attributes);

        // 设置菜谱原料或烹饪时间
        String displayText;
        if (pageType == PAGE_TYPE_HISTORY && recipe.getCookTime() != null) {
            displayText = "上次烹饪时间: " + recipe.getCookTime();
        } else {
            // 使用格式化后的原料字符串
            String needs = recipe.getNeeds();
            String formattedNeeds = formatNeeds(needs);
            displayText = "原料: " + formattedNeeds;
        }
        holder.tvRecipeNeeds.setText(displayText);

        // 设置菜谱图片 - 使用Glide优化
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            // 构造完整的图片URL
            String fullImageUrl = recipe.getImageUrl();
            if (!recipe.getImageUrl().startsWith("http")) {
                // 如果是相对路径，添加基础URL
                fullImageUrl = "http://10.0.2.2:8080" + (recipe.getImageUrl().startsWith("/") ? "" : "/") + recipe.getImageUrl();
            }

            // 使用Glide进行图片加载和缓存
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setImageResource(R.drawable.placeholder);
        }

        // 设置收藏图标状态
        if (recipe.isFavorite()) {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
        }

        // 设置购物车图标状态 - 关键修改点：根据购物车状态显示不同图标
        if (recipe.isInShoppingCart()) {
            holder.ivCart.setImageResource(R.drawable.buylist_1); // 已加入购物车
        } else {
            holder.ivCart.setImageResource(R.drawable.buylist); // 未加入购物车
        }

        // 设置收藏图标点击事件
        holder.ivFavorite.setOnClickListener(v -> {
            if (listener != null) {
                // 在编辑模式下，点击收藏图标不触发收藏/取消收藏
                if ((pageType == PAGE_TYPE_FAVORITE || pageType == PAGE_TYPE_HISTORY) && isEditMode) {
                    return;
                }
                listener.onFavoriteClick(recipe.getRecipeId(), recipe.isFavorite());
            }
        });

        // 设置购物车图标点击事件
        holder.ivCart.setOnClickListener(v -> {
            if (listener != null) {
                // 在编辑模式下，点击购物车图标不触发操作
                if ((pageType == PAGE_TYPE_FAVORITE || pageType == PAGE_TYPE_HISTORY) && isEditMode) {
                    return;
                }
                listener.onCartClick(recipe.getRecipeId(), recipe.isInShoppingCart());
            }
        });

        // 设置整个项目的点击事件（用于查看详情）
        holder.overlayClickArea.setOnClickListener(v -> {
            if (isSelectionMode || (pageType != PAGE_TYPE_NORMAL && isEditMode)) {
                // 在选择模式或编辑模式下，点击切换选择状态
                toggleSelection(position);
            } else {
                // 正常模式下，点击查看详情
                if (listener != null) {
                    listener.onDetailClick(recipe.getRecipeId());
                }
            }
        });

        // 设置整个项目的长按事件（用于进入选择模式）
        holder.overlayClickArea.setOnLongClickListener(v -> {
            if (!isSelectionMode && pageType == PAGE_TYPE_NORMAL) {
                isSelectionMode = true;
                selectedItems.add(position);
                notifyDataSetChanged();
                return true;
            }
            return false;
        });

        // 设置复选框点击事件
        holder.cbSelect.setOnClickListener(v -> {
            toggleSelection(position);
        });
    }

    private void toggleSelection(int position) {
        RecipeResponse recipe = recipeList.get(position);

        if (pageType == PAGE_TYPE_HISTORY || pageType == PAGE_TYPE_FAVORITE) {
            // 对于历史页和收藏页，使用对应的ID
            int targetId = (pageType == PAGE_TYPE_HISTORY) ? recipe.getHistoryId() : recipe.getRecipeId();

            if (selectedIds.contains(targetId)) {
                selectedIds.remove(targetId);
                selectedItems.remove(Integer.valueOf(position));
            } else {
                selectedIds.add(targetId);
                selectedItems.add(position);
            }
        } else {
            // 对于普通页，使用position
            if (selectedItems.contains(position)) {
                selectedItems.remove(Integer.valueOf(position));
            } else {
                selectedItems.add(position);
            }
        }
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivRecipeImage;
        TextView tvRecipeName;
        TextView tvRecipeAttributes;
        TextView tvRecipeNeeds;
        ImageView ivFavorite;
        ImageView ivCart;
        View overlayClickArea;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name);
            tvRecipeAttributes = itemView.findViewById(R.id.tv_recipe_attributes);
            tvRecipeNeeds = itemView.findViewById(R.id.tv_recipe_needs);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            ivCart = itemView.findViewById(R.id.iv_buy);
            overlayClickArea = itemView.findViewById(R.id.overlay_click_area);
        }
    }

}