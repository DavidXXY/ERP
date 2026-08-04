package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TotpServiceTest {
  private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

  @Test
  void verifiesRfc6238CompatibleSixDigitCode() {
    TotpService service = new TotpService(
        new SecureRandom(), Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC), "ERP");

    assertThat(service.generateCode(RFC_SECRET, 1)).isEqualTo("287082");
    assertThat(service.verify(RFC_SECRET, "287082")).isTrue();
    assertThat(service.verify(RFC_SECRET, "000000")).isFalse();
  }

  @Test
  void provisioningUriEncodesIssuerAndAccount() {
    TotpService service = new TotpService(
        new SecureRandom(), Clock.systemUTC(), "Engineering Ops ERP");

    assertThat(service.provisioningUri("user@example.com", RFC_SECRET))
        .isEqualTo("otpauth://totp/Engineering%20Ops%20ERP%3Auser%40example.com"
            + "?secret=" + RFC_SECRET
            + "&issuer=Engineering%20Ops%20ERP&algorithm=SHA1&digits=6&period=30");
  }
}
