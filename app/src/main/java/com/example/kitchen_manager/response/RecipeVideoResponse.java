package com.example.kitchen_manager.response;

public class RecipeVideoResponse {
    private int id;
    private int recipeId;
    private String platform;
    private String title;
    private String videoUrl;
    private String coverUrl;
    private int playCount;
    private String duration;
    private int score;
    private String createdAt;

    // Getter 方法
    public int getId() {
        return id;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getTitle() {
        return title;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getPlayCount() {
        return playCount;
    }

    public String getDuration() {
        return duration;
    }

    public int getScore() {
        return score;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Setter 方法
    public void setId(int id) {
        this.id = id;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RecipeVideoResponse{" +
                "id=" + id +
                ", recipeId=" + recipeId +
                ", platform='" + platform + '\'' +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", playCount=" + playCount +
                ", duration='" + duration + '\'' +
                ", score=" + score +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}