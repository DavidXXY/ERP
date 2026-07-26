package com.company.ops.api.modules.office.service;

import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.maintenance.domain.EquipmentStatus;
import com.company.ops.api.modules.maintenance.repository.EmployeeCertificateRepository;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class ReminderScheduler {
  private final EquipmentAssetRepository equipment; private final EmployeeCertificateRepository certificates;
  private final ServiceContractRepository contracts; private final ReceivableRepository receivables;
  private final InventoryPartRepository parts; private final SystemNotificationRepository notifications; private final OfficeService officeService;
  public ReminderScheduler(EquipmentAssetRepository equipment,EmployeeCertificateRepository certificates,ServiceContractRepository contracts,ReceivableRepository receivables,InventoryPartRepository parts,SystemNotificationRepository notifications,OfficeService officeService){this.equipment=equipment;this.certificates=certificates;this.contracts=contracts;this.receivables=receivables;this.parts=parts;this.notifications=notifications;this.officeService=officeService;}
  @Scheduled(cron="${ops.reminders.cron:0 15 1 * * *}") @Transactional
  @SchedulerLock(name = "officeReminderRefresh", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
  public int refresh(){LocalDate today=LocalDate.now();int count=0;
    Set<String> existing=new HashSet<>(notifications.findAllDedupKeys());
    for(var item:equipment.findByStatusNotAndNextMaintenanceDateLessThanEqualOrderByNextMaintenanceDateAsc(EquipmentStatus.RETIRED,today.plusDays(7)))count+=create(existing,"EQUIPMENT_DUE:"+item.getId()+":"+item.getNextMaintenanceDate(),"EQUIPMENT","设备服务到期",item.getCode()+" · "+item.getName()+" · "+item.getNextMaintenanceDate(),"EQUIPMENT",item.getId());
    for(var item:certificates.findByExpiryDateBetweenOrderByExpiryDateAsc(today,today.plusDays(30)))count+=create(existing,"CERTIFICATE_EXPIRY:"+item.getId()+":"+item.getExpiryDate(),"CERTIFICATE","人员证书即将到期",item.getCertificateType()+" · "+item.getCertificateNo()+" · "+item.getExpiryDate(),"CERTIFICATE",item.getId());
    for(var item:contracts.findByStatusNotAndEndDateLessThanEqualOrderByEndDateAsc(ContractStatus.CLOSED,today.plusDays(90)))count+=create(existing,"CONTRACT_RENEWAL:"+item.getId()+":"+item.getEndDate(),"CONTRACT","客户合同续约提醒",item.getCode()+" · "+item.getProjectName()+" · "+item.getEndDate(),"CONTRACT",item.getId());
    for(var item:receivables.findOverdueOutstanding(today))count+=create(existing,"RECEIVABLE_OVERDUE:"+item.getId()+":"+item.getDueDate(),"FINANCE","应收款已逾期",item.getCode()+" · 到期日 "+item.getDueDate(),"RECEIVABLE",item.getId());
    Set<String> activeLowStock=new HashSet<>();
    for(var item:parts.findLowStock()){
      String key="LOW_STOCK:"+item.getId();activeLowStock.add(key);
      count+=create(existing,key,"INVENTORY","物料库存不足",item.getCode()+" · "+item.getName()+" · 当前库存 "+item.getStockQty(),"PART",item.getId());
    }
    var resolved=existing.stream().filter(key->key.startsWith("LOW_STOCK:")&&!activeLowStock.contains(key)).toList();
    if(!resolved.isEmpty())notifications.deleteByDedupKeyIn(resolved);
    count+=officeService.scanApprovalSla();
    return count;}
  private int create(Set<String> existing,String key,String type,String title,String content,String relatedType,UUID relatedId){if(!existing.add(key))return 0;SystemNotification item=new SystemNotification();item.setDedupKey(key);item.setType(type);item.setTitle(title);item.setContent(content);item.setRelatedType(relatedType);item.setRelatedId(relatedId);item.setRead(false);notifications.save(item);return 1;}
}
