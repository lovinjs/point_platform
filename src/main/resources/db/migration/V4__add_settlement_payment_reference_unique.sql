ALTER TABLE `t_store_settlement`
    ADD UNIQUE KEY `uk_store_settlement_payment_reference` (`payment_reference`);
