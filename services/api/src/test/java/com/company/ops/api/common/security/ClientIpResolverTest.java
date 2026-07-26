package com.company.ops.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
  private final ClientIpResolver resolver = new ClientIpResolver("127.0.0.0/8,::1/128");

  @Test
  void ignoresForwardedHeadersFromUntrustedPeers() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.10");
    request.addHeader("X-Forwarded-For", "198.51.100.7");
    request.addHeader("X-Real-IP", "198.51.100.8");

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
  }

  @Test
  void usesLastForwardedAddressFromLocalProxy() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.2");

    assertThat(resolver.resolve(request)).isEqualTo("10.0.0.2");
  }

  @Test
  void rejectsMalformedForwardedAddress() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("::1");
    request.addHeader("X-Forwarded-For", "injected-value");
    request.addHeader("X-Real-IP", "192.0.2.4");

    assertThat(resolver.resolve(request)).isEqualTo("192.0.2.4");
  }

  @Test
  void removesTrustedProxyHopsFromRightSideOfChain() {
    var trustedNetworkResolver = new ClientIpResolver("127.0.0.0/8,10.0.0.0/8");
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.7, 10.0.0.2");

    assertThat(trustedNetworkResolver.resolve(request)).isEqualTo("198.51.100.7");
  }
}
