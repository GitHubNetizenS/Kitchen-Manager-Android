package com.example.kitchen_manager.models;

import java.sql.Timestamp;

public class UserHistory {
    private int id;
    private int user_id;
    private int recipe_id;

    public Timestamp getCook_time() {
        return cook_time;
    }

    public void setCook_time(Timestamp cook_time) {
        this.cook_time = cook_time;
    }

    public int getRecipe_id() {
        return recipe_id;
    }

    public void setRecipe_id(int recipe_id) {
        this.recipe_id = recipe_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private Timestamp cook_time;

    // Getters and Setters
    // ...
}