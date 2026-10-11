# 1Panel 后端内部部署（备案期间）

本阶段只部署后端并通过 SSH 做内部验证，不开放业务网站的公网访问，也不接入真实微信支付。

## 当前环境定位：预发布

目前这台 4 核 / 4 GB 服务器只运行一套后端。2026-09-30 起，用户确认将现有 `point_platform` 数据库及其 Redis 数据作为**预发布测试数据**，不作为正式财务账本；现在不必增加常驻测试分支或在服务器上再启动一套后端。
容器继续使用 Spring `prod` profile，是为了保留正式连接、权限与日志等安全配置；`prod` 配置名不代表当前数据库已经转为正式数据。
备案期间继续只通过 SSH 隧道访问，不开放业务公网端口。不要进行真实收款、真实门店结算或导入真实客户资料。

测试写入前，先为 MySQL 建立可恢复的备份并保存服务器外副本。Redis 持久化不等于备份。
不要把 `dev/sql/seed-local-demo-data.sql` 导入服务器：该脚本明确仅供本地开发使用，包含公开的演示账号与密码，也不适合当前已初始化的预发布库。
后续测试商户、门店、员工和客户应通过业务接口或单独审查过的预发布数据方案建立。

正式上线时不直接把此测试库改名或清除流水后复用。届时先备份预发布数据，再使用全新的正式数据库、专用账号、独立的 Redis 数据空间和新的 JWT 密钥；由 Flyway 初始化正式空库并重新创建管理员，验证完成后才切换服务。具体切换和数据保留方式需在上线前单独确认，不在本阶段执行任何删库、清空 Redis 或覆盖配置的操作。

## 1. 已确认的服务器环境

- Ubuntu Server 24.04 LTS，x86_64，4 核 / 4 GB。
- 1Panel 已安装。
- MySQL：`mysql:8.4.10`，容器名 `1Panel-mysql-UviB`。
- Redis：`redis:7.4.11`，容器名 `1Panel-redis-3ZpE`。
- 两者所在 Docker 网络：`1panel-network`。

Docker 检查界面有时把容器名显示成 `/1Panel-mysql-UviB`，连接时去掉开头的 `/`。
容器之间使用内部地址和端口（MySQL 3306 / Redis 6379），不使用公网 IP 或宿主机映射端口。
后端编排只启动一个 Java 服务，不创建、升级、清空或接管现有 MySQL / Redis 容器。

## 2. 数据库和 Redis 准备

