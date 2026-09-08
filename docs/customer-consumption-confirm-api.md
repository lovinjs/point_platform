# 客户待确认消费与确认扣款联调说明

本阶段已打通“门店发起待确认订单—消费者 H5 获取订单—本人输入消费密码—完成积分扣减”的闭环。V1 基线数据库已经包含消费订单、积分批次占用和流水表，不需要新增 Flyway 脚本。

## 前置条件

- 客户已通过微信 H5 登录，并绑定手机号、设置消费密码。
- 客户已通过线下充值获得足够积分；充值必须由现有服务产生积分批次，不能只手工修改 `t_point_account`。
- 门店和后台操作员均有效，店员或店长已经绑定对应门店。
- 后台先调用 `POST /api/v1/admin/consumption-orders/prepare` 创建订单，订单状态为 `PENDING_CONFIRM`。

后台按手机号查询客户时会返回 `consumePinConfigured`。该值为 `false` 时应先引导客户设置消费密码，后台不能为其创建待确认订单。

## 1. 查询待确认订单

~~~http
GET /api/v1/customer/consumption-orders/pending
Authorization: Bearer <customerAccessToken>
~~~

有待确认订单时，`data` 包含：

~~~json
{
  "orderNo": "CSM202609081234567890",
  "storeId": 1,
  "storeName": "演示门店",
  "consumePoints": 30,
  "amountCent": 3000,
  "expiresTime": "2026-09-08T14:05:00",
  "orderStatus": "PENDING_CONFIRM",
  "remark": "客户现场消费",
  "createTime": "2026-09-08T14:00:00"
}
~~~

没有待确认订单时请求仍然成功，`data` 为 `null`。H5 页面仅在前台每 3 秒查询一次，也支持手动刷新和下拉刷新；页面隐藏或离开后停止轮询。

## 2. 本人确认消费

~~~http
POST /api/v1/customer/consumption-orders/{orderNo}/confirm
Authorization: Bearer <customerAccessToken>
Content-Type: application/json

{
  "consumePin": "258369"
}
~~~

`customerId` 不在路径或请求体中，后端只使用客户 JWT 中的身份。成功响应示例：

~~~json
{
  "orderNo": "CSM202609081234567890",
  "storeId": 1,
  "storeName": "演示门店",
  "consumePoints": 30,
  "amountCent": 3000,
  "availablePoints": 70,
  "orderStatus": "COMPLETED",
  "completedTime": "2026-09-08T14:02:00"
}
~~~

确认事务会重新检查订单归属、订单状态、有效期、门店状态和实时积分余额，然后：

1. 按充值时间从早到晚扣减 `t_point_lot`。
2. 在 `t_point_lot_usage` 记录本次订单实际用了哪些充值批次。
3. 扣减 `t_point_account` 并写入 `CONSUME` 类型的 `t_point_ledger`。
4. 将消费订单更新为 `COMPLETED`，记录确认和完成时间。
5. 写入不含消费密码的客户确认审计记录。

上述写入在同一个数据库事务内，任一步失败都会整体回滚。已成功完成的订单因网络超时再次提交时只返回现状，不会重复扣减积分。

## 3. 主要失败场景

- `30023`：确认时实时积分余额不足。创建订单时不会冻结积分，需要门店重新核对后再发起。
- `30030`：消费密码错误；连续输错达到上限后进入锁定。
- `30031`：消费密码已锁定，默认 30 分钟后再试。
- `30053`：订单不存在，或订单不属于当前登录客户。
- `30054`：订单当前状态不允许确认。
- `30055`：订单已过期，HTTP 状态为 410，需要门店使用新的 `Idempotency-Key` 重新发起。

## 4. H5 联调步骤

1. 重启 Spring Boot，使新接口和 Swagger 文档生效。
2. 停止并重新运行 HBuilderX 的 H5 项目，因为本阶段修改了 `pages.json`。
3. 客户在 H5 登录，确认手机号已绑定、消费密码已设置且积分足够。
4. 后台创建一笔待确认消费订单。
5. 客户从首页进入“待确认消费”；已打开页面时最多等待约 3 秒即可看到新订单。
6. 核对门店、金额和积分，输入消费密码并确认。
7. 检查页面成功回执和最新余额；数据库中对应订单应为 `COMPLETED`，并出现积分批次占用、消费流水和审计记录。

消费完成后不提供普通退款。未来若发生确需处理的系统异常，应走独立的超级管理员冲正流程，不能直接修改余额或把订单改回待确认状态。
