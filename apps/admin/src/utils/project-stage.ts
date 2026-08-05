import type { ProjectStage } from "@/api/project";

export const projectStageLabels: Record<ProjectStage, string> = {
  INITIATED: "立项",
  BIDDING: "招投标",
  ENTRY: "进场",
  CONSTRUCTION: "施工",
  COMMISSIONING: "调试",
  INITIAL_ACCEPTANCE: "初验",
  FINAL_ACCEPTANCE: "终验",
  WARRANTY: "质保",
  CLOSED: "结项",
};

export function projectStageLabel(value?: string) {
  if (!value) return "-";
  return projectStageLabels[value as ProjectStage] || value;
}

export function projectStageColor(value?: string) {
  return (
    {
      INITIATED: "default",
      BIDDING: "cyan",
      ENTRY: "blue",
      CONSTRUCTION: "geekblue",
      COMMISSIONING: "purple",
      INITIAL_ACCEPTANCE: "orange",
      FINAL_ACCEPTANCE: "gold",
      WARRANTY: "green",
      CLOSED: "default",
    } as Record<string, string>
  )[value || ""];
}
