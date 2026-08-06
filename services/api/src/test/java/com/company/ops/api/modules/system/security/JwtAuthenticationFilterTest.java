package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
  private final JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
  private final SystemUserDetailsService users = org.mockito.Mockito.mock(SystemUserDetailsService.class);
  private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void downstreamRuntimeFailureDoesNotRunChainTwice() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/procurement/inquiries");
    request.addHeader("Authorization", "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
    when(jwtService.extractTenant("token")).thenReturn("default");
    when(jwtService.extractUsername("token")).thenReturn(null);
    doThrow(new IllegalStateException("downstream failure")).when(chain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilter(request, response, chain))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("downstream failure");
    verify(chain).doFilter(request, response);
  }
}
