package com.company.ops.api.modules.maintenance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.maintenance.domain.EquipmentAsset;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.*;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备资产（EquipmentAsset）管理。
 * 从 MaintenanceService 拆分而来，只负责设备的查询、新增与更新。
 */
@Service
public class MaintenanceEquipmentService {

  private final EquipmentAssetRepository equipmentRepository;
  private final CustomerRepository customerRepository;
  private final WorkOrderRepository workOrderRepository;
  private final CodeGenerator codeGenerator;

  public MaintenanceEquipmentService(
      EquipmentAssetRepository equipmentRepository,
      CustomerRepository customerRepository,
      WorkOrderRepository workOrderRepository,
      CodeGenerator codeGenerator) {
    this.equipmentRepository = equipmentRepository;
    this.customerRepository = customerRepository;
    this.workOrderRepository = workOrderRepository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public List<EquipmentResponse> listEquipment() {
    return equipmentRepository.findAllByOrderByNextMaintenanceDateAsc().stream()
        .map(this::toEquipResponse).toList();
  }

  @Transactional
  public EquipmentResponse createEquipment(CreateEquipmentRequest r) {
    String code = trimToNull(r.code());
    if (code == null) code = codeGenerator.generate("EQUIPMENT");
    if (equipmentRepository.existsByCode(code)) throw new BusinessException("设备编码已存在");
    EquipmentAsset asset = new EquipmentAsset();
    applyEquipment(asset, r, code);
    return toEquipResponse(equipmentRepository.save(asset));
  }

  @Transactional
  public EquipmentResponse updateEquipment(UUID id, CreateEquipmentRequest r) {
    EquipmentAsset asset = equipmentRepository.findById(id)
        .orElseThrow(() -> new BusinessException("设备不存在"));
    String code = trimToNull(r.code());
    if (code == null) code = asset.getCode();
    if (!code.equals(asset.getCode()) && equipmentRepository.existsByCode(code)) {
      throw new BusinessException("设备编码已存在");
    }
    applyEquipment(asset, r, code);
    return toEquipResponse(equipmentRepository.save(asset));
  }

  private EquipmentAsset requireEquipment(UUID id) {
    return equipmentRepository.findById(id).orElseThrow(() -> new BusinessException("设备不存在"));
  }

  private void applyEquipment(EquipmentAsset asset, CreateEquipmentRequest r, String code) {
    if (!customerRepository.existsById(r.customerId())) throw new BusinessException("客户不存在");
    asset.setCustomerId(r.customerId());
    asset.setContractId(r.contractId());
    asset.setCode(code);
    asset.setName(r.name().trim());
    asset.setCategory(r.category().trim());
    asset.setModel(trimToNull(r.model()));
    asset.setSerialNo(trimToNull(r.serialNo()));
    asset.setSiteAddress(r.siteAddress().trim());
    asset.setInstalledDate(r.installedDate());
    asset.setWarrantyEndDate(r.warrantyEndDate());
    asset.setMaintenanceCycleDays(r.maintenanceCycleDays() == null ? 90 : r.maintenanceCycleDays());
    asset.setNextMaintenanceDate(r.nextMaintenanceDate());
    asset.setRequiredCertificate(trimToNull(r.requiredCertificate()));
    asset.setNotes(trimToNull(r.notes()));
  }


  private EquipmentResponse toEquipResponse(EquipmentAsset a) {
    String cn = a.getCustomerId() == null ? null :
        customerRepository.findById(a.getCustomerId()).map(Customer::getName).orElse(null);
    long cnt = workOrderRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(o -> a.getId().equals(o.getEquipmentId())).count();
    return new EquipmentResponse(
        a.getId(), a.getCode(), a.getName(), a.getCustomerId(), cn,
        a.getCategory(), a.getModel(), a.getSerialNo(),
        a.getSiteAddress(), a.getInstalledDate(),
        a.getWarrantyEndDate(), a.getMaintenanceCycleDays(),
        a.getLastMaintenanceDate(), a.getNextMaintenanceDate(),
        a.getStatus(), cnt);
  }


  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

}
