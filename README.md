# Engineering Ops ERP

工程运维公司一体化管理系统，按可上线系统而不是页面 demo 建设。

## 技术路线

- 管理端：Vue 3 + TypeScript + Vite + Ant Design Vue
- 后端：Spring Boot 3 + Spring Data JPA + Spring Security + Flyway
- 数据库：PostgreSQL
- 缓存：Redis
- 文件档案：MinIO
- 流程引擎：Flowable

## 目录

```text
apps/admin        PC 管理后台
apps/mobile       外勤移动端规划入口
services/api      Spring Boot 后端服务
infra             本地数据库、缓存、对象存储配置
docs              架构和业务建模文档
src               现有 React 原型，作为产品交互参考保留
```

## 本地启动

当前机器需要先安装 Java 17、Maven 和 Docker Desktop。

最快开发启动方式使用本地 H2 文件数据库，不依赖 Docker/PostgreSQL：

```bash
npm install
npm run tools:install
npm run api:dev:local
npm run admin:dev
```

如果使用 PostgreSQL、Redis、MinIO 的完整本地基础设施：

```bash
npm install
npm run api:check
npm run infra:up
npm run api:dev
npm run admin:dev
```

管理端默认访问 `http://localhost:5174`，后端 API 默认访问 `http://localhost:8080/api`。

开发环境默认管理员账号：

- 用户名：`admin`
- 密码：`Admin@123`

## 当前阶段

已经搭好基础骨架：

- 后端应用入口、配置、统一响应、异常处理、审计基础实体
- JWT 登录、角色权限、菜单权限和接口权限控制
- CRM 客户档案的实体、仓储、服务和 REST API
- 项目管理、备件仓储、供应链采购的实体、仓储、服务和 REST API
- PostgreSQL 初始表结构、增量迁移与开发种子数据
- 管理端应用壳、登录页、菜单、API 客户端、客户池、项目、库存、采购页面
- 本地 PostgreSQL、Redis、MinIO 的 docker compose 配置

当前管理端可以单独启动查看界面；登录和业务数据需要后端与数据库同时启动。