1. 在 1Panel 创建空库 `point_platform`，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`。
2. 创建专用账号 `point_platform_app`，只授予此库的权限。Flyway 需要建表、改表、创建索引及读写权限，不能仅授予查询权限。
3. 账号允许后端容器所在的内部网络访问。MySQL 用户授权的来源限制与端口是否开放公网是两件事；只允许 `localhost` 的账号可能无法供另一个容器使用。
4. MySQL 使用 8.4 的默认认证方式，不为连接便利启用旧的 `mysql_native_password`。模板使用 `sslMode=REQUIRED`，要求 MySQL 已启用 TLS；它加密传输但不校验证书身份。公开上线前应配置可信证书并使用 `VERIFY_IDENTITY`，不要加入 `allowPublicKeyRetrieval=true` 绕过认证问题。
5. Redis 设置独立密码，并配置持久化（建议 AOF `everysec`）。不要把验证码、限流与失败次数当作可以任意丢弃的普通缓存。
6. 不导入旧商城 SQL、不导入本地演示数据，不手工执行 V1～V6。第一次启动由 Flyway 建表；已有数据或迁移失败时先诊断，禁止删库或修改已执行脚本来绕过问题。

公网安全组不要开放 3306、6379、8888。检查 MySQL / Redis 实际容器端口映射，不仅看 1Panel 外部访问开关；需要宿主机连接时，仅绑定 `127.0.0.1`。
配置数据库备份并另存到服务器之外；挂载目录和 Redis 持久化不能替代备份。

## 3. 本地构建可执行 JAR

先使用 Java 21；服务器不需要安装 Java、Maven 或 Node.js。

在 Windows 的 `point_platform` 目录运行：

```powershell
.\mvnw.cmd -Pserver test package
```

输出为 `target/core-boot-0.0.1-SNAPSHOT.jar`。

Maven `server` 构建配置使用 Connector/J 9.7.0，目标是服务器的 MySQL 8.4。
不加 `-Pserver` 时，现有本地 MySQL 5.7 的驱动版本保持不变。两种构建都生成可执行 Spring Boot JAR。
Maven `server` 与 Spring `prod` 是两个不同配置：前者在打包时选择依赖，后者在容器启动时选择服务器配置。

## 4. 本地构建镜像

将 JAR 复制到 `deploy/app.jar`；该构建产物不提交 Git。

```powershell
Copy-Item -LiteralPath .\target\core-boot-0.0.1-SNAPSHOT.jar -Destination .\deploy\app.jar
docker build --platform linux/amd64 --pull -t point-platform-backend:server-v1 .\deploy
docker save -o .\deploy\point-platform-backend-server-v1.tar point-platform-backend:server-v1
```

需要本机 Docker Desktop 已启动并使用 Linux 容器；镜像基于 Java 21 JRE，应用以 UID/GID 10001 非 root 运行。
构建上下文只包含 Dockerfile 和 JAR，不会复制 `.env`、源码、密钥或本地演示 SQL。

第一次构建需要下载官方基础镜像及 curl。若网络下载失败，保留产物并排查官方镜像访问；不要关闭 TLS 或从不可信镜像站下载运行环境。
每次发版使用新的镜像标签，同时保存旧镜像和数据库备份；不要始终覆盖同一个标签。

## 5. 上传和配置服务器

通过 SSH/SFTP 或 1Panel 文件管理上传下列文件到你选定的专用目录，例如 `/opt/point-platform/backend`：

- `deploy/point-platform-backend-server-v1.tar`
- `deploy/compose.yml`
- `deploy/compose.env.example`
- `deploy/backend.env.example`

Docker Compose 需要 2.30.0 或更新版本（`env_file.format: raw`），先检查：

```shell
docker compose version
```

在该部署目录中复制 `compose.env.example` 为 `.env`，复制 `backend.env.example` 为 `backend.env`，用编辑器填写，限制权限：

```shell
chmod 600 .env backend.env
```

`.env` 只放镜像标签、网络名和本机端口，模板已填 `1panel-network`。
`backend.env` 放数据库/Redis 密码、JWT 密钥和可选微信/短信参数。使用 raw 格式，**值不要额外加单引号或双引号**；`$` 和值中的 `#` 按原样传入，不会作为 Compose 变量展开。
不要把密码直接拼到 JDBC URL，不要将真实环境文件放入 Git、镜像或聊天。

模板已填写 MySQL / Redis 内部容器名。只需确认库、账号和实际端口，然后填入密码与 JWT 密钥。

两个 JWT 密钥分别生成，保持配置，不要复用本地环境密钥：

```shell
openssl rand -base64 32
openssl rand -base64 32
```

把两个结果分别填到后台与消费者配置。不要把输出发给别人；重置 JWT 密钥会使对应环境已有令牌失效。

上传目录挂载到容器的 `/opt/app/upload`，创建并设置所有者：

```shell
sudo install -d -m 750 -o 10001 -g 10001 /opt/point-platform/backend/upload
```

如果选择不同的部署路径，上面的目录同步替换为你实际选定的绝对路径。
编排不会自动创建宿主机挂载目录，避免权限错误；不要使用 `chmod 777`。

首次启动空库时，设置 `PLATFORM_BOOTSTRAP_ADMIN_ENABLED=true`，填写初始化用户名、密码、真实姓名及可选手机号。
密码要求 12～128 位，包含大写字母、小写字母、数字和特殊字符。使用独立的服务器账号密码。

暂时保留 `PLATFORM_WECHAT_H5_ENABLED=false` 和短信 `DISABLED`；后续联调时再启用 ALIYUN，不会因首次启动意外发送真实短信。

## 6. 启动并检查

在部署目录执行：

```shell
docker load -i point-platform-backend-server-v1.tar
docker compose config --quiet
docker compose up -d
docker compose ps
docker compose logs --tail=100 backend
curl --fail http://127.0.0.1:8888/actuator/health
```

