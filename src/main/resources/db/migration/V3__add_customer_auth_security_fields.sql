-- 客户登录令牌版本号。递增后，可立即使该客户此前签发的全部访问令牌失效。
ALTER TABLE `t_customer_user`
    ADD COLUMN `token_version` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '客户访问令牌版本，修改安全信息或强制下线时递增'
        AFTER `last_login_time`;
