-- 积分平台 V1 基线表结构
-- 兼容 MySQL 5.7+；只创建新业务表，不删除或修改旧商城表。
-- 金额统一使用人民币分，积分使用整数；V1 约束 1 元 = 1 积分。
-- 业务状态使用 VARCHAR，由应用层枚举和事务校验，避免依赖 MySQL 5.7 不生效的 CHECK 约束。

CREATE TABLE `t_customer_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `phone` VARCHAR(32) DEFAULT NULL COMMENT '绑定手机号',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_user_phone` (`phone`),
    KEY `idx_customer_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户';

CREATE TABLE `t_customer_identity` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `provider_code` VARCHAR(32) NOT NULL COMMENT 'WECHAT_H5/WECHAT_MINI_PROGRAM等',
    `app_id` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '微信应用AppId等身份命名空间',
    `external_id` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT 'OpenId等外部身份ID',
    `union_id` VARCHAR(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信UnionId',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_identity_external` (`provider_code`, `app_id`, `external_id`),
    KEY `idx_customer_identity_customer` (`customer_id`),
    KEY `idx_customer_identity_union` (`provider_code`, `union_id`),
    CONSTRAINT `fk_customer_identity_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户第三方身份';

CREATE TABLE `t_customer_security` (
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `consume_pin_hash` VARCHAR(255) DEFAULT NULL COMMENT '消费密码哈希，不保存明文',
    `failed_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '连续校验失败次数',
    `locked_until` DATETIME DEFAULT NULL COMMENT '消费密码锁定截止时间',
    `pin_updated_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`customer_id`),
    CONSTRAINT `fk_customer_security_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户安全设置';

CREATE TABLE `t_sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '后台用户ID',
    `username` VARCHAR(64) NOT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt或Argon2哈希',
    `real_name` VARCHAR(64) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/LOCKED',
    `last_login_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    UNIQUE KEY `uk_sys_user_phone` (`phone`),
    KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台员工账号';

CREATE TABLE `t_sys_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(32) NOT NULL,
    `role_name` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台角色';

CREATE TABLE `t_sys_user_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role` (`role_id`),
    CONSTRAINT `fk_sys_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_sys_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `t_sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台用户角色关系';

CREATE TABLE `t_merchant` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `merchant_code` VARCHAR(64) NOT NULL,
    `legal_name` VARCHAR(200) NOT NULL COMMENT '签约主体名称',
    `business_name` VARCHAR(200) NOT NULL COMMENT '商户展示名称',
    `unified_social_credit_code` VARCHAR(32) DEFAULT NULL,
    `contact_name` VARCHAR(64) NOT NULL,
    `contact_phone` VARCHAR(32) NOT NULL,
    `agreement_no` VARCHAR(100) DEFAULT NULL COMMENT '合作协议编号',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'PENDING/ACTIVE/SUSPENDED/TERMINATED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_code` (`merchant_code`),
    UNIQUE KEY `uk_merchant_credit_code` (`unified_social_credit_code`),
    KEY `idx_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作商户';

CREATE TABLE `t_store` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT UNSIGNED NOT NULL,
    `store_code` VARCHAR(64) NOT NULL,
    `store_name` VARCHAR(200) NOT NULL,
    `address` VARCHAR(500) NOT NULL,
    `contact_phone` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'PENDING/ACTIVE/SUSPENDED/CLOSED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_code` (`store_code`),
    KEY `idx_store_merchant` (`merchant_id`),
    KEY `idx_store_status` (`status`),
    CONSTRAINT `fk_store_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `t_merchant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作门店';

CREATE TABLE `t_sys_user_store` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `store_id` BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_store` (`user_id`, `store_id`),
    KEY `idx_sys_user_store_store` (`store_id`),
    CONSTRAINT `fk_sys_user_store_user` FOREIGN KEY (`user_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_sys_user_store_store` FOREIGN KEY (`store_id`) REFERENCES `t_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台用户门店数据范围';

