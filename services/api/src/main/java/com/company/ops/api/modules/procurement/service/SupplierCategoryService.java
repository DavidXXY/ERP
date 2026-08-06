package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.SupplierCategory;
import com.company.ops.api.modules.procurement.dto.SupplierCategoryRequest;
import com.company.ops.api.modules.procurement.dto.SupplierCategoryResponse;
import com.company.ops.api.modules.procurement.repository.SupplierCategoryRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierCategoryService {
  private final SupplierCategoryRepository categories;
  private final SupplierRepository suppliers;

  public SupplierCategoryService(
      SupplierCategoryRepository categories,
      SupplierRepository suppliers
  ) {
    this.categories = categories;
    this.suppliers = suppliers;
  }

  @Transactional(readOnly = true)
  public List<SupplierCategoryResponse> list() {
    return categories.findAllByOrderBySortOrderAscNameAsc().stream()
        .map(this::response)
        .toList();
  }

  @Transactional
  public SupplierCategoryResponse create(SupplierCategoryRequest request) {
    String name = request.name().trim();
    if (categories.findByNameIgnoreCase(name).isPresent()) {
      throw new BusinessException("供应商分类已存在");
    }
    SupplierCategory category = new SupplierCategory();
    category.setName(name);
    category.setDescription(trim(request.description()));
    category.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
    category.setEnabled(request.enabled() == null || request.enabled());
    category.setBuiltIn(false);
    return response(categories.save(category));
  }

  @Transactional
  public SupplierCategoryResponse update(UUID id, SupplierCategoryRequest request) {
    SupplierCategory category = categories.findById(id)
        .orElseThrow(() -> new BusinessException("供应商分类不存在"));
    String name = request.name().trim();
    if (!category.getName().equalsIgnoreCase(name)) {
      if (category.isBuiltIn()) throw new BusinessException("内置常用分类不能改名");
      if (suppliers.countByCategoryIgnoreCase(category.getName()) > 0) {
        throw new BusinessException("该分类已被供应商使用，不能改名；可以停用后新增分类");
      }
      if (categories.findByNameIgnoreCase(name).isPresent()) {
        throw new BusinessException("供应商分类已存在");
      }
      category.setName(name);
    }
    category.setDescription(trim(request.description()));
    category.setSortOrder(request.sortOrder() == null ? category.getSortOrder() : request.sortOrder());
    category.setEnabled(request.enabled() == null ? category.isEnabled() : request.enabled());
    return response(categories.save(category));
  }

  private SupplierCategoryResponse response(SupplierCategory category) {
    return new SupplierCategoryResponse(
        category.getId(), category.getName(), category.getDescription(), category.getSortOrder(),
        category.isEnabled(), category.isBuiltIn(), suppliers.countByCategoryIgnoreCase(category.getName())
    );
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
