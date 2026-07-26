package com.company.ops.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {
  @Test
  void encryptsWithRandomIvAndDecryptsLosslessly() {
    var converter = new EncryptedStringConverter(
        "primary", "test-encryption-key-with-at-least-32-characters", "");
    String first = converter.convertToDatabaseColumn("310101199001011234");
    String second = converter.convertToDatabaseColumn("310101199001011234");
    assertThat(first).startsWith("ENC2:primary:").isNotEqualTo(second);
    assertThat(converter.convertToEntityAttribute(first)).isEqualTo("310101199001011234");
  }

  @Test
  void rejectsWeakKeysAndReadsLegacyPlaintext() {
    assertThatThrownBy(() -> new EncryptedStringConverter("primary", "weak", ""))
        .isInstanceOf(IllegalArgumentException.class);
    var converter = new EncryptedStringConverter(
        "primary", "test-encryption-key-with-at-least-32-characters", "");
    assertThat(converter.convertToEntityAttribute("legacy-value")).isEqualTo("legacy-value");
  }

  @Test
  void readsPreviousAndLegacyKeysDuringRotation() {
    String oldSecret = "old-test-encryption-key-with-at-least-32-characters";
    var oldConverter = new EncryptedStringConverter("old", oldSecret, "");
    String encrypted = oldConverter.convertToDatabaseColumn("622202199001011234");
    var rotated = new EncryptedStringConverter(
        "primary", "new-test-encryption-key-with-at-least-32-characters", "old=" + oldSecret);

    assertThat(rotated.convertToEntityAttribute(encrypted)).isEqualTo("622202199001011234");
    String legacy = "ENC1:" + encrypted.substring(encrypted.indexOf(':', "ENC2:".length()) + 1);
    assertThat(rotated.convertToEntityAttribute(legacy)).isEqualTo("622202199001011234");
  }
}
