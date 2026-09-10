# 后台认证初始化说明

## 1. 确认 Flyway 迁移

项目已启用 Flyway。空库首次启动时会自动执行 V1～V3，无须手工执行 V2。启动成功后，在 `flyway_schema_history` 中确认 V2 已成功应用，再进行超级管理员初始化。

## 2. 生成后台 JWT 密钥

在 PowerShell 中生成 32 字节随机密钥的 Base64 表示：

~~~powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
~~~

不要把输出提交到 Git，也不要和旧商城的 JWT_KEY 共用。将结果配置到本机环境变量：

~~~text
PLATFORM_ADMIN_JWT_SECRET_BASE64=<上一步生成的值>
~~~

## 3. 仅首次启动时创建超级管理员

首次启动前临时设置以下环境变量：

~~~text
PLATFORM_BOOTSTRAP_ADMIN_ENABLED=true
PLATFORM_BOOTSTRAP_ADMIN_USERNAME=<管理员用户名>
PLATFORM_BOOTSTRAP_ADMIN_PASSWORD=<管理员强密码>
PLATFORM_BOOTSTRAP_ADMIN_REAL_NAME=<管理员姓名>
PLATFORM_BOOTSTRAP_ADMIN_PHONE=<可选手机号>
~~~

用户名只能包含字母、数字、点、下划线和横线。密码必须为 12～128 位，并包含大写字母、小写字母、数字和特殊字符。

应用成功启动后确认日志出现“首个超级管理员已创建”，并检查：

~~~sql
SELECT u.id, u.username, u.real_name, u.status, r.role_code
FROM t_sys_user u
JOIN t_sys_user_role ur ON ur.user_id = u.id
JOIN t_sys_role r ON r.id = ur.role_id
WHERE r.role_code = 'SUPER_ADMIN';
~~~

确认无误后立即执行以下操作：

1. 将 PLATFORM_BOOTSTRAP_ADMIN_ENABLED 改为 false。
2. 删除 PLATFORM_BOOTSTRAP_ADMIN_PASSWORD 环境变量。
3. 重新启动应用，确认管理员仍可登录。

如果数据库中已经存在 SUPER_ADMIN 关系，初始化程序不会再次创建管理员。

## 4. 验证登录

登录接口：

~~~http
POST /api/v1/admin/auth/login
Content-Type: application/json

{
  "username": "<管理员用户名>",
  "password": "<管理员密码>"
}
~~~

成功后使用返回的 accessToken：

~~~http
GET /api/v1/admin/auth/me
Authorization: Bearer <accessToken>
~~~

后台令牌默认有效 8 小时。连续登录失败默认 5 次后锁定 15 分钟。每次请求都会重新加载账号状态、角色、门店范围和 token_version。

## 5. 当前账号修改密码

超级管理员、店长和店员都可以在个人中心验证当前密码后修改自己的登录密码：

~~~http
PUT /api/v1/admin/auth/password
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "currentPassword": "<当前密码>",
  "newPassword": "<新的强密码>"
}
~~~

新密码仍须满足 12～128 位以及大小写字母、数字、特殊字符要求。修改成功会递增账号的 `token_version`，使当前及其他设备上的原登录凭证全部失效，后台前端随后退出并要求使用新密码重新登录。该操作会写入安全审计，但不会记录密码明文或哈希。

员工管理中的“重置员工密码”与此接口相互独立，当前仍只允许超级管理员执行。
