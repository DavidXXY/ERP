package com.company.ops.api.modules.sales.dto;

import com.company.ops.api.modules.sales.domain.SalesOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SalesDtos {
  private SalesDtos() {}

  public record SalesOrderLineRequest(
      @NotNull UUID partId,
      @NotNull @DecimalMin("0.01") BigDecimal quantity,
      @NotNull @DecimalMin("0") BigDecimal unitPrice
  ) {}

  public record CreateSalesOrderRequest(
      String code,
      @NotNull UUID customerId,
      UUID contractId,
      @NotNull LocalDate orderDate,
      @Size(max = 500) String remark,
      @NotEmpty List<@Valid SalesOrderLineRequest> lines
  ) {}

  public record SalesOrderLineResponse(
      UUID id, UUID partId, String partName, BigDecimal quantity, BigDecimal unitPrice,
      BigDecimal amount, BigDecimal shippedQty, BigDecimal returnedQty
  ) {}

  public record SalesOrderResponse(
      UUID id, String code, UUID customerId, String customerName, UUID contractId,
      LocalDate orderDate, SalesOrderStatus status, BigDecimal totalAmount, String remark,
      List<SalesOrderLineResponse> lines
  ) {}

  public record ShipmentLineRequest(
      @NotNull UUID orderLineId,
      @NotNull @DecimalMin("0.01") BigDecimal quantity
  ) {}

  public record CreateShipmentRequest(
      String code,
      @NotNull UUID orderId,
      @NotNull LocalDate shipmentDate,
      @Size(max = 80) String receiverName,
      @Size(max = 500) String remark,
      @NotEmpty List<@Valid ShipmentLineRequest> lines
  ) {}

  public record ShipmentLineResponse(
      UUID id, UUID orderLineId, UUID partId, String partName, BigDecimal quantity,
      BigDecimal unitCost, BigDecimal amount, BigDecimal returnedQty
  ) {}

  public record ShipmentResponse(
      UUID id, String code, UUID orderId, String orderCode, UUID customerId, UUID contractId,
      LocalDate shipmentDate, String receiverName, BigDecimal totalAmount, String remark,
      List<ShipmentLineResponse> lines
  ) {}

  public record ReturnLineRequest(
      @NotNull UUID shipmentLineId,
      @NotNull @DecimalMin("0.01") BigDecimal quantity
  ) {}

  public record CreateReturnRequest(
      String code,
      @NotNull UUID shipmentId,
      @NotNull LocalDate returnDate,
      @NotBlank @Size(max = 500) String reason,
      @NotEmpty List<@Valid ReturnLineRequest> lines
  ) {}

  public record ReturnLineResponse(
      UUID id, UUID shipmentLineId, UUID partId, String partName, BigDecimal quantity,
      BigDecimal unitCost, BigDecimal amount
  ) {}

  public record ReturnResponse(
      UUID id, String code, UUID shipmentId, UUID orderId, UUID customerId, UUID contractId,
      LocalDate returnDate, String reason, BigDecimal totalAmount, String status,
      List<ReturnLineResponse> lines
  ) {}
}