package com.company.ops.api.modules.hr.service;

import com.company.ops.api.modules.hr.domain.EmployeeEducation;
import com.company.ops.api.modules.hr.repository.EmployeeEducationRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import java.io.ByteArrayOutputStream;
import com.company.ops.api.modules.hr.domain.LeaveBalance;
import com.company.ops.api.modules.hr.repository.LeaveBalanceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HrExportImportService {

  private static final Logger log = LoggerFactory.getLogger(HrExportImportService.class);

  private final QualificationEmployeeRepository employeeRepository;
  private final EmployeeEducationRepository educationRepository;
  private final LeaveBalanceRepository leaveBalanceRepository;

  public HrExportImportService(
      QualificationEmployeeRepository employeeRepository,
      EmployeeEducationRepository educationRepository,
      LeaveBalanceRepository leaveBalanceRepository) {
    this.employeeRepository = employeeRepository;
    this.educationRepository = educationRepository;
    this.leaveBalanceRepository = leaveBalanceRepository;
  }

  @Transactional(readOnly = true)
  public Resource exportEmployeesExcel() {
    List<QualificationEmployee> employees = employeeRepository.findAll();

    try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
      Sheet sheet = wb.createSheet("员工数据");
      sheet.setColumnWidth(0, 8 * 256);
      sheet.setColumnWidth(1, 16 * 256);
      sheet.setColumnWidth(2, 12 * 256);
      sheet.setColumnWidth(3, 12 * 256);
      sheet.setColumnWidth(4, 16 * 256);
      sheet.setColumnWidth(5, 16 * 256);
      sheet.setColumnWidth(6, 12 * 256);
      sheet.setColumnWidth(7, 20 * 256);
      sheet.setColumnWidth(8, 12 * 256);
      sheet.setColumnWidth(9, 12 * 256);
      sheet.setColumnWidth(10, 12 * 256);

      // Header
      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 11);
      headerStyle.setFont(headerFont);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);

      String[] headers = {
          "序号", "姓名", "手机号", "身份证", "性别", "学历", "职位",
          "所属部门", "入职日期", "在职状态", "邮箱"
      };
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        var cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Data rows
      int rowNum = 1;
      for (QualificationEmployee emp : employees) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(rowNum - 1);
        row.createCell(1).setCellValue(emp.getName() != null ? emp.getName() : "");
        row.createCell(2).setCellValue(emp.getPhone() != null ? emp.getPhone() : "");
        row.createCell(3).setCellValue(emp.getIdCard() != null ? emp.getIdCard() : "");
        row.createCell(4).setCellValue("");
        String highestDegree = educationRepository.findByEmployeeIdOrderByStartDateDesc(emp.getId())
            .stream().filter(EmployeeEducation::isHighest)
            .map(EmployeeEducation::getDegree)
            .findFirst().orElse("");
        row.createCell(5).setCellValue(highestDegree);
        row.createCell(6).setCellValue(emp.getPosition() != null ? emp.getPosition() : "");
        row.createCell(7).setCellValue(
            emp.getOrganization() != null && emp.getOrganization().getName() != null
                ? emp.getOrganization().getName() : "");
        row.createCell(8).setCellValue(
            emp.getEntryDate() != null ? emp.getEntryDate().toString() : "");
        String statusLabel = switch (emp.getEmploymentStatus()) {
          case "ACTIVE" -> "在职"; case "LEFT" -> "离职"; case "DISABLED" -> "停用";
          default -> emp.getEmploymentStatus() != null ? emp.getEmploymentStatus() : "";
        };
        row.createCell(9).setCellValue(statusLabel);
        row.createCell(10).setCellValue("");
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return new ByteArrayResource(bos.toByteArray());
    } catch (Exception e) {
      log.error("Failed to export HR employees Excel", e);
      throw new RuntimeException("导出员工数据失败", e);
    }
  }

  public Resource downloadImportTemplate() {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("导入模板");
      sheet.setColumnWidth(0, 16 * 256);
      sheet.setColumnWidth(1, 12 * 256);
      sheet.setColumnWidth(2, 20 * 256);
      sheet.setColumnWidth(3, 16 * 256);
      sheet.setColumnWidth(4, 12 * 256);
      sheet.setColumnWidth(5, 12 * 256);
      sheet.setColumnWidth(6, 16 * 256);
      sheet.setColumnWidth(7, 16 * 256);

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      String[] headers = {
          "姓名*", "手机号", "身份证", "性别", "学历",
          "职位", "入职日期(yyyy-MM-dd)", "邮箱"
      };
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        var cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Hint row
      Row hintRow = sheet.createRow(1);
      hintRow.createCell(0).setCellValue("(必填)");

      // Example data
      Row exampleRow = sheet.createRow(2);
      exampleRow.createCell(0).setCellValue("张三");
      exampleRow.createCell(1).setCellValue("13800138000");
      exampleRow.createCell(2).setCellValue("110101199001011234");
      exampleRow.createCell(3).setCellValue("男");
      exampleRow.createCell(4).setCellValue("本科");
      exampleRow.createCell(5).setCellValue("工程师");
      exampleRow.createCell(6).setCellValue("2025-01-01");
      exampleRow.createCell(7).setCellValue("zhangsan@example.com");

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return new ByteArrayResource(bos.toByteArray());
    } catch (Exception e) {
      log.error("Failed to create HR import template", e);
      throw new RuntimeException("下载导入模板失败", e);
    }
  }

  @Transactional(readOnly = true)
  public Resource exportLeaveBalanceTemplate() {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("请假额度导入");
      sheet.setColumnWidth(0, 18 * 256);
      sheet.setColumnWidth(1, 14 * 256);
      sheet.setColumnWidth(2, 14 * 256);
      sheet.setColumnWidth(3, 10 * 256);
      sheet.setColumnWidth(4, 14 * 256);
      sheet.setColumnWidth(5, 14 * 256);

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      String[] headers = {"员工姓名", "工号", "假期类型", "年度", "总额度(天)", "已使用(天)"};
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        var cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }
      Row hint = sheet.createRow(1);
      hint.createCell(0).setCellValue("(请填写现有员工, 假期类型: 年假/病假/事假/调休/婚假/产假)");

      Row ex1 = sheet.createRow(2);
      ex1.createCell(0).setCellValue("张三(示例)"); ex1.createCell(1).setCellValue("EMP001");
      ex1.createCell(2).setCellValue("年假"); ex1.createCell(3).setCellValue(2026);
      ex1.createCell(4).setCellValue(15.0); ex1.createCell(5).setCellValue(0);

      Row ex2 = sheet.createRow(3);
      ex2.createCell(0).setCellValue("李四(示例)"); ex2.createCell(1).setCellValue("EMP002");
      ex2.createCell(2).setCellValue("病假"); ex2.createCell(3).setCellValue(2026);
      ex2.createCell(4).setCellValue(12.0); ex2.createCell(5).setCellValue(2.5);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return new ByteArrayResource(bos.toByteArray());
    } catch (Exception e) {
      log.error("Failed to create leave balance template", e);
      throw new RuntimeException("下载请假额度导入模板失败", e);
    }
  }

  @Transactional
  public ImportResult importLeaveBalances(MultipartFile file, String operatorName) {
    List<String> errors = new ArrayList<>();
    int success = 0;
    int fail = 0;

    try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = wb.getSheetAt(0);
      if (sheet == null) return new ImportResult(success, fail, errors);
      int rows = sheet.getLastRowNum();

      for (int i = 1; i <= rows; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        try {
          String empName = getCellString(row, 0);
          String empCode = getCellString(row, 1);
          String leaveTypeLabel = getCellString(row, 2);
          String yearStr = getCellString(row, 3);
          String totalStr = getCellString(row, 4);
          String usedStr = getCellString(row, 5);

          // Find employee
          String fCode = empCode;
          String fName = empName;
          QualificationEmployee emp = null;
          if (fCode != null && !fCode.isBlank()) {
            String finalCode = fCode.trim();
            emp = employeeRepository.findAll().stream()
                .filter(e -> finalCode.equalsIgnoreCase(e.getWorkNo() != null ? e.getWorkNo().trim() : ""))
                .findFirst().orElse(null);
          }
          if (emp == null && fName != null && !fName.isBlank()) {
            String finalName = fName.trim();
            emp = employeeRepository.findAll().stream()
                .filter(e -> finalName.equalsIgnoreCase(e.getName() != null ? e.getName().trim() : ""))
                .findFirst().orElse(null);
          }
          if (emp == null) {
            fail++; errors.add("第" + (i+1) + "行: 员工不存在");
            continue;
          }

          // Map leave type
          String[] validTypes = {"ANNUAL","SICK","PERSONAL","COMPENSATORY","MARRIAGE","MATERNITY"};
          String[] validLabels = {"年假","病假","事假","调休","婚假","产假"};
          String leaveType = null;
          String label = leaveTypeLabel != null ? leaveTypeLabel.trim() : "";
          for (int j = 0; j < validLabels.length; j++) {
            if (validLabels[j].equals(label)) { leaveType = validTypes[j]; break; }
          }
          if (leaveType == null) {
            fail++; errors.add("第" + (i+1) + "行: 无效假期类型");
            continue;
          }

          int year = yearStr != null ? Integer.parseInt(yearStr.trim()) : java.time.Year.now().getValue();
          double totalDays = totalStr != null ? Double.parseDouble(totalStr.trim()) : 0;
          double usedDays = usedStr != null ? Double.parseDouble(usedStr.trim()) : 0;

          var existing = leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(emp.getId(), leaveType, year);
          LeaveBalance bal;
          if (existing.isPresent()) {
            bal = existing.get();
          } else {
            bal = new LeaveBalance();
            bal.setEmployee(emp);
            bal.setLeaveType(leaveType);
            bal.setYear(year);
          }
          bal.setTotalDays(totalDays);
          bal.setUsedDays(usedDays);
          leaveBalanceRepository.save(bal);
          success++;
        } catch (Exception e) {
          fail++;
          errors.add("第" + (i+1) + "行: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Leave balance import failed", e);
      throw new RuntimeException("导入失败: " + e.getMessage());
    }
    return new ImportResult(success, fail, errors);
  }

  @Transactional
  public ImportResult importEmployeesExcel(MultipartFile file, String operatorName) {
    List<String> errors = new ArrayList<>();
    int success = 0;
    int fail = 0;

    try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = wb.getSheetAt(0);
      int rows = sheet.getLastRowNum();

      for (int i = 1; i <= rows; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        try {
          String name = getCellString(row, 0);
          if (name == null || name.isBlank()) {
            fail++;
            errors.add("第" + (i + 1) + "行: 姓名为必填项");
            continue;
          }

          QualificationEmployee emp = new QualificationEmployee();
          emp.setName(name.trim());
          emp.setPhone(getCellString(row, 1));
          emp.setIdCard(getCellString(row, 2));
          // Gender field not available on entity
          emp.setPosition(getCellString(row, 5));
          // Email field not available on entity
          emp.setEmploymentStatus("ACTIVE");

          // Parse entry date if provided
          String entryDateStr = getCellString(row, 6);
          if (entryDateStr != null && !entryDateStr.isBlank()) {
            try {
              emp.setEntryDate(java.time.LocalDate.parse(entryDateStr.trim()));
            } catch (Exception e) {
              // Skip invalid date
            }
          }

          employeeRepository.save(emp);
          success++;
        } catch (Exception e) {
          fail++;
          errors.add("第" + (i + 1) + "行: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("HR import failed", e);
      throw new RuntimeException("导入失败: " + e.getMessage());
    }

    return new ImportResult(success, fail, errors);
  }

  private String getCellString(Row row, int idx) {
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
