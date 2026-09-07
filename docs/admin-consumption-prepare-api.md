# 后台创建待确认消费订单联调说明

本阶段只创建待客户确认的消费订单，不扣减积分，不提供绕过客户授权的直接消费接口，也不需要执行新的数据库迁移脚本。

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

成功响应中的金额单位为“分”。消费 30 积分时：

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
- 创建时预检查客户积分，但不会提前扣减或冻结积分；客户确认时必须再次校验余额。
- 一个客户同时最多存在一笔 `PENDING_CONFIRM` 订单。
- 待确认订单默认 5 分钟过期，可通过 `PLATFORM_CONSUMPTION_PENDING_TTL` 调整，最大 30 分钟。
- 过期订单会在相关业务请求到达时更新为 `EXPIRED`，不能再确认。
- 同一请求重试必须保持 `Idempotency-Key` 和请求体一致；新的消费必须使用新的键。
- 创建订单时保存 5% 平台手续费和 95% 门店应付金额快照，后续修改费率不会改变历史订单。

## 当前阶段边界

现在可以验证订单是否正确进入 `t_consumption_order` 和 `t_audit_log`。积分余额不会在这一接口中变化。

下一阶段实现客户消费密码设置、失败锁定、待确认订单查询和确认扣款。在客户身份认证完成前，不会开放接受前端 `customerId` 的确认接口。
