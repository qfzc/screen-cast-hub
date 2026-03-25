package com.opencast.screencast.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("code")
    private Integer code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return code != null && (code == 0 || code == 200);
    }
}
