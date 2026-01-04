package com.example.kitchen_manager.models;

import java.sql.Timestamp;

public class UserFavoriteRecipe {
    private int id;
    private int user_id;
    private int recipe_id;
    private Timestamp favorite_time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getRecipe_id() {
        return recipe_id;
    }

    public void setRecipe_id(int recipe_id) {
        this.recipe_id = recipe_id;
    }

    public Timestamp getFavorite_time() {
        return favorite_time;
    }

    public void setFavorite_time(Timestamp favorite_time) {
        this.favorite_time = favorite_time;
    }
// Getters and Setters
    // ...
}