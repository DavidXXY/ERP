# React 原型

此目录用于存放 React+TypeScript 的 ERP 原型应用。

## 当前状态

React 原型源码仍在项目根目录的 `src/`、`index.html`、`vite.config.ts` 等文件中。
后续迭代中会逐步将此原型迁移至 `prototypes/react/` 下，使其完全独立于 Vue Admin 主应用。

## 运行方式（迁移后）

```bash
cd prototypes/react
npm install
npm run dev
```

## 与 Admin 应用的关系

- React 原型 & Vue Admin 共享同一后端 API
- React 原型使用 JWT key: `ops_erp_token`
- Vue Admin 使用 JWT key: `ops_erp_admin_token`
