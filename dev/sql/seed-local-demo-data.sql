-- ============================================================================
-- 仅限本地开发环境：积分平台演示数据
-- 禁止在测试、预发布或生产数据库执行。本文件包含公开的演示账号和密码。
-- 前置条件：Flyway 已成功执行 V1～V3，flyway_schema_history 中对应记录均为成功。
-- 本脚本可重复执行；不会重置客户现有积分，也不会修改超级管理员。
-- ============================================================================

START TRANSACTION;

-- 1. 演示合作商户
INSERT INTO `t_merchant` (
    `merchant_code`, `legal_name`, `business_name`, `unified_social_credit_code`,
    `contact_name`, `contact_phone`, `agreement_no`, `status`
)
VALUES (
    'DEMO-MERCHANT-001', '本地演示商户有限公司', '本地演示商户', NULL,
    '演示联系人', '13800000001', 'DEMO-AGREEMENT-001', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `legal_name` = '本地演示商户有限公司',
    `business_name` = '本地演示商户',
    `contact_name` = '演示联系人',
    `contact_phone` = '13800000001',
    `agreement_no` = 'DEMO-AGREEMENT-001',
    `status` = 'ACTIVE';

SET @demo_merchant_id = (
    SELECT `id` FROM `t_merchant` WHERE `merchant_code` = 'DEMO-MERCHANT-001' LIMIT 1
);

-- 2. 两家演示门店
INSERT INTO `t_store` (
    `merchant_id`, `store_code`, `store_name`, `address`, `contact_phone`, `status`
)
VALUES (
    @demo_merchant_id, 'DEMO-STORE-A', '演示门店 A', '本地开发测试地址 A', '13800000011', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `merchant_id` = @demo_merchant_id,
    `store_name` = '演示门店 A',
    `address` = '本地开发测试地址 A',
    `contact_phone` = '13800000011',
    `status` = 'ACTIVE';

SET @demo_store_a_id = (
    SELECT `id` FROM `t_store` WHERE `store_code` = 'DEMO-STORE-A' LIMIT 1
);

INSERT INTO `t_store` (
    `merchant_id`, `store_code`, `store_name`, `address`, `contact_phone`, `status`
)
VALUES (
    @demo_merchant_id, 'DEMO-STORE-B', '演示门店 B', '本地开发测试地址 B', '13800000012', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `merchant_id` = @demo_merchant_id,
    `store_name` = '演示门店 B',
    `address` = '本地开发测试地址 B',
    `contact_phone` = '13800000012',
    `status` = 'ACTIVE';

SET @demo_store_b_id = (
    SELECT `id` FROM `t_store` WHERE `store_code` = 'DEMO-STORE-B' LIMIT 1
);

-- 3. 演示后台账号
-- 店员账号：demo.clerk / ClerkDemo@2026
-- 店长账号：demo.manager / ManagerDemo@2026
-- 密码使用 BCrypt cost=12，仅适用于本地公开测试账号。
INSERT INTO `t_sys_user` (
    `username`, `phone`, `password_hash`, `real_name`, `status`,
    `failed_login_count`, `locked_until`, `token_version`, `password_updated_time`
)
VALUES (
    'demo.clerk', '13900000001',
    '$2a$12$2wGlifYsGxE39WkF/NZIcu60iiRTnzt1JLdDFuxrVjA.N7id.5UOO',
    '演示店员', 'ACTIVE', 0, NULL, 0, CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `password_hash` = '$2a$12$2wGlifYsGxE39WkF/NZIcu60iiRTnzt1JLdDFuxrVjA.N7id.5UOO',
    `real_name` = '演示店员',
    `status` = 'ACTIVE',
    `failed_login_count` = 0,
    `locked_until` = NULL,
    `token_version` = `token_version` + 1,
    `password_updated_time` = CURRENT_TIMESTAMP;

SET @demo_clerk_id = (
    SELECT `id` FROM `t_sys_user` WHERE `username` = 'demo.clerk' LIMIT 1
);

INSERT INTO `t_sys_user` (
    `username`, `phone`, `password_hash`, `real_name`, `status`,
    `failed_login_count`, `locked_until`, `token_version`, `password_updated_time`
)
VALUES (
    'demo.manager', '13900000002',
    '$2a$12$NeY2Db/9TuVZs7L0s6BckO5OtvsArLh4Zp.yP.9Xvmji.PBuSVqbS',
    '演示店长', 'ACTIVE', 0, NULL, 0, CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `password_hash` = '$2a$12$NeY2Db/9TuVZs7L0s6BckO5OtvsArLh4Zp.yP.9Xvmji.PBuSVqbS',
    `real_name` = '演示店长',
    `status` = 'ACTIVE',
    `failed_login_count` = 0,
    `locked_until` = NULL,
    `token_version` = `token_version` + 1,
    `password_updated_time` = CURRENT_TIMESTAMP;

SET @demo_manager_id = (
    SELECT `id` FROM `t_sys_user` WHERE `username` = 'demo.manager' LIMIT 1
);

SET @clerk_role_id = (
    SELECT `id` FROM `t_sys_role` WHERE `role_code` = 'CLERK' AND `status` = 'ACTIVE' LIMIT 1
);
SET @manager_role_id = (
    SELECT `id` FROM `t_sys_role` WHERE `role_code` = 'STORE_MANAGER' AND `status` = 'ACTIVE' LIMIT 1
);

INSERT IGNORE INTO `t_sys_user_role` (`user_id`, `role_id`)
VALUES
    (@demo_clerk_id, @clerk_role_id),
    (@demo_manager_id, @manager_role_id);

-- 当前每个店员/店长只绑定一家门店，表结构仍支持未来多门店店长。
INSERT IGNORE INTO `t_sys_user_store` (`user_id`, `store_id`)
VALUES
    (@demo_clerk_id, @demo_store_a_id),
    (@demo_manager_id, @demo_store_b_id);

-- 4. 演示客户；积分账户只在首次创建，重复执行不会覆盖充值后的余额。
INSERT INTO `t_customer_user` (`phone`, `nickname`, `status`)
VALUES ('13800138000', '演示客户', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `id` = LAST_INSERT_ID(`id`),
    `nickname` = '演示客户',
    `status` = 'ACTIVE';

SET @demo_customer_id = (
    SELECT `id` FROM `t_customer_user` WHERE `phone` = '13800138000' LIMIT 1
);

INSERT IGNORE INTO `t_point_account` (`customer_id`, `available_points`, `version`)
VALUES (@demo_customer_id, 0, 0);

COMMIT;

-- 执行结果核对
SELECT `id`, `merchant_code`, `business_name`, `status`
FROM `t_merchant`
WHERE `merchant_code` = 'DEMO-MERCHANT-001';

SELECT `id`, `store_code`, `store_name`, `status`
FROM `t_store`
WHERE `store_code` IN ('DEMO-STORE-A', 'DEMO-STORE-B')
ORDER BY `store_code`;

SELECT u.`id`, u.`username`, u.`real_name`, r.`role_code`, s.`store_code`
FROM `t_sys_user` u
INNER JOIN `t_sys_user_role` ur ON ur.`user_id` = u.`id`
INNER JOIN `t_sys_role` r ON r.`id` = ur.`role_id`
LEFT JOIN `t_sys_user_store` us ON us.`user_id` = u.`id`
LEFT JOIN `t_store` s ON s.`id` = us.`store_id`
WHERE u.`username` IN ('demo.clerk', 'demo.manager')
ORDER BY u.`username`;

SELECT c.`id`, c.`phone`, c.`nickname`, c.`status`, p.`available_points`
FROM `t_customer_user` c
LEFT JOIN `t_point_account` p ON p.`customer_id` = c.`id`
WHERE c.`phone` = '13800138000';
