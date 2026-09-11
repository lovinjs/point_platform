CREATE TABLE `t_platform_business_setting` (
    `id` BIGINT UNSIGNED NOT NULL,
    `platform_fee_rate_bps` SMALLINT UNSIGNED NOT NULL DEFAULT 500 COMMENT '平台手续费率基点，500表示5%',
    `consumption_pending_ttl_minutes` TINYINT UNSIGNED NOT NULL DEFAULT 5 COMMENT '待消费订单确认有效期，单位分钟',
    `version` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '后台修改时的乐观锁版本',
    `last_updated_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人，初始值为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台全局业务配置';

INSERT INTO `t_platform_business_setting` (
    `id`, `platform_fee_rate_bps`, `consumption_pending_ttl_minutes`, `version`
) VALUES (1, 500, 5, 0);
