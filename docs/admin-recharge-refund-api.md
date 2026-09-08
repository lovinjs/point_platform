# 后台未消费充值整笔退款联调说明

本接口用于超级管理员在实际资金已经退还给客户后，登记退款并整笔收回该充值产生的积分。它不会调用银行、微信或其他支付渠道自动打款。生产环境必须先确认真实退款成功并取得退款交易流水号，再调用本接口。

V1 已经包含 `t_recharge_refund` 和积分批次字段，不需要新增 Flyway 脚本。

## 退款条件

- 仅 `SUPER_ADMIN` 可以操作，店员和店长均返回 HTTP 403。
- 原充值订单必须为 `COMPLETED`。
- 只能整笔退款，退款金额和积分由原充值订单计算，请求体不能指定。
- 对应积分批次必须保持完整，`remaining_points = total_points` 且状态为 `AVAILABLE`。
- 对应积分批次从未出现在 `t_point_lot_usage`。即使以后通过异常冲正归还积分，发生过消费的充值仍不能走普通退款。
- 退款方式、实际退款交易参考号和原因必须填写。
- 一笔充值最多生成一笔退款记录。

## 接口

~~~http
POST /api/v1/admin/recharge-orders/{orderNo}/refund
Authorization: Bearer <superAdminAccessToken>
Idempotency-Key: <本次退款的新UUID>
Content-Type: application/json

{
  "refundMethod": "OTHER",
  "refundReference": "LOCAL-REFUND-20260908-001",
  "reason": "客户申请退回未消费充值"
}
~~~

退款方式：

- `ORIGINAL_CHANNEL`：已通过原支付渠道实际退回；`refundReference` 填退款交易号，不能重复填写原充值交易号。
- `BANK_TRANSFER`：平台通过银行转账退款；填写银行退款流水号。
- `OTHER`：其他线下退款方式，本地联调可以使用。

`refundReference` 在相同退款方式下必须唯一。调用超时重试时保持 `Idempotency-Key`、订单号和请求体完全一致；新的退款必须生成新的键。

## 成功结果

~~~json
{
  "refundNo": "RFD202609081800001234",
  "rechargeOrderNo": "RCH202609081700001234",
  "customerId": 2,
  "refundPoints": 100,
  "refundAmountCent": 10000,
  "refundMethod": "OTHER",
  "refundStatus": "COMPLETED",
  "availablePoints": 970,
  "completedTime": "2026-09-08T18:00:00"
}
~~~

成功后会在一个数据库事务中：

1. 创建状态为 `COMPLETED` 的 `t_recharge_refund`。
2. 将对应 `t_point_lot` 标记为 `REFUNDED`。
3. 从 `t_point_account` 整笔扣回充值积分。
4. 将原 `t_recharge_order` 标记为 `REFUNDED`。
5. 写入负向 `REFUND` 积分流水和超级管理员审计记录。

任一步失败都会整体回滚。消费者刷新“账单记录”后，会看到原充值订单变为“已退款”，积分明细新增一笔“充值退款”。

## 建议联调步骤

你之前充值的 1000 积分已经消费过 30，按照规则不能退款，调用时应返回 `30059`。测试成功退款时：

1. 再为同一客户充值 100 积分，使用新的充值收款参考号和幂等键。
2. 不要消费这 100 积分，记下新充值响应中的 `orderNo`。
3. 先模拟已在线下向客户退回 100 元，再使用新的退款参考号调用本接口。
4. 成功后客户余额应回到退款前减 100 的数值；刷新 H5 账单确认充值状态和退款流水。

常见业务错误：

- `30057`：充值订单不存在。
- `30058`：订单已经退款或当前状态不允许退款。
- `30059`：该充值积分已被全部或部分消费。
- `30023`：积分账户余额异常不足。
- `30061`：退款交易参考号已经登记。
