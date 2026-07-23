package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "biz_responsibility_bindings")
public class ResponsibilityBinding extends BaseEntity {
  @Column(name = "source_type", nullable = false, length = 40) private String sourceType;
  @Column(name = "source_id", nullable = false) private UUID sourceId;
  @Column(name = "owner_user_id") private UUID ownerUserId;
  @Column(name = "department_id") private UUID departmentId;
  @Column(name = "collaborator_department_ids", length = 1000) private String collaboratorDepartmentIds;
  public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
  public UUID getSourceId(){return sourceId;} public void setSourceId(UUID v){sourceId=v;}
  public UUID getOwnerUserId(){return ownerUserId;} public void setOwnerUserId(UUID v){ownerUserId=v;}
  public UUID getDepartmentId(){return departmentId;} public void setDepartmentId(UUID v){departmentId=v;}
  public String getCollaboratorDepartmentIds(){return collaboratorDepartmentIds;} public void setCollaboratorDepartmentIds(String v){collaboratorDepartmentIds=v;}
}
