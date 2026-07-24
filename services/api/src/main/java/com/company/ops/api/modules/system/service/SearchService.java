package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import java.util.ArrayList;
import java.util.List;
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

  public SearchService(CustomerRepository customerRepo, ServiceContractRepository contractRepo,
      InventoryPartRepository partRepo, ProjectRepository projectRepo, QualificationEmployeeRepository empRepo) {
    this.customerRepo = customerRepo; this.contractRepo = contractRepo; this.partRepo = partRepo;
    this.projectRepo = projectRepo; this.empRepo = empRepo;
  }

  @Transactional(readOnly = true)
  public List<SearchResult> search(String q) {
    if (q == null || q.trim().isEmpty()) return List.of();
    String keyword = q.trim();
    var limit = PageRequest.of(0, 10);
    List<SearchResult> results = new ArrayList<>();

    customerRepo.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, limit).forEach(c -> {
        results.add(new SearchResult("customer", c.getId().toString(),
            (c.getCode() != null ? c.getCode() : "") + " - " + c.getName(),
            c.getLevel() != null ? c.getLevel().name() : "", "/crm/customers/" + c.getId()));
    });

    contractRepo.findByCodeContainingIgnoreCaseOrProjectNameContainingIgnoreCase(keyword, keyword, limit).forEach(c -> {
        results.add(new SearchResult("contract", c.getId().toString(),
            (c.getCode() != null ? c.getCode() : "") + " - " + c.getProjectName(),
            "", "/crm/contracts/" + c.getId()));
    });

    projectRepo.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword, limit).forEach(p -> {
        results.add(new SearchResult("project", p.getId().toString(),
            (p.getCode() != null ? p.getCode() : "") + " - " + p.getName(),
            p.getManagerName() != null ? p.getManagerName() : "", "/projects/list?id=" + p.getId()));
    });

    partRepo.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword, limit).forEach(p -> {
        results.add(new SearchResult("part", p.getId().toString(),
            (p.getCode() != null ? p.getCode() : "") + " - " + p.getName(),
            String.valueOf(p.getStockQty()), "/inventory/parts?id=" + p.getId()));
    });

    empRepo.findByNameContainingIgnoreCase(keyword, limit).forEach(e -> {
        results.add(new SearchResult("employee", e.getId().toString(), e.getName(),
            e.getPosition() != null ? e.getPosition() : "", "/hr/employees/" + e.getId()));
    });

    return results;
  }

  public record SearchResult(String type, String id, String title, String subtitle, String url) {}
}
