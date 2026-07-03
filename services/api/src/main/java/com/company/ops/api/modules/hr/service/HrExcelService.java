package com.company.ops.api.modules.hr.service;

import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HrExcelService {

    private final QualificationEmployeeRepository employeeRepository;
    private final PersonnelCertificateRepository certificateRepository;

    public HrExcelService(QualificationEmployeeRepository employeeRepository,
                           PersonnelCertificateRepository certificateRepository) {
        this.employeeRepository = employeeRepository;
        this.certificateRepository = certificateRepository;
    }

    // ====== Export ======

    public byte[] exportEmployees() throws IOException {
        var employees = employeeRepository.findAllByOrderByNameAsc();
        try (var wb = new SXSSFWorkbook()) {
            // Styles
            var headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            var boldFont = wb.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            // Employee sheet
            var empSheet = wb.createSheet("员工信息");
            var empHeaders = new String[]{"姓名", "工号", "身份证号", "手机号", "组织", "岗位",
                "入职日期", "状态", "合同起始", "合同截止", "社保单位", "备注"};
            var empRow = empSheet.createRow(0);
            for (int i = 0; i < empHeaders.length; i++) {
                var cell = empRow.createCell(i);
                cell.setCellValue(empHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            int idx = 1;
            for (var e : employees) {
                var row = empSheet.createRow(idx++);
                row.createCell(0).setCellValue(e.getName() != null ? e.getName() : "");
                row.createCell(1).setCellValue(e.getWorkNo() != null ? e.getWorkNo() : "");
                row.createCell(2).setCellValue(e.getIdCard() != null ? e.getIdCard() : "");
                row.createCell(3).setCellValue(e.getPhone() != null ? e.getPhone() : "");
                row.createCell(4).setCellValue(e.getOrganization() != null ? e.getOrganization().getName() : "");
                row.createCell(5).setCellValue(e.getPosition() != null ? e.getPosition() : "");
                row.createCell(6).setCellValue(e.getEntryDate() != null ? e.getEntryDate().toString() : "");
                row.createCell(7).setCellValue(employmentLabel(e.getEmploymentStatus()));
                row.createCell(8).setCellValue(e.getContractStart() != null ? e.getContractStart().toString() : "");
                row.createCell(9).setCellValue(e.getContractEnd() != null ? e.getContractEnd().toString() : "");
                row.createCell(10).setCellValue(e.getSocialSecurityUnit() != null ? e.getSocialSecurityUnit() : "");
                row.createCell(11).setCellValue(e.getRemark() != null ? e.getRemark() : "");
            }
            for (int i = 0; i < empHeaders.length; i++) empSheet.autoSizeColumn(i);

            // Certificate sheet
            var certs = certificateRepository.findAllByOrderByEmployeeNameAscNameAsc();
            var certSheet = wb.createSheet("人员证书");
            var certHeaders = new String[]{"员工姓名", "证书名称", "证书编号", "专业/类别", "发证日期", "有效期至", "注册单位", "备注"};
            var certRow = certSheet.createRow(0);
            for (int i = 0; i < certHeaders.length; i++) {
                var cell = certRow.createCell(i);
                cell.setCellValue(certHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            idx = 1;
            for (var c : certs) {
                var row = certSheet.createRow(idx++);
                row.createCell(0).setCellValue(c.getEmployee() != null ? c.getEmployee().getName() : "");
                row.createCell(1).setCellValue(c.getName() != null ? c.getName() : "");
                row.createCell(2).setCellValue(c.getCertificateNo() != null ? c.getCertificateNo() : "");
                row.createCell(3).setCellValue(c.getSpecialty() != null ? c.getSpecialty() : "");
                row.createCell(4).setCellValue(c.getIssueDate() != null ? c.getIssueDate().toString() : "");
                row.createCell(5).setCellValue(c.getValidTo() != null ? c.getValidTo().toString() : "");
                row.createCell(6).setCellValue(c.isCompanyRegistered() ? "是" : "否");
                row.createCell(7).setCellValue(c.getRemark() != null ? c.getRemark() : "");
            }
            for (int i = 0; i < certHeaders.length; i++) certSheet.autoSizeColumn(i);

            var out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] downloadTemplate() throws IOException {
        try (var wb = new SXSSFWorkbook()) {
            var headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var boldFont = wb.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            var sheet = wb.createSheet("导入模板");
            var headers = new String[]{"姓名*", "工号", "身份证号", "手机号", "组织", "岗位", "入职日期", "状态"};
            var row = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                var cell = row.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createRow(1).createCell(0).setCellValue("张三");
            sheet.createRow(1).createCell(4).setCellValue("工程部");
            sheet.createRow(1).createCell(6).setCellValue("2025-01-01");

            var remarkRow = sheet.createRow(3);
            remarkRow.createCell(0).setCellValue("说明：带*为必填。状态可选：在职/离职/停用。日期格式：YYYY-MM-DD。");

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            var out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ====== Import ======
    public record ImportResult(int success, int fail, List<String> errors) {}

    public ImportResult importEmployees(MultipartFile file, String operatorName) throws IOException {
        int success = 0, fail = 0;
        var errors = new ArrayList<String>();
        try (var wb = new XSSFWorkbook(file.getInputStream())) {
            var sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    var name = getCellValue(row.getCell(0));
                    if (name == null || name.isBlank()) { continue; }
                    var emp = new QualificationEmployee();
                    emp.setExternalId(java.util.UUID.randomUUID().toString());
                    emp.setName(name.trim());
                    emp.setWorkNo(getCellValue(row.getCell(1)));
                    emp.setIdCard(getCellValue(row.getCell(2)));
                    emp.setPhone(getCellValue(row.getCell(3)));
                    emp.setDepartment(getCellValue(row.getCell(4)));
                    emp.setPosition(getCellValue(row.getCell(5)));
                    var entryDateStr = getCellValue(row.getCell(6));
                    if (entryDateStr != null && !entryDateStr.isBlank()) {
                        try { emp.setEntryDate(LocalDate.parse(entryDateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE)); } catch (Exception ignored) {}
                    }
                    var statusStr = getCellValue(row.getCell(7));
                    emp.setEmploymentStatus(statusStr != null ? switch (statusStr.trim()) {
                        case "离职" -> "LEFT"; case "停用" -> "DISABLED"; default -> "ACTIVE";
                    } : "ACTIVE");
                    emp.setRemark(getCellValue(row.getCell(8)));
                    employeeRepository.save(emp);
                    success++;
                } catch (Exception e) {
                    fail++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        }
        return new ImportResult(success, fail, errors);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private String employmentLabel(String status) {
        return switch (status) { case "ACTIVE" -> "在职"; case "LEFT" -> "离职"; case "DISABLED" -> "停用"; default -> status; };
    }
}
