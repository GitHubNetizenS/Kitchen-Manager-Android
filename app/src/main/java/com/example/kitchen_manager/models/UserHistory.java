package com.example.kitchen_manager.models;

import com.google.gson.annotations.SerializedName;
import java.sql.Timestamp;

public class UserHistory {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("recipe_id")
    private int recipeId;

    @SerializedName("cook_time")
    private String cookTime; // 注意：这里用String而不是Timestamp，因为Gson处理起来更方便

    // Getter和Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public String getCookTime() {
        return cookTime;
    }

    public void setCookTime(String cookTime) {
        this.cookTime = cookTime;
    }
}