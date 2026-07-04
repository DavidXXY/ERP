package com.company.ops.api.modules.crm.service;

import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmExportService {

  private static final Logger log = LoggerFactory.getLogger(CrmExportService.class);

  private final CustomerRepository customerRepository;
  private final ServiceContractRepository contractRepository;

  public CrmExportService(CustomerRepository customerRepository, ServiceContractRepository contractRepository) {
    this.customerRepository = customerRepository;
    this.contractRepository = contractRepository;
  }

  @Transactional(readOnly = true)
  public Resource exportCustomersExcel() {
    List<Customer> customers = customerRepository.findAllByOrderByCreatedAtDesc();
    try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
      Sheet sheet = wb.createSheet("客户数据");
      sheet.setColumnWidth(0, 16*256); sheet.setColumnWidth(1, 24*256); sheet.setColumnWidth(2, 16*256);
      sheet.setColumnWidth(3, 12*256); sheet.setColumnWidth(4, 12*256); sheet.setColumnWidth(5, 16*256);
      sheet.setColumnWidth(6, 16*256); sheet.setColumnWidth(7, 20*256);

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont(); headerFont.setBold(true); headerStyle.setFont(headerFont);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);

      String[] headers = {"客户编码", "客户名称", "行业", "客户等级", "负责人", "联系方式", "付款习惯", "风险状态"};
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) { var cell = headerRow.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle); }

      int rowNum = 1;
      for (Customer c : customers) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(c.getCode() != null ? c.getCode() : "");
        row.createCell(1).setCellValue(c.getName() != null ? c.getName() : "");
        row.createCell(2).setCellValue(c.getIndustry() != null ? c.getIndustry() : "");
        row.createCell(3).setCellValue(c.getLevel() != null ? c.getLevel().name() : "");
        row.createCell(4).setCellValue(c.getOwnerName() != null ? c.getOwnerName() : "");
        row.createCell(5).setCellValue("");
        row.createCell(6).setCellValue(c.getPaymentHabit() != null ? c.getPaymentHabit() : "");
        row.createCell(7).setCellValue(c.getRiskStatus() != null ? c.getRiskStatus().name() : "");
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return new ByteArrayResource(bos.toByteArray());
    } catch (Exception e) {
      log.error("Failed to export customers Excel", e);
      throw new RuntimeException("导出客户数据失败", e);
    }
  }

  @Transactional(readOnly = true)
  public Resource exportContractsExcel() {
    List<ServiceContract> contracts = contractRepository.findAllByOrderByEndDateAsc();
    try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
      Sheet sheet = wb.createSheet("合同数据");
      sheet.setColumnWidth(0, 16*256); sheet.setColumnWidth(1, 20*256); sheet.setColumnWidth(2, 24*256);
      sheet.setColumnWidth(3, 14*256); sheet.setColumnWidth(4, 14*256); sheet.setColumnWidth(5, 14*256);
      sheet.setColumnWidth(6, 12*256); sheet.setColumnWidth(7, 12*256);

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont(); headerFont.setBold(true); headerStyle.setFont(headerFont);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);

      String[] headers = {"合同编号", "客户名称", "项目名称", "合同金额", "合同类型", "开始日期", "结束日期", "合同状态"};
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) { var cell = headerRow.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle); }

      int rowNum = 1;
      for (ServiceContract c : contracts) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(c.getCode() != null ? c.getCode() : "");
        row.createCell(1).setCellValue(c.getCustomerId() != null ? c.getCustomerId().toString() : "");
        row.createCell(2).setCellValue(c.getProjectName() != null ? c.getProjectName() : "");
        row.createCell(3).setCellValue(c.getAmount() != null ? c.getAmount().doubleValue() : 0);
        row.createCell(4).setCellValue(c.getContractType() != null ? c.getContractType() : "");
        row.createCell(5).setCellValue(c.getStartDate() != null ? c.getStartDate().toString() : "");
        row.createCell(6).setCellValue(c.getEndDate() != null ? c.getEndDate().toString() : "");
        String statusLabel = switch (c.getStatus()) {
          case ACTIVE -> "履约中"; case RENEWAL_PENDING -> "待续约"; case OVERDUE_RISK -> "履约风险"; case CLOSED -> "已关闭";
          default -> c.getStatus() != null ? c.getStatus().name() : "";
        };
        row.createCell(7).setCellValue(statusLabel);
      }
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return new ByteArrayResource(bos.toByteArray());
    } catch (Exception e) {
      log.error("Failed to export contracts Excel", e);
      throw new RuntimeException("导出合同数据失败", e);
    }
  }
}
