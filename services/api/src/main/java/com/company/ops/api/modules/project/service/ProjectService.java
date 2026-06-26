package com.company.ops.api.modules.project.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final CustomerRepository customerRepository;

  public ProjectService(ProjectRepository projectRepository, CustomerRepository customerRepository) {
    this.projectRepository = projectRepository;
    this.customerRepository = customerRepository;
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> listProjects() {
    List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, String> customerNames = loadCustomerNames(projects);
    return projects.stream()
        .map(project -> toResponse(project, customerNames))
        .toList();
  }

  @Transactional
  public ProjectResponse createProject(CreateProjectRequest request) {
    if (projectRepository.existsByCode(request.code())) {
      throw new BusinessException("项目编码已存在");
    }
    if (request.customerId() != null && !customerRepository.existsById(request.customerId())) {
      throw new BusinessException("客户不存在");
    }

    Project project = new Project();
    project.setCustomerId(request.customerId());
    project.setCode(request.code());
    project.setName(request.name());
    project.setStage(request.stage() == null ? ProjectStage.INITIATED : request.stage());
    project.setBudgetAmount(defaultAmount(request.budgetAmount()));
    project.setActualCost(defaultAmount(request.actualCost()));
    project.setProgress(request.progress() == null ? 0 : request.progress());
    project.setWarrantyEndDate(request.warrantyEndDate());

    Project saved = projectRepository.save(project);
    Map<UUID, String> customerNames = loadCustomerNames(List.of(saved));
    return toResponse(saved, customerNames);
  }

  private Map<UUID, String> loadCustomerNames(List<Project> projects) {
    List<UUID> customerIds = projects.stream()
        .map(Project::getCustomerId)
        .filter(id -> id != null)
        .distinct()
        .toList();
    if (customerIds.isEmpty()) {
      return Map.of();
    }
    return customerRepository.findAllById(customerIds).stream()
        .collect(Collectors.toMap(Customer::getId, Customer::getName, (left, right) -> left));
  }

  private ProjectResponse toResponse(Project project, Map<UUID, String> customerNames) {
    return new ProjectResponse(
        project.getId(),
        project.getCustomerId(),
        project.getCustomerId() == null ? null : customerNames.get(project.getCustomerId()),
        project.getCode(),
        project.getName(),
        project.getStage(),
        project.getBudgetAmount(),
        project.getActualCost(),
        project.getProgress(),
        project.getWarrantyEndDate()
    );
  }

  private BigDecimal defaultAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
