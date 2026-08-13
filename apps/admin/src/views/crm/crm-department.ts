import { listOwnerDepartments } from "@/api/crm";

export type OwnerDepartmentMap = Map<string, string>;

let cachedMap: OwnerDepartmentMap | null = null;
let inflight: Promise<OwnerDepartmentMap> | null = null;

/**
 * 加载“负责人 -> 部门”映射，结果按模块缓存，避免每个 CRM 列表重复请求。
 * 后端通过员工档案（姓名、账号显示名、账号用户名）解析部门。
 */
export function loadOwnerDepartmentMap(): Promise<OwnerDepartmentMap> {
  if (cachedMap) return Promise.resolve(cachedMap);
  if (!inflight) {
    inflight = listOwnerDepartments()
      .then((rows) => {
        const map: OwnerDepartmentMap = new Map();
        rows.forEach((row) => {
          const department = row.department.trim();
          if (!department) return;
          if (!map.has(row.ownerName)) map.set(row.ownerName, department);
        });
        cachedMap = map;
        return map;
      })
      .catch(() => {
        cachedMap = new Map();
        return cachedMap;
      });
  }
  return inflight;
}

export function ownerDepartment(
  ownerName: string | undefined,
  map: OwnerDepartmentMap,
): string {
  return ownerName ? map.get(ownerName) || "" : "";
}