CREATE TABLE `t_point_account` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `available_points` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本；核心扣减仍需事务行锁',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_account_customer` (`customer_id`),
    CONSTRAINT `fk_point_account_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台积分账户';

CREATE TABLE `t_recharge_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `recharge_store_id` BIGINT UNSIGNED NOT NULL,
    `recharge_points` BIGINT UNSIGNED NOT NULL,
    `amount_cent` BIGINT UNSIGNED NOT NULL COMMENT '实收金额，V1必须为100的整数倍',
    `channel` VARCHAR(32) NOT NULL DEFAULT 'OFFLINE' COMMENT 'OFFLINE/WECHAT_PAY',
    `payment_method` VARCHAR(32) NOT NULL COMMENT 'PLATFORM_QR/BANK_TRANSFER/OTHER',
    `fund_receiver` VARCHAR(32) NOT NULL DEFAULT 'PLATFORM' COMMENT 'V1只允许PLATFORM',
    `payment_reference` VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '平台收款交易参考号',
    `order_status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/COMPLETED/REFUNDED/CANCELLED',
    `operator_id` BIGINT UNSIGNED NOT NULL,
    `paid_time` DATETIME DEFAULT NULL,
    `completed_time` DATETIME DEFAULT NULL,
    `idempotency_key` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recharge_order_no` (`order_no`),
    UNIQUE KEY `uk_recharge_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_recharge_payment_reference` (`payment_method`, `payment_reference`),
    KEY `idx_recharge_customer_time` (`customer_id`, `create_time`),
    KEY `idx_recharge_store_time` (`recharge_store_id`, `create_time`),
    KEY `idx_recharge_status` (`order_status`),
    CONSTRAINT `fk_recharge_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_recharge_store` FOREIGN KEY (`recharge_store_id`) REFERENCES `t_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_recharge_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单';

CREATE TABLE `t_point_lot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `source_recharge_order_id` BIGINT UNSIGNED NOT NULL,
    `total_points` BIGINT UNSIGNED NOT NULL,
    `remaining_points` BIGINT UNSIGNED NOT NULL,
    `lot_status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/DEPLETED/REFUNDED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_lot_recharge` (`source_recharge_order_id`),
    KEY `idx_point_lot_fifo` (`customer_id`, `lot_status`, `create_time`, `id`),
    CONSTRAINT `fk_point_lot_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_point_lot_recharge` FOREIGN KEY (`source_recharge_order_id`) REFERENCES `t_recharge_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值积分批次';

CREATE TABLE `t_recharge_refund` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `refund_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `recharge_order_id` BIGINT UNSIGNED NOT NULL,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `refund_points` BIGINT UNSIGNED NOT NULL,
    `refund_amount_cent` BIGINT UNSIGNED NOT NULL,
    `refund_method` VARCHAR(32) NOT NULL COMMENT 'ORIGINAL_CHANNEL/BANK_TRANSFER/OTHER',
    `refund_reference` VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '实际退款交易参考号',
    `refund_status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/COMPLETED/CANCELLED/FAILED',
    `operator_id` BIGINT UNSIGNED NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `completed_time` DATETIME DEFAULT NULL,
    `idempotency_key` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recharge_refund_no` (`refund_no`),
    UNIQUE KEY `uk_recharge_refund_order` (`recharge_order_id`),
    UNIQUE KEY `uk_recharge_refund_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_recharge_refund_reference` (`refund_method`, `refund_reference`),
    KEY `idx_recharge_refund_customer` (`customer_id`, `create_time`),
    CONSTRAINT `fk_recharge_refund_order` FOREIGN KEY (`recharge_order_id`) REFERENCES `t_recharge_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_recharge_refund_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_recharge_refund_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值退款';

CREATE TABLE `t_consumption_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `store_id` BIGINT UNSIGNED NOT NULL,
    `consume_points` BIGINT UNSIGNED NOT NULL,
    `gross_amount_cent` BIGINT UNSIGNED NOT NULL,
    `platform_fee_rate_bps` SMALLINT UNSIGNED NOT NULL DEFAULT 500 COMMENT '平台费率基点，500表示5%',
    `platform_fee_cent` BIGINT UNSIGNED NOT NULL,
    `store_payable_cent` BIGINT UNSIGNED NOT NULL,
    `verification_mode` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER_PIN',
    `expires_time` DATETIME NOT NULL,
    `order_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM/COMPLETED/CANCELLED/EXPIRED/REVERSED',
    `operator_id` BIGINT UNSIGNED NOT NULL COMMENT '创建订单的后台员工',
    `confirmed_time` DATETIME DEFAULT NULL,
    `completed_time` DATETIME DEFAULT NULL,
    `reversed_by` BIGINT UNSIGNED DEFAULT NULL,
    `reversed_time` DATETIME DEFAULT NULL,
    `reversal_reason` VARCHAR(500) DEFAULT NULL,
    `settlement_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_INCLUDED' COMMENT 'NOT_INCLUDED/INCLUDED/SETTLED/ADJUSTED',
    `idempotency_key` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `active_pending_customer_id` BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN `order_status` = 'PENDING_CONFIRM' THEN `customer_id` ELSE NULL END) STORED COMMENT '保证一个客户最多一笔待确认订单',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_consumption_order_no` (`order_no`),
    UNIQUE KEY `uk_consumption_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_consumption_active_pending` (`active_pending_customer_id`),
    KEY `idx_consumption_customer_time` (`customer_id`, `create_time`),
    KEY `idx_consumption_store_time` (`store_id`, `create_time`),
    KEY `idx_consumption_status_expiry` (`order_status`, `expires_time`),
    KEY `idx_consumption_settlement` (`settlement_status`, `completed_time`),
    CONSTRAINT `fk_consumption_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_consumption_store` FOREIGN KEY (`store_id`) REFERENCES `t_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_consumption_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_consumption_reversed_by` FOREIGN KEY (`reversed_by`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分消费订单';

CREATE TABLE `t_point_lot_usage` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `consumption_order_id` BIGINT UNSIGNED NOT NULL,
    `point_lot_id` BIGINT UNSIGNED NOT NULL,
    `used_points` BIGINT UNSIGNED NOT NULL,
    `reversed_points` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '异常冲正时归还到本批次的积分',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_lot_usage_order_lot` (`consumption_order_id`, `point_lot_id`),
    KEY `idx_point_lot_usage_lot` (`point_lot_id`),
    CONSTRAINT `fk_point_lot_usage_order` FOREIGN KEY (`consumption_order_id`) REFERENCES `t_consumption_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_point_lot_usage_lot` FOREIGN KEY (`point_lot_id`) REFERENCES `t_point_lot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费积分批次占用';

CREATE TABLE `t_point_ledger` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `ledger_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `account_id` BIGINT UNSIGNED NOT NULL,
    `customer_id` BIGINT UNSIGNED NOT NULL,
    `delta_points` BIGINT NOT NULL COMMENT '正数增加，负数扣减',
    `balance_after` BIGINT UNSIGNED NOT NULL,
    `ledger_type` VARCHAR(32) NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/ADJUSTMENT/REVERSAL',
    `business_type` VARCHAR(32) NOT NULL COMMENT 'RECHARGE_ORDER/CONSUMPTION_ORDER/RECHARGE_REFUND等',
    `business_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '后台操作员；系统任务可为空',
    `store_id` BIGINT UNSIGNED DEFAULT NULL,
    `idempotency_key` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_ledger_no` (`ledger_no`),
    UNIQUE KEY `uk_point_ledger_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_point_ledger_business` (`business_type`, `business_no`, `ledger_type`),
    KEY `idx_point_ledger_customer_time` (`customer_id`, `create_time`),
    KEY `idx_point_ledger_account_time` (`account_id`, `create_time`),
    KEY `idx_point_ledger_store_time` (`store_id`, `create_time`),
    CONSTRAINT `fk_point_ledger_account` FOREIGN KEY (`account_id`) REFERENCES `t_point_account` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_point_ledger_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_point_ledger_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_point_ledger_store` FOREIGN KEY (`store_id`) REFERENCES `t_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='不可变积分流水';

