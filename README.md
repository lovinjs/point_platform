# core-boot

## 1 swagger 文档

http://localhost:8888/swagger-ui/index.html

## 2 环境变量

在项目根目录下新建 `.env` 文件，配置环境变量，具体参考项目配置文件 `application.yml`，配合 `EnvFile` 插件实现勾选不同开发环境

## 3 积分平台重构

- 领域设计：[`docs/v1-domain-design.md`](docs/v1-domain-design.md)
- 后台认证初始化：[`docs/admin-auth-setup.md`](docs/admin-auth-setup.md)
- V1 表结构：[`src/main/resources/db/migration/V1__init_point_platform.sql`](src/main/resources/db/migration/V1__init_point_platform.sql)
- V2 后台认证字段：[`src/main/resources/db/migration/V2__add_admin_auth_security_fields.sql`](src/main/resources/db/migration/V2__add_admin_auth_security_fields.sql)
- 新业务代码统一放在 `com.core.coreboot.platform` 包下，旧商城模块暂时保留。

当前项目尚未启用 Flyway，迁移脚本不会随应用启动自动执行。请勿直接在保留重要数据的数据库中手工执行 V1 脚本。
