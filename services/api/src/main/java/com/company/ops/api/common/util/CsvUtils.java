package com.company.ops.api.common.util;

import java.util.Objects;

public final class CsvUtils {
  private CsvUtils() {}

  public static String cell(Object value) {
    String text = Objects.toString(value, "");
    if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) text = "'" + text;
    return '"' + text.replace("\"", "\"\"") + '"';
  }
}
