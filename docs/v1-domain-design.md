# 积分平台 V1 领域设计

## 1. 设计目标

本版本将现有单商城项目逐步重构为多门店积分平台。V1 只实现最小业务闭环：

- 用户充值获得平台通用积分，1 元对应 1 个积分。
- 用户可以在任意合作门店消费通用积分。
- 消费门店按照消费金额参与平台结算。
- 平台抽取消费结算金额的 5%，门店获得 95%。
- 支持线下充值、后台创建消费订单和用户消费密码确认。
- 充值积分不过期。
- 消费完成后不支持普通退款；未消费的充值积分可以退款。

V1 暂不实现：

- 门店赠送积分和其他营销奖励。
- 微信支付。
- 商品、商品分类、购物车和物流订单。
- 多门店店长的前端操作界面。
- 自动银行付款和自动财务对账。

现有商城模块不做数据迁移，测试数据可以舍弃。新业务采用新表、新包和新接口，旧商城代码暂时保留，待新业务稳定后清理。

## 2. 领域边界

~~~text
合作商户 Merchant
    └── 门店 Store
          └── 员工门店权限 StaffStoreScope

客户 Customer
    └── 平台积分账户 PointAccount
          ├── 积分批次 PointLot
          └── 积分流水 PointLedger

充值订单 RechargeOrder
    └── 增加客户通用积分

消费订单 ConsumptionOrder
    └── 扣除客户通用积分
          └── 生成门店待结算金额

结算周期 SettlementPeriod
    └── 门店结算单 StoreSettlement
          └── 结算明细 StoreSettlementItem
~~~

平台积分是客户侧的权益记录。门店不拥有客户积分，也不在门店之间转移积分。消费发生时，只记录消费门店和平台对该门店的应付金额。

## 3. 业务规则

### 3.1 充值

- 充值订单记录客户、充值金额、充值积分、充值渠道、操作员和充值门店。
- V1 只有线下充值渠道，后续增加微信支付渠道。
- 线下充值必须由后台员工发起，不能由前端直接修改余额。
- V1 的“线下充值”表示不接支付回调、由员工人工核对平台收款记录，不表示充值款可以长期留在门店。
- 建议 V1 只允许款项直接进入平台对公收款渠道，并记录支付方式和唯一交易参考号，防止同一收款凭证重复充值。
- 已确认 V1 所有线下充值款都直接进入平台账户，门店不代收充值款；后台充值服务固定 fund_receiver = PLATFORM，调用方不能自行指定收款方。
- 如果后续允许门店收取现金或使用门店自己的收款码，必须先增加门店代收款、应收款和缴款对账模型，否则会出现 A 店收款、B 店消费而平台需要垫付结算资金的问题。
- V1 充值金额按整元处理，amount_cent 必须填写且为 100 的整数倍；recharge_points = amount_cent / 100。
- 充值订单完成后，增加一条正向积分流水和一个积分批次。
- 充值订单支持幂等，重复点击不能重复增加积分。

### 3.2 消费

- 消费订单必须绑定客户和消费门店。
- 消费金额等于消费积分乘以 1 元。
- 消费完成后，在一个数据库事务中完成订单、积分批次扣减、积分流水写入。
- 消费门店不能由前端任意指定，后台必须校验员工是否有该门店权限。
- 消费完成后不允许普通退款。
- 如果 H5 或网络整体不可用，V1 不执行离线消费；不得先记账后补扣积分。
- 对于重复扣减、操作错误等异常情况，保留超级管理员发起冲正的能力。冲正不是普通退款，必须单独记录原因和操作信息，不需要二次审批。

V1 消费只支持客户消费密码确认。

- 员工只能创建待确认消费订单，不能绕过客户授权直接完成消费。
- 消费密码必须在客户自己的设备上输入，不建议由客户口头告知店员密码。
- 消费二维码作为后续扩展方式，暂不进入 V1 操作流程。


### 3.3 积分批次和退款

每一笔充值产生一个充值积分批次。消费记录必须关联实际扣除的批次。

