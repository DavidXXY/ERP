# Engineering Ops ERP

面向普通企业的一体化管理系统，按可上线系统而不是页面 demo 建设。

## 技术路线

- 管理端：Vue 3 + TypeScript + Vite + Ant Design Vue
- 后端：Spring Boot 3 + Spring Data JPA + Spring Security + Flyway
- 数据库：PostgreSQL
- 缓存：Redis
- 文件档案：统一存储服务（本地开发默认本地磁盘，生产默认 MinIO/S3 兼容对象存储）
- 流程审批：内置条件化审批配置与权限控制，支持金额、业务类型、部门、项目、供应商风险、客户等级匹配，以及多级审批、转交、加签、撤回（Flowable 作为后续可选增强）

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

本地开发统一使用 PostgreSQL、Redis、MinIO 基础设施：

```bash
npm install
npm run tools:install
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
- 服务：资产设备、服务计划、自动工单、人员派工、现场签到、完工验收；维修证书、排班和工单删除已拆分细粒度权限
- 供应链：采购申请、审批、订单、分批收货、库存入库与应付
- 仓储：物料、库存流水、项目领退料、工单物料消耗与安全库存
- 财务：应收开票回款、应付申请审批付款、自动凭证、总账与财务报表
- OA：统一审批、费用报销、外包服务、电子档案、消息预警、操作审计；档案上传统一大小、扩展名和路径安全校验；审计日志记录模块、对象、操作类型与查询参数
- 人员：证书到期预警、人员排班、移动签到与工时统计
- 资质：公司资质、人员档案、人员证书、项目业绩、投标组合查询与到期预警
- BI：经营趋势、客户利润、设备故障成本、人员产值

## 生产加强状态

- 已加强：维修模块细粒度权限、维保超期风险沉淀、历史环境权限补齐、CRM/OA/资质附件统一存储、MinIO/S3 兼容对象存储适配、预签名临时链接、上传文件白名单与路径穿越防护、统一待办预警、后端统一风险聚合、风险规则配置、自动责任人、SLA 超时升级、每日风险快照、风险趋势与模块分布、统一风险中心批量处理、持久化闭环与处理轨迹、采购三单匹配、库存补货建议、项目利润摘要、经营驾驶舱时间筛选与公司级 KPI、统一编号规则接口与组织维度编号生成、条件化多级审批、审批转交/加签/撤回、审计详情与导出、审计模块/对象/操作类型追踪、核心后端测试覆盖。
- 本轮系统加固：JWT 短时令牌与登录限流、租户级 JPA 隔离、敏感字段 AES-GCM 加密、乐观锁、分布式任务锁、Flyway 空库集成测试、API 登录链路集成测试、前端单测/静态检查/500KB 分包预算、Prometheus 告警、HTTPS 安全响应头、可校验备份与受保护恢复演练。
- 规划中：移动端代码实现，以及按真实业务租户接入独立域名/身份源。

运行 `npm run data:backup` 可生成并校验 PostgreSQL 备份。

恢复前先做校验，并且必须显式指定演练数据库与同值确认：

```bash
RESTORE_TARGET=ops_erp_restore_drill RESTORE_CONFIRM=ops_erp_restore_drill \
  scripts/restore-backup.sh backups/ops-erp-postgres-YYYYMMDD-HHMMSS.dump
```
