# 管理后台域名预发布部署

2026-10-13 用户确认该预发布后台已经部署并验证。以下保留当时的操作和回退说明；正式域名和干净账本切换另见 production-launch.md，不把本次反馈视为正式服务号或真实资金业务已经验收。

## 当前边界

- 域名：`admin-stage.qiongyintech.com`，与 H5 的 `stage.qiongyintech.com` 分开。
- 2026-10-11 用户提供的网站宿主机根目录：`/opt/1panel/www/sites/point-platform-admin-stage/index`。
- 仍共用现有预发布 Java、MySQL、Redis，不另起一套后端，不把测试账本转为正式账本。
- 用户选择暂不增加 IP 白名单、VPN 或站点密码，方便多人测试；系统登录、角色和门店范围校验保持有效。测试账号由超管单独创建，不共用超管凭证，不使用本地演示 SQL 的公开密码。
- Java 仍只发布 `127.0.0.1:8888`，MySQL、Redis 不开放公网。H5 站点继续屏蔽所有后台接口。
- 用户确认新退款资格检查镜像已更新并验证通过；10 月数据尚不能生成自然月结算，完整月结线上验收暂缓，不修改服务器时间或业务订单日期。
- 本文步骤由用户在服务器执行；本地检查不代表线上部署已经完成。申请证书时使用域名证书，不勾选 IP 证书；HTTP 验证可以继续使用。

## 1. 核对 HTTPS 与准备回退

打开 `https://admin-stage.qiongyintech.com/`，确认新站点的默认页面能够打开、证书没有警告；HTTP 应跳转 HTTPS。没有确认成功前，不发布后台登录入口。

在新站点根目录之外保存完整 OpenResty 站点配置和当前入口文件。备份、ZIP、源码、环境文件和证书私钥不放在公开网站目录内。保留 H5 配置和当前后台 SSH 隧道，不修改已有后端环境变量、数据库或 JWT 密钥。

1Panel 配置中的 `root` 使用容器内路径，通常为 `/www/sites/point-platform-admin-stage/index`；以面板现有值为准，不把宿主机 `/opt/1panel/...` 路径替换进去。

## 2. 仅为后台域名添加 API 代理

1. 打开新后台网站的 OpenResty 配置，把 `deploy/openresty/admin-stage-api.conf` 的完整内容插入该域名现有 `server { ... }` 内、`root` 前，只插入一次。
2. 这是配置片段，不是完整配置：不要替换现有 `server`，不要粘贴到 `location` 内，也不要插入 H5 站点或全局配置。若已存在相同的 `location`，先核对，不能重复定义。
3. 保留现有监听端口、域名、根目录、证书、HTTP 跳转、ACME 验证和隐藏文件规则。确认 `server` 层级恰好有一条 `proxy_set_header X-Forwarded-Proto https;`；已有则不重复添加，没有则补上。
4. 保存并应用前确认 1Panel 的语法校验通过。失败时恢复备份，不强制重启 OpenResty，不扩大 8888 的监听范围。

只允许 `/api/v1/admin/` 转发到现有 Java。`proxy_pass` 不带 URI，保留原接口前缀；Bearer 请求头正常传递。其他 `/api/`、健康检查、Swagger 均返回 404。API 不缓存、不记录含客户查询参数的访问日志，也不自动重试上游；后端的业务审计不关闭。没有 CDN 时覆盖客户端传入的转发 IP，避免把伪造来源当成可信来源。

首页与 `_app.config.js` 禁止长期缓存。片段没有在子 `location` 新增响应头，以保留现有站点 HSTS 等响应头；前端使用 hash 路由，不需要把所有不存在的路径兜底成 HTML。

代理保存后，先只做以下无令牌、无写入检查（Windows 使用 `curl.exe`）：

```bash
curl -i --max-time 15 'https://admin-stage.qiongyintech.com/api/v1/admin/auth/me'
curl -i --max-time 15 'https://admin-stage.qiongyintech.com/api/v1/customer/stores?pageNum=1&pageSize=1'
curl -i --max-time 15 'https://admin-stage.qiongyintech.com/actuator/health'
curl -i --max-time 15 'https://admin-stage.qiongyintech.com/v3/api-docs'
curl -i --max-time 15 'https://stage.qiongyintech.com/api/v1/admin/auth/me'
```

第一项预期 HTTP 401 JSON，表明未登录被后端拒绝；其余四项预期 HTTP 404。后台默认首页应继续可打开。401 不是部署失败，404 网页也正常；若出现 502，先检查 Java UP 和 OpenResty 的 host 网络，不开放公网业务端口。

## 3. 在本机生成后台静态包