V1 采用先进先出方式使用充值积分：

- 消费时优先扣除最早的未消费充值批次。
- V1 只允许整笔充值订单退款，不支持部分退款。
- 只有该充值订单从未发生过积分消费时，才允许整笔退款。
- 退款会减少积分账户余额，增加一条退款流水，并生成退款记录。
- 消费订单不能被修改为未消费状态。

即使 V1 没有赠送积分，也建议保留积分批次设计，因为“未消费充值积分可退款”需要知道每一笔充值还剩多少。

### 3.4 结算

V1 统一按以下公式计算：

~~~text
消费积分 = P
消费金额（分） = P × 100
平台手续费 = 消费金额 × 5%
门店应结算金额 = 消费金额 - 平台手续费
~~~

例如消费 100 个积分：

~~~text
消费金额：100.00 元
平台手续费：5.00 元
门店应结算：95.00 元
~~~

订单完成时保存费率和金额快照，不根据未来变更后的费率重新计算历史订单。

结算只统计已完成且未冲正的消费订单，不根据客户积分余额计算。如果消费在结算单关闭后才被超级管理员冲正，不回写历史结算单，而是在下一结算周期生成负向 REVERSAL_ADJUSTMENT 明细。

### 3.5 资金和合规边界

业务设计暂按“平台统一收取充值资金、平台根据已完成消费向门店结算”的商业假设建模，但技术实现不能据此直接认定业务已经合规。

- 充值金额、客户未消费积分和平台待结算门店金额必须分别记账。
- 未消费充值不应简单作为平台已实现收入，具体收入确认和采购成本由财务确认。
- 门店结算同时保存消费金额、平台费率、平台手续费和门店应付金额。
- 合作协议、用户协议、退款规则、发票和实际资金流必须保持一致。
- 上线真实资金业务前，需要由熟悉支付、预付消费和平台业务的律师及会计确认主体关系、资金存管/清分和税务处理。
- 若采用微信支付或其他在线渠道，优先评估持牌机构、银行或支付机构提供的商户入驻、定向清分和分账方案，不把平台普通账户作为长期资金池。

因此，本设计只固化业务账务和结算数据，不对“采购服务”模式作最终法律定性。

## 4. 角色和数据权限

普通用户是客户身份，不属于后台角色。后台只设置以下三类角色：

| 角色 | 主要权限 | 数据范围 |
|---|---|---|
| 店员 | 查询客户、发起充值、发起消费、查看交易记录 | 已绑定门店 |
| 店长 | 店员权限、查看门店统计、查看门店结算单、提交异常处理申请 | 当前绑定门店 |
| 超级管理员 | 商户、门店、员工、客户、积分、订单、结算和系统配置 | 全部数据 |

店长当前只绑定一个门店，但数据库使用员工与门店关联表，未来可以扩展为多个门店。

关键权限必须在后端校验：

- 前端传来的 storeId 不能直接信任。
- 员工只能操作自己有权限的门店。
- 普通用户只能查看和确认自己的数据。
- 客户余额只能通过积分领域服务变更。
- 消费不能由店员绕过客户授权直接完成。
- 退款、冲正、费率配置等高风险操作不能由店员直接完成。

## 5. V1 数据表建议

### 5.1 客户和员工

#### customer_user

客户基本信息：

- id
- phone
- nickname
- avatar
- status
- last_login_time
- create_time
- update_time

phone 允许先为空，后续微信 H5 再完成手机号绑定。手机号建立唯一索引，但要允许未绑定手机号的客户存在。

#### customer_identity

用于支持微信 H5 和未来小程序：

- id
- customer_id
- provider_code
- app_id
- external_id
- union_id
- create_time
- update_time

建议唯一约束为 provider_code + app_id + external_id。同一个用户在微信 H5 和小程序中可能有不同 openId，因此 app_id 不能省略；不要把 openId 直接放入客户主表。

#### customer_security

用于客户消费密码：

- customer_id
- consume_pin_hash
- failed_count
- locked_until
- pin_updated_time
- update_time