CREATE TABLE `t_settlement_period` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `period_code` CHAR(7) NOT NULL COMMENT '自然月，格式YYYY-MM',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `period_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/FROZEN/GENERATED/CONFIRMED/PAID/CLOSED',
    `frozen_time` DATETIME DEFAULT NULL,
    `generated_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_period_code` (`period_code`),
    UNIQUE KEY `uk_settlement_period_range` (`start_date`, `end_date`),
    KEY `idx_settlement_period_status` (`period_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自然月结算周期';

CREATE TABLE `t_store_settlement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `settlement_no` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `period_id` BIGINT UNSIGNED NOT NULL,
    `merchant_id` BIGINT UNSIGNED NOT NULL,
    `store_id` BIGINT UNSIGNED NOT NULL,
    `total_consume_points` BIGINT NOT NULL DEFAULT 0 COMMENT '扣除冲正调整后的结算积分',
    `gross_amount_cent` BIGINT NOT NULL DEFAULT 0,
    `platform_fee_cent` BIGINT NOT NULL DEFAULT 0,
    `adjustment_amount_cent` BIGINT NOT NULL DEFAULT 0 COMMENT '人工调整，正数增加应付，负数减少应付',
    `payable_amount_cent` BIGINT NOT NULL DEFAULT 0,
    `settlement_status` VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/CONFIRMED/PAID/CLOSED',
    `confirmed_time` DATETIME DEFAULT NULL,
    `paid_time` DATETIME DEFAULT NULL,
    `payment_reference` VARCHAR(128) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_settlement_no` (`settlement_no`),
    UNIQUE KEY `uk_store_settlement_period_store` (`period_id`, `store_id`),
    KEY `idx_store_settlement_merchant` (`merchant_id`, `period_id`),
    KEY `idx_store_settlement_status` (`settlement_status`),
    CONSTRAINT `fk_store_settlement_period` FOREIGN KEY (`period_id`) REFERENCES `t_settlement_period` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_store_settlement_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `t_merchant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_store_settlement_store` FOREIGN KEY (`store_id`) REFERENCES `t_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店月度结算单';

CREATE TABLE `t_store_settlement_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `settlement_id` BIGINT UNSIGNED NOT NULL,
    `item_type` VARCHAR(32) NOT NULL COMMENT 'CONSUMPTION/REVERSAL_ADJUSTMENT/MANUAL_ADJUSTMENT',
    `consumption_order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '人工调整时可为空',
    `points_delta` BIGINT NOT NULL DEFAULT 0,
    `gross_amount_cent` BIGINT NOT NULL DEFAULT 0,
    `platform_fee_cent` BIGINT NOT NULL DEFAULT 0,
    `store_payable_cent` BIGINT NOT NULL DEFAULT 0,
    `adjustment_reason` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_item_order_type` (`consumption_order_id`, `item_type`),
    KEY `idx_settlement_item_settlement` (`settlement_id`),
    CONSTRAINT `fk_settlement_item_settlement` FOREIGN KEY (`settlement_id`) REFERENCES `t_store_settlement` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_settlement_item_consumption` FOREIGN KEY (`consumption_order_id`) REFERENCES `t_consumption_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店结算明细';

