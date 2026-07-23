package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name="biz_responsibility_collaborators")
public class ResponsibilityCollaborator extends BaseEntity {
  @Column(name="binding_id",nullable=false) private UUID bindingId;
  @Column(name="department_id",nullable=false) private UUID departmentId;
  public UUID getBindingId(){return bindingId;} public void setBindingId(UUID v){bindingId=v;}
  public UUID getDepartmentId(){return departmentId;} public void setDepartmentId(UUID v){departmentId=v;}
}
