ALTER TABLE `t_recharge_order`
    ADD KEY `idx_recharge_completed_store` (`completed_time`, `recharge_store_id`, `order_status`);

ALTER TABLE `t_recharge_refund`
    ADD KEY `idx_recharge_refund_status_completed` (`refund_status`, `completed_time`);

ALTER TABLE `t_consumption_order`
    ADD KEY `idx_consumption_status_completed_store` (`order_status`, `completed_time`, `store_id`);

ALTER TABLE `t_store_settlement`
    ADD KEY `idx_store_settlement_status_paid_store` (`settlement_status`, `paid_time`, `store_id`);

ALTER TABLE `t_audit_log`
    ADD KEY `idx_audit_time_id` (`create_time`, `id`);
