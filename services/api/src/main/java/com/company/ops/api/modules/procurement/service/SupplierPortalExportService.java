package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 供应商门户文档导出（报价单/采购订单 PDF 与 Excel）。
 * 从 SupplierPortalService 拆分而来，只负责导出，不参与业务写操作。
 */
@Service
public class SupplierPortalExportService {

  private volatile BaseFont pdfCjkFont;

  private final SupplierPortalAccountRepository accounts;
  private final SupplierRepository suppliers;
  private final ProcurementInquiryRepository inquiries;
  private final PurchaseRequestRepository purchaseRequests;
  private final SupplierQuotationRepository quotes;
  private final SupplierQuotationLineRepository quoteLines;
  private final ProcurementContractRepository contracts;
  private final PurchaseOrderRepository orders;
  private final ProcurementShipmentRepository shipments;
  private final GoodsReceiptRepository receipts;

  public SupplierPortalExportService(
      SupplierPortalAccountRepository accounts,
      SupplierRepository suppliers,
      ProcurementInquiryRepository inquiries,
      PurchaseRequestRepository purchaseRequests,
      SupplierQuotationRepository quotes,
      SupplierQuotationLineRepository quoteLines,
      ProcurementContractRepository contracts,
      PurchaseOrderRepository orders,
      ProcurementShipmentRepository shipments,
      GoodsReceiptRepository receipts) {
    this.accounts = accounts;
    this.suppliers = suppliers;
    this.inquiries = inquiries;
    this.purchaseRequests = purchaseRequests;
    this.quotes = quotes;
    this.quoteLines = quoteLines;
    this.contracts = contracts;
    this.orders = orders;
    this.shipments = shipments;
    this.receipts = receipts;
  }

