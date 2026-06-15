package com.ocean.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommonResp<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private T content;

    public CommonResp() {
    }

    public CommonResp(boolean success, String message, T content) {
        this.success = success;
        this.message = message;
        this.content = content;
    }

    public static <T> CommonResp<T> ok() {
        return new CommonResp<>(true, "操作成功", null);
    }

    public static <T> CommonResp<T> ok(String message) {
        return new CommonResp<>(true, message, null);
    }

    public static <T> CommonResp<T> ok(T content) {
        return new CommonResp<>(true, "查询成功", content);
    }

    public static <T> CommonResp<T> ok(String message, T content) {
        return new CommonResp<>(true, message, content);
    }

    public static <T> CommonResp<T> fail(String message) {
        return new CommonResp<>(false, message, null);
    }

    public static <T> CommonResp<T> fail(String message, T content) {
        return new CommonResp<>(false, message, content);
    }
}
