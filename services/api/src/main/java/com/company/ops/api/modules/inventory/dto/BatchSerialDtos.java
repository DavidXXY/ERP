package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BatchSerialDtos {
  private BatchSerialDtos() {}

  public record CreateBatchRequest(
      @NotNull UUID partId,
      @NotBlank @Size(max = 64) String batchNo,
      @NotNull @DecimalMin("0.01") BigDecimal quantity,
      @NotNull @DecimalMin("0") BigDecimal unitCost,
      LocalDate receivedDate,
      LocalDate expiryDate,
      @Size(max = 300) String remark
  ) {}

  public record BatchResponse(
      UUID id, UUID partId, String partName, String batchNo, BigDecimal quantity, BigDecimal unitCost,
      LocalDate receivedDate, LocalDate expiryDate, String status, String remark
  ) {}

  public record ConsumeBatchRequest(
      @NotNull @DecimalMin("0.01") BigDecimal quantity,
      @Size(max = 64) String sourceNo
  ) {}

  public record RegisterSerialRequest(
      @NotNull UUID partId,
      @Size(max = 64) String batchNo,
      @NotEmpty List<@NotBlank @Size(max = 64) String> serialNos,
      @Size(max = 64) String inboundSource
  ) {}

  public record SerialResponse(
      UUID id, UUID partId, String partName, String serialNo, String batchNo, String status,
      String inboundSource, String outboundSource
  ) {}

  public record SerialActionRequest(
      @Size(max = 64) String sourceNo
  ) {}
}