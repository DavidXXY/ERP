package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.procurement.domain.ProcurementOrderDocument;
import com.company.ops.api.modules.procurement.domain.ProcurementShipment;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.dto.ConfirmShipmentRequest;
import com.company.ops.api.modules.procurement.dto.OrderDocumentResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementShipmentResponse;
import com.company.ops.api.modules.procurement.repository.ProcurementOrderDocumentRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementShipmentRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 采购订单合同附件与发货记录管理。
 * 从 ProcurementService 拆分而来。
 */
@Service
public class ProcurementOrderDocumentService {

  private static final FileStorageService.FilePolicy ORDER_DOCUMENT_POLICY = new FileStorageService.FilePolicy(
      20L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"),
      "采购合同附件不能超过20MB",
      "仅支持图片、PDF、Word 和 Excel 文件",
      true);

  private final PurchaseOrderRepository orderRepository;
  private final ProcurementOrderDocumentRepository orderDocumentRepository;
  private final ProcurementShipmentRepository shipmentRepository;
  private final SupplierRepository supplierRepository;
  private final FileStorageService storage;
  private final SupplierPortalNotifier portalNotifier;

  public ProcurementOrderDocumentService(
      PurchaseOrderRepository orderRepository,
      ProcurementOrderDocumentRepository orderDocumentRepository,
      ProcurementShipmentRepository shipmentRepository,
      SupplierRepository supplierRepository,
      FileStorageService storage,
      SupplierPortalNotifier portalNotifier) {
    this.orderRepository = orderRepository;
    this.orderDocumentRepository = orderDocumentRepository;
    this.shipmentRepository = shipmentRepository;
    this.supplierRepository = supplierRepository;
    this.storage = storage;
    this.portalNotifier = portalNotifier;
  }

  @Transactional
  public OrderDocumentResponse uploadOrderDocument(UUID orderId, MultipartFile file, String docType) {
    PurchaseOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    FileStorageService.StoredFile stored = null;
    try {
      String checksum = sha256(file);
      stored = storage.store(file, "procurement-orders", ORDER_DOCUMENT_POLICY);
      ProcurementOrderDocument document = new ProcurementOrderDocument();
      document.setOrderId(order.getId());
      document.setFileName(stored.originalName());
      document.setObjectKey(stored.objectKey());
      document.setContentType(stored.contentType());
      document.setSizeBytes(stored.sizeBytes());
      document.setSha256(checksum);
      document.setDocType(normalizeDocType(docType));
      document.setUploadedBy(currentName());
      document.setUploadedAt(OffsetDateTime.now());
      OrderDocumentResponse saved = toOrderDocumentResponse(
          orderDocumentRepository.save(document), order);
      portalNotifier.notify(order.getSupplierId(), "ORDER_DOCUMENT",
          "采购订单合同附件已上传",
          "采购订单 " + order.getCode() + " 已上传合同附件，可在供应商门户查看下载。",
          "ORDER", order.getId());
      return saved;
    } catch (RuntimeException exception) {
      if (stored != null) {
        storage.delete(stored.relativePath());
      }
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<OrderDocumentResponse> listOrderDocuments(UUID orderId) {
    orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    return orderDocumentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
        .map(document -> toOrderDocumentResponse(document, null))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProcurementShipmentResponse> listOrderShipments(UUID orderId) {
    orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    Supplier supplier = orderRepository.findById(orderId)
        .map(PurchaseOrder::getSupplierId)
        .flatMap(id -> supplierRepository.findById(id)).orElse(null);
    return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
        .map(item -> toShipmentResponse(item, supplier)).toList();
  }

  @Transactional
  public ProcurementShipmentResponse confirmShipment(
      UUID orderId,
      UUID shipmentId,
      ConfirmShipmentRequest request
  ) {
    ProcurementShipment shipment = shipmentRepository.findById(shipmentId)
        .orElseThrow(() -> new BusinessException("发货记录不存在"));
    if (!shipment.getOrderId().equals(orderId)) {
      throw new BusinessException("发货记录不属于该采购订单");
    }
    if (!"PENDING".equals(shipment.getStatus())) {
      throw new BusinessException("该发货记录已处理，不能重复确认");
    }
    PurchaseOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    String comment = request.comment() == null ? null : request.comment().trim();
    shipment.setStatus(request.action());
    shipment.setReviewComment(comment);
    shipment.setReviewedBy(currentName());
    shipment.setReviewedAt(OffsetDateTime.now());
    ProcurementShipment saved = shipmentRepository.save(shipment);
    String deliveryNo = saved.getDeliveryNo() == null ? "—" : saved.getDeliveryNo();
    if ("CONFIRMED".equals(request.action())) {
      portalNotifier.notify(order.getSupplierId(), "SHIPMENT",
          "发货已确认到货",
          "采购订单 " + order.getCode() + " 的送货单 " + deliveryNo + " 已确认到货，感谢配合。",
          "SHIPMENT", orderId);
    } else {
      portalNotifier.notify(order.getSupplierId(), "SHIPMENT",
          "发货信息被退回",
          "采购订单 " + order.getCode() + " 的送货单 " + deliveryNo + " 未确认"
              + (comment == null || comment.isBlank() ? "。" : "，原因：" + comment),
          "SHIPMENT", orderId);
    }
    return toShipmentResponse(saved, supplierRepository.findById(order.getSupplierId()).orElse(null));
  }

  private ProcurementShipmentResponse toShipmentResponse(
      ProcurementShipment item,
      Supplier supplier
  ) {
    return new ProcurementShipmentResponse(
        item.getId(), item.getOrderId(), null, item.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        item.getDeliveryNo(), item.getCarrier(), item.getExpectedArrival(),
        item.getRemark(), item.getStatus(), item.getCreatedBy(), item.getCreatedAt(),
        item.getReviewComment(), item.getReviewedBy(), item.getReviewedAt());
  }

  @Transactional(readOnly = true)
  public Resource loadOrderDocument(UUID id) {
    ProcurementOrderDocument document = requireOrderDocument(id);
    return storage.loadInNamespace("procurement-orders", document.getObjectKey());
  }

  @Transactional
  public void deleteOrderDocument(UUID id) {
    ProcurementOrderDocument document = requireOrderDocument(id);
    orderDocumentRepository.delete(document);
    storage.deleteInNamespace("procurement-orders", document.getObjectKey());
  }

  private ProcurementOrderDocument requireOrderDocument(UUID id) {
    return orderDocumentRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购合同附件不存在"));
  }

  private OrderDocumentResponse toOrderDocumentResponse(
      ProcurementOrderDocument document,
      PurchaseOrder order
  ) {
    return new OrderDocumentResponse(
        document.getId(),
        document.getOrderId(),
        order == null ? null : order.getCode(),
        document.getFileName(),
        document.getContentType(),
        document.getSizeBytes(),
        document.getDocType(),
        document.getUploadedBy(),
        document.getUploadedAt()
    );
  }

  private static String normalizeDocType(String docType) {
    if (docType == null || docType.isBlank()) {
      return "OTHER";
    }
    String normalized = docType.trim().toUpperCase();
    if (normalized.equals("ORIGINAL") || normalized.equals("STAMPED")) {
      return normalized;
    }
    return "OTHER";
  }

  private static String sha256(MultipartFile file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(file.getBytes()));
    } catch (NoSuchAlgorithmException | IOException exception) {
      throw new BusinessException("合同附件校验失败");
    }
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

}
