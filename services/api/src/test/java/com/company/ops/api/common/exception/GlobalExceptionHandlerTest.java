package com.company.ops.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void unsupportedMethodReturnsMethodNotAllowedWithAllowHeader() {
    var response = handler.handleMethodNotSupported(
        new HttpRequestMethodNotSupportedException("GET", List.of("POST")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.POST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().message()).isEqualTo("请求方法不支持");
  }
}
