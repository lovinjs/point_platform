# 平台业务参数接口

本阶段把影响消费订单的新单规则集中到平台业务参数中。接口只允许超级管理员访问。

## 当前规则

- 充值兑换固定为 1 元 = 1 积分，不可修改。
- 积分不过期。
- 赠送积分暂不启用，不可修改。
- 平台手续费率默认 500 基点，即 5%。
- 待消费订单确认有效期默认 5 分钟，可配置范围为 1 至 30 分钟。

手续费率和确认有效期只影响修改后新创建的消费订单。订单创建时会保存费率、手续费、门店应付金额和过期时间，已有待确认订单及历史订单不会被重新计算。

## 查询配置

~~~http
GET /api/v1/admin/platform-settings/business
Authorization: Bearer <access-token>
~~~

响应中的 `platformFeeRateBps` 使用基点表示：100 基点 = 1%，500 基点 = 5%。`version` 必须在保存时原样回传。

## 修改配置

~~~http
PUT /api/v1/admin/platform-settings/business
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "platformFeeRateBps": 600,
  "consumptionPendingTtlMinutes": 5,
  "version": 0,
  "changeReason": "根据新签署的合作协议调整平台手续费"
}
~~~

修改原因必填，最多 500 个字符。若页面版本落后于数据库，接口返回数据冲突，后台页面会重新加载最新配置，避免覆盖其他管理员刚保存的内容。

成功修改后会写入 `PLATFORM_BUSINESS_SETTING_UPDATED` 审计日志，包含修改人、原因、客户端 IP，以及修改前后的完整业务参数快照。

## 数据库升级

Flyway 会自动执行 `V6__add_platform_business_setting.sql`，创建全局配置表并写入默认值。不要手工重复执行该脚本。

原环境变量 `PLATFORM_CONSUMPTION_PENDING_TTL` 已不再使用，消费确认有效期统一由数据库配置管理。
