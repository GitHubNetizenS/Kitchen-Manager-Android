package com.example.kitchen_manager.response;

import com.google.gson.annotations.SerializedName;

public class DoubaoApiResponse {
    @SerializedName("choices")
    private Choice[] choices;

    @SerializedName("error")
    private Error error;

    public String getContent() {
        if (choices != null && choices.length > 0) {
            return choices[0].message.content;
        }
        return null;
    }

    public String getErrorMessage() {
        if (error != null) {
            return error.message;
        }
        return null;
    }

    private static class Choice {
        @SerializedName("message")
        Message message;
    }

    private static class Message {
        @SerializedName("content")
        String content;
    }

    private static class Error {
        @SerializedName("message")
        String message;
    }
}