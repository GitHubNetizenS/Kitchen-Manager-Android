package com.example.kitchen_manager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchen_manager.R;
import com.example.kitchen_manager.models.Ingredient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        private TextView tvRemainingDays; // 修改：改为剩余天数
        private TextView tvCategory;
        private ImageButton btnDelete;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIngredient = itemView.findViewById(R.id.iv_ingredient);
            tvName = itemView.findViewById(R.id.tv_name);
            tvRemainingDays = itemView.findViewById(R.id.tv_remaining_days); // 修改ID
            tvCategory = itemView.findViewById(R.id.tv_category);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Ingredient ingredient) {
            // 使用 Glide 加载图片
            if (ingredient.getImageUrl() != null && !ingredient.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(ingredient.getImageUrl())
                        .placeholder(R.drawable.ic_logo_orange)
                        .error(R.drawable.ic_logo_orange)
                        .into(ivIngredient);
            } else {
                ivIngredient.setImageResource(R.drawable.ic_logo_orange);
            }

            // 设置名称
            if (tvName != null) {
                tvName.setText(ingredient.getName());
            }

            // 设置分类
            if (tvCategory != null) {
                tvCategory.setText(ingredient.getCategory());
            }

            // 设置剩余天数（计算并设置颜色）
            if (tvRemainingDays != null) {
                // 计算剩余天数
                long remainingDays = calculateRemainingDays(ingredient.getExpiryDate());
                long abremainingDays = -remainingDays;

                // 设置文本
                tvRemainingDays.setText("剩余" + remainingDays + "天");

                // 设置颜色（使用与IngredientDetailActivity相同的逻辑）
                if (remainingDays > 7) {
                    // 设置文本
                    tvRemainingDays.setText("剩余" + remainingDays + "天");
                    tvRemainingDays.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green));
                } else if (remainingDays >= 3) {
                    // 设置文本
                    tvRemainingDays.setText("剩余" + remainingDays + "天");
                    tvRemainingDays.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.orange));
                } else if (remainingDays >= 0) {
                    // 设置文本
                    tvRemainingDays.setText("剩余" + remainingDays + "天");
                    tvRemainingDays.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red));
                } else {
                    // 设置文本
                    tvRemainingDays.setText("已过期" + abremainingDays + "天");
                    tvRemainingDays.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.brown));
                }
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

        /**
         * 计算剩余天数（与IngredientDetailActivity中的逻辑一致）
         */
        private long calculateRemainingDays(String expiryDateStr) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date expiryDate = sdf.parse(expiryDateStr);
                Date currentDate = new Date();
                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
}