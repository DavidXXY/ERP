package com.company.ops.api.common.validation;

import com.company.ops.api.common.exception.BusinessException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {
  private static final Set<String> COMMON_PASSWORDS = Set.of(
      "password123!", "admin123456!", "qwerty123456!", "welcome12345!", "changeme123!"
  );

  private PasswordPolicy() {}

  public static boolean hasRequiredStructure(String password) {
    if (password == null || password.length() < 12 || password.length() > 100) return false;
    boolean lower = false;
    boolean upper = false;
    boolean digit = false;
    boolean special = false;
    for (int i = 0; i < password.length(); i++) {
      char value = password.charAt(i);
      if (Character.isWhitespace(value)) return false;
      lower |= Character.isLowerCase(value);
      upper |= Character.isUpperCase(value);
      digit |= Character.isDigit(value);
      special |= !Character.isLetterOrDigit(value);
    }
    return lower && upper && digit && special;
  }

  public static void requireValid(String password, String username) {
    if (!hasRequiredStructure(password)) {
      throw new BusinessException("密码须为12-100位，且包含大小写字母、数字和特殊字符");
    }
    String normalized = password.toLowerCase(Locale.ROOT);
    if (COMMON_PASSWORDS.contains(normalized)) throw new BusinessException("不能使用常见弱密码");
    String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    if (normalizedUsername.length() >= 3 && normalized.contains(normalizedUsername)) {
      throw new BusinessException("密码不能包含用户名");
    }
  }

  public static String generate(String username) {
    final String lower = "abcdefghijklmnopqrstuvwxyz";
    final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    final String digits = "0123456789";
    final String specials = "!@#$%^&*()-_=+";
    final String all = lower + upper + digits + specials;
    SecureRandom random = new SecureRandom();
    String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    for (int attempt = 0; attempt < 10; attempt++) {
      char[] chars = new char[16];
      chars[0] = lower.charAt(random.nextInt(lower.length()));
      chars[1] = upper.charAt(random.nextInt(upper.length()));
      chars[2] = digits.charAt(random.nextInt(digits.length()));
      chars[3] = specials.charAt(random.nextInt(specials.length()));
      for (int i = 4; i < chars.length; i++) {
        chars[i] = all.charAt(random.nextInt(all.length()));
      }
      for (int i = chars.length - 1; i > 0; i--) {
        int j = random.nextInt(i + 1);
        char tmp = chars[i];
        chars[i] = chars[j];
        chars[j] = tmp;
      }
      String candidate = new String(chars);
      String normalized = candidate.toLowerCase(Locale.ROOT);
      if (normalizedUsername.length() >= 3 && normalized.contains(normalizedUsername)) continue;
      if (COMMON_PASSWORDS.contains(normalized)) continue;
      return candidate;
    }
    throw new BusinessException("生成初始密码失败，请重试");
  }
}
