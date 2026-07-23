package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "biz_project_handovers")
public class ProjectHandover extends BaseEntity {
  @Column(name="contract_id",nullable=false) private UUID contractId;
  @Column(name="project_id",nullable=false) private UUID projectId;
  @Column(name="sales_owner_id") private UUID salesOwnerId;
  @Column(name="project_manager_id") private UUID projectManagerId;
  @Column(name="sales_department_id") private UUID salesDepartmentId;
  @Column(name="delivery_department_id") private UUID deliveryDepartmentId;
  @Column(name="scope_summary",length=1000) private String scopeSummary;
  @Column(name="payment_terms",length=500) private String paymentTerms;
  @Column(name="acceptance_criteria",length=1000) private String acceptanceCriteria;
  @Column(name="customer_contact",length=500) private String customerContact;
  @Column(name="technical_solution",length=1000) private String technicalSolution;
  @Column(name="quotation_summary",length=1000) private String quotationSummary;
  @Column(name="risk_notes",length=1000) private String riskNotes;
  @Column(nullable=false,length=32) private String status="PENDING";
  @Column(name="submitted_at") private OffsetDateTime submittedAt;
  @Column(name="accepted_at") private OffsetDateTime acceptedAt;
  @Column(name="accepted_by",length=80) private String acceptedBy;
  @Column(length=500) private String comment;
  public UUID getContractId(){return contractId;} public void setContractId(UUID v){contractId=v;}
  public UUID getProjectId(){return projectId;} public void setProjectId(UUID v){projectId=v;}
  public UUID getSalesOwnerId(){return salesOwnerId;} public void setSalesOwnerId(UUID v){salesOwnerId=v;}
  public UUID getProjectManagerId(){return projectManagerId;} public void setProjectManagerId(UUID v){projectManagerId=v;}
  public UUID getSalesDepartmentId(){return salesDepartmentId;} public void setSalesDepartmentId(UUID v){salesDepartmentId=v;}
  public UUID getDeliveryDepartmentId(){return deliveryDepartmentId;} public void setDeliveryDepartmentId(UUID v){deliveryDepartmentId=v;}
  public String getScopeSummary(){return scopeSummary;} public void setScopeSummary(String v){scopeSummary=v;}
  public String getPaymentTerms(){return paymentTerms;} public void setPaymentTerms(String v){paymentTerms=v;}
  public String getAcceptanceCriteria(){return acceptanceCriteria;} public void setAcceptanceCriteria(String v){acceptanceCriteria=v;}
  public String getCustomerContact(){return customerContact;} public void setCustomerContact(String v){customerContact=v;}
  public String getTechnicalSolution(){return technicalSolution;} public void setTechnicalSolution(String v){technicalSolution=v;}
  public String getQuotationSummary(){return quotationSummary;} public void setQuotationSummary(String v){quotationSummary=v;}
  public String getRiskNotes(){return riskNotes;} public void setRiskNotes(String v){riskNotes=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public OffsetDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(OffsetDateTime v){submittedAt=v;}
  public OffsetDateTime getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(OffsetDateTime v){acceptedAt=v;}
  public String getAcceptedBy(){return acceptedBy;} public void setAcceptedBy(String v){acceptedBy=v;}
  public String getComment(){return comment;} public void setComment(String v){comment=v;}
}
