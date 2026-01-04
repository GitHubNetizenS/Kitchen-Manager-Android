package com.example.kitchen_manager.models;

public class RecipeTag {
    private int id;
    private int recipe_id;
    private int tag_id;
    private int match_amount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecipe_id() {
        return recipe_id;
    }

    public void setRecipe_id(int recipe_id) {
        this.recipe_id = recipe_id;
    }

    public int getTag_id() {
        return tag_id;
    }

    public void setTag_id(int tag_id) {
        this.tag_id = tag_id;
    }

    public int getMatch_amount() {
        return match_amount;
    }

    public void setMatch_amount(int match_amount) {
        this.match_amount = match_amount;
    }
// Getters and Setters
    // ...
}