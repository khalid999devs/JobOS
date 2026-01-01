package com.jobos.shared.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ApiResponse<T> {
    private boolean success;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T result) {
        this.success = success;
        this.result = result;
    }

    public ApiResponse(boolean success, T result, String message) {
        this.success = success;
        this.result = result;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(true, result);
    }

    public static <T> ApiResponse<T> success(T result, String message) {
        return new ApiResponse<>(true, result, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static <T> ApiResponse<T> error(T result, String message) {
        return new ApiResponse<>(false, result, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
