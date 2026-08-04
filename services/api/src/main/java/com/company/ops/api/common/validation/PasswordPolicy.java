package com.company.ops.api.common.validation;

import com.company.ops.api.common.exception.BusinessException;
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
}
