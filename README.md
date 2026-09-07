# core-boot

## 1 swagger 文档

http://localhost:8888/swagger-ui/index.html

## 2 环境变量

在项目根目录下新建 `.env` 文件，配置环境变量，具体参考项目配置文件 `application.yml`，配合 `EnvFile` 插件实现勾选不同开发环境

## 3 积分平台重构

- 领域设计：[`docs/v1-domain-design.md`](docs/v1-domain-design.md)
- 后台认证初始化：[`docs/admin-auth-setup.md`](docs/admin-auth-setup.md)
- 后台线下充值接口联调：[`docs/admin-offline-recharge-api.md`](docs/admin-offline-recharge-api.md)
- 后台待确认消费订单联调：[`docs/admin-consumption-prepare-api.md`](docs/admin-consumption-prepare-api.md)
- 消费密码后端核心设计：[`docs/customer-consume-pin-core.md`](docs/customer-consume-pin-core.md)
- 客户认证基础：[`docs/customer-auth-foundation.md`](docs/customer-auth-foundation.md)
- 客户手机号绑定：[`docs/customer-phone-binding.md`](docs/customer-phone-binding.md)
- 数据库版本管理：[`docs/database-migrations.md`](docs/database-migrations.md)
- 本地演示数据：[`dev/sql/seed-local-demo-data.sql`](dev/sql/seed-local-demo-data.sql)（禁止生产执行）
- V1 表结构：[`src/main/resources/db/migration/V1__init_point_platform.sql`](src/main/resources/db/migration/V1__init_point_platform.sql)
- V2 后台认证字段：[`src/main/resources/db/migration/V2__add_admin_auth_security_fields.sql`](src/main/resources/db/migration/V2__add_admin_auth_security_fields.sql)
- V3 客户认证字段：[`src/main/resources/db/migration/V3__add_customer_auth_security_fields.sql`](src/main/resources/db/migration/V3__add_customer_auth_security_fields.sql)
- 新业务代码统一放在 `com.core.coreboot.platform` 包下，旧商城模块暂时保留。

当前项目已启用 Flyway，后端启动时自动校验并执行尚未应用的迁移。已经由 Flyway 执行过的脚本禁止修改；本地首次接管原手工建库时，请按数据库版本管理文档重建为空库，不要再次手工导入 V1～V3。
