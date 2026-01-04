package com.example.kitchen_manager.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Ingredient implements Parcelable {
    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    private int ingredientId;
    private String name;
    private String category;
    private String storageDate;
    private String expiryDate;
    private int imageResId;
    private String nutrition;
    private String benefit;
    private String imageUrl; // 新增图片URL字段

    public Ingredient(String name, String category, String storageDate, String expiryDate,
                      int imageResId, String nutrition, String benefit, String imageUrl) {
        this.name = name;
        this.category = category;
        this.storageDate = storageDate;
        this.expiryDate = expiryDate;
        this.imageResId = imageResId;
        this.nutrition = nutrition;
        this.benefit = benefit;
        this.imageUrl = imageUrl; // 新增
    }

    protected Ingredient(Parcel in) {
        name = in.readString();
        category = in.readString();
        storageDate = in.readString();
        expiryDate = in.readString();
        imageResId = in.readInt();
        nutrition = in.readString();
        benefit = in.readString();
        imageUrl = in.readString(); // 新增

    }

    public static final Creator<Ingredient> CREATOR = new Creator<Ingredient>() {
        @Override
        public Ingredient createFromParcel(Parcel in) {
            return new Ingredient(in);
        }

        @Override
        public Ingredient[] newArray(int size) {
            return new Ingredient[size];
        }
    };

    // Getters
    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getStorageDate() {
        return storageDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getNutrition() {
        return nutrition;
    }

    public String getBenefit() {
        return benefit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(storageDate);
        dest.writeString(expiryDate);
        dest.writeInt(imageResId);
        dest.writeString(nutrition);
        dest.writeString(benefit);
        dest.writeString(imageUrl); // 新增
    }
    // 添加一个辅助方法用于获取格式化后的营养信息
    public String getFormattedNutrition() {
        if (nutrition == null || nutrition.isEmpty()) {
            return "暂无数据";
        }

        try {
            JSONObject json = new JSONObject(nutrition);
            JSONArray components = json.getJSONArray("components");

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < components.length(); i++) {
                if (i > 0) {
                    builder.append("、");
                }
                builder.append(components.getString(i));
            }

            return builder.toString();
        } catch (JSONException e) {
            return nutrition;
        }
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setStorageDate(String storageDate) {
        this.storageDate = storageDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBenefit(String benefit) {
        this.benefit = benefit;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setNutrition(String nutrition) {
        this.nutrition = nutrition;
    }
}