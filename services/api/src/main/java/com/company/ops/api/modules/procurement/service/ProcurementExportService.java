package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 采购列表 Excel 导出。 */
@Service
public class ProcurementExportService {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final PurchaseRequestRepository requests;
  private final PurchaseOrderRepository orders;
  private final SupplierRepository suppliers;
  private final ProcurementControlService controlService;

  public ProcurementExportService(
      PurchaseRequestRepository requests,
      PurchaseOrderRepository orders,
      SupplierRepository suppliers,
      ProcurementControlService controlService
  ) {
    this.requests = requests;
    this.orders = orders;
    this.suppliers = suppliers;
    this.controlService = controlService;
  }

  @Transactional(readOnly = true)
  public byte[] exportRequests() {
    List<PurchaseRequest> rows = requests.findAllByOrderByCreatedAtDesc();
    String[] headers = {
        "申请编码", "批次", "申请人", "物料", "数量", "单价", "税率%", "金额",
        "期望到货日", "成本归属", "审批级别", "状态", "审批状态", "创建时间"};
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("采购申请");
      header(sheet, headers);
      int rowIndex = 1;
      for (PurchaseRequest item : rows) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, item.getCode());
        cell(row, col++, item.getBatchName());
        cell(row, col++, item.getRequesterName());
        cell(row, col++, item.getPartName());
        cell(row, col++, item.getQuantity());
        cell(row, col++, item.getUnitPrice());
        cell(row, col++, item.getTaxRate());
        cell(row, col++, item.getTotalAmount());
        cell(row, col++, item.getExpectedDate());
        cell(row, col++, item.getCostTargetName());
        cell(row, col++, item.getApprovalLevel() == null ? "" : item.getApprovalLevel());
        cell(row, col++, statusLabel(item.getStatus().name()));
        cell(row, col++, statusLabel(item.getApprovalStatus().name()));
        cell(row, col++, item.getCreatedAt());
      }
      autoSize(sheet, headers.length);
      return toBytes(workbook);
    } catch (Exception ex) {
      throw new IllegalStateException("导出采购申请失败", ex);
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportInquiries() {
    List<Map<String, Object>> rows = controlService.listInquiries();
    String[] headers = {
        "询价单", "标题", "申请人/负责采购", "状态", "截止时间", "报价数",
        "中标供应商", "定标金额", "创建时间"};
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("询价管理");
      header(sheet, headers);
      int rowIndex = 1;
      for (Map<String, Object> item : rows) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, str(item.get("code")));
        cell(row, col++, str(item.get("title")));
        cell(row, col++, str(item.get("createdByName")));
        cell(row, col++, statusLabel(str(item.get("status"))));
        cell(row, col++, str(item.get("deadline")));
        cell(row, col++, item.get("quoteCount"));
        cell(row, col++, str(item.get("winnerSupplierName")));
        cell(row, col++, item.get("awardAmount"));
        cell(row, col++, str(item.get("createdAt")));
      }
      autoSize(sheet, headers.length);
      return toBytes(workbook);
    } catch (Exception ex) {
      throw new IllegalStateException("导出询价单失败", ex);
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportOrders() {
    List<PurchaseOrder> rows = orders.findAllByOrderByCreatedAtDesc();
    String[] headers = {
        "订单编码", "供应商", "物料", "数量", "已到货", "单价", "税率%", "订单金额",
        "期望交期", "负责人", "订单版本", "状态", "审批状态", "创建时间"};
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("采购订单");
      header(sheet, headers);
      int rowIndex = 1;
      for (PurchaseOrder item : rows) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, item.getCode());
        cell(row, col++, item.getSupplierId() == null ? "" : findSupplierName(item.getSupplierId()));
        cell(row, col++, item.getPartName());
        cell(row, col++, item.getOrderedQty());
        cell(row, col++, item.getReceivedQty());
        cell(row, col++, item.getUnitPrice());
        cell(row, col++, item.getTaxRate());
        cell(row, col++, item.getOrderAmount());
        cell(row, col++, item.getExpectedDeliveryDate());
        cell(row, col++, item.getResponsibleName());
        cell(row, col++, item.getOrderVersion() == null ? 1 : item.getOrderVersion());
        cell(row, col++, statusLabel(item.getStatus().name()));
        cell(row, col++, statusLabel(item.getApprovalStatus().name()));
        cell(row, col++, item.getCreatedAt());
      }
      autoSize(sheet, headers.length);
      return toBytes(workbook);
    } catch (Exception ex) {
      throw new IllegalStateException("导出采购订单失败", ex);
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportSuppliers() {
    List<Supplier> rows = suppliers.findAllByOrderByCreatedAtDesc();
    String[] headers = {
        "供应商编码", "供应商名称", "类别", "联系人", "电话", "统一社会信用代码",
        "准入状态", "资质有效期至", "风险状态", "创建时间"};
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("供应商");
      header(sheet, headers);
      int rowIndex = 1;
      for (Supplier item : rows) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, item.getCode());
        cell(row, col++, item.getName());
        cell(row, col++, item.getCategory());
        cell(row, col++, item.getContactName());
        cell(row, col++, item.getPhone());
        cell(row, col++, item.getUnifiedSocialCreditCode());
        cell(row, col++, item.getAdmissionStatus());
        cell(row, col++, item.getQualificationValidTo() == null ? item.getLicenseValidTo() : item.getQualificationValidTo());
        cell(row, col++, item.getRiskStatus() == null ? "" : item.getRiskStatus().name());
        cell(row, col++, item.getCreatedAt());
      }
      autoSize(sheet, headers.length);
      return toBytes(workbook);
    } catch (Exception ex) {
      throw new IllegalStateException("导出供应商失败", ex);
    }
  }

  private String findSupplierName(java.util.UUID id) {
    return suppliers.findById(id).map(Supplier::getName).orElse("");
  }

  private void header(Sheet sheet, String[] headers) {
    CellStyle style = sheet.getWorkbook().createCellStyle();
    Font font = sheet.getWorkbook().createFont();
    font.setBold(true);
    style.setFont(font);
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(style);
    }
  }

  private void autoSize(Sheet sheet, int columns) {
    for (int i = 0; i < columns; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private void cell(Row row, int col, Object value) {
    Cell cell = row.createCell(col);
    if (value == null) {
      cell.setCellValue("");
    } else if (value instanceof BigDecimal number) {
      cell.setCellValue(number.doubleValue());
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else if (value instanceof LocalDate date) {
      cell.setCellValue(date.format(DATE));
    } else if (value instanceof OffsetDateTime dateTime) {
      cell.setCellValue(dateTime.format(DATE_TIME));
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private String str(Object value) {
    return value == null ? "" : value.toString();
  }

  private String statusLabel(String raw) {
    if (raw == null) {
      return "";
    }
    return switch (raw) {
      case "DRAFT" -> "草稿";
      case "SUBMITTED" -> "已提交";
      case "APPROVED" -> "已审批";
      case "ORDERED" -> "已下单";
      case "PARTIAL_RECEIVED" -> "部分到货";
      case "RECEIVED" -> "已到货";
      case "CANCELLED" -> "已取消";
      case "CLOSED" -> "已关闭";
      case "PENDING" -> "待审批";
      case "REJECTED" -> "已驳回";
      case "AWARDED" -> "已定标";
      case "PUBLISHED" -> "询价中";
      case "DRAFTING" -> "草稿";
      case "ACTIVE" -> "生效";
      case "PENDING_REVIEW" -> "待审核";
      case "NORMAL" -> "正常";
      case "WATCHLIST" -> "关注";
      case "BLOCKED" -> "停用";
      default -> raw;
    };
  }

  private byte[] toBytes(Workbook workbook) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out.toByteArray();
  }
}
