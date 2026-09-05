-- 后台认证安全字段（在 V1 导入完成后执行）
-- 只增量修改 t_sys_user，不删除数据。

ALTER TABLE `t_sys_user`
    ADD COLUMN `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '连续登录失败次数' AFTER `status`,
    ADD COLUMN `locked_until` DATETIME DEFAULT NULL COMMENT '临时锁定截止时间' AFTER `failed_login_count`,
    ADD COLUMN `token_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '递增后使历史后台令牌失效' AFTER `locked_until`,
    ADD COLUMN `password_updated_time` DATETIME DEFAULT NULL COMMENT '密码最后更新时间' AFTER `token_version`,
    ADD KEY `idx_sys_user_locked_until` (`locked_until`);
