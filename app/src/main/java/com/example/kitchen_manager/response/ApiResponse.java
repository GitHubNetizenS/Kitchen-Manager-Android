package com.example.kitchen_manager.response;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // 无参构造函数（Gson 反序列化需要）
    public ApiResponse() {
    }

    // 带参构造函数
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters and setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // 实现 isSuccess() 方法
    public boolean isSuccess() {
        // 通常 code 200 表示成功，但可以根据实际 API 设计调整
        return code == 200 || code == 0; // 0 是常见自定义成功码
    }

}