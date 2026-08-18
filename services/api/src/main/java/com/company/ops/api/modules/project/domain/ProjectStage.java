package com.company.ops.api.modules.project.domain;

/**
 * 项目交付阶段，自带顺序与基准进度，避免在服务层硬编码 switch。
 */
public enum ProjectStage {
  INITIATED(0, 0),
  BIDDING(1, 0),
  ENTRY(2, 0),
  CONSTRUCTION(3, 20),
  COMMISSIONING(4, 45),
  INITIAL_ACCEPTANCE(5, 65),
  FINAL_ACCEPTANCE(6, 85),
  WARRANTY(7, 100),
  CLOSED(8, 100);

  private final int order;
  private final int progress;

  ProjectStage(int order, int progress) {
    this.order = order;
    this.progress = progress;
  }

  public int order() {
    return order;
  }

  public int progress() {
    return progress;
  }

  public ProjectStage next() {
    for (ProjectStage stage : values()) {
      if (stage.order == order + 1) return stage;
    }
    return null;
  }

  public ProjectStage previous() {
    for (ProjectStage stage : values()) {
      if (stage.order == order - 1) return stage;
    }
    return null;
  }
}
