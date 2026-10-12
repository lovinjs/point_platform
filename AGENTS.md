# point_platform 开发说明

本仓库是多门店通用积分平台后端，远端为 lovinjs/point_platform。本文路径均相对本仓库。优先用中文沟通，沿用已有命名和代码风格。

## 协作方式

用户选择“云端阅读和修改代码，本地运行与验收”：云端完成代码分析、实现和可执行的必要检查；完整 Spring Boot、MySQL、Redis 联调及手机验收默认在用户本地或既有预发布环境进行。不要为了开始代码任务而默认搭建完整云端数据库或迁移服务器。

- 开始前查看 Git 分支、提交和工作区差异，保留用户已有修改。三个项目是独立仓库，不能假定其他仓库一定在相邻目录。
- 本地与云端通过 Git 分支和提交同步，未推送的本地文件不会自动到云端。接续时核对对应提交；有未提交修改时先检查差异，不强行 reset、clean 或覆盖文件。
- 本次交接由用户自行提交推送；后续提交、推送、PR、部署等按用户任务授权执行。
- 完成时说明涉及文件、接口影响、实际执行的命令和结果，以及需要用户在本地验收的步骤。没有运行的检查明确标为未执行；历史文档中的通过记录不能当作本次测试。
- 阅读与修改集中在相关模块，不扫描所有旧商城代码、不进行无关格式化或依赖升级。

## 技术与目录

Java 21、Spring Boot 3.5、Maven Wrapper 3.9.6、MyBatis-Plus、Spring Security/JWT、Flyway、MySQL、Redis。

- src/main/java/com/core/coreboot/platform/：新积分平台业务，按 auth、customer、merchant、staff、recharge、consumption、point、settlement、finance、report、setting、audit 等领域组织。
- src/test/java/com/core/coreboot/platform/：对应服务、控制器、认证与部署配置测试。
- src/main/resources/db/migration/：Flyway 增量迁移，当前 V1～V6。
- src/main/resources/application.yml、application-prod.yml：应用配置及环境变量定义。
- docs/：领域规则、接口联调与部署说明。
- deploy/：服务器容器部署文件；dev/sql/：仅限本地的演示数据。

新业务继续放在 com.core.coreboot.platform。保留旧商城模块，除非任务明确要求清理；不要把旧商城数据模型混入新积分账本。

## 运行与检查

要求 Java 21，优先使用 Wrapper。以下命令在仓库根目录运行：

~~~bash
./mvnw test
./mvnw -Dtest=RechargeRefundServiceImplTest,AdminRechargeRefundControllerTest test
./mvnw spring-boot:run
./mvnw -Pserver test package
~~~

Windows 对应使用 .\mvnw.cmd。定向测试名按改动选择，修改资金、认证、权限或迁移时补充有意义的相关测试，并运行受影响测试；发布前完整执行 test/package。

默认本地后端端口 8888。启动前准备独立开发 MySQL/Redis，并将配置变量传给进程；IDE 的 EnvFile 或本地 .env 是配置方式，不应假设 Maven/Spring 自动读取 .env。开发 Swagger 位于 /swagger-ui/index.html，健康检查为 /actuator/health，实际开放范围取决于运行配置。

本地默认面向 MySQL 5.7；Maven -Pserver 选择面向服务器 MySQL 8.4 的驱动。Maven server profile 与 Spring prod profile 含义不同。已有 deploy/compose.yml 只启动后端，依赖外部数据库/Redis 网络和预先准备的镜像。

## 必须保持的业务规则

- 1 元 = 1 积分，充值按整元、积分按整数处理，金额接口单位为“分”，费率单位为基点（500 = 5%）。沿用 PointMoneyPolicy 的换算和舍入，不用浮点数计算资金。
- 所有 V1 充值款进入平台账户，fund_receiver 固定 PLATFORM；员工登记线下充值，客户端不能修改余额、指定后台操作员或伪造门店范围。
- 一笔充值形成一个积分批次，消费先进先出；账户、订单、批次、流水与审计更新保持事务一致性、行锁和幂等约束。Redis 不能替代数据库资金账本。
- 员工只创建待确认消费，不提前扣分或冻结积分；客户在自己的设备输入消费密码后由后端再次校验并扣款。PIN 失败次数及锁定状态应按现有独立事务逻辑保留。
- 请求重试沿用同一 Idempotency-Key 和相同请求体，新业务操作使用新的键。
- 整笔充值退款仅限该充值批次从未发生消费的订单；即使消费后来冲正，也不能仅凭批次余额恢复就判定可退。只读退款资格检查不预约、不冻结，实际退款事务仍锁定并重新校验。
- 普通消费退款不在 V1 范围，异常冲正走超级管理员专用流程并记录原因和审计。
- 平台手续费默认 5%，新订单保存费率及金额快照，参数调整不重算已创建或历史订单。
- 门店结算按已经结束的自然月和消费金额快照生成，保持幂等；不能为测试月结修改服务器时间或已有业务日期。
- 后台认证、客户认证、角色和员工门店权限由后端校验。微信支付、赠送积分和自动银行付款不属于当前 V1。

## 数据与配置

- Flyway 初始化新开发空库。已执行的迁移不可修改、重命名或删除；新结构变化增加新版本。以实际迁移目录为准，旧数据库管理文档仅列到 V4。
- dev/sql/seed-local-demo-data.sql 含公开演示凭证，仅用于本地独立测试库，不导入服务器。
- 当前服务器账本是预发布测试数据，Spring prod 配置名不代表正式运营；正式上线使用独立干净数据库和 Redis 数据空间。
- 数据库密码、JWT 密钥、微信 AppSecret、短信 AccessKey、backend.env、私钥、上传数据、数据库备份及构建产物不提交 Git，也不输出到日志或前端配置。
- 服务启动默认不调用真实短信、微信授权或资金流程作测试；使用已有 Mock/本地验证方式，外部联调按具体任务范围执行。

## 文档入口与跨仓库关系

- README.md：构建与文档入口。
- docs/v1-domain-design.md：领域边界与业务规则。
- docs/cloud-development-handoff.md：三仓库项目状态、同步和本地验收方案。
- docs/admin-auth-setup.md、docs/customer-auth-foundation.md：两类认证。
- docs/customer-consumption-confirm-api.md、docs/recharge-refund-eligibility.md、docs/admin-settlement-api.md：核心资金闭环。
- docs/server-private-deployment.md、docs/h5-staging-deployment.md、docs/admin-staging-deployment.md：部署记录，按日期核对，不当作实时线上状态。

管理端为 lovinjs/point_platform_admin，消费者端为 lovinjs/point_platform_customer_app。后台 API 通常为 /api/v1/admin，客户 API 为 /api/v1/customer，H5 认证为 /api/v1/h5/auth。改接口时同步核对调用端类型、请求路径、错误处理及相关文档；其他仓库不可用时明确列出跨仓库待办。
