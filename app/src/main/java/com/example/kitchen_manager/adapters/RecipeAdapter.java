package com.example.kitchen_manager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final Context context;
    private List<RecipeResponse> recipeList;
    private final OnItemClickListener listener;

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

            // 只比较影响显示的字段，而不是所有字段
            return Objects.equals(oldRecipe.getName(), newRecipe.getName()) &&
                    Objects.equals(oldRecipe.getImageUrl(), newRecipe.getImageUrl()) &&
                    oldRecipe.isFavorite() == newRecipe.isFavorite();
        }
    }

    public RecipeAdapter(Context context, List<RecipeResponse> recipes, OnItemClickListener listener) {
        this.context = context;
        this.recipeList = recipes;
        this.listener = listener;
    }

    public void setRecipes(List<RecipeResponse> newRecipes) {
        // 确保传入的是新的列表
        List<RecipeResponse> newList = new ArrayList<>(newRecipes);

        RecipeDiffCallback diffCallback = new RecipeDiffCallback(recipeList, newList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        // 更新数据
        this.recipeList.clear();
        this.recipeList.addAll(newList);

        // 必须在主线程调用
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        if (position < 0 || position >= recipeList.size()) {
            return;
        }

        RecipeResponse recipe = recipeList.get(position);

        if(holder.itemView.getTag()!=null && (int)holder.itemView.getTag()==recipe.getRecipeId()) {

            return;
        }
        holder.itemView.setTag(recipe.getRecipeId());
        // 设置菜谱名称
        holder.tvRecipeName.setText(recipe.getName());

        // 设置属性：口味、方法、时间、难度
        String attributes = String.format("%s · %s · %s · %s",
                recipe.getTaste() != null ? recipe.getTaste() : "",
                recipe.getMethod() != null ? recipe.getMethod() : "",
                recipe.getTime() != null ? recipe.getTime() : "",
                recipe.getDifficulty() != null ? recipe.getDifficulty() : "");

        holder.tvRecipeAttributes.setText(attributes);

        // 设置原料（needs字段）
        String needs = recipe.getNeeds();
        String formattedNeeds = "暂无原料信息";

        if (needs != null && !needs.isEmpty()) {
            try {
                // 使用 Gson 解析 JSON 数组
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> ingredients = gson.fromJson(needs, listType);

                // 使用中文顿号连接原料
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
                e.printStackTrace();
            }

            // 如果处理后的字符串过长，截断显示
            if (formattedNeeds.length() > 50) {
                formattedNeeds = formattedNeeds.substring(0, 50) + "...";
            }
        }

        holder.tvRecipeNeeds.setText(formattedNeeds);

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    // 添加以下优化配置
                    .thumbnail(0.25f) // 先加载缩略图（原图的25%）
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存所有版本的图片
                    .skipMemoryCache(false) // 启用内存缓存
                    .override(300, 300) // 限制图片尺寸，根据你的布局调整
                    .centerCrop() // 使用合适的裁剪方式
                    .into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setImageResource(R.drawable.placeholder);
        }

        // 更新收藏图标状态
        int favoriteIcon = recipe.isFavorite() ?
                R.drawable.ic_favorite : R.drawable.ic_favorite_border;
        holder.ivFavorite.setImageResource(favoriteIcon);

        // 设置收藏按钮点击事件 - 简化处理
        holder.ivFavorite.setOnClickListener(v -> {
            // 直接传递菜谱ID
            listener.onFavoriteClick(recipe.getRecipeId());
        });

        // 详情图标点击
        holder.ivDetail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetailClick(recipe.getRecipeId());
            }
        });
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

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name);
            tvRecipeAttributes = itemView.findViewById(R.id.tv_recipe_attributes);
            tvRecipeNeeds = itemView.findViewById(R.id.tv_recipe_needs);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            ivDetail = itemView.findViewById(R.id.iv_detail);
        }
    }

    // 获取食谱列表
    public List<RecipeResponse> getRecipes() {
        return recipeList;
    }
}