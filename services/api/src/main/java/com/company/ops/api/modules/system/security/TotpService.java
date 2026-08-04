package com.company.ops.api.modules.system.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TotpService {
  private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private static final int SECRET_BYTES = 20;
  private static final long STEP_SECONDS = 30;
  private final SecureRandom random;
  private final Clock clock;
  private final String issuer;

  @Autowired
  public TotpService(@Value("${ops.security.mfa-issuer:Engineering Ops ERP}") String issuer) {
    this(new SecureRandom(), Clock.systemUTC(), issuer);
  }

  TotpService(SecureRandom random, Clock clock, String issuer) {
    this.random = random;
    this.clock = clock;
    this.issuer = issuer;
  }

  public String generateSecret() {
    byte[] bytes = new byte[SECRET_BYTES];
    random.nextBytes(bytes);
    return encodeBase32(bytes);
  }

  public boolean verify(String secret, String suppliedCode) {
    if (secret == null || suppliedCode == null) return false;
    String code = suppliedCode.replaceAll("\\s", "");
    if (!code.matches("\\d{6}")) return false;
    long counter = clock.instant().getEpochSecond() / STEP_SECONDS;
    for (long offset = -1; offset <= 1; offset++) {
      byte[] expected = generateCode(secret, counter + offset).getBytes(StandardCharsets.US_ASCII);
      if (MessageDigest.isEqual(expected, code.getBytes(StandardCharsets.US_ASCII))) return true;
    }
    return false;
  }

  public String provisioningUri(String username, String secret) {
    String account = issuer + ":" + username;
    return "otpauth://totp/" + encode(account)
        + "?secret=" + secret
        + "&issuer=" + encode(issuer)
        + "&algorithm=SHA1&digits=6&period=" + STEP_SECONDS;
  }

  String generateCode(String secret, long counter) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
      byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
      int offset = digest[digest.length - 1] & 0x0f;
      int binary = ((digest[offset] & 0x7f) << 24)
          | ((digest[offset + 1] & 0xff) << 16)
          | ((digest[offset + 2] & 0xff) << 8)
          | (digest[offset + 3] & 0xff);
      return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
    } catch (Exception exception) {
      throw new IllegalStateException("无法生成动态验证码", exception);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String encodeBase32(byte[] input) {
    StringBuilder output = new StringBuilder((input.length * 8 + 4) / 5);
    int buffer = 0;
    int bitsLeft = 0;
    for (byte value : input) {
      buffer = (buffer << 8) | (value & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        output.append(BASE32[(buffer >> (bitsLeft - 5)) & 0x1f]);
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) output.append(BASE32[(buffer << (5 - bitsLeft)) & 0x1f]);
    return output.toString();
  }

  private byte[] decodeBase32(String value) {
    String normalized = value.replace("=", "").replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    byte[] result = new byte[normalized.length() * 5 / 8];
    int buffer = 0;
    int bitsLeft = 0;
    int index = 0;
    for (char character : normalized.toCharArray()) {
      int digit = character >= 'A' && character <= 'Z' ? character - 'A'
          : character >= '2' && character <= '7' ? character - '2' + 26 : -1;
      if (digit < 0) throw new IllegalArgumentException("MFA 密钥格式无效");
      buffer = (buffer << 5) | digit;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
        bitsLeft -= 8;
      }
    }
    return result;
  }
}
