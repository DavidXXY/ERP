# Engineering Ops ERP

面向普通企业的一体化管理系统，按可上线系统而不是页面 demo 建设。

## 技术路线

- 管理端：Vue 3 + TypeScript + Vite + Ant Design Vue
- 供应商门户：Vue 3 + TypeScript + Vite + Ant Design Vue
- 微信小程序：uni-app + Vue 3 + TypeScript
- 后端：Spring Boot 3 + Spring Data JPA + Spring Security + Flyway
- 数据库：PostgreSQL
- 缓存：Redis
- 文件档案：统一存储服务（本地开发默认本地磁盘，生产默认 MinIO/S3 兼容对象存储）
- 流程审批：内置条件化审批配置与权限控制，支持金额、业务类型、部门、项目、供应商风险、客户等级匹配，以及多级审批、转交、加签、撤回（Flowable 作为后续可选增强）

## 目录

```text
apps/admin        PC 管理后台
apps/supplier-portal  供应商资料与报价协作门户
apps/mobile       微信小程序与 H5 移动办公端
services/api      Spring Boot 后端服务
infra             本地数据库、缓存、对象存储配置
docs              架构和业务建模文档
src               现有 React 原型，作为产品交互参考保留
```

## 本地启动

当前机器需要先安装 Node.js 22、Java 17、Maven 3.9+ 和 Docker Desktop。

本地开发统一使用 PostgreSQL、Redis、MinIO 基础设施：

```bash
npm install
npm run deps:install
npm run tools:install
npm run infra:up
npm run api:dev
npm run admin:dev
npm run supplier:dev
npm run mobile:dev
```

管理端默认访问 `http://localhost:5174`，供应商门户默认访问 `http://localhost:5176`，移动 H5 默认访问 `http://localhost:5180`，后端 API 默认访问 `http://localhost:8080/api`。

开发环境默认管理员账号：

- 用户名：`admin`
- 密码：`Admin@123`

## 已实现业务

- CRM：客户、商机、报价审批、合同、跟进、续约、应收与客户画像
- 项目：预算审批、阶段推进、人工/材料/差旅/外包成本归集
- 服务：资产设备、服务计划、自动工单、人员派工、现场签到、完工与客户验收；非质保收费工单验收后自动生成应收
- 供应链：采购申请、审批、供应商准入、定向询价、采购代录/供应商自报双通道报价、注册邀请码、报价附件、询价澄清、报价版本与确认留痕、订单、分批收货、质检、退换货、库存入库与应付
- 仓储：物料、库存流水、项目领退料、工单物料消耗与安全库存
- 财务：应收开票回款、应付申请审批付款、费用报销付款、自动凭证、总账与财务报表；付款申请、审批和执行实施不相容职责校验
- 经营治理：31 类跨模块经营控制、预算/承诺/实际/预测对比、会计期间关账、银行流水自动候选对账、凭证制单复核记账分权
- OA：统一审批、费用报销及付款、外包服务、电子档案、消息预警、操作审计；档案上传统一大小、扩展名和路径安全校验；审计日志记录模块、对象、操作类型与查询参数
- 人员：证书到期预警、人员排班、移动签到与工时统计
- 资质：公司资质、人员档案、人员证书、项目业绩、投标组合查询与到期预警
- BI：经营趋势、客户利润、设备故障成本、人员产值
- 移动端：审批、消息、请假/报销/出差申请、工单接单/改派、定位签到、现场照片、材料消耗、客户签字、完工、备件领退报废、弱网离线补传、微信账号绑定登录

## 生产加强状态

