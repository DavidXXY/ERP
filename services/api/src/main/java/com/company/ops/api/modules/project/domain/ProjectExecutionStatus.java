package com.company.ops.api.modules.project.domain;

/** Operational lifecycle independent from the delivery stage. */
public enum ProjectExecutionStatus {
  ACTIVE,
  PAUSED,
  CANCELLED,
  CLOSED
}
