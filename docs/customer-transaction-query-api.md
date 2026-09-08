# 客户账单与订单记录联调说明

本阶段提供客户积分余额、积分流水、充值订单和消费订单查询。所有接口只读取客户 JWT 中的身份，不接受 `customerId`，并按当前客户严格隔离数据。本阶段只使用 V1 已有表和索引，不需要新增 Flyway 脚本。

## 接口

~~~http
GET /api/v1/customer/points/balance
GET /api/v1/customer/points/ledger?pageNum=1&pageSize=20
GET /api/v1/customer/recharge-orders?pageNum=1&pageSize=20
GET /api/v1/customer/consumption-orders?pageNum=1&pageSize=20
Authorization: Bearer <customerAccessToken>
~~~

分页从 `1` 开始，`pageSize` 允许 `1`～`50`，默认值为 `20`。分页响应统一为：

~~~json
{
  "pageNum": 1,
  "pageSize": 20,
  "total": 2,
  "totalPages": 1,
  "hasNext": false,
  "items": []
}
~~~

## 积分流水

积分流水按照 `createTime` 和主键倒序排列。每条记录包含：

- 积分增减值 `deltaPoints`。
- 该笔业务完成后的余额 `balanceAfter`。
- 流水类型、业务类型及业务订单号。
- 相关门店名称和备注。

积分流水是客户核对实际余额变化的权威记录。待确认、已过期或已取消的消费订单不会产生消费流水。

## 充值与消费订单

充值记录返回充值门店、积分、金额、渠道、支付方式、状态及业务时间，不向客户端返回平台收款参考号、后台操作员和幂等键。

消费记录返回消费门店、积分、金额、状态、过期时间和完成时间。查询消费记录前会把该客户已经超过有效期的 `PENDING_CONFIRM` 订单更新为 `EXPIRED`，避免页面继续显示为待确认。

## H5 页面

客户登录后可从首页进入“账单记录”，页面包含：

- 积分明细：展示充值、消费、充值退款以及未来调整和冲正产生的余额变化。
- 充值记录：展示充值金额、到账积分和订单状态。
- 消费记录：展示门店、消费金额、积分和订单状态。

页面支持下拉刷新和触底分页。切换标签会重新读取第一页，进入页面时会同步刷新最新积分余额。

修改了 `pages.json` 后，需要停止并重新运行 HBuilderX 的 H5 项目；重启 Spring Boot 后，新接口会出现在 Swagger 的“积分平台 - 客户账单与订单”分组中。
