# 消费密码后端核心设计

## 当前实现范围

消费密码领域服务和客户 HTTP 接口已经实现。接口统一放在 `/api/v1/customer/**` 下，由客户 JWT 保护，H5 与未来小程序共用；请求体不接受 `customerId`，客户身份只从登录凭证读取。

已有能力：

- 查询当前消费者是否已设置消费密码，以及是否处于锁定状态。
- 首次设置消费密码。
- 使用原密码修改消费密码。
- 核销前校验消费密码。
- 连续输错计数及临时锁定。
- 设置、修改和触发锁定的安全审计。

## 密码策略

- 固定为 6 位数字，使用字符串传递，避免丢失开头的 `0`。
- 禁止全相同数字及明显连续数字，例如 `000000`、`123456`、`654321`。
- 使用项目统一的 BCrypt 编码器保存哈希，不保存明文，也不在日志或审计快照中保存密码和哈希。
- 默认连续输错 5 次后锁定 30 分钟。
- 锁定次数和时长可以通过环境变量调整：
  - `PLATFORM_CUSTOMER_PIN_MAX_FAILED_ATTEMPTS`
  - `PLATFORM_CUSTOMER_PIN_LOCK_DURATION`

## 数据库

V1 基线脚本已经包含 `t_customer_security`，本阶段不需要执行新 SQL。使用字段：

- `consume_pin_hash`
- `failed_count`
- `locked_until`
- `pin_updated_time`

## 事务边界

密码记录通过 `SELECT ... FOR UPDATE` 串行修改。密码校验和错误计数使用独立事务，确保核销失败或外层事务回滚时，错误次数及锁定状态仍然有效。

消费确认已接入密码校验服务。密码校验成功后再进入独立的积分扣减事务；扣减事务会重新锁定积分账户和订单，并检查订单归属、状态、有效期、门店状态及余额，不能依赖创建待确认订单时的余额检查。

## 客户接口

```text
GET  /api/v1/customer/security/consume-pin/status
POST /api/v1/customer/security/consume-pin
PUT  /api/v1/customer/security/consume-pin
```

首次设置请求：

```json
{
  "newPin": "258369"
}
```

修改密码请求：

```json
{
  "currentPin": "258369",
  "newPin": "369258"
}
```

设置和修改密码要求当前客户已经绑定手机号。查询状态不要求绑定手机号，便于客户端展示账户安全状态。成功后返回最新的 `configured`、`locked`、`lockedUntil` 和 `pinUpdatedTime`。

忘记密码功能后续采用“微信登录状态 + 绑定手机号验证码 + 一次性重置凭证”，并在重置成功后取消该消费者全部待确认消费订单。本阶段不提供店员或管理员代设密码接口。

## 消费确认

当前客户待确认订单查询和本人输入密码确认扣款已经实现，详见 `docs/customer-consumption-confirm-api.md`。前端不保存消费密码，后端日志、积分流水和审计快照也不记录密码或密码哈希。
