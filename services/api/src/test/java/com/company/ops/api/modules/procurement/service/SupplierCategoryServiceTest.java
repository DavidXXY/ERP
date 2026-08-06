package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.SupplierCategory;
import com.company.ops.api.modules.procurement.dto.SupplierCategoryRequest;
import com.company.ops.api.modules.procurement.repository.SupplierCategoryRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierCategoryServiceTest {

  @Mock private SupplierCategoryRepository categories;
  @Mock private SupplierRepository suppliers;
  @InjectMocks private SupplierCategoryService service;

  @Test
  void createsTrimmedCustomCategory() {
    when(categories.findByNameIgnoreCase("工业气体")).thenReturn(Optional.empty());
    when(categories.save(any(SupplierCategory.class))).thenAnswer(invocation -> {
      SupplierCategory category = invocation.getArgument(0);
      category.setId(UUID.randomUUID());
      return category;
    });

    var result = service.create(new SupplierCategoryRequest(
        "  工业气体  ", "  氧气、氮气等生产用气体  ", 150, true));

    assertThat(result.name()).isEqualTo("工业气体");
    assertThat(result.description()).isEqualTo("氧气、氮气等生产用气体");
    assertThat(result.sortOrder()).isEqualTo(150);
    assertThat(result.enabled()).isTrue();
    assertThat(result.builtIn()).isFalse();
  }

  @Test
  void usedCustomCategoryCannotBeRenamed() {
    UUID id = UUID.randomUUID();
    SupplierCategory category = category(id, "旧类别", false, true);
    when(categories.findById(id)).thenReturn(Optional.of(category));
    when(suppliers.countByCategoryIgnoreCase("旧类别")).thenReturn(2L);

    assertThatThrownBy(() -> service.update(
        id, new SupplierCategoryRequest("新类别", null, 100, true)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已被供应商使用");
  }

  @Test
  void usedCategoryCanBeDisabledWithoutRenaming() {
    UUID id = UUID.randomUUID();
    SupplierCategory category = category(id, "维修维保", true, true);
    when(categories.findById(id)).thenReturn(Optional.of(category));
    when(categories.save(category)).thenReturn(category);
    when(suppliers.countByCategoryIgnoreCase("维修维保")).thenReturn(3L);

    var result = service.update(
        id, new SupplierCategoryRequest("维修维保", "设备维修服务", 70, false));

    assertThat(result.enabled()).isFalse();
    assertThat(result.supplierCount()).isEqualTo(3L);
  }

  private SupplierCategory category(
      UUID id,
      String name,
      boolean builtIn,
      boolean enabled
  ) {
    SupplierCategory category = new SupplierCategory();
    category.setId(id);
    category.setName(name);
    category.setBuiltIn(builtIn);
    category.setEnabled(enabled);
    category.setSortOrder(100);
    return category;
  }
}
