# 客户认证基础（H5 / 微信小程序共用）

## 当前已实现

- 后台账号与客户账号使用两套独立 JWT 密钥、签发方和受众，令牌不能混用。
- 客户访问令牌默认有效期为 2 小时，每次请求都会重新读取客户状态与 `token_version`。
- `GET /api/v1/h5/auth/wechat/start`：创建一次性 OAuth `state` 并跳转到微信网页授权。
- `GET /api/v1/h5/auth/wechat/callback`：校验并消费 `state`、换取微信身份、创建或匹配客户，最后携带一次性登录票据跳回 H5。票据放在 H5 顶层查询参数 `loginTicket` 中，兼容部分微信 WebView 丢失 URL fragment 的情况；H5 启动后会立即兑换并清理地址栏。
- `POST /api/v1/h5/auth/session/exchange`：使用一次性票据换取客户访问令牌。
- `GET /api/v1/customer/me`：查询当前客户资料、消费密码状态和实时积分余额。
- OAuth `state` 和登录票据都只可使用一次；Redis 键只保存随机值的 SHA-256 摘要。
- 登录回调响应与 H5 页面均使用 `no-referrer`，降低短期票据经 Referer 泄漏的风险。登录票据默认仅存活 1 分钟，生产环境还应避免在反向代理访问日志中记录 `loginTicket` 查询参数。
- Swagger 会收录上述接口。两个微信 GET 接口返回的是 302 跳转，不是普通 JSON 数据。

微信公众号网页授权不会返回手机号。首次授权会先创建 `phone = NULL` 的临时完整客户账户和零余额积分账户；客户随后通过短信验证码绑定手机号，绑定前只能登录和查看账户，不能通过手机号进行线下充值、消费或找回消费密码。

## 数据库前置条件

项目已启用 Flyway。空库首次启动时会自动执行 V1～V3，无须再手工执行客户认证字段脚本。启动成功后可在 `flyway_schema_history` 中确认 V3 已应用；详细步骤见 `docs/database-migrations.md`。

## 客户 JWT 密钥

客户密钥必须与 `PLATFORM_ADMIN_JWT_SECRET_BASE64` 不同。可在 PowerShell 中生成：

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

把结果放入服务端环境变量：

```text
PLATFORM_CUSTOMER_JWT_SECRET_BASE64=<生成的 Base64 字符串>
```

密钥不进入数据库、不写进 Git，也不返回前端。未配置时，客户票据兑换接口会返回 503；后台登录不受影响。

## 微信 H5 环境变量

本地微信测试号联调示例（局域网地址按本机实际地址修改）：

```text
PLATFORM_WECHAT_H5_ENABLED=true
PLATFORM_WECHAT_H5_APP_ID=<测试号 AppID>
PLATFORM_WECHAT_H5_APP_SECRET=<测试号 AppSecret>
PLATFORM_WECHAT_H5_CALLBACK_URL=http://192.168.1.220:5173/api/v1/h5/auth/wechat/callback
PLATFORM_H5_BASE_URL=http://192.168.1.220:5173
PLATFORM_WECHAT_H5_SCOPE=snsapi_userinfo
PLATFORM_CUSTOMER_JWT_SECRET_BASE64=<独立的客户 JWT 密钥>
```

测试号网页授权域名按页面校验规则填写，通常不含协议和路径；本地联调可先尝试 `192.168.1.220:5173`，若输入框不接受端口则填写 `192.168.1.220`。如果测试号拒绝局域网 IP，就需要临时 HTTPS 公网隧道。手机必须与电脑在同一可互访局域网内，Windows 防火墙需允许访问 H5 的 5173 端口。回调先到 Vite，再由 `/api` 代理到本机 8888 后端。

AppSecret 和 JWT 密钥只放在服务端运行环境，不能提交到 Git，也不能放进 UniApp。正式部署时必须改成已备案且配置到微信公众平台的 HTTPS 域名，并同步修改两个 URL 环境变量。

## 下一步

手机号绑定已经实现，联调方式见 `docs/customer-phone-binding.md`。下一阶段开放客户消费密码 H5 接口，再实现待确认消费订单查询与本人确认扣款。
