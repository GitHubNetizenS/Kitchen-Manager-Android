package com.example.kitchen_manager.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.response.RecipeResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final Context context;
    private List<RecipeResponse> recipeList;
    private final OnItemClickListener listener;
    private boolean isEditMode = false;
    private Set<Integer> selectedIds = new HashSet<>();

    // 新增：页面类型常量
    public static final int PAGE_TYPE_NORMAL = 0;    // 普通页面（显示原材料）
    public static final int PAGE_TYPE_HISTORY = 1;   // 历史记录页面（显示烹饪时间）
    public static final int PAGE_TYPE_FVORITE = 2;   // 收藏页面
    private int pageType = PAGE_TYPE_NORMAL;

    public interface OnItemClickListener {
        void onFavoriteClick(int recipeId); // 只传递菜谱ID
        void onDetailClick(int recipeId);   // 只传递菜谱ID
    }

    private static class RecipeDiffCallback extends DiffUtil.Callback {
        private final List<RecipeResponse> oldList;
        private final List<RecipeResponse> newList;

        public RecipeDiffCallback(List<RecipeResponse> oldList, List<RecipeResponse> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getRecipeId() ==
                    newList.get(newItemPosition).getRecipeId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            RecipeResponse oldRecipe = oldList.get(oldItemPosition);
            RecipeResponse newRecipe = newList.get(newItemPosition);

            return Objects.equals(oldRecipe.getName(), newRecipe.getName()) &&
                    Objects.equals(oldRecipe.getImageUrl(), newRecipe.getImageUrl()) &&
                    oldRecipe.isFavorite() == newRecipe.isFavorite();
        }
    }

    // 修改构造函数：添加页面类型参数
    public RecipeAdapter(Context context, List<RecipeResponse> recipes,
                         OnItemClickListener listener, int pageType) {
        this.context = context;
        this.recipeList = recipes;
        this.listener = listener;
        this.pageType = pageType;
    }

    // 保留原来的构造函数（兼容其他页面）
    public RecipeAdapter(Context context, List<RecipeResponse> recipes, OnItemClickListener listener) {
        this(context, recipes, listener, PAGE_TYPE_NORMAL);
    }

    // 新增：设置页面类型的方法
    public void setPageType(int pageType) {
        this.pageType = pageType;
        notifyDataSetChanged(); // 刷新显示
    }

    public void setRecipes(List<RecipeResponse> newRecipes) {
        List<RecipeResponse> newList = new ArrayList<>(newRecipes);

        RecipeDiffCallback diffCallback = new RecipeDiffCallback(recipeList, newList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.recipeList.clear();
        this.recipeList.addAll(newList);

        diffResult.dispatchUpdatesTo(this);
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedIds.clear();
        }
    }

    public Set<Integer> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void removeSelectedId(int id) {
        selectedIds.remove(id);
    }

    public void selectAllHistory(boolean select) {
        selectedIds.clear();
        if (select) {
            for (RecipeResponse recipe : recipeList) {
                selectedIds.add(recipe.getHistoryId());
            }
        }
    }

    public void selectAllFavorite(boolean select) {
        selectedIds.clear();
        if (select) {
            for (RecipeResponse recipe : recipeList) {
                selectedIds.add(recipe.getRecipeId());
            }
        }
    }

    public void clearSelection() {
        selectedIds.clear();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeResponse recipe = recipeList.get(position);

        holder.tvRecipeName.setText(recipe.getName());

        String attributes = String.format("%s · %s · %s · %s",
                recipe.getTaste(), recipe.getMethod(), recipe.getTime(), recipe.getDifficulty());
        holder.tvRecipeAttributes.setText(attributes);

        // 关键修改：根据页面类型显示不同内容
        if (pageType == PAGE_TYPE_HISTORY) {
            // 历史记录页面：显示烹饪时间
            String cookTimeText = formatCookTime(recipe.getCookTime());
            holder.tvRecipeNeeds.setText(cookTimeText);
        } else {
            // 普通页面：显示原材料
            String formattedNeeds = formatNeeds(recipe.getNeeds());
            holder.tvRecipeNeeds.setText(formattedNeeds);
        }

        // 图片加载
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .override(300, 300)
                    .centerCrop()
                    .into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setImageResource(R.drawable.placeholder);
        }

        // 更新收藏图标状态
        int favoriteIcon = recipe.isFavorite() ?
                R.drawable.ic_favorite : R.drawable.ic_favorite_border;
        holder.ivFavorite.setImageResource(favoriteIcon);

        // 设置收藏按钮点击事件
        holder.ivFavorite.setOnClickListener(v -> {
            if (!isEditMode) {
                listener.onFavoriteClick(recipe.getRecipeId());
            }
        });

        // 详情图标点击
        holder.ivDetail.setOnClickListener(v -> {
            if (!isEditMode) {
                Log.d("RecipeAdapter", "点击 recipeId = " + recipe.getRecipeId());
                listener.onDetailClick(recipe.getRecipeId());
            }
        });

        // 编辑模式处理
        holder.cbSelect.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        if (isEditMode) {
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedIds.contains(recipe.getHistoryId())); // 使用historyId
            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (pageType==1)
                {
                    if (isChecked) {
                        selectedIds.add(recipe.getHistoryId()); // 使用historyId
                    } else {
                        selectedIds.remove(recipe.getHistoryId()); // 使用historyId
                    }
                }
                else if(pageType==2)
                {
                    if (isChecked) {
                        selectedIds.add(recipe.getRecipeId()); // 使用recipeId
                    } else {
                        selectedIds.remove(recipe.getRecipeId()); // 使用recipeId
                    }
                }
            });
        }
    }

    // 新增：格式化烹饪时间的方法
    private String formatCookTime(String cookTime) {
        if (TextUtils.isEmpty(cookTime)) {
            return "暂无烹饪时间";
        }

        // 方法1：直接截取前16个字符
        if (cookTime.length() >= 16) {
            // 确保格式是YYYY-MM-DD HH:MM:SS
            // 截取YYYY-MM-DD HH:MM
            return "上次烹饪：" + cookTime.substring(0, 16);
        }

        // 方法2：如果是其他格式，尝试解析
        try {
            // 尝试常见的日期格式
            String[] formats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS"
            };

            for (String format : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    Date date = sdf.parse(cookTime);
                    if (date != null) {
                        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        return "上次烹饪：" + output.format(date);
                    }
                } catch (ParseException e) {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果都失败，返回原始值
        return "上次烹饪：" + cookTime;
    }

    // 提取原材料格式化逻辑为独立方法
    private String formatNeeds(String needs) {
        String formattedNeeds = "未知原料";
        if (needs != null && !needs.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> ingredients = gson.fromJson(needs, listType);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ingredients.size(); i++) {
                    sb.append(ingredients.get(i));
                    if (i < ingredients.size() - 1) {
                        sb.append("、");
                    }
                }
                formattedNeeds = sb.toString();
            } catch (Exception e) {
                // 如果解析失败，尝试简单处理
                formattedNeeds = needs
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .replace(",", "、");
            }

            // 如果处理后的字符串过长，截断显示
            if (formattedNeeds.length() > 50) {
                formattedNeeds = formattedNeeds.substring(0, 50) + "...";
            }
        }
        return formattedNeeds;
    }

    @Override
    public void onViewRecycled(@NonNull RecipeViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(context).clear(holder.ivRecipeImage);
        holder.itemView.setTag(null);
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName;
        TextView tvRecipeAttributes;
        TextView tvRecipeNeeds;
        ImageView ivFavorite;
        ImageView ivDetail;
        CheckBox cbSelect;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name);
            tvRecipeAttributes = itemView.findViewById(R.id.tv_recipe_attributes);
            tvRecipeNeeds = itemView.findViewById(R.id.tv_recipe_needs);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            ivDetail = itemView.findViewById(R.id.iv_detail);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }
    }

    // 获取食谱列表
    public List<RecipeResponse> getRecipes() {
        return recipeList;
    }
}