package com.company.ops.api.modules.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_sequences")
public class CodeSequence {

  @Id
  @Column(name = "entity_type", length = 64, nullable = false)
  private String entityType;

  @Column(name = "prefix", length = 16, nullable = false)
  private String prefix;

  @Column(name = "next_number", nullable = false)
  private long nextNumber = 1L;

  public CodeSequence() {}

  public CodeSequence(String entityType, String prefix) {
    this.entityType = entityType;
    this.prefix = prefix;
    this.nextNumber = 1L;
  }

  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public String getPrefix() { return prefix; }
  public void setPrefix(String prefix) { this.prefix = prefix; }
  public long getNextNumber() { return nextNumber; }
  public void setNextNumber(long nextNumber) { this.nextNumber = nextNumber; }
}
