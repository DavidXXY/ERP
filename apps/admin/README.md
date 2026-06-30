# 管理端

Vue 3 + TypeScript + Vite + Ant Design Vue。

## 启动

```bash
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

## 当前页面

- 经营驾驶舱
- CRM / 客户池
- CRM / 线索商机占位
- CRM / 客户合同
- 供应链采购占位
- 项目管理占位
- 库存管理
- 财务资金占位
- 系统设置占位
