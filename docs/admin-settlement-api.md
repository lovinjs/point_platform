# 门店自然月结算接口

本阶段完成“平台生成月账单—店长确认—平台登记付款”的后台核心闭环。结算金额直接汇总消费订单完成时固化的金额快照，不按生成账单时的费率重新计算。

## 数据库迁移

重启后端后，Flyway 会自动执行 `V4__add_settlement_payment_reference_unique.sql`，为结算付款参考号增加唯一约束。不要手工执行或修改已经应用的迁移脚本；可在 `flyway_schema_history` 中确认 V4 成功。

## 状态流转

```text
消费订单：NOT_INCLUDED -> INCLUDED -> SETTLED

门店结算单：GENERATED -> CONFIRMED -> PAID
                  店长确认       平台登记付款
```

- 只统计指定自然月内状态为 `COMPLETED`、结算状态为 `NOT_INCLUDED` 的消费订单。
- 只能生成已经结束的自然月，不能提前生成当前月或未来月份。
- 同一个月份重复调用生成接口会返回已生成结果，不会重复纳入消费订单。
- 某门店的结算单登记付款后，其消费订单才从 `INCLUDED` 变为 `SETTLED`。
- 金额字段均以“分”为单位；积分字段为整数。

## 权限范围

| 操作 | 超级管理员 | 店长 | 店员 |
| --- | --- | --- | --- |
| 生成自然月结算单 | 可以 | 不可以 | 不可以 |
| 查询结算单 | 全部门店 | 仅负责门店 | 不可以 |
| 确认结算单 | 不代替门店确认 | 仅负责门店 | 不可以 |
| 登记付款 | 可以 | 不可以 | 不可以 |

服务层会再次读取数据库中的账号、角色和门店关系，不只依赖登录令牌中的权限信息。

## 1. 生成月度结算单

```http
POST /api/v1/admin/settlement-periods/2026-08/generate
Authorization: Bearer <超级管理员 accessToken>
```

无需请求体。响应中按门店返回消费积分、消费总额、平台手续费和门店应付金额。当前月没有结束时会返回 `30064`；月份内没有可结算订单时返回 `30065`。

## 2. 查询结算单列表

```http
GET /api/v1/admin/settlements?pageNum=1&pageSize=20&periodCode=2026-08&status=GENERATED
Authorization: Bearer <后台 accessToken>
```

`periodCode` 和 `status` 均可省略。`pageSize` 最大为 50。店长的结果会自动限制为其负责的门店。

## 3. 查询结算单明细

```http
GET /api/v1/admin/settlements/{settlementNo}
Authorization: Bearer <后台 accessToken>
```

明细包含本结算单纳入的消费订单号、消费完成时间、积分、消费金额、平台手续费和门店应付金额。

## 4. 店长确认

```http
POST /api/v1/admin/settlements/{settlementNo}/confirm
Authorization: Bearer <店长 accessToken>
Content-Type: application/json

{
  "remark": "金额核对无误"
}
```

请求体可以省略。只有负责该门店的店长可以确认；重复确认会直接返回当前账单，不重复写入。

## 5. 超级管理员登记付款

平台完成实际转账后，再调用此接口登记，不由系统接口直接发起银行付款：

```http
POST /api/v1/admin/settlements/{settlementNo}/mark-paid
Authorization: Bearer <超级管理员 accessToken>
Content-Type: application/json

{
  "paymentReference": "BANK-SETTLEMENT-202608-001",
  "remark": "已通过平台对公账户转账"
}
```

`paymentReference` 必填且全局唯一。只有已经由店长确认的账单可以登记付款；使用相同付款参考号重试同一账单会安全返回，换用其他参考号重复付款会被拒绝。

生成、确认和付款都会写入 `t_audit_log`。账单已付款后暂不提供回退接口，若实际付款登记错误，应先人工核对并在后续实现专门的财务纠错流程，不能直接修改数据库。