密码只保存哈希值，不能保存明文。

消费密码领域服务和客户接口已实现首次设置、修改、校验、连续输错锁定和安全审计。微信 H5 登录与手机号绑定已完成；接口中的 customerId 来自登录凭证，不能接受前端自行指定。

#### sys_user

后台员工账号：

- id
- username
- phone
- password_hash
- status
- failed_login_count
- locked_until
- token_version
- password_updated_time
- last_login_time
- create_time
- update_time

后台账号使用 BCrypt 或 Argon2，不继续使用固定盐 MD5。

后台连续登录失败默认 5 次后锁定 15 分钟。token_version 用于在密码修改、账号禁用或管理员主动撤销登录时使旧令牌立即失效。

#### sys_role、sys_user_role、sys_user_store

分别记录角色、员工角色关系和员工门店范围。

V1 角色编码：

- CLERK
- STORE_MANAGER
- SUPER_ADMIN

超级管理员可以不绑定门店，通过角色获得全局数据范围。

### 5.2 商户和门店

#### merchant

- id
- merchant_code
- legal_name
- business_name
- unified_social_credit_code
- contact_name
- contact_phone
- status
- create_time
- update_time

#### store

- id
- merchant_id
- store_code
- store_name
- address
- contact_phone
- status
- create_time
- update_time

store_code 必须唯一。消费和结算始终绑定 store_id；充值也记录 recharge_store_id，用于审计和后续渠道奖励扩展。

### 5.3 积分

#### point_account

每个客户一条：

- id
- customer_id
- available_points
- version
- create_time
- update_time

available_points 是查询优化字段，不是唯一事实来源。更新时必须锁定账户行。

#### point_lot

每一笔充值一个批次：

- id
- customer_id
- source_recharge_order_no
- total_points
- remaining_points
- lot_status
- create_time
- update_time

#### point_lot_usage

记录消费订单实际使用的充值批次：

- id
- consumption_order_no
- point_lot_id
- used_points
- create_time

#### point_ledger

不可变积分流水：

- id
- ledger_no
- customer_id
- delta_points
- balance_after
- ledger_type
- business_type
- business_no
- operator_id
- store_id
- idempotency_key
- remark
- create_time

V1 ledger_type：

- RECHARGE
- CONSUME
- REFUND
- ADJUSTMENT
- REVERSAL

任何积分变化都必须通过积分领域服务写入流水，不能由业务代码直接 update point_account。

### 5.4 充值和消费

#### recharge_order

- id
- order_no
- customer_id
- recharge_store_id
- recharge_points
- amount_cent
- channel
- payment_method
- fund_receiver
- payment_reference
- order_status
- operator_id
- paid_time
- completed_time
- idempotency_key
- remark
- create_time
- update_time

V1 channel 为 OFFLINE。未来增加 WECHAT_PAY。

V1 order_status：

- CREATED
- COMPLETED
- REFUNDED
- CANCELLED

V1 退款校验：refund_points 必须等于 recharge_points，且对应充值批次不存在任何 point_lot_usage 记录；不支持部分退款。

#### recharge_refund

- id
- refund_no
- recharge_order_no
- customer_id
- refund_points
- refund_amount_cent
- refund_method
- refund_reference
- refund_status
- operator_id
- reason
- completed_time
- idempotency_key
- create_time
- update_time

退款记录独立保存，避免修改原充值订单的历史事实。线下退款也必须记录退款方式和交易参考号，不能只修改积分而不留下实际退款凭证。

#### consumption_order

- id
- order_no
- customer_id
- store_id
- consume_points
- gross_amount_cent
- platform_fee_rate_bps
- platform_fee_cent
- store_payable_cent
- verification_mode
- expires_time
- order_status
- operator_id
- confirmed_time
- completed_time
- settlement_status
- idempotency_key
- remark
- create_time
- update_time

V1 verification_mode：

- CUSTOMER_PIN

V1 order_status：

- PENDING_CONFIRM
- COMPLETED
- CANCELLED
- EXPIRED
- REVERSED