- 已加强：维修模块细粒度权限、维保超期风险沉淀、历史环境权限补齐、CRM/OA/资质附件统一存储、MinIO/S3 兼容对象存储适配、预签名临时链接、上传文件白名单与路径穿越防护、统一待办预警、后端统一风险聚合、风险规则配置、自动责任人、SLA 超时升级、每日风险快照、风险趋势与模块分布、统一风险中心批量处理、持久化闭环与处理轨迹、采购三单匹配、库存补货建议、项目利润摘要、经营驾驶舱时间筛选与公司级 KPI、统一编号规则接口与组织维度编号生成、条件化多级审批、审批转交/加签/撤回、审计详情与导出、审计模块/对象/操作类型追踪、核心后端测试覆盖。
- 本轮系统加固：JWT 版本失效、Redis/本地降级登录限流、可信代理 IP 解析、按用户通知回执与审批私有投递、库存/项目成本编号一致性、CSV 公式注入防护、数据库级分页与 200 条单页上限、移动审批可见范围索引、异步审计与 365 天默认保留、三套独立前端依赖锁及完整 CI 构建。
- 本轮全系统优化：稳定分页 DTO、审批摘要/详情分离、跨财务/总账/采购/资质/BI/提醒任务的聚合查询与 N+1 治理、审计短事务分批清理、可轮换数据密钥、PostgreSQL 与对象存储联合备份、Prometheus 最小暴露，以及桌面/移动浏览器关键流程 E2E。
- 本轮业务强化：新增经营治理中心，将合同履约、项目预测、采购预算、库存资产、服务 SLA、主数据和 KPI 纳入统一控制台账及异常扫描；财务补齐 V87 会计期间、关账检查、反结账留痕、银行对账，以及手工凭证草稿/复核/记账/冲销分权。
- 本轮流程闭环：补齐采购取消、质检、退换货和关单约束，维修工单验收转应收，生效合同变更审批，费用报销付款凭证，付款申请/审批/执行三岗分离，以及强制关账和反结账双人复核。
- 本轮供应商协作：新增独立供应商门户、企业资料草稿及资质自助维护、邀请码绑定、账号停用/恢复与临时密码、拆分审核权限、定向询价、双通道报价、代录确认、放弃响应、报价附件、询价澄清、截止日调整、报价版本快照和内部独立评分；数据库增量版本为 V107。
- 待生产配置：替换正式小程序 AppID，配置已备案 HTTPS API 域名及微信 AppSecret，并提交微信平台隐私声明和版本审核。

## 文档

- [系统详细使用教程](docs/system-usage.md)
- [系统架构说明](docs/architecture.md)
- [2026-07-26 系统加固与优化记录](docs/system-hardening-2026-07-26.md)
- [2026-07-26 全系统优化与运维基线](docs/system-optimization-2026-07-26.md)
- [2026-07-26 经营治理强化说明](docs/business-governance-2026-07-26.md)
- [2026-08-05 流程控制闭环说明](docs/process-control-optimization-2026-08-05.md)
- [供应商门户与双通道报价说明](docs/supplier-portal.md)
- [生产部署指南](deploy/README.md)
- [微信小程序发布指南](docs/WECHAT_MINIPROGRAM_DEPLOYMENT.md)

## 验证

```bash
npm run verify
npm run test:e2e
node scripts/check-bundle-size.js
cd services/api && mvn test
```

前端依赖分为根目录、管理端、供应商门户和移动端四套。CI 和可复现安装使用 `npm ci`、`npm ci --prefix apps/admin`、`npm ci --prefix apps/supplier-portal`、`npm ci --prefix apps/mobile`。

运行 `BACKUP_OBJECTS=required npm run data:backup` 可生成并校验 PostgreSQL 与 MinIO/S3 对象的 V2 联合备份（`.tar.gz` 和外层 `.sha256`）。生产环境应使用 `required`，避免缺少 MinIO Client 时静默退化为数据库单备份。

恢复前先做校验，并且必须显式指定演练数据库与同值确认：

```bash
RESTORE_TARGET=ops_erp_restore_drill RESTORE_CONFIRM=ops_erp_restore_drill \
RESTORE_OBJECTS_CONFIRM=ops-erp-drill MINIO_BUCKET=ops-erp-drill \
  scripts/restore-backup.sh backups/ops-erp-backup-YYYYMMDD-HHMMSS.tar.gz
```

对象恢复必须指向隔离的演练 bucket；完整备份、轮换和监控步骤见部署指南。
