package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {

  private final CustomerRepository customerRepo;
  private final ServiceContractRepository contractRepo;
  private final InventoryPartRepository partRepo;
  private final ProjectRepository projectRepo;
  private final QualificationEmployeeRepository empRepo;
  private final DataScopeService dataScope;

  public SearchService(CustomerRepository customerRepo, ServiceContractRepository contractRepo,
      InventoryPartRepository partRepo, ProjectRepository projectRepo, QualificationEmployeeRepository empRepo,
      DataScopeService dataScope) {
    this.customerRepo = customerRepo; this.contractRepo = contractRepo; this.partRepo = partRepo;
    this.projectRepo = projectRepo; this.empRepo = empRepo; this.dataScope = dataScope;
  }

  @Transactional(readOnly = true)
  public List<SearchResult> search(String q, UserPrincipal principal) {
    if (q == null || q.trim().isEmpty()) return List.of();
    String keyword = q.trim();
    var limit = PageRequest.of(0, 10);
    List<SearchResult> results = new ArrayList<>();
    boolean admin = principal != null && principal.roleCodes().contains("ADMIN");

    if (admin || can(principal, "crm:customer:view")) {
      customerRepo.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, limit).stream()
          .filter(c -> dataScope.canViewOwner(c.getOwnerUserId()))
          .forEach(c -> {
        results.add(new SearchResult("customer", c.getId().toString(),
            (c.getCode() != null ? c.getCode() : "") + " - " + c.getName(),
            customerLevelLabel(c.getLevel()), "/crm/customers/" + c.getId()));
          });
    }

    if (admin || can(principal, "crm:contract:view")) {
      var contracts = contractRepo.findByCodeContainingIgnoreCaseOrProjectNameContainingIgnoreCase(
          keyword, keyword, limit);
      Map<UUID, com.company.ops.api.modules.crm.domain.Customer> customers = new HashMap<>();
      customerRepo.findAllById(contracts.stream().map(c -> c.getCustomerId()).collect(java.util.stream.Collectors.toSet()))
          .forEach(customer -> customers.put(customer.getId(), customer));
      contracts.stream().filter(contract -> {
        var customer = customers.get(contract.getCustomerId());
        return customer != null && dataScope.canViewOwner(customer.getOwnerUserId());
      }).forEach(c -> {
        results.add(new SearchResult("contract", c.getId().toString(),
            (c.getCode() != null ? c.getCode() : "") + " - " + c.getProjectName(),
            "", "/crm/contracts/" + c.getId()));
      });
    }

    if (admin || can(principal, "project:view")) {
      boolean allScope = dataScope.hasAllDataScope();
      Set<UUID> visibleUserIds = dataScope.visibleUserIds();
      Set<String> visibleNames = dataScope.visibleOwnerNames();
      boolean canApprove = dataScope.hasAuthority("project:approve");
      projectRepo.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword, limit).stream()
          .filter(p -> allScope
              || p.getManagerUserId() != null && visibleUserIds.contains(p.getManagerUserId())
              || p.getManagerUserId() == null && visibleNames.contains(p.getManagerName())
              || canApprove && (p.getApprovalStatus() == ProjectApprovalStatus.PENDING
                  || p.getManagerName() == null || p.getManagerName().startsWith("待")))
          .forEach(p -> {
        results.add(new SearchResult("project", p.getId().toString(),
            (p.getCode() != null ? p.getCode() : "") + " - " + p.getName(),
            p.getManagerName() != null ? p.getManagerName() : "", "/projects/list?id=" + p.getId()));
          });
    }

    if (admin || canAny(principal, "inventory:view", "procurement:view")) {
      partRepo.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword, limit).forEach(p -> {
        results.add(new SearchResult("part", p.getId().toString(),
            (p.getCode() != null ? p.getCode() : "") + " - " + p.getName(),
            String.valueOf(p.getStockQty()), "/inventory/parts?id=" + p.getId()));
      });
    }

    if (admin || can(principal, "qualification:employee:view")) {
      boolean allScope = dataScope.hasAllDataScope();
      Set<UUID> visibleOrganizations = allScope ? Set.of() : dataScope.visibleOrganizationIds();
      Set<UUID> visibleUsers = allScope ? Set.of() : dataScope.visibleUserIds();
      empRepo.findByNameContainingIgnoreCase(keyword, limit).stream().filter(e -> allScope
          || e.getOrganization() != null && visibleOrganizations.contains(e.getOrganization().getId())
          || e.getSystemUser() != null && visibleUsers.contains(e.getSystemUser().getId())).forEach(e -> {
        results.add(new SearchResult("employee", e.getId().toString(), e.getName(),
            e.getPosition() != null ? e.getPosition() : "", "/hr/employees/" + e.getId()));
      });
    }

    return results;
  }

  private boolean can(UserPrincipal principal, String permission) {
    return principal != null && principal.permissions().contains(permission);
  }

  private boolean canAny(UserPrincipal principal, String... permissions) {
    return java.util.Arrays.stream(permissions).anyMatch(permission -> can(principal, permission));
  }

  private String customerLevelLabel(CustomerLevel level) {
    if (level == null) return "";
    return switch (level) {
      case STRATEGIC -> "战略客户";
      case KEY -> "重点客户";
      case NORMAL -> "普通客户";
    };
  }

  public record SearchResult(String type, String id, String title, String subtitle, String url) {}
}
