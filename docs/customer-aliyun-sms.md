# 阿里云短信验证码接入

## 1. 当前用途

真实短信共用已有 `PhoneVerificationSender` 接口，覆盖：

- 首次绑定手机号：`POST /api/v1/customer/phone/verification-codes`。
- 找回消费密码：`POST /api/v1/customer/security/consume-pin/reset/verification-codes`，接收号码只能是当前客户已绑定手机号。

使用阿里云官方 V2 SDK `com.aliyun:dysmsapi20170525:4.6.0`，调用国内短信 `SendSms`。不需要新建数据库表，也不需要执行 Flyway 迁移。

## 2. 阿里云准备事项

- 签名、验证码模板已审核通过，并确认运营商报备状态。
- 短信账户有可用余额或套餐额度。
- 为程序创建专用 RAM 用户，授予自定义权限 `dysms:SendSms`。
- 为该 RAM 用户创建 AccessKey，ID 和 Secret 保存在后端运行环境中。

程序仅调用发送接口，不需要签名管理、模板管理、群发或短信查询权限。

## 3. 后端环境变量

以下参数只配置到后端 Spring Boot 的启动环境中；不要配置到 H5 或后台管理前端。

```text
PLATFORM_PHONE_VERIFICATION_MODE=ALIYUN
ALIBABA_CLOUD_ACCESS_KEY_ID=<RAM用户的AccessKey ID>
ALIBABA_CLOUD_ACCESS_KEY_SECRET=<RAM用户的AccessKey Secret>
PLATFORM_SMS_ALIYUN_SIGN_NAME=贵州诚辰互联网销售
PLATFORM_SMS_ALIYUN_TEMPLATE_CODE=SMS_338940210
PLATFORM_PHONE_VERIFICATION_CODE_TTL=PT5M
```

已经配置过 AccessKey 时保留原值，只补充模式、签名和模板。原 `PLATFORM_PHONE_VERIFICATION_MODE=LOG` 必须替换成 `ALIYUN`，避免仍从日志获取验证码。

签名必须与控制台一致，不带 `【】` 等括号。配置后重新启动后端；新增 Maven 依赖需在 IDE 中重新加载。

可选参数：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PLATFORM_PHONE_VERIFICATION_CODE_TTL` | `PT5M` | 验证码有效期，允许 1 到 10 分钟 |
| `PLATFORM_SMS_ALIYUN_CONNECT_TIMEOUT` | `PT5S` | 连接超时，允许 1 到 30 秒 |
| `PLATFORM_SMS_ALIYUN_READ_TIMEOUT` | `PT8S` | 响应超时，允许 1 到 30 秒 |
| `PLATFORM_PHONE_VERIFICATION_GLOBAL_DAILY_LIMIT` | `5000` | 全平台 24 小时发送上限；初期可设为 `200` |

原有客户和手机号的小时/24 小时限额、60 秒发送间隔、连续输错限制继续生效，不引入 IP 限制。

密钥不要提交到 Git。若使用本地 `.env`，需由现有 IDE 的 EnvFile 配置加载，Spring Boot 不会自动读取任意 `.env` 文件。

## 4. 模板与有效期

当前审核通过的模板正文为：

> 您的验证码为：${code}，请勿泄露于他人！

发送时只传入 `{"code":"六位验证码"}`，保留以零开头的验证码。不向模板额外传入有效期参数。

验证码有效期由本平台决定，与短信模板是否注明有效期无关。验证码生成后以 BCrypt 哈希写入 Redis，并设置 `code-ttl`；默认从写入 Redis 开始计时，5 分钟后失效。校验成功后立即删除，新验证码替换旧验证码。

如果希望短信也显示“5分钟内有效”，在阿里云申请新模板或修改模板并重新审核。静态有效期文案需与后端 `code-ttl` 一致。现有模板可以继续使用，不需要为了打通接口先等待模板修改。

## 5. 发送结果与异常

- SDK 业务响应 `Code=OK` 表示阿里云接受发送请求；是否实际送达仍以运营商回执和手机收到为准。
- SDK 自动重试关闭。阿里云 `SendSms` 不支持幂等，超时后反复提交可能重复扣费和重复发送。
- 缺少密钥、签名、模板或超时配置不合法：返回 `30042`，不写入验证码、不占用发送配额。
- 阿里云返回非 `OK`、空响应或调用异常：返回 `30050`，清理该次验证码。发送频率和配额保留，下一次发送仍需满足限流要求。
- 网络超时时无法确定阿里云是否已受理：服务端仍清理该次验证码。即使稍后收到短信，该验证码也不能使用；等待发送间隔后重新获取。
- 正式发送日志不记录手机号、验证码、密钥、完整请求、原始 SDK 异常消息或堆栈。提交失败只记录阿里云错误码和请求 ID，便于在控制台排查。

常见排查：`30042` 检查后端启动配置是否加载；`30050` 检查后端日志中的阿里云错误码、RAM 授权、签名/模板状态、账户余额及运营商报备。

## 6. 联调顺序

1. 用未绑定手机号的微信账户登录 H5，在绑定手机号页面获取验证码。
2. 确认手机收到签名与模板一致的短信，用验证码完成绑定。
3. 设置消费密码，进入“忘记消费密码”，确认短信发往当前已绑定手机号并能完成重置。
4. 验证重复获取受发送间隔限制、错误验证码被拒绝、已经使用的验证码不能重复使用。
5. 获取一个新验证码后等待超过 `code-ttl`，确认过期验证码被拒绝。

自动化测试使用模拟 SDK，不发送真实短信、不读取实际 AccessKey。真实送达需由开发者在 H5 中触发并在手机上验证。

## 7. 官方参考

- [Java SDK](https://help.aliyun.com/zh/sms/developer-reference/using-java-openapi-example)
- [SendSms 请求参数与返回结果](https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms)
- [仅发送短信的自定义 RAM 策略](https://help.aliyun.com/zh/sms/custom-permission-policy-reference)
