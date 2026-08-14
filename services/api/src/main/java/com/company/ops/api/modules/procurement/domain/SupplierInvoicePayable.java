package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_invoice_payables")
public class SupplierInvoicePayable extends BaseEntity {

  @Column(name = "invoice_id", nullable = false)
  private UUID invoiceId;

  @Column(name = "payable_id", nullable = false)
  private UUID payableId;

  public UUID getInvoiceId() { return invoiceId; }
  public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
  public UUID getPayableId() { return payableId; }
  public void setPayableId(UUID payableId) { this.payableId = payableId; }
}
