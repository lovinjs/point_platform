# 后台线下充值接口联调说明

本阶段不需要执行新的数据库迁移脚本。调用以下接口时都需要在 Swagger 的 `Bearer Token` 中填写后台登录返回的 `accessToken`。

## 1. 查询可操作门店

~~~http
GET /api/v1/admin/stores
Authorization: Bearer <accessToken>
~~~

- 超级管理员可以看到全部 `ACTIVE` 门店。
- 店长和店员只能看到 `t_sys_user_store` 中绑定的 `ACTIVE` 门店。
- 返回空数组表示当前没有符合条件的门店。

## 2. 按手机号查询客户

~~~http
GET /api/v1/admin/customers?phone=13800138000
Authorization: Bearer <accessToken>
~~~

手机号只支持精确查询，不提供模糊搜索或客户全量列表。返回客户基本信息、状态、当前平台积分余额和 `consumePinConfigured`。尚未创建积分账户的客户返回余额 `0`；`consumePinConfigured = false` 时可以充值，但还不能发起消费订单。

## 3. 完成线下充值

~~~http
POST /api/v1/admin/recharge-orders/offline
Authorization: Bearer <accessToken>
Idempotency-Key: 42e0a54a-318d-4fd9-ae31-b51501420c61
Content-Type: application/json

{
  "customerId": 1,
  "storeId": 1,
  "amountYuan": 100,
  "paymentMethod": "BANK_TRANSFER",
  "paymentReference": "PLATFORM-TX-20260905-001",
  "remark": "已核对平台账户收款"
}
~~~

字段说明：

- `amountYuan` 是整元金额。示例中的 `100` 表示充值 100 元并增加 100 积分。
- `paymentMethod` 可选值为 `PLATFORM_QR`、`BANK_TRANSFER`、`OTHER`。
- `paymentReference` 是平台收款交易参考号，必须真实、唯一。
- `Idempotency-Key` 由后台前端为本次提交生成，建议使用 UUID。同一请求超时重试时必须保持键和请求内容完全一致；一笔新的充值必须生成新的键。
- 请求体不接受 `operatorId` 和收款方。操作员取自当前后台登录账号，收款方固定为平台。

成功后会在一个事务中完成：

1. 创建已完成的充值订单。
2. 增加客户积分余额。
3. 创建对应积分批次。
4. 写入不可变积分流水。
5. 写入包含操作员、角色、门店和客户端 IP 的审计记录。

任何一步失败都会回滚。店长或店员提交无权操作的 `storeId` 时返回 HTTP 403。

## 4. 联调前置数据

开始测试前，数据库中至少需要：

- 一条状态为 `ACTIVE` 的商户和门店。
- 一条已绑定手机号、状态为 `ACTIVE` 的客户。
- 店长或店员还需要在 `t_sys_user_store` 中绑定门店；超级管理员不需要绑定。

客户正式来源仍然是后续微信 H5 登录注册流程，本阶段不增加后台创建客户接口。

本地数据库没有上述数据时，可以手工执行：

~~~text
dev/sql/seed-local-demo-data.sql
~~~

该脚本只用于本地开发，不属于 Flyway/生产迁移，包含公开的演示账号密码，禁止在生产环境执行。重复执行不会覆盖客户已经充值后的积分余额。
