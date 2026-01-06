package com.example.kitchen_manager.response;

public class IngredientResponse {
    private String name;
    private String storageDate;
    private String expiryDate;
    private Integer expiryDays; // 新增：保质期天数
    private String nutrition;
    private String benefit;
    private String imageUrl;
    private String category;

    // 修改构造器，添加expiryDays参数
    public IngredientResponse(String name, String storageDate, String expiryDate,
                              Integer expiryDays, String nutrition, String benefit,
                              String imageUrl, String category) {
        this.name = name;
        this.storageDate = storageDate;
        this.expiryDate = expiryDate;
        this.expiryDays = expiryDays; // 新增
        this.nutrition = nutrition;
        this.benefit = benefit;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    // 空构造器
    public IngredientResponse() {
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

    public Integer getExpiryDays() { // 新增
        return expiryDays;
    }

    public void setExpiryDays(Integer expiryDays) { // 新增
        this.expiryDays = expiryDays;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}