package com.example.kitchen_manager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.models.Ingredient;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {

    private List<Ingredient> ingredients;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onDeleteClick(int position);
    }

    public IngredientAdapter(List<Ingredient> ingredients, OnItemClickListener listener) {
        this.ingredients = ingredients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient ingredient = ingredients.get(position);
        holder.bind(ingredient);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public void updateList(List<Ingredient> newList) {
        ingredients = newList;
        notifyDataSetChanged();
    }

    class IngredientViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIngredient;
        private TextView tvName;
        private TextView tvExpiryDate;
        private TextView tvNutrition;
        private TextView tvCategory;
        private ImageButton btnDelete;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIngredient = itemView.findViewById(R.id.iv_ingredient);
            tvName = itemView.findViewById(R.id.tv_name);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvNutrition=itemView.findViewById(R.id.tv_nutrition);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Ingredient ingredient) {
            // 使用 Glide 加载图片
            if (ingredient.getImageUrl() != null && !ingredient.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(ingredient.getImageUrl())
                        .placeholder(R.drawable.ic_logo_orange) // 占位图
                        .error(R.drawable.ic_logo_orange)       // 错误图
                        .into(ivIngredient);
            } else {
                ivIngredient.setImageResource(R.drawable.ic_logo_orange);
            }

            // 设置文本前检查视图是否为空
            if (tvName != null) {
                tvName.setText(ingredient.getName());
            }

            if (tvCategory != null) {
                tvCategory.setText(ingredient.getCategory());
            }

            if (tvExpiryDate != null) {
                tvExpiryDate.setText("到期: " + ingredient.getExpiryDate());
            }

            if (tvNutrition != null) {
                tvNutrition.setText(ingredient.getFormattedNutrition());
            }

            // 设置点击监听器
            if (itemView != null) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(getAdapterPosition());
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(getAdapterPosition());
                    }
                });
            }
        }
    }
}