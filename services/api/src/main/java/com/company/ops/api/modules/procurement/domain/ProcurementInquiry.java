package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="procurement_inquiries")
public class ProcurementInquiry extends BaseEntity {
  @Column(nullable=false,length=64) private String code;
  @Column(name="request_id",nullable=false) private UUID requestId;
  @Column(nullable=false,length=180) private String title;
  private LocalDate deadline;
  @Column(nullable=false,length=32) private String status="OPEN";
  @Column(name="created_by_name",length=80) private String createdByName;
  public String getCode(){return code;} public void setCode(String v){code=v;}
  public UUID getRequestId(){return requestId;} public void setRequestId(UUID v){requestId=v;}
  public String getTitle(){return title;} public void setTitle(String v){title=v;}
  public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate v){deadline=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
}
