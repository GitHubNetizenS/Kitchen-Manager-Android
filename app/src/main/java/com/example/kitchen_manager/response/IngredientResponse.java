package com.example.kitchen_manager.response;

public class IngredientResponse {
    private String name;
    private String storageDate;
    private String expiryDate;
    private String nutrition;
    private String benefit;
    private String imageUrl; // 新增图片URL字段
    private String category; // 新增分类字段

    // 构造器和getter/setter
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    public IngredientResponse() {
        // 空构造器
    }

    public IngredientResponse(String name, String storageDate, String expiryDate,
                              String nutrition, String benefit, String imageUrl) {
        this.name = name;
        this.storageDate = storageDate;
        this.expiryDate = expiryDate;
        this.nutrition = nutrition;
        this.benefit = benefit;
        this.imageUrl = imageUrl; // 新增
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorageDate() {
        return storageDate;
    }

    public void setStorageDate(String storageDate) {
        this.storageDate = storageDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNutrition() {
        return nutrition;
    }

    public void setNutrition(String nutrition) {
        this.nutrition = nutrition;
    }

    public String getBenefit() {
        return benefit;
    }

    public void setBenefit(String benefit) {
        this.benefit = benefit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}