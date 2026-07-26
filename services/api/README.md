# 后端服务

Spring Boot 3 模块化单体，提供 ERP 全业务 API、认证授权、审批、通知、审计、文件存储和跨模块事务。

## 运行要求

- Java 17
- Maven 3.9+
- PostgreSQL 16
- Redis 7
- MinIO

## 启动

```bash
# 从仓库根目录启动本地基础设施和 API
npm run infra:up
npm run api:dev

# 或直接启动后端
cd services/api
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

API 文档：

```text
http://localhost:8080/swagger-ui
```

OpenAPI JSON：

```text
http://localhost:8080/api-docs
```

## 模块

- `crm`：客户、商机、报价、合同和应收联动
- `project`：立项、预算、阶段、成本和利润
- `procurement` / `inventory`：供应商、采购、到货、库存、领退料和项目成本
- `finance` / `ledger`：应收应付、开票回款、付款、凭证和报表
- `maintenance` / `mobile`：设备、工单和移动作业
- `office` / `collaboration`：审批、报销、档案、通知、审计和协作
- `hr` / `qualification`：员工、人事、证书、资质和投标查询
- `risk` / `bi`：风险闭环、快照和经营分析
- `governance`：经营控制、会计期间、关账守卫和银行对账
- `system`：认证、组织、用户、角色、权限和数据范围

## 数据库迁移

生产使用 PostgreSQL 16 和 Flyway。新数据库从 `B77__fresh_install_baseline.sql` 建立基线，再执行 V78 之后的增量迁移；当前增量版本为 V87。不要手工修改数据库结构或已执行迁移。

## 验证

```bash
mvn test
```

PostgreSQL 基线迁移测试使用 Testcontainers，需要本机或 CI 的 Docker 可用。其余集成测试使用 H2 专用迁移链。

分页接口通过稳定 `PageResponse` 输出，不直接序列化 Spring Data `Page`。生产监控仅暴露 `health,prometheus`；数据密钥轮换、联合备份恢复、审计分批清理和本轮查询优化见 `../../docs/system-optimization-2026-07-26.md`。