不要执行或截图完整的 `docker compose config` / `docker inspect` 环境变量输出，它们可能包含密码；`config --quiet` 仅校验配置。
`up` 会按需启动/重建后端，但不会修改外部 MySQL / Redis。
健康检查成功为 `{"status":"UP"}`，包含数据库和 Redis 连通性检查，但不公开连接信息。
`UP` 只表示服务及依赖连通，不能代替业务验收。

检查：

- Flyway V1～V6 成功，`flyway_schema_history.success` 全为 1。
- 首个超级管理员已创建；注意不把带有个人信息的完整日志发到公共渠道。
- 后端映射地址为 `127.0.0.1:8888`，不是 `0.0.0.0:8888`。
- MySQL / Redis 实际网络与模板一致；若连接失败，先检查内部 DNS、授权、密码和 MySQL TLS，不扩大公网端口访问。

### 已知 Flyway 版本提醒

当前 Spring Boot 管理的 Flyway 版本为 11.7.2，连接 MySQL 8.4 时可能出现 `Flyway upgrade recommended`，提示数据库超出该版本声明的验证范围。
本次已使用隔离的 MySQL 8.4.10 / Redis 7.4.11 验证 V1～V6 空库迁移、管理员初始化及登录、后台查询和接口权限；这不代表全部业务 SQL 都已完成兼容验收。
保留警告，不修改迁移校验设置来消除它；若部署出现 `ERROR`、迁移失败或健康状态非 `UP`，必须单独诊断。
正式公开上线前评估 Flyway 升级并完成全业务回归，不能只凭启动成功判断兼容性。
上游资料：[Flyway MySQL 文档](https://documentation.red-gate.com/fd/mysql-277579322.html)、[MySQL 8.4 支持议题](https://github.com/flyway/flyway/issues/4168)。

### 关闭一次性管理员初始化

管理员登录验证成功后，把 `PLATFORM_BOOTSTRAP_ADMIN_ENABLED=false`，清空所有 `PLATFORM_BOOTSTRAP_ADMIN_*` 初始化身份字段，然后执行：

```shell
docker compose up -d --force-recreate backend
```

必须重建容器才会更新环境变量，仅 `restart` 不会移除旧容器里的初始化密码。
不要移除长期需要的数据库、Redis、JWT 等配置。

## 7. 从自己的电脑做内部联调

服务器业务端口不开放公网，SSH 连接使用密钥认证并限制来源。

```powershell
ssh -N -L 127.0.0.1:18888:127.0.0.1:8888 登录用户名@服务器IP
```

保持连接窗口打开，在本机 Apifox 请求 `http://127.0.0.1:18888/api/v1/admin/auth/login`。
后台、订单等接口继续使用原有认证规则；不用新增跳过登录的测试入口。
通过开发代理连接本机隧道地址可以做管理端联调，生产前端部署另行准备。

服务器模式关闭 Swagger 与旧商城路由，需要接口文档时查看本地开发环境。
手机微信不会自动走电脑隧道；正式服务号授权和真实支付回调待备案通过后再联调。

## 8. 当前安全默认与限制

- 后端仅发布服务器回环地址端口，不创建公网网站。
- 复用数据库网络，密码保留在服务器文件中；端口限制不能替代账号权限。
- 使用正式 MySQL 驱动，不使用 P6Spy 输出 SQL / 参数。
- 限制后端内存约 1.25 GiB、Java 堆 640 MiB；4 GB 主机仍需同时观察数据库与系统内存，出现 OOM 时依据实测调整，不直接无限制增加内存。
- 容器只读根目录、非 root、限制权限，上传数据单独挂载，临时目录单独提供。
- 日志只输出控制台，由 Docker 限制每份 10 MB、保留 3 份，不使用本地开发的无限增长日志目录。
- 服务启动检查必要配置、JWT 密钥及 Flyway 安全设置，服务器模式禁止短信 LOG 模式。
- 只公开最小健康状态，禁用 Swagger、旧商城入口与其他管理端点；新后台、客户接口保持原有权限。
- 当前只是服务器内部测试配置，不等于已完成公网生产上线；还需备案、HTTPS、可信数据库 TLS、正式服务号、备份恢复演练及完整业务验收。

参考：[Docker 外部网络](https://docs.docker.com/reference/compose-file/networks/)、[Compose 环境文件](https://docs.docker.com/reference/compose-file/services/#env_file)、[Spring Boot 打包](https://docs.spring.io/spring-boot/maven-plugin/packaging.html)。
