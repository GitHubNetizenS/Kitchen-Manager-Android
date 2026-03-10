package com.example.kitchen_manager.models;

public class Recipe {
    private int recipe_id;
    private String name;
    private String image_url;
    private String needs; // JSON 字符串
    private String steps; // JSON 数组
    private String taste;
    private String difficulty;
    private String time;
    private String method;
    private int popularity;

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getNeeds() {
        return needs;
    }

    public void setNeeds(String needs) {
        this.needs = needs;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getTaste() {
        return taste != null ? taste : "";
    }

    public void setTaste(String taste) {
        this.taste = taste;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }


    // Getters and Setters
    public int getRecipe_id() { return recipe_id; }
    public void setRecipe_id(int recipe_id) { this.recipe_id = recipe_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }


}