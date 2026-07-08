package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrmImportService {
  private static final Logger log = LoggerFactory.getLogger(CrmImportService.class);
  private final CustomerRepository customerRepo;
  private final com.company.ops.api.common.service.CodeGenerator codeGen;

  public CrmImportService(CustomerRepository customerRepo, com.company.ops.api.common.service.CodeGenerator codeGen) {
    this.customerRepo = customerRepo; this.codeGen = codeGen;
  }

  @Transactional
  public ImportResult importCustomers(MultipartFile file) {
    List<String> errors = new ArrayList<>();
    int success = 0, fail = 0;
    try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = wb.getSheetAt(0);
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        try {
          String name = getCell(row, 0);
          if (name == null || name.isBlank()) { fail++; continue; }
          Customer c = new Customer();
          c.setCode(codeGen.generate("CUSTOMER"));
          c.setName(name.trim());
          c.setIndustry(getCell(row, 1));
          String level = getCell(row, 2);
          try { c.setLevel(CustomerLevel.valueOf(level != null ? level.toUpperCase() : "NORMAL")); } catch (Exception e) { c.setLevel(CustomerLevel.NORMAL); }
          c.setOwnerName(getCell(row, 3));
          c.setPaymentHabit(getCell(row, 4));
          c.setRiskStatus(RiskStatus.NORMAL);
          customerRepo.save(c); success++;
        } catch (Exception e) { fail++; errors.add("Row " + (i + 1) + ": " + e.getMessage()); }
      }
    } catch (Exception e) { throw new BusinessException("Import failed: " + e.getMessage()); }
    return new ImportResult(success, fail, errors);
  }

  private String getCell(Row row, int idx) {
    var cell = row.getCell(idx);
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
      default -> null;
    };
  }

  public record ImportResult(int success, int fail, List<String> errors) {}
}
