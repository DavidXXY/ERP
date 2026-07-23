package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name="procurement_inquiries")
public class ProcurementInquiry extends BaseEntity {
  @Column(nullable=false,length=64) private String code;
  @Column(name="request_id",nullable=false) private UUID requestId;
  @Column(nullable=false,length=180) private String title;
  private LocalDate deadline;
  @Column(nullable=false,length=32) private String status="OPEN";
  @Column(name="created_by_name",length=80) private String createdByName;
  @Column(name="sourcing_method",nullable=false,length=32) private String sourcingMethod="COMPETITIVE";
  @Column(name="min_quote_count",nullable=false) private Integer minQuoteCount=3;
  @Column(name="exception_reason",length=500) private String exceptionReason;
  @Column(name="selected_quote_id") private UUID selectedQuoteId;
  @Column(name="selection_reason",length=1000) private String selectionReason;
  @Column(name="selected_by_name",length=80) private String selectedByName;
  @Column(name="selected_at") private OffsetDateTime selectedAt;
  public String getCode(){return code;} public void setCode(String v){code=v;}
  public UUID getRequestId(){return requestId;} public void setRequestId(UUID v){requestId=v;}
  public String getTitle(){return title;} public void setTitle(String v){title=v;}
  public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate v){deadline=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
  public String getSourcingMethod(){return sourcingMethod;} public void setSourcingMethod(String v){sourcingMethod=v;}
  public Integer getMinQuoteCount(){return minQuoteCount;} public void setMinQuoteCount(Integer v){minQuoteCount=v;}
  public String getExceptionReason(){return exceptionReason;} public void setExceptionReason(String v){exceptionReason=v;}
  public UUID getSelectedQuoteId(){return selectedQuoteId;} public void setSelectedQuoteId(UUID v){selectedQuoteId=v;}
  public String getSelectionReason(){return selectionReason;} public void setSelectionReason(String v){selectionReason=v;}
  public String getSelectedByName(){return selectedByName;} public void setSelectedByName(String v){selectedByName=v;}
  public OffsetDateTime getSelectedAt(){return selectedAt;} public void setSelectedAt(OffsetDateTime v){selectedAt=v;}
}
