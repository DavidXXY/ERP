package com.company.ops.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {
  private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

  @Test
  void preservesValidRequestId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(RequestCorrelationFilter.HEADER, "erp-request-42");
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {};

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("erp-request-42");
  }

  @Test
  void replacesUnsafeRequestId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(RequestCorrelationFilter.HEADER, "bad request id\n");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).matches("[0-9a-f-]{36}");
  }
}