在 `point_platform_admin` 仓库根目录执行：

```powershell
pnpm --filter @vben/web-antd typecheck
pnpm build:antd:staging
```

生成 `apps/web-antd/dist.zip`，其内部直接包含 `index.html`、`_app.config.js`、`favicon.ico`、`css/`、`js/`、`jse/`。上传前保留带日期的候选 ZIP 副本及 SHA-256，后续普通 production 构建会覆盖同名 dist.zip，不能混用。

本次候选包已保存到管理端 `dist/releases/point-platform-admin-stage-20261011-192110.zip`：1295553 字节，218 个 ZIP 条目；SHA-256 为 `4387E6D7E9B1ADB1408B0A0453F63E7F9A565E7715B684C6D00CE672246E6C6C`。包内入口和 `_app.config.js` 已确认位于根目录，运行时 API 配置为 `/api/v1/admin`。

本次类型检查与 staging 构建通过。用户随后选择直接上传服务器自行验证，因此未继续执行本地浏览器或实际 Nginx 代理回归，也未操作线上站点；配置仍须经过服务器 1Panel 的语法检查和线上验收。

`.env.staging` 只有可公开的浏览器配置：标题为「积分平台管理后台（预发布）」、API 为 `/api/v1/admin`，使用独立的缓存命名空间，关闭 Mock、PWA 和开发工具。它不是后端环境配置，不能填写 JWT 密钥、数据库密码、微信 AppSecret 或短信 AccessKey；所有 `VITE_` 值最终都可能被浏览器读取。现有 `.env.production` 和本地 SSH 代理设置不改。

登录页显示测试环境提醒及现有 ICP 备案链接。模板原有百度统计脚本已移除，不部署 Vite 开发服务器，不在服务器安装前端依赖或运行 Node。

## 4. 上传与发布

代理检查符合预期后，再发布静态包：

1. 在网站根目录之外创建一个带版本日期的发布目录，例如 `/opt/point-platform/frontend/admin-stage-release-20261011`，上传指定候选 ZIP 并解压。
2. 保存当前站点的入口和运行时配置（首次只有默认入口也正常）。不要清空网站根目录。
3. 先把包内 `css`、`js`、`jse` 目录和 `favicon.ico` 复制到 `/opt/1panel/www/sites/point-platform-admin-stage/index`，再复制 `_app.config.js`，最后复制并覆盖 `index.html`。不能额外套一层 `dist` 或发布目录。
4. 不把 ZIP、源码仓库、`.env`、部署脚本或备份上传到公开网站目录；保留已有 ACME、证书和其他站点文件。
5. 刷新后台域名，确认显示预发布标题和登录提醒；需要时强制刷新。静态文件更新不需要重建 Java、执行 SQL、重启 MySQL/Redis 或运行 `docker compose down`。

## 5. 首轮线上验收（只读）

1. 用服务器超管账号在浏览器登录，确认工作台、合作商户、门店、订单等只读页面正常，并与 SSH 隧道中看到的数据一致。不要发送密码、令牌、完整手机号或浏览器存储截图。
2. 刷新一次，确认会话和接口仍正常；退出登录后受限页面应回到登录页。
3. 使用仅授权 A 店的店长和店员分别登录，核对菜单与门店范围；角色权限不能只看菜单，后端的跨店拒绝仍然有效。
4. 从自己的电脑访问 H5，确认门店列表、微信登录和已有页面未受影响；H5 域名的后台接口仍为 404。
5. 首轮只读验收通过后再按需验证模拟充值、消费和退款。暂不做真实收付款，不提前生成 10 月月结，不把「期间实际付款」当作待付余额。

公开后台后，需要保持强密码、独立测试账号、现有失败次数限制及备份；不提供免登录接口，不发送公开演示账号。正式运营前重新评估访问保护并处理正式账本切换，不能仅删除预发布标题就转正。

## 6. 回退

静态页面异常时先检查目录层级、静态资源状态和 `_app.config.js`；API 异常先检查代理路径、Java 健康和登录是否过期，不反复提交财务请求。

回退前端时恢复同一版本的 `index.html` 和 `_app.config.js`，旧版对应资源必须保留；首次部署可恢复默认入口。代理异常时恢复该新站点的完整配置并通过语法校验。无需回滚数据库或 H5，不删除当前 Java 镜像，不撤销已经成功登记的业务操作。

## 参考

- [1Panel 网站基础配置](https://1panel.pro/docs/v2/user_manual/websites/website_config_basic/)
- [Nginx 代理路径、请求头和重试规则](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [Nginx location 优先级](https://nginx.org/en/docs/http/ngx_http_core_module.html#location)