  @Transactional(readOnly = true)
  public byte[] exportQuotePdf(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价不存在"));
    Supplier supplier = suppliers.findById(principal.supplierId()).orElse(null);
    List<SupplierQuotationLine> lines = quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId());
    return buildQuotePdf(inquiry, quote, lines, supplier);
  }

  @Transactional(readOnly = true)
  public byte[] exportOrderPdf(SupplierPortalPrincipal principal, UUID orderId) {
    requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权导出该订单");
    }
    ProcurementContract contract = order.getContractId() == null
        ? null : contracts.findById(order.getContractId()).orElse(null);
    Supplier supplier = suppliers.findById(principal.supplierId()).orElse(null);
    return buildOrderPdf(order, contract, supplier);
  }

  private byte[] buildQuotePdf(
      ProcurementInquiry inquiry,
      SupplierQuotation quote,
      List<SupplierQuotationLine> lines,
      Supplier supplier
  ) {
    Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(doc, out);
      doc.open();
      Font title = pdfFont(18, Font.BOLD);
      Font head = pdfFont(12, Font.NORMAL);
      Font small = pdfFont(10, Font.NORMAL);
      Map<UUID, PurchaseRequest> requestMap = purchaseRequests.findAllById(
          lines.stream().map(SupplierQuotationLine::getRequestId).toList()
      ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item, (a, b) -> a));
      Paragraph p = new Paragraph("报价单 " + inquiry.getCode(), title);
      p.setAlignment(Element.ALIGN_CENTER);
      doc.add(p);
      doc.add(new Paragraph("供应商：" + (supplier == null ? "" : supplier.getName()), head));
      doc.add(new Paragraph("询价主题：" + inquiry.getTitle(), head));
      doc.add(new Paragraph("报价有效期：" + (quote.getValidUntil() == null ? "—" : quote.getValidUntil()), head));
      doc.add(new Paragraph("币种：" + quote.getCurrency() + "　提交时间："
          + (quote.getSubmittedAt() == null ? "—" : quote.getSubmittedAt().toLocalDate()), head));
      doc.add(new Paragraph(" "));
      PdfPTable table = new PdfPTable(7);
      table.setWidthPercentage(100);
      table.setWidths(new float[]{1f, 2.4f, 1.2f, 1.4f, 1f, 1.6f, 2f});
      String[] headers = {"序号", "物料", "数量", "含税单价", "税率", "交付日期", "备注"};
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, head));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
      }
      int index = 1;
      for (SupplierQuotationLine line : lines) {
        PurchaseRequest source = requestMap.get(line.getRequestId());
        table.addCell(new Phrase(String.valueOf(index++), small));
        table.addCell(new Phrase(source == null ? String.valueOf(line.getRequestId())
            : source.getPartName() == null ? source.getCode() : source.getPartName(), small));
        table.addCell(new Phrase(plain(line.getQuantity()), small));
        table.addCell(new Phrase(plain(line.getUnitPrice()), small));
        table.addCell(new Phrase(plain(line.getTaxRate()) + "%", small));
        table.addCell(new Phrase(line.getDeliveryDate() == null ? "—" : line.getDeliveryDate().toString(), small));
        table.addCell(new Phrase(line.getRemark() == null ? "" : line.getRemark(), small));
      }
      doc.add(table);
      doc.add(new Paragraph(" "));
      BigDecimal material = lines.stream()
          .map(l -> value(l.getQuantity()).multiply(value(l.getUnitPrice())))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      doc.add(new Paragraph("物料金额：" + plain(material), head));
      doc.add(new Paragraph("运费：" + plain(quote.getFreightAmount()), head));
      doc.add(new Paragraph("其他费用：" + plain(quote.getOtherCostAmount()), head));
      doc.add(new Paragraph("报价总额：" + plain(material.add(value(quote.getFreightAmount())).add(value(quote.getOtherCostAmount()))), title));
      if (isNotBlank(quote.getRemark())) {
        doc.add(new Paragraph("报价说明：" + quote.getRemark(), small));
      }
      doc.close();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成报价 PDF 失败");
    }
    return out.toByteArray();
  }

  private byte[] buildOrderPdf(
      PurchaseOrder order,
      ProcurementContract contract,
      Supplier supplier
  ) {
    Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(doc, out);
      doc.open();
      Font title = pdfFont(18, Font.BOLD);
      Font head = pdfFont(12, Font.NORMAL);
      Paragraph p = new Paragraph("采购订单 " + order.getCode(), title);
      p.setAlignment(Element.ALIGN_CENTER);
      doc.add(p);
      doc.add(new Paragraph("供应商：" + (supplier == null ? "" : supplier.getName()), head));
      doc.add(new Paragraph("物料：" + order.getPartName(), head));
      doc.add(new Paragraph("订购数量：" + plain(order.getOrderedQty())
          + "　单价：" + plain(order.getUnitPrice())
          + "　税率：" + plain(order.getTaxRate()) + "%", head));
      doc.add(new Paragraph("订单总额：" + plain(order.getOrderAmount()), title));
      doc.add(new Paragraph("预计交货：" + (order.getExpectedDeliveryDate() == null
          ? "未设置" : order.getExpectedDeliveryDate()), head));
      if (contract != null) {
        doc.add(new Paragraph("合同编号：" + contract.getContractNo() + "　合同名称：" + contract.getName(), head));
        doc.add(new Paragraph("付款条款：" + (contract.getPaymentTerms() == null ? "—" : contract.getPaymentTerms()), head));
      }
      if (isNotBlank(order.getSourceReason())) {
        doc.add(new Paragraph("采购说明：" + order.getSourceReason(), head));
      }
      doc.close();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成订单 PDF 失败");
    }
    return out.toByteArray();
  }

  @Transactional(readOnly = true)
  public byte[] exportQuoteExcel(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价不存在"));
    List<SupplierQuotationLine> lines = quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId());
    Map<UUID, PurchaseRequest> requestMap = purchaseRequests.findAllById(
        lines.stream().map(SupplierQuotationLine::getRequestId).toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item, (a, b) -> a));
    BigDecimal material = lines.stream()
        .map(l -> value(l.getQuantity()).multiply(value(l.getUnitPrice())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal total = material
        .add(value(quote.getFreightAmount()))
        .add(value(quote.getOtherCostAmount()));
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("报价单");
      String[] headers = {"询价单", "主题", "供应商", "币种", "报价有效期", "提交时间", "总额"};
      headerRow(sheet, headers);
      Row meta = sheet.createRow(1);
      cell(meta, 0, inquiry.getCode());
      cell(meta, 1, inquiry.getTitle());
      cell(meta, 2, suppliers.findById(principal.supplierId()).map(Supplier::getName).orElse(""));
      cell(meta, 3, quote.getCurrency());
      cell(meta, 4, quote.getValidUntil());
      cell(meta, 5, quote.getSubmittedAt());
      cell(meta, 6, total);
      Sheet linesSheet = workbook.createSheet("分项报价");
      String[] lineHeaders = {"序号", "物料", "数量", "含税单价", "税率%", "交付日期", "备注"};
      headerRow(linesSheet, lineHeaders);
      int rowIndex = 1;
      int index = 1;
      for (SupplierQuotationLine line : lines) {
        PurchaseRequest source = requestMap.get(line.getRequestId());
        Row row = linesSheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, index++);
        cell(row, col++, source == null ? String.valueOf(line.getRequestId())
            : source.getPartName() == null ? source.getCode() : source.getPartName());
        cell(row, col++, line.getQuantity());
        cell(row, col++, line.getUnitPrice());
        cell(row, col++, line.getTaxRate());
        cell(row, col++, line.getDeliveryDate());
        cell(row, col++, line.getRemark());
      }
      autoSize(linesSheet, lineHeaders.length);
      return toBytes(workbook);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成报价 Excel 失败");
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportOrderExcel(SupplierPortalPrincipal principal, UUID orderId) {
    requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权导出该订单");
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("采购订单");
      String[] headers = {"订单号", "物料", "数量", "已收", "单价", "税率%", "总额", "预计交货", "状态", "创建时间"};
      headerRow(sheet, headers);
      Row row = sheet.createRow(1);
      int col = 0;
      cell(row, col++, order.getCode());
      cell(row, col++, order.getPartName());
      cell(row, col++, order.getOrderedQty());
      cell(row, col++, order.getReceivedQty());
      cell(row, col++, order.getUnitPrice());
      cell(row, col++, order.getTaxRate());
      cell(row, col++, order.getOrderAmount());
      cell(row, col++, order.getExpectedDeliveryDate());
      cell(row, col++, order.getStatus() == null ? "" : order.getStatus().name());
      cell(row, col++, order.getCreatedAt());
      Sheet records = workbook.createSheet("收货与发货");
      String[] recordHeaders = {"类型", "订单号", "单号", "送货单号/数量", "承运方/收货人", "时间", "说明"};
      headerRow(records, recordHeaders);
      int rowIndex = 1;
      for (ProcurementShipment shipment : shipments.findByOrderIdOrderByCreatedAtDesc(orderId)) {
        Row record = records.createRow(rowIndex++);
        int c = 0;
        cell(record, c++, "发货");
        cell(record, c++, order.getCode());
        cell(record, c++, "");
        cell(record, c++, shipment.getDeliveryNo());
        cell(record, c++, shipment.getCarrier());
        cell(record, c++, shipment.getCreatedAt());
        cell(record, c++, shipment.getRemark());
      }
      for (GoodsReceipt receipt : receipts.findByOrderId(orderId)) {
        Row record = records.createRow(rowIndex++);
        int c = 0;
        cell(record, c++, "收货");
        cell(record, c++, order.getCode());
        cell(record, c++, receipt.getCode());
        cell(record, c++, receipt.getQuantity());
        cell(record, c++, receipt.getReceiverName());
        cell(record, c++, receipt.getReceivedDate());
        cell(record, c++, receipt.getInspectionStatus());
      }
      autoSize(records, recordHeaders.length);
      return toBytes(workbook);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成订单 Excel 失败");
    }
  }

  private void headerRow(Sheet sheet, String[] headers) {
    CellStyle style = sheet.getWorkbook().createCellStyle();
    org.apache.poi.ss.usermodel.Font font = sheet.getWorkbook().createFont();
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
      cell.setCellValue(date.toString());
    } else if (value instanceof OffsetDateTime dateTime) {
      cell.setCellValue(dateTime.toLocalDateTime().toString().replace("T", " "));
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private byte[] toBytes(Workbook workbook) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out.toByteArray();
  }

  private BaseFont pdfFont() {
    BaseFont current = pdfCjkFont;
    if (current != null) return current;
    synchronized (this) {
      if (pdfCjkFont != null) return pdfCjkFont;
      // 优先使用服务器本机中文字体（可嵌入 PDF）；macOS 与常见 Linux 发行版路径都尝试。
      String[] candidates = {
          "/System/Library/Fonts/STHeiti Light.ttc,0",
          "/System/Library/Fonts/PingFang.ttc,0",
          "/System/Library/Fonts/Hiragino Sans GB.ttc,0",
          "/System/Library/Fonts/Supplemental/Songti.ttc,0",
          "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
          "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc",
          "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
          "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
          "/usr/share/fonts/truetype/arphic/uming.ttc",
      };
      for (String candidate : candidates) {
        try {
          pdfCjkFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
          return pdfCjkFont;
        } catch (Exception ignored) {
          // 尝试下一个候选字体
        }
      }
      // openpdf 自带 Adobe 中文（简体）字体包，不依赖系统字体，可保证任何平台都能导出。
      try {
        pdfCjkFont = BaseFont.createFont(
            "STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        return pdfCjkFont;
      } catch (Exception fallbackFailure) {
        throw new BusinessException("服务器缺少中文字体，无法导出 PDF");
      }
    }
  }

  private Font pdfFont(float size, int style) {
    return new Font(pdfFont(), size, style);
  }

  private static String plain(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  private Supplier requireActiveSupplier(SupplierPortalPrincipal principal) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (!"ACTIVE".equals(account.getStatus())) throw new BusinessException("门户账号仍在等待内部审核");
    if (account.isMustChangePassword()) throw new BusinessException("请先修改临时密码");
    Supplier supplier = requireSupplier(principal.supplierId());
    if (!"APPROVED".equals(supplier.getAdmissionStatus())) throw new BusinessException("供应商尚未通过准入审批");
    if (supplier.getRiskStatus() == SupplierRiskStatus.BLOCKED) throw new BusinessException("供应商已冻结，不能报价");
    return supplier;
  }

  private SupplierPortalAccount requireAccount(UUID id) {
    return accounts.findById(id).orElseThrow(() -> new BusinessException("供应商门户账号不存在"));
  }

  private Supplier requireSupplier(UUID id) {
    return suppliers.findById(id).orElseThrow(() -> new BusinessException("供应商不存在"));
  }

  private static boolean isBlank(String value) { return value == null || value.isBlank(); }
  private static boolean isNotBlank(String value) { return !isBlank(value); }
  private static BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
