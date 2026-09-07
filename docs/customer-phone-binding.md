# 客户手机号绑定

## 当前实现

- `POST /api/v1/customer/phone/verification-codes`：向待绑定手机号发送验证码。
- `PUT /api/v1/customer/phone`：校验验证码并绑定当前登录客户，成功后返回最新客户资料。
- 两个接口都必须携带客户 Bearer Token，不能由请求体指定 `customerId`。
- 验证码默认有效 5 分钟，只保留最后一次发送的验证码，验证成功后立即删除。
- 验证码在 Redis 中使用 BCrypt 哈希保存，不保存明文；连续输错 5 次后当前验证码失效。
- 发送频率只按登录客户和手机号限制，当前不使用 IP 限制。
- 手机号已属于其他客户时禁止自动合并账户，返回冲突并由后续人工核验流程处理。
- 成功绑定会写入 `t_audit_log`，审计中只保存掩码手机号。

本功能复用 V1 中 `t_customer_user.phone` 的唯一索引，不需要新增 Flyway 脚本。

## 本地联调配置

本地开发暂不接短信供应商。显式开启日志发送模式：

```text
PLATFORM_PHONE_VERIFICATION_MODE=LOG
```

重启后端后，在 H5 点击“立即绑定”并获取验证码。验证码会出现在后端日志中，示例：

```text
本地联调短信验证码=123456，手机号=138****8000，有效期=300秒；正式环境禁止使用 LOG 模式
```

默认模式为 `DISABLED`，未配置时发送接口返回 503。正式环境禁止使用 `LOG`，接入真实短信供应商后应由对应 `PhoneVerificationSender` 实现替换。

## 默认防刷规则

| 维度 | 小时上限 | 24 小时上限 |
| --- | ---: | ---: |
| 同一登录客户 | 5 | 10 |
| 同一手机号 | 5 | 10 |
| 全平台 | - | 5000 |

同一“登录客户 + 手机号”两次发送至少间隔 60 秒。上述参数可通过 `application.yml` 中对应的 `PLATFORM_PHONE_VERIFICATION_*` 环境变量调整。当前不强制图形验证码，错误码 `30051` 预留给后续按风险触发的人机验证。

## 请求示例

```http
POST /api/v1/customer/phone/verification-codes
Authorization: Bearer <customerAccessToken>
Content-Type: application/json

{
  "phone": "13800138000"
}
```

```http
PUT /api/v1/customer/phone
Authorization: Bearer <customerAccessToken>
Content-Type: application/json

{
  "phone": "13800138000",
  "verificationCode": "123456"
}
```
