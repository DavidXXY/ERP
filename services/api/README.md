# 后端服务

Spring Boot 3 模块化单体，第一阶段先建设 CRM 客户档案和系统底座。

## 运行要求

- Java 17
- Maven 3.9+
- PostgreSQL 16
- Redis 7
- MinIO

## 启动

```bash
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

## 已实现接口

```text
GET  /api/crm/customers
GET  /api/crm/customers/{id}
POST /api/crm/customers
```

## 后续扩展顺序

1. 系统权限：用户、角色、菜单、数据范围、登录认证。
2. CRM：线索、商机、报价、合同审批。
3. 项目与工单：项目立项、工单派发、设备台账、外勤执行。
4. 库存与采购：备件台账、出入库、安全库存、采购申请。
5. 财务：应收、开票、回款核销、应付、凭证。