CREATE TABLE `t_audit_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `actor_type` VARCHAR(32) NOT NULL COMMENT 'CUSTOMER/SYS_USER/SYSTEM',
    `actor_id` BIGINT UNSIGNED DEFAULT NULL,
    `operator_role` VARCHAR(32) DEFAULT NULL,
    `store_id` BIGINT UNSIGNED DEFAULT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resource_type` VARCHAR(64) NOT NULL,
    `resource_no` VARCHAR(64) DEFAULT NULL,
    `request_id` VARCHAR(64) DEFAULT NULL,
    `before_snapshot` JSON COMMENT '变更前快照',
    `after_snapshot` JSON COMMENT '变更后快照',
    `remark` VARCHAR(500) DEFAULT NULL,
    `client_ip` VARCHAR(45) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audit_actor_time` (`actor_type`, `actor_id`, `create_time`),
    KEY `idx_audit_resource` (`resource_type`, `resource_no`),
    KEY `idx_audit_request` (`request_id`),
    KEY `idx_audit_store_time` (`store_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关键业务审计日志';

INSERT INTO `t_sys_role` (`role_code`, `role_name`, `description`)
VALUES
    ('CLERK', '店员', '门店充值与消费操作'),
    ('STORE_MANAGER', '店长', '门店经营与结算查询'),
    ('SUPER_ADMIN', '超级管理员', '平台全局管理');
