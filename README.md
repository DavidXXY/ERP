# Engineering Ops ERP

面向普通企业的一体化管理系统，按可上线系统而不是页面 demo 建设。

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
apps/mobile       移动办公端规划入口
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

## 已实现业务

- CRM：客户、商机、报价审批、合同、跟进、续约、应收与客户画像
- 项目：预算审批、阶段推进、人工/材料/差旅/外包成本归集
- 服务：资产设备、服务计划、自动工单、人员派工、现场签到、完工验收
- 供应链：采购申请、审批、订单、分批收货、库存入库与应付
- 仓储：物料、库存流水、项目领退料、工单物料消耗与安全库存
- 财务：应收开票回款、应付申请审批付款、自动凭证、总账与财务报表
- OA：统一审批、费用报销、外包服务、电子档案、消息预警、操作审计
- 人员：证书到期预警、人员排班、移动签到与工时统计
- 资质：公司资质、人员档案、人员证书、项目业绩、投标组合查询与到期预警
- BI：经营趋势、客户利润、设备故障成本、人员产值

本地 H2 数据备份前先停止 API，然后运行 `npm run data:backup`。PostgreSQL 模式使用
`BACKUP_MODE=postgres npm run data:backup`。
