package com.company.ops.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvUtilsTest {
  @ParameterizedTest
  @ValueSource(strings = {"=1+1", "+cmd", "-2+3", "@SUM(A1:A2)", "\t=1", "\r=1"})
  void neutralizesSpreadsheetFormulaInput(String value) {
    assertThat(CsvUtils.cell(value)).isEqualTo("\"'" + value + "\"");
  }
}
