# 数据库版本管理

## 当前策略

项目已经启用 Flyway。后端每次启动时都会先检查并执行 `src/main/resources/db/migration` 下尚未执行的版本脚本，然后才启动业务服务。

当前版本：

- `V1__init_point_platform.sql`：积分平台基线表与系统角色。
- `V2__add_admin_auth_security_fields.sql`：后台登录安全字段。
- `V3__add_customer_auth_security_fields.sql`：客户令牌版本字段。
- `V4__add_settlement_payment_reference_unique.sql`：结算付款参考号唯一约束。

Flyway 使用业务数据源，并在数据库中维护 `flyway_schema_history`。配置默认启用脚本校验、严格命名校验和顺序执行，同时禁止 Flyway `clean`。

## 首次接管本地数据库

当前本地库的 V1～V3 曾经手工执行，没有 Flyway 历史记录。由于数据均为测试数据，采用以下方式接管：

1. 停止后端。
2. 删除并重新创建空的 `core_boot` 数据库，字符集使用 `utf8mb4`。
3. 确认数据库连接环境变量指向这个空库。
4. 启动后端；不要再手工执行 V1～V4。
5. 查询 `flyway_schema_history`，确认 V1、V2、V3、V4 均为成功。
6. 按 `docs/admin-auth-setup.md` 重新创建一次超级管理员。
7. 本地需要门店和员工测试数据时，手工执行 `dev/sql/seed-local-demo-data.sql`。

示例建库语句：

```sql
CREATE DATABASE `core_boot`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

删除数据库是破坏性操作。执行前必须再次确认目标确实是本地测试库，而不是测试、预发布或生产库。

## 后续新增迁移

- 已经在任何环境执行成功的 V1～V4 禁止修改、重命名或删除。
- 下一次表结构变化从 `V5__简短英文说明.sql` 开始。
- 一个版本号只能对应一个脚本。
- 迁移脚本只放表结构、约束以及所有环境都必须存在的基础数据。
- 演示门店、演示账号、超级管理员和业务测试数据不能进入 Flyway。
- 不使用 `baseline-on-migrate` 绕过未知数据库，也不通过关闭校验来掩盖校验和不一致。

`FLYWAY_ENABLED=false` 只用于临时故障诊断，不应成为日常启动配置，更不能在生产环境长期关闭。
