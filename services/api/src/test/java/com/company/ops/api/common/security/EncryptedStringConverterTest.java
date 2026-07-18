package com.company.ops.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {
  @Test
  void encryptsWithRandomIvAndDecryptsLosslessly() {
    var converter = new EncryptedStringConverter("test-encryption-key-with-at-least-32-characters");
    String first = converter.convertToDatabaseColumn("310101199001011234");
    String second = converter.convertToDatabaseColumn("310101199001011234");
    assertThat(first).startsWith("ENC1:").isNotEqualTo(second);
    assertThat(converter.convertToEntityAttribute(first)).isEqualTo("310101199001011234");
  }

  @Test
  void rejectsWeakKeysAndReadsLegacyPlaintext() {
    assertThatThrownBy(() -> new EncryptedStringConverter("weak"))
        .isInstanceOf(IllegalArgumentException.class);
    var converter = new EncryptedStringConverter("test-encryption-key-with-at-least-32-characters");
    assertThat(converter.convertToEntityAttribute("legacy-value")).isEqualTo("legacy-value");
  }
}
