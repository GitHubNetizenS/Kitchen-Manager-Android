package com.example.kitchen_manager.response;

import com.google.gson.annotations.SerializedName;

public class RecipeResponse {
    @SerializedName("recipe_id")
    private int recipeId;
    private String name;
    @SerializedName("image_url")
    private String imageUrl;
    private String taste;
    private String method;
    private String time;
    private String difficulty;
    private String needs;
    private boolean isFavorite; // 收藏状态字段

    // Getter和Setter
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTaste() { return taste; }
    public void setTaste(String taste) { this.taste = taste; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getNeeds() { return needs; }
    public void setNeeds(String needs) { this.needs = needs; }
}