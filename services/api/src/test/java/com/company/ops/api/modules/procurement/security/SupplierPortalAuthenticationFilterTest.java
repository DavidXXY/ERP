package com.company.ops.api.modules.procurement.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.system.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class SupplierPortalAuthenticationFilterTest {
  private final JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
  private final SupplierPortalAccountRepository accounts =
      org.mockito.Mockito.mock(SupplierPortalAccountRepository.class);
  private final SupplierPortalAuthenticationFilter filter =
      new SupplierPortalAuthenticationFilter(jwtService, accounts);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void downstreamRuntimeFailureDoesNotRunChainTwice() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/supplier-portal/me");
    request.addHeader("Authorization", "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
    when(jwtService.extractTenant("token")).thenReturn("default");
    when(jwtService.isSupplierPortalToken("token")).thenReturn(false);
    doThrow(new IllegalStateException("downstream failure")).when(chain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilter(request, response, chain))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("downstream failure");
    verify(chain).doFilter(request, response);
  }
}
