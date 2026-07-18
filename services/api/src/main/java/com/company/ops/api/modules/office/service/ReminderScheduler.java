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
    for(var item:equipment.findAllByOrderByNextMaintenanceDateAsc())if(item.getStatus()!=EquipmentStatus.RETIRED&&item.getNextMaintenanceDate()!=null&&!item.getNextMaintenanceDate().isAfter(today.plusDays(7)))count+=create("EQUIPMENT_DUE:"+item.getId()+":"+item.getNextMaintenanceDate(),"EQUIPMENT","设备服务到期",item.getCode()+" · "+item.getName()+" · "+item.getNextMaintenanceDate(),"EQUIPMENT",item.getId());
    for(var item:certificates.findAllByOrderByExpiryDateAsc())if(!item.getExpiryDate().isBefore(today)&&!item.getExpiryDate().isAfter(today.plusDays(30)))count+=create("CERTIFICATE_EXPIRY:"+item.getId()+":"+item.getExpiryDate(),"CERTIFICATE","人员证书即将到期",item.getCertificateType()+" · "+item.getCertificateNo()+" · "+item.getExpiryDate(),"CERTIFICATE",item.getId());
    for(var item:contracts.findAllByOrderByEndDateAsc())if(item.getStatus()!=ContractStatus.CLOSED&&!item.getEndDate().isAfter(today.plusDays(90)))count+=create("CONTRACT_RENEWAL:"+item.getId()+":"+item.getEndDate(),"CONTRACT","客户合同续约提醒",item.getCode()+" · "+item.getProjectName()+" · "+item.getEndDate(),"CONTRACT",item.getId());
    for(var item:receivables.findAllByOrderByDueDateAsc())if(item.getStatus()!=ReceivableStatus.SETTLED&&item.getDueDate().isBefore(today))count+=create("RECEIVABLE_OVERDUE:"+item.getId()+":"+item.getDueDate(),"FINANCE","应收款已逾期",item.getCode()+" · 到期日 "+item.getDueDate(),"RECEIVABLE",item.getId());
    for(var item:parts.findAllByOrderByCreatedAtDesc())if(item.isLowStock())count+=create("LOW_STOCK:"+item.getId(),"INVENTORY","物料库存不足",item.getCode()+" · "+item.getName()+" · 当前库存 "+item.getStockQty(),"PART",item.getId());
    count+=officeService.scanApprovalSla();
    return count;}
  private int create(String key,String type,String title,String content,String relatedType,UUID relatedId){if(notifications.existsByDedupKey(key))return 0;SystemNotification item=new SystemNotification();item.setDedupKey(key);item.setType(type);item.setTitle(title);item.setContent(content);item.setRelatedType(relatedType);item.setRelatedId(relatedId);item.setRead(false);notifications.save(item);return 1;}
}
