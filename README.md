# Engineering Ops ERP

[![CI](https://github.com/DavidXXY/ERP/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DavidXXY/ERP/actions/workflows/ci.yml)
[![Security](https://github.com/DavidXXY/ERP/actions/workflows/security.yml/badge.svg?branch=main)](https://github.com/DavidXXY/ERP/actions/workflows/security.yml)

面向普通企业的全栈 ERP，覆盖客户、项目、采购、供应商协作、服务、仓储、财务和移动办公。系统按可上线产品建设，核心流程带审批、权限、审计、附件安全和数据备份能力。

## 产品入口

| 使用者 | 入口 | 主要工作 |
| --- | --- | --- |
| 内部员工、管理员 | `http://localhost:5174` | CRM、项目、采购、库存、服务、财务、OA、资质、BI 和系统管理 |
| 注册供应商 | `http://localhost:5176` | 企业资料、资质文件、受邀询价、供应商自报和报价确认 |
| 现场服务人员 | `http://localhost:5180` | 审批、工单、签到、照片、材料消耗、客户签字和离线补传 |
| 后端 API | `http://localhost:8080/api` | Spring Boot REST API 与健康检查 |

供应商门户使用独立认证和令牌，不进入内部用户菜单。采购员既可以在管理端代录报价，也可以邀请供应商在门户自助报价；两条路径写入同一套比选、定标和审计流程。

## 核心流程

```text
供应商注册/邀请码绑定
        -> 资料与资质审核
        -> 采购建立询价并定向邀请
        -> 供应商自助报价 或 采购员代录报价
        -> 供应商确认（代录报价）
        -> 技术/商务评分与比选
        -> 定标、采购订单、收货、质检和应付
```

供应商只能看到明确邀请自己的询价。账号启用、供应商准入通过、风险状态正常并完成临时密码修改后，才允许提交报价。报价支持草稿、撤回重报、版本快照、附件、放弃响应和询价澄清；详细规则见[供应商门户与双通道报价说明](docs/supplier-portal.md)。

## 已覆盖模块

- **CRM**：客户、联系人、商机、销售报价、合同、跟进、续约和应收
- **项目**：预算审批、阶段推进、人工/材料/差旅/外包成本和利润摘要
- **供应链**：采购申请、审批、供应商准入、询价报价、订单、收货、质检、退换货、库存和应付
- **服务与仓储**：资产设备、服务计划、工单、派工、现场验收、领退料和安全库存
- **财务与经营治理**：发票回款、付款、费用报销、总账、会计期间、银行对账、预算/承诺/实际/预测和经营 KPI
- **OA 与人员**：统一审批、电子档案、消息预警、审计、证书、排班、移动签到和工时
- **资质与 BI**：公司资质、人员证书、项目业绩、投标组合、经营趋势、客户利润和故障成本

## 技术栈

- 管理端：Vue 3、TypeScript、Vite、Ant Design Vue
- 供应商门户：Vue 3、TypeScript、Vite、Ant Design Vue
- 移动端：uni-app、Vue 3、TypeScript（H5 与微信小程序）
- 后端：Spring Boot 3、Spring Data JPA、Spring Security、Flyway
- 基础设施：PostgreSQL、Redis、MinIO/S3 兼容对象存储、Docker Compose

## 目录结构

```text
apps/admin            内部 PC 管理后台
apps/supplier-portal  供应商资料与报价协作门户
apps/mobile           微信小程序与 H5 移动办公端
services/api          Spring Boot 后端服务
infra                 本地 PostgreSQL、Redis、对象存储配置
deploy                Nginx、systemd、备份和生产部署脚本
docs                  架构、使用和业务流程文档
scripts                本地开发、验证、备份和恢复脚本
src                   React 原型（仅作交互参考保留）
```

## 本地开发

### 环境要求

- Node.js 22+（CI 使用 Node.js 24）
- npm 11.6.2+
- Java 17
- Maven 3.9+
- Docker Desktop

### 安装与启动

```bash
# 安装根目录及三个前端的锁定依赖
npm ci
npm ci --prefix apps/admin
npm ci --prefix apps/supplier-portal
npm ci --prefix apps/mobile

# 启动 PostgreSQL、Redis、MinIO
npm run infra:up
```

然后在不同终端启动服务：

```bash
npm run api:dev       # Spring Boot API，8080
npm run admin:dev     # 内部管理端，5174
npm run supplier:dev  # 供应商门户，5176
npm run mobile:dev    # 移动 H5，5180
```

首次本地登录的管理端账号由后端启动配置创建，默认值为：用户名 `admin`，密码 `Admin@123`。该账号仅用于开发，生产环境必须通过 `BOOTSTRAP_ADMIN_PASSWORD` 设置随机密码并及时修改。供应商账号通过门户注册，已有供应商主档的账号需要采购邀请生成的 7 天一次性注册码绑定。

常用命令：

```bash
npm run infra:down       # 停止本地基础设施
npm run admin:build      # 构建管理端
npm run supplier:build   # 构建供应商门户
npm run mobile:build:h5 # 构建移动 H5
npm run mobile:build     # 构建微信小程序
```

## 验证与质量门禁

一键执行前端、移动端和供应商门户验证：

```bash
npm run verify
```

后端完整测试与打包：

```bash
cd services/api
mvn --batch-mode verify
```

端到端测试首次运行前安装 Chromium：

```bash
npm run test:e2e:install
npm run test:e2e
```

CI 还会执行依赖漏洞审查、Secret scan、CodeQL、SBOM 生成、迁移测试和前端 bundle 预算检查。四个前端目录各自保留锁文件，更新依赖时请同步运行对应的 typecheck、lint 和 build。

## 生产部署

生产环境使用 Nginx HTTPS 反向代理，Spring Boot 默认监听 8080，PostgreSQL、Redis 和 MinIO/S3 作为持久化基础设施。部署前必须配置 JWT、数据加密、数据库、Redis、对象存储和可信代理网段等密钥与参数；不要使用仓库中的开发默认值。

- [生产部署指南](deploy/README.md)
- [供应商门户部署与安全配置](docs/supplier-portal.md#部署配置)
- [备份与恢复脚本](scripts/backup-data.sh)
- [微信小程序发布指南](docs/WECHAT_MINIPROGRAM_DEPLOYMENT.md)

生产发布前请先在隔离环境验证 Flyway 迁移、PostgreSQL 与对象存储联合备份、恢复演练和健康检查。供应商上传的企业资料和报价附件应使用 MinIO/S3，并纳入备份策略。

## 文档导航

- [超详细使用教程（带实际界面截图）](docs/使用教程-带截图.md)
- [系统详细使用教程](docs/system-usage.md)
- [系统架构说明](docs/architecture.md)
- [供应商门户与双通道报价说明](docs/supplier-portal.md)
- [流程控制闭环说明](docs/process-control-optimization-2026-08-05.md)
- [经营治理强化说明](docs/business-governance-2026-07-26.md)
- [系统加固与优化记录](docs/system-hardening-2026-07-26.md)
- [全系统优化与运维基线](docs/system-optimization-2026-07-26.md)

## 许可证

当前仓库未声明开源许可证。除非获得仓库所有者明确授权，请不要将代码或生产数据用于外部发布。
