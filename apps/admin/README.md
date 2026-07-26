# 管理端

Vue 3 + TypeScript + Vite + Ant Design Vue。

## 启动

以下命令从仓库根目录执行：

```bash
npm ci --prefix apps/admin
npm run admin:dev
```

默认访问：

```text
http://localhost:5174
```

## API 地址

默认走 Vite 代理 `/api -> http://localhost:8080`。

如果后端不在本机 8080，可以在启动前设置：

```bash
VITE_API_BASE_URL=http://your-host:8080/api npm run admin:dev
```

## 验证

以下命令从仓库根目录执行：

```bash
npm run lint
npm run format:check
npm run test
npm --prefix apps/admin run typecheck
npm run admin:build
node scripts/check-bundle-size.js
```

管理端已覆盖经营驾驶舱、CRM、项目、采购、库存、维修、人事、资质、OA、财务、风险、BI 和系统管理。列表接口使用分页结构，需要完整数据的导出或聚合通过 `src/api/http.ts` 的 `requestAllPages` 逐页拉取。

CSV 导出必须使用 `src/utils/csv.ts`，不要在页面内自行拼接未转义的 CSV 单元格。
