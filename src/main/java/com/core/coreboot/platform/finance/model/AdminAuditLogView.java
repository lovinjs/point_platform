package com.core.coreboot.platform.finance.model;

import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;

import java.time.LocalDateTime;

public record AdminAuditLogView(
        Long logId,
        AuditActorType actorType,
        Long actorId,
        String actorName,
        RoleCode operatorRole,
        Long storeId,
        String storeCode,
        String storeName,
        String action,
        String resourceType,
        String resourceNo,
        String requestId,
        String beforeSnapshot,
        String afterSnapshot,
        String remark,
        String clientIp,
        LocalDateTime createTime
) {
}
