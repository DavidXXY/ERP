package com.company.ops.api.common.api;

public record ApiResponse<T>(boolean success, String message, T data) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "ok", data);
  }

  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, "ok", null);
  }

  public static ApiResponse<Void> fail(String message) {
    return new ApiResponse<>(false, message, null);
  }
}

