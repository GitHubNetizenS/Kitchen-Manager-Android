package com.example.kitchen_manager.api;

import com.example.kitchen_manager.models.*;
import com.example.kitchen_manager.response.*;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {
    String BASE_URL = "http://10.0.2.2:8080/";

    // 用户相关接口
    @FormUrlEncoded
    @POST("api/user")
    Call<ApiResponse<User>> register(
            @Field("action") String action,
            @Field("username") String username,
            @Field("password") String password,
            @Field("phone") String phone
    );

    @FormUrlEncoded
    @POST("api/user")
    Call<ApiResponse<User>> login(
            @Field("action") String action,
            @Field("login_id") String loginId,
            @Field("password") String password
    );

    @Multipart
    @POST("api/upload")
    Call<ApiResponse<String>> uploadAvatar(
            @Part MultipartBody.Part userId,
            @Part MultipartBody.Part file
    );

    @FormUrlEncoded
    @POST("api/user")
    Call<ApiResponse<User>> updateUserProfile(
            @Field("action") String action,
            @Field("user_id") int userId,
            @Field("username") String username,
            @Field("phone") String phone,
            @Field("title") String title,
            @Field("avatar_url") String avatarUrl,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("api/user/profile")
    Call<ApiResponse<User>> getUserProfile(@Field("user_id") int userId);

    // 食材相关接口
    @FormUrlEncoded
    @POST("api/user/ingredients")
    Call<ApiResponse<List<IngredientResponse>>> getUserIngredients(@Field("user_id") int userId);

    @FormUrlEncoded
    @POST("api/addingredient")
    Call<ApiResponse<Void>> addIngredients(
            @Field("user_id") int userId,
            @Field("ingredients") String ingredientsJson
    );

    // 获取菜谱详情
    @GET("api/recipedetail")
    Call<ApiResponse<RecipeDetailResponse>> getRecipeDetail(
            @Query("recipe_id") int recipeId
    );

    // 点击增加菜谱热度
    @FormUrlEncoded
    @POST("api/recipe/popularity")
    Call<ApiResponse<Void>> incrementPopularity(
            @Field("recipe_id") int recipeId
    );

    // 收藏菜谱
    @FormUrlEncoded
    @POST("api/favoriterecipe")
    Call<ApiResponse<Void>> favoriteRecipe(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );

    @FormUrlEncoded
    @POST("api/addhistory")
    Call<ApiResponse<Void>> addUserHistory(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );

    // 修正的API定义：移除@FormUrlEncoded，使用@Query参数
    @GET("api/recipe/ingredients")
    Call<ApiResponse<List<Ingredient>>> getRecipeIngredients(@Query("recipe_id") int recipeId);

    // 删除用户库存中的食材
    @FormUrlEncoded
    @POST("api/user/ingredients/delete")
    Call<ApiResponse<Void>> deleteUserIngredients(
            @Field("user_id") int userId,
            @Field("ingredient_ids") String ingredientIdsJson // 改为字符串类型
    );

    // 获取用户收藏的菜谱（新增）
    @GET("api/favorites")
    Call<ApiResponse<List<RecipeResponse>>> getFavoriteRecipes(
            @Query("user_id") int userId,
            @Query("sort") String sortType // "time"或"match"
    );

    // 取消收藏（新增）
    @FormUrlEncoded
    @POST("api/unfavorite")
    Call<ApiResponse<Void>> unfavoriteRecipe(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );
    @GET("api/favorites/count")
    Call<ApiResponse<Integer>> getFavoriteCount(
            @Query("user_id") int userId
    );

    // 新增: 删除历史记录
    @POST("/api/deletehistory")
    Call<ApiResponse<Void>> deleteHistoryRecord(@Query("history_id") Integer historyId);

    @POST("/api/deletehistory")
    Call<ApiResponse<Void>> deleteHistoryRecipe(@Query("user_id") Integer userId, @Query("recipe_id") Integer recipeId);

    @GET("api/history")
    Call<ApiResponse<List<RecipeResponse>>> getHistoryRecipes(
            @Query("user_id") int userId,
            @Query("sort") String sortType
    );

    // 获取用户历史记录数量
    @GET("api/history/count")
    Call<ApiResponse<Integer>> getHistoryCount(
            @Query("user_id") int userId
    );


    // 获取所有标签
    @GET("api/tags")
    Call<ApiResponse<List<TagResponse>>> getAllTags();

    // 获取用户选择的标签
    @GET("api/user/tags")
    Call<ApiResponse<Map<String, List<Integer>>>> getUserTags(
            @Query("user_id") int userId
    );

    // 保存用户标签
    @FormUrlEncoded
    @POST("api/user/tags")
    Call<ApiResponse<Void>> saveUserTags(
            @Field("user_id") int userId,
            @Field("category") String category,
            @Field("tag_ids") String tagIdsJson
    );
    // 按匹配值获取菜谱
    @GET("api/recipes/match")
    Call<ApiResponse<List<RecipeResponse>>> getRecipesByMatchValue(
            @Query("user_id") int userId
    );
    // 在 ApiService 接口中添加搜索方法
    @GET("api/search")
    Call<ApiResponse<List<RecipeResponse>>> searchRecipes(
            @Query("keyword") String keyword,
            @Query("sort") String sortType,   // "all", "tag_match", "ingredient_match"
            @Query("user_id") int userId
    );

    // 添加删除用户食材关联的接口
    @FormUrlEncoded
    @POST("api/deleteingredient")
    Call<ApiResponse<Void>> deleteUserIngredient(
            @Field("user_id") int userId,
            @Field("ingredient_name") String ingredientName
    );
    // 添加更新食材接口
    @FormUrlEncoded
    @POST("api/updateingredient")
    Call<ApiResponse<Void>> updateIngredient(
            @Field("user_id") int userId,
            @Field("ingredient_name") String ingredientName,
            @Field("category") String category,
            @Field("storage_date") String storageDate,
            @Field("custom_expiry_days") int customExpiryDays);

    @GET("api/recipelist")
    Call<ApiResponse<Map<String, Object>>> getRecipeList(
            @Query("tag_id") int tagId,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("user_id") int userId
    );

    // 购物车相关接口
    @FormUrlEncoded
    @POST("api/cart/add")
    Call<ApiResponse<Void>> addToCart(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );

    @FormUrlEncoded
    @POST("api/cart/remove")
    Call<ApiResponse<Void>> removeFromCart(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );

    // 新增：切换购物车状态接口
    @FormUrlEncoded
    @POST("api/cart/toggle")
    Call<ApiResponse<Void>> toggleCart(
            @Field("user_id") int userId,
            @Field("recipe_id") int recipeId
    );

    @GET("api/cart/check")
    Call<ApiResponse<Boolean>> checkIfInCart(
            @Query("user_id") int userId,
            @Query("recipe_id") int recipeId
    );


}