V1 settlement_status：

- NOT_INCLUDED
- INCLUDED
- SETTLED
- ADJUSTED

### 5.5 结算和审计

#### settlement_period

- id
- period_code
- start_date
- end_date
- period_status
- frozen_time
- generated_time
- create_time
- update_time

V1 按自然月生成结算周期，period_code 使用 YYYY-MM，周期范围为当月第一天至当月最后一天。

period_status：

- OPEN
- FROZEN
- GENERATED
- CONFIRMED
- PAID
- CLOSED

#### store_settlement

- id
- settlement_no
- period_id
- merchant_id
- store_id
- total_consume_points
- gross_amount_cent
- platform_fee_cent
- adjustment_amount_cent
- payable_amount_cent
- settlement_status
- confirmed_time
- paid_time
- payment_reference
- remark
- create_time
- update_time

#### store_settlement_item

- id
- settlement_id
- item_type
- consumption_order_id
- points_delta
- gross_amount_cent
- platform_fee_cent
- store_payable_cent
- adjustment_reason
- create_time

结算明细金额允许为负数，以便将已结算月份之后发生的冲正在下一结算周期中记为调整项；不修改已经关闭的历史结算单。

#### audit_log

记录充值、消费、退款、冲正、权限变更和结算确认：

- operator_id
- operator_role
- store_id
- action
- resource_type
- resource_no
- request_id
- before_snapshot
- after_snapshot
- remark
- client_ip
- create_time

### 5.6 数据库约束原则

- 新表统一使用 t_ 前缀，与当前 MyBatis Plus 配置一致。
- 财务订单、积分流水、批次使用和结算明细不做物理删除，也不依赖旧商城的逻辑删除字段。
- 同一客户最多只能有一笔 PENDING_CONFIRM 消费订单，数据库通过生成列和唯一索引兜底；服务层创建新订单前仍需主动处理已过期订单。
- MySQL 5.7 不可靠执行 CHECK 约束，因此正积分、金额换算、费率计算、状态流转和关联客户一致性必须由领域服务校验，并通过事务测试覆盖。
- 外部请求和积分流水均保存幂等键；订单号、交易参考号、结算单号建立唯一约束。
- 历史资金和积分事实使用 RESTRICT 外键，不级联删除。

## 6. 接口边界

接口前缀建议：

- /api/v1/h5：客户端
- /api/v1/admin：后台管理端

### 6.1 后台接口

~~~text
POST /api/v1/admin/auth/login
GET  /api/v1/admin/customers?phone=
GET  /api/v1/admin/stores
GET  /api/v1/admin/staff

POST /api/v1/admin/recharge-orders/offline
GET  /api/v1/admin/recharge-orders
POST /api/v1/admin/recharge-orders/{orderNo}/refund

POST /api/v1/admin/consumption-orders/prepare
POST /api/v1/admin/consumption-orders/{orderNo}/cancel
GET  /api/v1/admin/consumption-orders

GET  /api/v1/admin/settlements
GET  /api/v1/admin/settlements/{settlementNo}
POST /api/v1/admin/settlements/{settlementNo}/confirm
POST /api/v1/admin/settlements/{settlementNo}/mark-paid
~~~

### 6.2 客户端接口

~~~text
GET  /api/v1/customer/me
POST /api/v1/customer/phone/verification-codes
PUT  /api/v1/customer/phone
GET  /api/v1/customer/security/consume-pin/status
POST /api/v1/customer/security/consume-pin
PUT  /api/v1/customer/security/consume-pin
GET  /api/v1/customer/points/balance
GET  /api/v1/customer/points/ledger
GET  /api/v1/customer/recharge-orders
GET  /api/v1/customer/consumption-orders
GET  /api/v1/customer/consumption-orders/pending
POST /api/v1/customer/consumption-orders/{orderNo}/confirm
~~~

微信 H5 登录、短信验证码绑定手机号、消费密码配置、消费确认以及客户账单查询已经实现。除渠道登录过程外，客户业务接口统一使用 `/api/v1/customer/**`，由 H5 和未来小程序共用；不同渠道只增加各自的 customer_identity。

