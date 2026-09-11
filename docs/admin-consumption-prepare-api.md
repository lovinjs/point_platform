# 后台创建待确认消费订单联调说明

本接口只创建待客户确认的消费订单，不扣减积分，也不提供绕过客户授权的直接消费接口。客户待确认查询和本人确认扣款接口现已实现，完整联调见 `docs/customer-consumption-confirm-api.md`。本阶段不需要执行新的数据库迁移脚本。

## 接口

~~~http
POST /api/v1/admin/consumption-orders/prepare
Authorization: Bearer <accessToken>
Idempotency-Key: e33ca3c5-bd22-424d-b98b-93b91c0cbb79
Content-Type: application/json

{
  "customerId": 1,
  "storeId": 1,
  "consumePoints": 30,
  "remark": "客户现场消费"
}
~~~

成功响应中的金额单位为“分”。平台费率保持默认 5%、消费 30 积分时：

~~~text
grossAmountCent       = 3000
platformFeeRateBps    = 500
platformFeeCent       = 150
storePayableCent      = 2850
orderStatus           = PENDING_CONFIRM
verificationMode      = CUSTOMER_PIN
~~~

## 规则

- 操作员取自当前后台 JWT，请求体不能指定操作人。
- 超级管理员可以操作全部有效门店，店员和店长只能操作已绑定门店。
- 客户必须处于有效状态、已经绑定手机号并且已经设置消费密码；否则不能创建待确认订单。
- 创建时预检查客户积分，但不会提前扣减或冻结积分；客户确认时必须再次校验余额。
- 一个客户同时最多存在一笔 `PENDING_CONFIRM` 订单。
- 待确认订单默认 5 分钟过期，超级管理员可在后台“系统设置 → 业务参数”中调整，范围为 1 至 30 分钟。修改只影响之后新建的订单。
- 过期订单会在相关业务请求到达时更新为 `EXPIRED`，不能再确认。
- 同一请求重试必须保持 `Idempotency-Key` 和请求体一致；新的消费必须使用新的键。
- 创建订单时保存当前平台费率、手续费和门店应付金额快照，后续修改费率不会改变已有待确认订单和历史订单。

## 创建后的处理

创建成功后，订单进入 `PENDING_CONFIRM`，积分余额不会在这一接口中变化。消费者在 H5 的“待确认消费”页面核对门店和金额并输入自己的消费密码；确认成功后才会扣减积分并将订单变为 `COMPLETED`。

客户确认接口只从客户登录凭证读取身份，不接受前端提交 `customerId`。创建新订单时应使用新的 `Idempotency-Key`，不要重复使用已经完成或过期订单的键。
