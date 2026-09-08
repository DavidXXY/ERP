package com.company.ops.api.modules.reporting;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.reporting.service.ReportHoursService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportHoursServiceTest {

  @Test
  void rejectsBelowHalfDay() {
    assertThatThrownBy(() -> ReportHoursService.validateHours(BigDecimal.valueOf(3), UUID.randomUUID(), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("半日");
  }

  @Test
  void acceptsHalfAndFullDay() {
    assertThatCode(() -> ReportHoursService.validateHours(BigDecimal.valueOf(4), UUID.randomUUID(), null))
        .doesNotThrowAnyException();
    assertThatCode(() -> ReportHoursService.validateHours(BigDecimal.valueOf(8), UUID.randomUUID(), null))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsOverFullDay() {
    assertThatThrownBy(() -> ReportHoursService.validateHours(BigDecimal.valueOf(9), UUID.randomUUID(), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("全日");
  }

  @Test
  void additionalProjectOnlyForHalfDay() {
    UUID project = UUID.randomUUID();
    assertThatThrownBy(() -> ReportHoursService.validateHours(BigDecimal.valueOf(8), project, UUID.randomUUID()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("第二个项目");
  }

  @Test
  void additionalProjectCannotEqualMain() {
    UUID project = UUID.randomUUID();
    assertThatThrownBy(() -> ReportHoursService.validateHours(BigDecimal.valueOf(4), project, project))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("相同");
  }
}