涉及写入的接口建议支持 Idempotency-Key，后端将其保存到对应订单或流水表，并建立唯一约束。

### 6.3 待消费订单到达 H5

V1 不要求用户手动刷新页面，采用“客户已登录 H5 + 短轮询”的方式：

1. 员工在后台按手机号选择客户并填写消费积分，调用 prepare 接口。
2. 后端创建 PENDING_CONFIRM 消费订单，写入客户、门店、积分数量、确认方式和过期时间。
3. 客户打开消费确认页后，H5 调用 pending 接口查询自己的待确认订单。
4. 页面处于前台时，每 3 秒自动查询一次；发现新订单后展示门店、消费金额、消费积分和剩余确认时间。
5. 客户输入消费密码。
6. 后端校验订单归属、消费密码、有效期和订单状态后完成消费。
7. 消费完成、取消或过期后，H5 停止继续展示该订单。

为了避免多个订单混淆，V1 同一客户同时只能存在一个 PENDING_CONFIRM 消费订单。待确认订单建议设置较短有效期，例如 5 分钟，具体时长做成配置。

未来增加二维码时，二维码不应只包含客户 ID。应绑定具体订单、客户、门店和随机一次性凭证；扫描只允许确认这一笔消费，不能被重复使用。

如果客户没有提前打开 H5，可以由员工展示短订单码或确认入口，引导客户登录后进入待确认页。V1 不建议为了这个场景一开始就引入 WebSocket；后续在用户量和实时体验有需要时，再把轮询替换为 WebSocket 或 SSE。

## 7. 事务和并发要求

充值、消费、退款和冲正都必须是事务操作。

消费的核心步骤：

1. 校验客户、门店、员工权限。
2. 锁定 point_account。
3. 校验可用余额。
4. 在独立事务中校验客户 PIN，并持久化失败次数或锁定状态。
5. 重新锁定 consumption_order，校验归属、状态、有效期和门店状态。
6. 按充值时间 FIFO 锁定并扣减 point_lot。
7. 写入 point_lot_usage。
8. 更新 point_account。
9. 完成 consumption_order。
10. 写入 point_ledger 和 audit_log。

任何一步失败都回滚。Redis 可以用于限流和短期消费确认，但不能替代数据库事务和账户行锁。

## 8. 当前实施顺序

### 第一步：确认本设计

阶段 0 已确认：

- 充值只允许整笔退款，不允许部分退款。
- 消费必须由客户 PIN 授权。
- 超级管理员冲正不需要二次审批。
- 结算周期按自然月。
- 线下充值的 amount_cent 必须填写。

### 第二步：重建数据库

- 新建版本化数据库脚本。
- 新业务表先与旧商城测试表并存，核心闭环验证后再单独清理旧表。
- 创建客户、员工、商户、门店、积分、订单、结算和审计表。
- 插入三类后台角色；超级管理员通过安全的启动初始化流程创建，不在 SQL 中写默认密码。

### 第三步：重构认证和权限

- 引入 Spring Security。
- 建立后台员工登录。
- 建立门店数据范围校验。
- 移除对 UserRoleFilter 和 AdminRoleFilter 的新业务依赖。

### 第四步：实现积分核心服务

- 充值积分。
- 消费扣减。
- 退款。
- 流水查询。
- 幂等和并发测试。

### 第五步：实现线下业务闭环

- 后台查询客户。（已实现）
- 查询当前员工可操作的有效门店。（已实现）
- 线下充值。（已实现）
- 未消费充值整笔退款。（已实现，仅超级管理员）
- 后台创建待客户确认的消费订单。（已实现）
- 客户 PIN 确认、积分批次扣减和消费流水。（已实现）
- 客户积分流水、充值记录和消费记录查询。（已实现）
- 结算单生成和查询。

uni-app H5 和 Vben 管理界面在核心服务稳定后接入；消费二维码与微信支付作为后续能力单独增加。
