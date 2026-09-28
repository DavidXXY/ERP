package com.company.ops.api.modules.inventory.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryBatch;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.InventorySerialNumber;
import com.company.ops.api.modules.inventory.dto.BatchSerialDtos.*;
import com.company.ops.api.modules.inventory.repository.InventoryBatchRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.InventorySerialNumberRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchSerialService {

  private final InventoryBatchRepository batchRepository;
  private final InventorySerialNumberRepository serialRepository;
  private final InventoryPartRepository partRepository;

  public BatchSerialService(InventoryBatchRepository batchRepository,
      InventorySerialNumberRepository serialRepository,
      InventoryPartRepository partRepository) {
    this.batchRepository = batchRepository;
    this.serialRepository = serialRepository;
    this.partRepository = partRepository;
  }

  @Transactional(readOnly = true)
  public List<BatchResponse> listBatches(UUID partId) {
    return batchRepository.findByPartIdOrderByReceivedDateAscCreatedAtAsc(partId).stream()
        .map(this::toBatchResponse).toList();
  }

  @Transactional
  public BatchResponse createBatch(CreateBatchRequest request) {
    InventoryPart part = partRepository.findById(request.partId())
        .orElseThrow(() -> new BusinessException("物料不存在"));
    if (batchRepository.findByPartIdAndBatchNo(request.partId(), request.batchNo()).isPresent()) {
      throw new BusinessException("该物料批次号已存在");
    }
    InventoryBatch batch = new InventoryBatch();
    batch.setPartId(part.getId());
    batch.setBatchNo(request.batchNo());
    batch.setQuantity(request.quantity());
    batch.setUnitCost(request.unitCost());
    batch.setReceivedDate(request.receivedDate());
    batch.setExpiryDate(request.expiryDate());
    batch.setRemark(request.remark());
    return toBatchResponse(batchRepository.save(batch));
  }

  @Transactional
  public BatchResponse consumeBatch(UUID id, ConsumeBatchRequest request) {
    InventoryBatch batch = batchRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("批次不存在"));
    if (!"ACTIVE".equals(batch.getStatus())) throw new BusinessException("批次已停用，不能出库");
    if (request.quantity().compareTo(batch.getQuantity()) > 0) {
      throw new BusinessException("批次出库数量超过剩余数量" + batch.getQuantity());
    }
    batch.setQuantity(batch.getQuantity().subtract(request.quantity()));
    if (batch.getQuantity().signum() == 0) batch.setStatus("DEPLETED");
    return toBatchResponse(batchRepository.save(batch));
  }

  @Transactional(readOnly = true)
  public List<SerialResponse> listSerials(UUID partId) {
    return serialRepository.findByPartIdOrderBySerialNoAsc(partId).stream()
        .map(this::toSerialResponse).toList();
  }

  @Transactional
  public List<SerialResponse> registerSerials(RegisterSerialRequest request) {
    InventoryPart part = partRepository.findById(request.partId())
        .orElseThrow(() -> new BusinessException("物料不存在"));
    List<InventorySerialNumber> created = request.serialNos().stream().map(no -> {
      if (serialRepository.existsByPartIdAndSerialNo(request.partId(), no)) {
        throw new BusinessException("序列号已存在：" + no);
      }
      InventorySerialNumber serial = new InventorySerialNumber();
      serial.setPartId(part.getId());
      serial.setSerialNo(no);
      serial.setBatchNo(request.batchNo());
      serial.setInboundSource(request.inboundSource());
      return serial;
    }).toList();
    return serialRepository.saveAll(created).stream().map(this::toSerialResponse).toList();
  }

  @Transactional
  public SerialResponse issueSerial(UUID id, SerialActionRequest request) {
    InventorySerialNumber serial = serialRepository.findById(id)
        .orElseThrow(() -> new BusinessException("序列号不存在"));
    if (!"IN_STOCK".equals(serial.getStatus())) throw new BusinessException("序列号不在库，不能出库");
    serial.setStatus("ISSUED");
    serial.setOutboundSource(request.sourceNo());
    return toSerialResponse(serialRepository.save(serial));
  }

  @Transactional
  public SerialResponse returnSerial(UUID id) {
    InventorySerialNumber serial = serialRepository.findById(id)
        .orElseThrow(() -> new BusinessException("序列号不存在"));
    if (!"ISSUED".equals(serial.getStatus())) throw new BusinessException("序列号未出库，不能退库");
    serial.setStatus("IN_STOCK");
    serial.setOutboundSource(null);
    return toSerialResponse(serialRepository.save(serial));
  }

  private BatchResponse toBatchResponse(InventoryBatch b) {
    InventoryPart part = partRepository.findById(b.getPartId()).orElse(null);
    return new BatchResponse(b.getId(), b.getPartId(), part == null ? null : part.getName(), b.getBatchNo(),
        amount(b.getQuantity()), b.getUnitCost(), b.getReceivedDate(), b.getExpiryDate(), b.getStatus(), b.getRemark());
  }

  private SerialResponse toSerialResponse(InventorySerialNumber s) {
    InventoryPart part = partRepository.findById(s.getPartId()).orElse(null);
    return new SerialResponse(s.getId(), s.getPartId(), part == null ? null : part.getName(), s.getSerialNo(),
        s.getBatchNo(), s.getStatus(), s.getInboundSource(), s.getOutboundSource());
  }
}