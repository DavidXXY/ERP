package com.company.ops.api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
  @Test
  void acceptsPasswordWithAllRequiredCharacterClasses() {
    assertThat(PasswordPolicy.hasRequiredStructure("Ops-Secure-2026")).isTrue();
  }

  @Test
  void rejectsShortOrIncompletePasswords() {
    assertThat(PasswordPolicy.hasRequiredStructure("Short1!A")).isFalse();
    assertThat(PasswordPolicy.hasRequiredStructure("alllowercase123!")).isFalse();
    assertThat(PasswordPolicy.hasRequiredStructure("No-Space 2026A")).isFalse();
  }

  @Test
  void rejectsPasswordsContainingUsername() {
    assertThatThrownBy(() -> PasswordPolicy.requireValid("Alice-Secure-2026!", "alice"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("密码不能包含用户名");
  }
}
